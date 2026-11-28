package GUI;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;

/**
 * Builds and runs the backend with the JavaFX frontend as one application.
 * The backend process remains owned by this launcher for the GUI's lifetime.
 */
public final class Launcher {
    private static final String BACKEND_EXECUTABLE = "health-support-server";
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);
    private static final Path REQUEST_PIPE = Path.of("/tmp/com.hogwarts.health.req");
    private static final Path RESPONSE_PIPE = Path.of("/tmp/com.hogwarts.health.res");

    private Launcher() {}

    /** Entry point used by the JavaFX Maven plugin and command-line launches. */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Builds and starts the backend, launches the GUI, then stops the backend.
     *
     * @param args command-line arguments forwarded to JavaFX
     */
    public static void launch(String[] args) {
        Process backend = null;
        try {
            Path projectRoot = findProjectRoot();
            ensurePipesAvailable();
            Path executable = buildBackend(projectRoot);
            backend = new ProcessBuilder(executable.toString())
                    .directory(projectRoot.toFile())
                    .inheritIO()
                    .start();
            waitForBackend(backend);
            Application.launch(Main.class, args);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Application startup was interrupted.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the health application.", e);
        } finally {
            stopBackend(backend);
        }
    }

    /** Finds the repository by walking upward from the process working directory. */
    private static Path findProjectRoot() throws IOException {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            if (Files.isRegularFile(directory.resolve("src/Server/main.cpp"))
                    && Files.isDirectory(directory.resolve("lib/json-cpp/include"))) {
                return directory;
            }
            directory = directory.getParent();
        }
        throw new IOException("Could not locate the project root from the current directory.");
    }

    /** Compiles the server executable from the repository's backend sources. */
    private static Path buildBackend(Path projectRoot)
            throws IOException, InterruptedException {
        Path buildDirectory = projectRoot.resolve("build");
        Files.createDirectories(buildDirectory);
        Path executable = buildDirectory.resolve(BACKEND_EXECUTABLE);

        Process compiler = new ProcessBuilder(
                "g++",
                "-std=c++20",
                "-I",
                projectRoot.resolve("lib/json-cpp/include").toString(),
                projectRoot.resolve("src/Server/main.cpp").toString(),
                "-lsqlite3",
                "-o",
                executable.toString())
                .directory(projectRoot.toFile())
                .inheritIO()
                .start();
        int exitCode = compiler.waitFor();
        if (exitCode != 0) {
            throw new IOException("Backend compilation failed with exit code " + exitCode + ".");
        }
        return executable;
    }

    /** Waits until the backend has created both IPCP FIFO endpoints. */
    private static void waitForBackend(Process backend)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            if (!backend.isAlive()) {
                throw new IOException("Backend exited during startup with code "
                        + backend.exitValue() + ".");
            }
            if (isOtherFile(REQUEST_PIPE) && isOtherFile(RESPONSE_PIPE)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IOException("Backend did not create its IPCP pipes before the startup timeout.");
    }

    /** FIFO endpoints are reported as special files rather than regular files. */
    private static boolean isOtherFile(Path path) throws IOException {
        return Files.exists(path)
                && Files.readAttributes(path, BasicFileAttributes.class).isOther();
    }

    /** Avoids attaching this launch to another or stale server's IPCP pipes. */
    private static void ensurePipesAvailable() throws IOException {
        if (Files.exists(REQUEST_PIPE) || Files.exists(RESPONSE_PIPE)) {
            throw new IOException(
                    "IPCP pipe paths already exist; another or stale backend may be present.");
        }
    }

    /** Sends SIGTERM so the server can close its IPCP pipes before exiting. */
    private static void stopBackend(Process backend) {
        if (backend == null) {
            return;
        }

        try {
            if (backend.isAlive()) {
                Process signal = new ProcessBuilder(
                        "kill", "-TERM", Long.toString(backend.pid()))
                        .start();
                if (!signal.waitFor(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    signal.destroyForcibly();
                    throw new IOException("Timed out while sending SIGTERM to the backend.");
                }
                if (signal.exitValue() != 0 && backend.isAlive()) {
                    throw new IOException("The kill command could not send SIGTERM to the backend.");
                }
            }

            if (!backend.waitFor(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                System.err.println(
                        "Backend did not exit after SIGTERM; its IPCP pipes may remain active.");
            } else {
                deleteIpcpPipes();
            }
        } catch (IOException e) {
            System.err.println("Could not send SIGTERM to the backend: " + e.getMessage());
            // Process.destroy() is the Java/Linux graceful-termination fallback (SIGTERM).
            backend.destroy();
            waitForBackendShutdown(backend);
        } catch (InterruptedException e) {
            backend.destroy();
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while sending SIGTERM to the backend.");
        }
    }

    /** Waits for the server's SIGTERM handler to close and remove its IPCP pipes. */
    private static void waitForBackendShutdown(Process backend) {
        try {
            if (!backend.waitFor(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                System.err.println(
                        "Backend did not exit after graceful termination; its IPCP pipes may remain active.");
            } else {
                deleteIpcpPipes();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while waiting for the backend to exit.");
        }
    }

    /** Removes the known IPCP FIFO files after the owned backend has exited. */
    private static void deleteIpcpPipes() {
        deleteIpcpPipe(REQUEST_PIPE);
        deleteIpcpPipe(RESPONSE_PIPE);
    }

    /** Deletes a pipe only when the path still identifies a special file. */
    private static void deleteIpcpPipe(Path pipe) {
        try {
            if (!Files.exists(pipe, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            BasicFileAttributes attributes = Files.readAttributes(
                    pipe, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isOther()) {
                throw new IOException("Refusing to remove a non-FIFO path.");
            }
            Files.deleteIfExists(pipe);
        } catch (IOException e) {
            System.err.println("Could not remove IPCP pipe " + pipe + ": " + e.getMessage());
        }
    }
}
