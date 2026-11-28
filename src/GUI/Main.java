package GUI;

/**
 * Starts the Hogwarts IT Support JavaFX application and manages its main screens.
 *
 * <p>This class loads FXML views, routes navigation actions, scales each view to fit
 * the window, and keeps the selected measurement system reflected in the settings
 * page's units menu.
 * 
 * @author Gabriel Villalpando
 */


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;


public class Main extends Application {
    // The FXML files share this fixed canvas size for consistent layout and scaling.
    private static final String LOGIN_VIEW = "login_page.fxml";
    private static final String HOME_VIEW = "BaseUISoruce.fxml";
    private static final double DESIGN_WIDTH = 1920.0;
    private static final double DESIGN_HEIGHT = 1080.0;
    private static final byte IPCP_VERSION = 1;
    private static final byte IPCP_PUT = 1;
    private static final byte IPCP_LOGIN = 4;
    private static final int IPCP_OK = 0;
    private static final int IPCP_OK_NO_CONTENT = 1;
    private static final int IPCP_REQUEST_SIZE = 10;
    private static final Path REQUEST_PIPE = Path.of("/tmp/com.hogwarts.health.req");
    private static final Path RESPONSE_PIPE = Path.of("/tmp/com.hogwarts.health.res");
    private static final int PASSWORD_HASH_ITERATIONS = 120_000;
    private static final int PASSWORD_HASH_BITS = 256;
    private static final int MAX_RESPONSE_BODY_BYTES = 1_048_576;
    // Per-user editable amounts and all daily, weekly, and monthly targets.
    private static final String[] ZERO_DEFAULT_KEYS = {
        "current.daily.steps", "current.daily.water",
        "current.daily.calories", "current.daily.sleep",
        "daily.steps", "weekly.steps", "monthly.steps",
        "daily.water", "weekly.water", "monthly.water",
        "daily.calories", "weekly.calories", "monthly.calories",
        "daily.sleep", "weekly.sleep", "monthly.sleep"
    };
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static DataOutputStream backendOutput;
    private static DataInputStream backendInput;

    // Shared application state and references used while switching JavaFX views.
    private Stage window;
    private StackPane responsiveRoot;
    private Scale contentScale;
    private String currentView = LOGIN_VIEW;
    private String previousView = LOGIN_VIEW;
    private int userId;
    private UnitSystem unitSystem = UnitSystem.METRIC;
    private final Preferences legacyGoalPreferences = Preferences.userNodeForPackage(Main.class);
    private Preferences goalPreferences = legacyGoalPreferences;
    private final Preferences accountPreferences =
            Preferences.userNodeForPackage(Main.class).node("accounts");

    // Injected by FXML: this menu displays the selected measurement system.
    @FXML
    private Menu unitsMenu;

    @FXML
    private TextField UsernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorMessage;
    @FXML
    private Button loginButton;

    // Injected by FXML: displays the current date on the home view.
    @FXML
    private TitledPane dateTitlePane;

    @FXML
    private TextField dailyStepsGoalInput, weeklyStepsGoalInput, monthlyStepsGoalInput;
    @FXML
    private TextField dailyWaterGoalInput, weeklyWaterGoalInput, monthlyWaterGoalInput;
    @FXML
    private TextField dailyCaloriesGoalInput, weeklyCaloriesGoalInput, monthlyCaloriesGoalInput;
    @FXML
    private TextField dailySleepGoalInput, weeklySleepGoalInput, monthlySleepGoalInput;
    @FXML
    private TextField dailyStepsCurrentInput, dailyWaterCurrentInput;
    @FXML
    private TextField dailyCaloriesCurrentInput, dailySleepCurrentInput;
    @FXML
    private Label goalStatusLabel, currentAmountStatusLabel;

    // Injected label values and goals for the daily, weekly, and monthly summaries.
    @FXML
    private Label dailyStepsValue, dailyWaterValue, dailyCaloriesValue, dailySleepValue;
    @FXML
    private Label dailyStepsGoalValue, dailyWaterGoalValue, dailyCaloriesGoalValue, dailySleepGoalValue;
    @FXML
    private Label weeklyStepsValue, weeklyWaterValue, weeklyCaloriesValue, weeklySleepValue;
    @FXML
    private Label weeklyStepsGoalValue, weeklyWaterGoalValue, weeklyCaloriesGoalValue, weeklySleepGoalValue;
    @FXML
    private Label monthlyStepsValue, monthlyWaterValue, monthlyCaloriesValue, monthlySleepValue;
    @FXML
    private Label monthlyStepsGoalValue, monthlyWaterGoalValue, monthlyCaloriesGoalValue, monthlySleepGoalValue;

    @FXML
    private StackPane dailyStepsGraph, dailyWaterGraph, dailyCaloriesGraph, dailySleepGraph;
    @FXML
    private StackPane weeklyStepsGraph, weeklyWaterGraph, weeklyCaloriesGraph, weeklySleepGraph;
    @FXML
    private StackPane monthlyStepsGraph, monthlyWaterGraph, monthlyCaloriesGraph, monthlySleepGraph;

    /** Holds one set of steps, water, calorie, and sleep measurements. */
    private record MetricValues(
            int steps, double waterLiters, int calories, double sleepHours) {}

    /** Pairs current measurements with their goals for a time period. */
    private record PeriodValues(MetricValues values, MetricValues goals) {}

    /** Measurement system selected in Settings. */
    private enum UnitSystem {
        METRIC,
        IMPERIAL
    }

    /** Enters through the lifecycle launcher so the backend is started first. */
    public static void main(String[] args) {
        Launcher.launch(args);
    }

    /**
     * Creates the main window, installs the responsive scene container, and loads Login.
     *
     * @param primaryStage JavaFX's primary application window
     * @throws IOException if the initial FXML view cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        window = primaryStage;
        responsiveRoot = new StackPane();
        Scene scene = new Scene(responsiveRoot, 1280.0, 720.0);
        // Recalculate the uniform scale whenever the window area changes.
        responsiveRoot.widthProperty().addListener((observable, oldWidth, newWidth) -> scaleContent());
        responsiveRoot.heightProperty().addListener((observable, oldHeight, newHeight) -> scaleContent());
        window.setScene(scene);
        window.setTitle("Hogwarts IT Support");
        setResponsiveView(loadView(LOGIN_VIEW));
        window.show();
    }

    /** Routes a navigation menu item or button to its corresponding FXML view. */
    @FXML
    private void navigate(ActionEvent event) {
        if (userId == 0) {
            setLoginStatus("Log in before opening the application.");
            return;
        }

        // Menu items and buttons can both trigger navigation.
        Object source = event.getSource();
        String label = switch (source) {
            case MenuItem menuItem -> menuItem.getText();
            case Button button -> button.getText();
            default -> throw new IllegalArgumentException(
                    "Navigation requires a MenuItem or Button.");
        };

        // Translate the visible navigation label into the matching FXML resource.
        String view = switch (label) {
            case "Home" -> HOME_VIEW;
            case "Daily" -> "DailyOverview.fxml";
            case "Weekly" -> "WeeklyOverview.fxml";
            case "Monthly" -> "MonthlyOverview.fxml";
            case "Settings" -> "health_app_settings_page.fxml";
            default -> throw new IllegalArgumentException(
                    "No scene is configured for navigation label: " + label);
        };

        showView(view);
    }

    /**
     * Loads and displays a view while recording the prior view for Settings cancellation.
     *
     * @param view FXML resource name to display
     */
    private void showView(String view) {
        try {
            setResponsiveView(loadView(view));
            if (!view.equals(currentView)) {
                previousView = currentView;
                currentView = view;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load scene: " + view, e);
        }
    }

    /** Returns from Settings to the view that was open before Settings. */
    @FXML
    private void cancelSettings() {
        showView(previousView);
    }

    /** Clears the active account and returns to the login form. */
    private void logout() {
        if (userId == 0) {
            return;
        }

        userId = 0;
        goalPreferences = legacyGoalPreferences;
        showView(LOGIN_VIEW);
        UsernameField.clear();
        passwordField.clear();
        setLoginStatus("");
    }

    /** Sizes a view on the shared design canvas and installs its scale transform. */
    private void setResponsiveView(Parent view) {
        if (!(view instanceof Region region)) {
            throw new IllegalArgumentException("Scene root must be a resizable layout region.");
        }

        // Enforce the shared design size before applying uniform scaling.
        region.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        region.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        region.setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);

        // Scale around the canvas center to preserve proportions.
        contentScale = new Scale(1.0, 1.0, DESIGN_WIDTH / 2.0, DESIGN_HEIGHT / 2.0);
        view.getTransforms().setAll(contentScale);
        responsiveRoot.getChildren().setAll(view);
        scaleContent();
    }

    /** Recalculates the uniform scale to fit the design canvas inside the window. */
    private void scaleContent() {
        // Wait until the window and shared scale transform have valid sizes.
        if (contentScale == null || responsiveRoot.getWidth() <= 0.0
                || responsiveRoot.getHeight() <= 0.0) {
            return;
        }

        // Fit the design canvas without stretching it.
        double scale = Math.min(
                responsiveRoot.getWidth() / DESIGN_WIDTH,
                responsiveRoot.getHeight() / DESIGN_HEIGHT);
        contentScale.setX(scale);
        contentScale.setY(scale);
    }

    /** Handles selection of Imperial units in the Settings menu. */
    @FXML
    private void changeImp(ActionEvent event) {
        setUnitSystem(UnitSystem.IMPERIAL);
    }

    /** Handles selection of Metric units in the Settings menu. */
    @FXML
    private void changeMet(ActionEvent event) {
        setUnitSystem(UnitSystem.METRIC);
    }

    /** Stores the selected unit system and synchronizes its menu label. */
    private void setUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem;
        // Keep the settings menu label synchronized with the selected unit system.
        if (unitsMenu != null) {
            unitsMenu.setText(unitSystem == UnitSystem.METRIC ? "Metric" : "Imperial");
        }
    }

    /** Initializes controller state after the current FXML view injects its fields. */
    @FXML
    private void initialize() {
        // FXML calls this after injecting its fields.
        setUnitSystem(unitSystem);
        updateDateTitle();
        updateCurrentValues();
    }

    /** Formats a set of metric values with their display units. */
    private void setMetricValues(MetricValues values, Label steps, Label water,
            Label calories, Label sleep) {
        // Format numeric values consistently with the units displayed by the UI.
        steps.setText("%,d steps".formatted(values.steps()));
        water.setText("%.1f L".formatted(values.waterLiters()));
        calories.setText("%,d kcal".formatted(values.calories()));
        sleep.setText("%.1f h".formatted(values.sleepHours()));
    }

    /** Formats each metric as its current amount over its goal. */
    private void setMetricFractionValues(MetricValues current, MetricValues goal,
            Label steps, Label water, Label calories, Label sleep) {
        // Show progress toward each goal as current amount over the target amount.
        steps.setText("%,d / %,d steps".formatted(current.steps(), goal.steps()));
        water.setText("%.1f / %.1f L".formatted(current.waterLiters(), goal.waterLiters()));
        calories.setText("%,d / %,d kcal".formatted(current.calories(), goal.calories()));
        sleep.setText("%.1f / %.1f h".formatted(current.sleepHours(), goal.sleepHours()));
    }

    /** Updates both actual-value labels and goal-progress labels for one period. */
    private void setPeriodValues(PeriodValues period, Label steps, Label water,
            Label calories, Label sleep, Label stepsGoal, Label waterGoal,
            Label caloriesGoal, Label sleepGoal) {
        // Populate actual values normally and goals as current-over-goal fractions.
        setMetricValues(period.values(), steps, water, calories, sleep);
        setMetricFractionValues(period.values(), period.goals(),
                stepsGoal, waterGoal, caloriesGoal, sleepGoal);
    }

    /** Displays today's date when the current FXML view includes the date pane. */
    private void updateDateTitle() {
        // Update the date only when the home view has provided the target pane.
        if (dateTitlePane != null) {
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
            dateTitlePane.setText("Date: " + currentDate);
        }
    }

    /**
     * Loads an FXML resource using this controller and initializes its editable inputs.
     *
     * @param resource FXML resource name relative to this class
     * @return the loaded view root
     * @throws IOException if the FXML resource cannot be read or loaded
     */
    private Parent loadView(String resource) throws IOException {
        // Resolve FXML relative to this Java controller and reject missing resources.
        var location = Main.class.getResource(resource);
        if (location == null) {
            throw new IllegalArgumentException("Scene resource not found: " + resource);
        }

        // Reuse this controller so navigation and injected fields remain connected.
        FXMLLoader loader = new FXMLLoader(location);
        loader.setController(this);
        Parent view = loader.load();
        if (!LOGIN_VIEW.equals(resource)) {
            installLogoutButton(view);
        }
        if (LOGIN_VIEW.equals(resource)) {
            initializeLoginInputs();
        } else if ("health_app_settings_page.fxml".equals(resource)) {
            initializeGoalInputs();
        } else if ("DailyOverview.fxml".equals(resource)) {
            initializeDailyCurrentInputs();
        }
        return view;
    }

    /** Adds a logout action to the top-right of each authenticated screen's menu bar. */
    private void installLogoutButton(Parent view) {
        if (!(view instanceof VBox screen) || screen.getChildren().isEmpty()
                || !(screen.getChildren().get(0) instanceof MenuBar menuBar)) {
            throw new IllegalStateException("Authenticated screen is missing its menu bar.");
        }

        Button logoutButton = new Button("Log out");
        logoutButton.setStyle("-fx-font-size: 18px;");
        logoutButton.setOnAction(event -> logout());

        HBox toolbar = new HBox(menuBar, logoutButton);
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setPrefWidth(DESIGN_WIDTH);
        menuBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        screen.getChildren().set(0, toolbar);
    }

    /** Connects the login controls after their FXML fields have been injected. */
    private void initializeLoginInputs() {
        if (UsernameField == null || passwordField == null
                || errorMessage == null || loginButton == null) {
            throw new IllegalStateException("A login form control is missing.");
        }
        errorMessage.setDisable(false);
        errorMessage.setText("");
        loginButton.setOnAction(event -> login());
        passwordField.setOnAction(event -> login());
    }

    /** Checks the form off the JavaFX thread and opens Home after backend login. */
    private void login() {
        String username = UsernameField.getText().trim();
        char[] password = passwordField.getText().toCharArray();
        if (username.isEmpty() || password.length == 0) {
            Arrays.fill(password, '\0');
            setLoginStatus("Enter both a username and password.");
            return;
        }

        setLoginFormDisabled(true);
        errorMessage.setText("Signing in...");
        Task<Integer> loginTask = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                try {
                    return createOrLoginAccount(username, password);
                } finally {
                    Arrays.fill(password, '\0');
                }
            }
        };
        loginTask.setOnSucceeded(event -> {
            userId = loginTask.getValue();
            goalPreferences = accountDataPreferences(userId);
            passwordField.clear();
            setLoginFormDisabled(false);
            showView(HOME_VIEW);
        });
        loginTask.setOnFailed(event -> {
            passwordField.clear();
            setLoginFormDisabled(false);
            Throwable failure = loginTask.getException();
            setLoginStatus(failure == null
                    ? "Login failed."
                    : failure.getMessage() == null ? "Login failed." : failure.getMessage());
        });

        Thread worker = new Thread(loginTask, "account-login");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Reuses a locally mapped ID or creates one, then creates/logs into it remotely.
     * The password verifier stays local; only the numeric ID is sent over IPCP.
     */
    private int createOrLoginAccount(String username, char[] password) throws Exception {
        String accountKey = "account-" + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(
                        username.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));
        Preferences account = accountPreferences.node(accountKey);

        int id = account.getInt("userId", 0);
        boolean newAccount = id == 0;
        String storedSalt = account.get("passwordSalt", null);
        String storedHash = account.get("passwordHash", null);
        if (newAccount) {
            if (storedSalt != null || storedHash != null) {
                throw new IOException("Saved account information is incomplete.");
            }

            id = SECURE_RANDOM.nextInt(Integer.MAX_VALUE) + 1;
            byte[] salt = new byte[16];
            SECURE_RANDOM.nextBytes(salt);
            // Persist a salted verifier instead of retaining the supplied password.
            byte[] hash = hashPassword(password, salt);
            account.putInt("userId", id);
            account.put("passwordSalt", Base64.getEncoder().encodeToString(salt));
            account.put("passwordHash", Base64.getEncoder().encodeToString(hash));
            flushPreferences();
        } else {
            if (storedSalt == null || storedHash == null) {
                throw new IOException("Saved account information is incomplete.");
            }

            byte[] salt;
            byte[] expectedHash;
            try {
                salt = Base64.getDecoder().decode(storedSalt);
                expectedHash = Base64.getDecoder().decode(storedHash);
            } catch (IllegalArgumentException e) {
                throw new IOException("Saved account information is invalid.", e);
            }
            if (!MessageDigest.isEqual(expectedHash, hashPassword(password, salt))) {
                throw new IOException("Incorrect username or password.");
            }
        }

        initializeAccountData(id, newAccount);
        sendAccountRequest(IPCP_PUT, id);
        sendAccountRequest(IPCP_LOGIN, id);
        return id;
    }

    /**
     * Creates per-account numeric defaults once, migrating prior local values for returning users.
     */
    private void initializeAccountData(int id, boolean newAccount) throws IOException {
        Preferences data = accountDataPreferences(id);
        if (data.getBoolean("zeroDefaultsInitialized", false)) {
            return;
        }

        for (String key : ZERO_DEFAULT_KEYS) {
            String previousValue = newAccount ? null : legacyGoalPreferences.get(key, null);
            data.put(key, previousValue == null || previousValue.isEmpty()
                    ? "0" : previousValue);
        }
        data.putBoolean("zeroDefaultsInitialized", true);
        flushPreferences();
    }

    /** Returns the local values and goals node for one backend user ID. */
    private Preferences accountDataPreferences(int id) {
        return accountPreferences.node("data").node(Integer.toString(id));
    }

    /** Derives the local password verifier with PBKDF2 and the account's random salt. */
    private static byte[] hashPassword(char[] password, byte[] salt)
            throws GeneralSecurityException {
        PBEKeySpec keySpec = new PBEKeySpec(
                password, salt, PASSWORD_HASH_ITERATIONS, PASSWORD_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec).getEncoded();
        } finally {
            keySpec.clearPassword();
        }
    }

    /** Sends an empty-body create/login IPCP packet and checks its response status. */
    private static void sendAccountRequest(byte type, int id) throws IOException {
        synchronized (Main.class) {
            int version;
            int status;
            byte[] body;
            try {
                openBackendPipes();
                // Encode the six-byte header and four-byte length for an empty body.
                ByteBuffer request = ByteBuffer.allocate(IPCP_REQUEST_SIZE)
                        .order(ByteOrder.BIG_ENDIAN);
                request.put(IPCP_VERSION).put(type).putInt(id).putInt(0);
                backendOutput.write(request.array());
                backendOutput.flush();

                version = backendInput.readUnsignedByte();
                status = backendInput.readUnsignedByte();
                int bodyLength = backendInput.readInt();
                if (bodyLength < 0 || bodyLength > MAX_RESPONSE_BODY_BYTES) {
                    throw new IOException("Backend returned an invalid response.");
                }
                body = new byte[bodyLength];
                backendInput.readFully(body);
            } catch (IOException e) {
                closeBackendPipes();
                throw e;
            }

            if (version != IPCP_VERSION
                    || (status != IPCP_OK && status != IPCP_OK_NO_CONTENT)) {
                String detail = new String(body, StandardCharsets.UTF_8).trim();
                throw new IOException(detail.isEmpty()
                        ? "Backend rejected the account request (status " + status + ")."
                        : "Backend rejected the account request: " + detail);
            }
        }
    }

    /** Opens the backend's persistent request and response FIFOs once per app run. */
    private static void openBackendPipes() throws IOException {
        if (backendOutput != null && backendInput != null) {
            return;
        }
        if (!Files.exists(REQUEST_PIPE) || !Files.exists(RESPONSE_PIPE)) {
            throw new IOException("Could not connect to IPCP; make sure the backend is running.");
        }

        DataOutputStream output = new DataOutputStream(
                new FileOutputStream(REQUEST_PIPE.toFile()));
        try {
            DataInputStream input = new DataInputStream(
                    new FileInputStream(RESPONSE_PIPE.toFile()));
            backendOutput = output;
            backendInput = input;
        } catch (IOException e) {
            output.close();
            throw e;
        }
    }

    /** Releases both FIFO streams when the GUI lifecycle ends or IPCP fails. */
    private static void closeBackendPipes() {
        DataInputStream input = backendInput;
        DataOutputStream output = backendOutput;
        backendInput = null;
        backendOutput = null;
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            System.err.println("Could not close IPCP response pipe: " + e.getMessage());
        }
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException e) {
            System.err.println("Could not close IPCP request pipe: " + e.getMessage());
        }
    }

    /** Flushes the username-to-ID and password-verifier mapping to local storage. */
    private void flushPreferences() throws IOException {
        try {
            accountPreferences.flush();
        } catch (BackingStoreException e) {
            throw new IOException("Could not save this account on the device.", e);
        }
    }

    /** Prevents duplicate login requests while the background request is active. */
    private void setLoginFormDisabled(boolean disabled) {
        UsernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        loginButton.setDisable(disabled);
    }

    /** Displays validation or backend errors on the login screen. */
    private void setLoginStatus(String message) {
        if (errorMessage != null) {
            errorMessage.setText(message);
            errorMessage.setDisable(false);
        }
    }

    /** Closes the frontend's IPCP streams before the launcher stops the backend. */
    @Override
    public void stop() {
        closeBackendPipes();
    }

    /** Connects the daily overview's current-amount inputs to saved preferences. */
    private void initializeDailyCurrentInputs() {
        configureCurrentInput(dailyStepsCurrentInput, "current.daily.steps", true);
        configureCurrentInput(dailyWaterCurrentInput, "current.daily.water", false);
        configureCurrentInput(dailyCaloriesCurrentInput, "current.daily.calories", true);
        configureCurrentInput(dailySleepCurrentInput, "current.daily.sleep", false);
    }

    /** Connects all daily, weekly, and monthly goal fields to saved preferences. */
    private void initializeGoalInputs() {
        configureGoalInput(dailyStepsGoalInput, "daily.steps", true);
        configureGoalInput(weeklyStepsGoalInput, "weekly.steps", true);
        configureGoalInput(monthlyStepsGoalInput, "monthly.steps", true);
        configureGoalInput(dailyWaterGoalInput, "daily.water", false);
        configureGoalInput(weeklyWaterGoalInput, "weekly.water", false);
        configureGoalInput(monthlyWaterGoalInput, "monthly.water", false);
        configureGoalInput(dailyCaloriesGoalInput, "daily.calories", true);
        configureGoalInput(weeklyCaloriesGoalInput, "weekly.calories", true);
        configureGoalInput(monthlyCaloriesGoalInput, "monthly.calories", true);
        configureGoalInput(dailySleepGoalInput, "daily.sleep", false);
        configureGoalInput(weeklySleepGoalInput, "weekly.sleep", false);
        configureGoalInput(monthlySleepGoalInput, "monthly.sleep", false);
    }

    /**
     * Loads a daily current amount and saves it when Enter is pressed or focus leaves.
     *
     * @param field field containing the amount
     * @param preferenceKey key used to persist the amount
     * @param integer whether the amount must be a whole number
     */
    private void configureCurrentInput(TextField field, String preferenceKey, boolean integer) {
        if (field == null || currentAmountStatusLabel == null) {
            throw new IllegalStateException("A Daily Overview amount input is missing.");
        }

        field.setTextFormatter(new TextFormatter<String>(change ->
                isValidAmount(change.getControlNewText(), integer) ? change : null));
        field.setText(goalPreferences.get(preferenceKey, "0"));
        field.setOnAction(event -> saveCurrentAmount(field, preferenceKey, integer));
        field.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) {
                saveCurrentAmount(field, preferenceKey, integer);
            }
        });
    }

    /**
     * Loads a goal into a text field, validates edits, and saves when editing is committed.
     *
     * @param field FXML text field for one goal
     * @param preferenceKey key used to persist this goal
     * @param integer whether the goal must be a whole number
     */
    private void configureGoalInput(TextField field, String preferenceKey, boolean integer) {
        if (field == null) {
            throw new IllegalStateException("Goal input is missing from the Settings view.");
        }

        field.setTextFormatter(new TextFormatter<String>(change ->
                isValidAmount(change.getControlNewText(), integer) ? change : null));
        field.setText(goalPreferences.get(preferenceKey, "0"));
        field.setOnAction(event -> saveGoal(field, preferenceKey, integer));
        field.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) {
                saveGoal(field, preferenceKey, integer);
            }
        });
    }

    /**
     * Checks that a goal is a non-negative integer or finite decimal as required.
     *
     * @param value text being entered
     * @param integer whether only whole numbers are allowed
     * @return true when the text is empty during editing or contains a valid goal
     */
    private boolean isValidAmount(String value, boolean integer) {
        if (value.isEmpty()) {
            return true;
        }

        try {
            if (integer) {
                return value.matches("\\d+") && Integer.parseInt(value) >= 0;
            }

            if (!value.matches("\\d+(\\.\\d*)?")) {
                return false;
            }
            double amount = Double.parseDouble(value);
            return Double.isFinite(amount) && amount >= 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Persists a daily amount and refreshes its summary and progress chart. */
    private void saveCurrentAmount(TextField field, String preferenceKey, boolean integer) {
        String value = field.getText().isEmpty() ? "0" : field.getText();
        if (!isValidAmount(value, integer)) {
            currentAmountStatusLabel.setText("Enter a valid non-negative current amount.");
            return;
        }

        try {
            goalPreferences.put(preferenceKey, value);
            goalPreferences.flush();
            field.setText(value);
            currentAmountStatusLabel.setText("Current amount saved.");
            updateCurrentValues();
        } catch (BackingStoreException e) {
            currentAmountStatusLabel.setText("Could not save current amount: " + e.getMessage());
        }
    }

    /** Persists a committed goal and reports save success or failure in Settings. */
    private void saveGoal(TextField field, String preferenceKey, boolean integer) {
        String value = field.getText();
        if (!value.isEmpty() && !isValidAmount(value, integer)) {
            goalStatusLabel.setText("Enter a valid non-negative goal.");
            return;
        }

        try {
            String storedValue = value.isEmpty() ? "0" : value;
            goalPreferences.put(preferenceKey, storedValue);
            goalPreferences.flush();
            field.setText(storedValue);
            goalStatusLabel.setText("Goal saved.");
            updateGoalDisplays();
            updateGoalCharts();
        } catch (BackingStoreException e) {
            goalStatusLabel.setText("Could not save goal: " + e.getMessage());
        }
    }

    /** Refreshes daily current values, goal fractions, and progress charts. */
    private void updateCurrentValues() {
        updateCurrentValue(dailyStepsValue, "current.daily.steps", true, "steps");
        updateCurrentValue(dailyWaterValue, "current.daily.water", false, "L");
        updateCurrentValue(dailyCaloriesValue, "current.daily.calories", true, "kcal");
        updateCurrentValue(dailySleepValue, "current.daily.sleep", false, "h");
        updateGoalDisplays();
        updateGoalCharts();
    }

    /**
     * Updates a Home summary label from its persisted daily amount.
     *
     * @param valueLabel summary label to update
     * @param preferenceKey key holding the current amount
     * @param integer whether the amount is formatted as a whole number
     * @param unit display unit for the metric
     */
    private void updateCurrentValue(Label valueLabel, String preferenceKey,
            boolean integer, String unit) {
        if (valueLabel != null) {
            valueLabel.setText(formatAmount(goalPreferences.get(preferenceKey, "0"), integer)
                    + " " + unit);
        }
    }

    /** Formats a persisted numeric amount for display. */
    private String formatAmount(String value, boolean integer) {
        return integer
                ? "%,d".formatted(Integer.parseInt(value))
                : "%.1f".formatted(Double.parseDouble(value));
    }

    /** Refreshes the daily, weekly, and monthly goal fractions from saved preferences. */
    private void updateGoalDisplays() {
        updateGoalFraction(dailyStepsValue, dailyStepsGoalValue, "daily.steps", true, "steps");
        updateGoalFraction(dailyWaterValue, dailyWaterGoalValue, "daily.water", false, "L");
        updateGoalFraction(dailyCaloriesValue, dailyCaloriesGoalValue,
                "daily.calories", true, "kcal");
        updateGoalFraction(dailySleepValue, dailySleepGoalValue, "daily.sleep", false, "h");
        updateGoalFraction(weeklyStepsValue, weeklyStepsGoalValue, "weekly.steps", true, "steps");
        updateGoalFraction(weeklyWaterValue, weeklyWaterGoalValue, "weekly.water", false, "L");
        updateGoalFraction(weeklyCaloriesValue, weeklyCaloriesGoalValue,
                "weekly.calories", true, "kcal");
        updateGoalFraction(weeklySleepValue, weeklySleepGoalValue, "weekly.sleep", false, "h");
        updateGoalFraction(monthlyStepsValue, monthlyStepsGoalValue,
                "monthly.steps", true, "steps");
        updateGoalFraction(monthlyWaterValue, monthlyWaterGoalValue,
                "monthly.water", false, "L");
        updateGoalFraction(monthlyCaloriesValue, monthlyCaloriesGoalValue,
                "monthly.calories", true, "kcal");
        updateGoalFraction(monthlySleepValue, monthlySleepGoalValue,
                "monthly.sleep", false, "h");
    }

    /**
     * Updates one goal label using the current label value and its saved target.
     *
     * @param currentValue label containing the current metric amount
     * @param goalValue label to display the current/goal fraction
     * @param preferenceKey key holding the saved goal
     * @param integer whether the goal is formatted as a whole number
     * @param unit display unit for the metric
     */
    private void updateGoalFraction(Label currentValue, Label goalValue,
            String preferenceKey, boolean integer, String unit) {
        if (currentValue == null || goalValue == null) {
            return;
        }

        String currentText = currentValue.getText();
        int unitSeparator = currentText.indexOf(' ');
        if (unitSeparator < 0) {
            throw new IllegalStateException("Metric value is missing its unit.");
        }

        String goal = goalPreferences.get(preferenceKey, "0");
        String formattedGoal = integer
                ? "%,d".formatted(Integer.parseInt(goal))
                : "%.1f".formatted(Double.parseDouble(goal));
        goalValue.setText(currentText.substring(0, unitSeparator)
                + " / " + formattedGoal + " " + unit);
    }

    /** Regenerates charts for whichever overview view is currently loaded. */
    private void updateGoalCharts() {
        updateGoalChart(dailyStepsGraph, dailyStepsValue, "daily.steps", "steps");
        updateGoalChart(dailyWaterGraph, dailyWaterValue, "daily.water", "L");
        updateGoalChart(dailyCaloriesGraph, dailyCaloriesValue, "daily.calories", "kcal");
        updateGoalChart(dailySleepGraph, dailySleepValue, "daily.sleep", "h");
        updateGoalChart(weeklyStepsGraph, weeklyStepsValue, "weekly.steps", "steps");
        updateGoalChart(weeklyWaterGraph, weeklyWaterValue, "weekly.water", "L");
        updateGoalChart(weeklyCaloriesGraph, weeklyCaloriesValue, "weekly.calories", "kcal");
        updateGoalChart(weeklySleepGraph, weeklySleepValue, "weekly.sleep", "h");
        updateGoalChart(monthlyStepsGraph, monthlyStepsValue, "monthly.steps", "steps");
        updateGoalChart(monthlyWaterGraph, monthlyWaterValue, "monthly.water", "L");
        updateGoalChart(monthlyCaloriesGraph, monthlyCaloriesValue, "monthly.calories", "kcal");
        updateGoalChart(monthlySleepGraph, monthlySleepValue, "monthly.sleep", "h");
    }

    /**
     * Replaces a GraphOver container with a pie chart showing completed and remaining goal.
     *
     * @param graphContainer graph placeholder in an overview FXML
     * @param currentValue label containing the current metric value
     * @param preferenceKey key holding the saved goal
     * @param unit display unit for the metric
     */
    private void updateGoalChart(StackPane graphContainer, Label currentValue,
            String preferenceKey, String unit) {
        if (graphContainer == null) {
            return;
        }

        double current = readCurrentAmount(currentValue);
        String savedGoal = goalPreferences.get(preferenceKey, "0");
        double goal = Double.parseDouble(savedGoal);
        PieChart chart = createGoalPieChart(current, goal, unit);
        boolean integer = "steps".equals(unit) || "kcal".equals(unit);
        Label amountLabel = new Label("%s / %s %s".formatted(
                formatChartAmount(current, integer),
                formatChartAmount(goal, integer), unit));
        amountLabel.setFont(Font.font(20.0));
        amountLabel.setWrapText(true);
        amountLabel.setMaxWidth(300.0);

        HBox chartWithAmount = new HBox(18.0, chart, amountLabel);
        chartWithAmount.setAlignment(Pos.CENTER);
        graphContainer.getChildren().setAll(chartWithAmount);
    }

    /**
     * Creates a pie chart for the fulfilled portion of a goal, capping progress at 100%.
     *
     * @param current amount completed so far
     * @param goal target amount
     * @param unit display unit for the metric
     * @return pie chart showing completed and remaining amounts
     */
    private PieChart createGoalPieChart(double current, double goal, String unit) {
        PieChart chart = new PieChart();
        chart.setAnimated(false);
        chart.setLabelsVisible(false);
        chart.setLegendVisible(false);
        chart.setPrefSize(280.0, 220.0);
        chart.setMaxSize(280.0, 220.0);

        if (goal <= 0.0) {
            chart.setTitle("0%");
            Tooltip.install(chart, new Tooltip("Goal is 0 " + unit + "; no progress slice is shown."));
            return chart;
        }

        double completed = Math.min(current, goal);
        double remaining = goal - completed;
        chart.getData().setAll(
                new PieChart.Data("Completed", completed),
                new PieChart.Data("Remaining", remaining));
        chart.setTitle("%.0f%%".formatted(completed / goal * 100.0));
        boolean integer = "steps".equals(unit) || "kcal".equals(unit);
        Tooltip.install(chart, new Tooltip("%s / %s %s".formatted(
                formatChartAmount(current, integer), formatChartAmount(goal, integer), unit)));
        return chart;
    }

    /** Formats a chart amount as an integer count or a one-decimal measurement. */
    private String formatChartAmount(double amount, boolean integer) {
        return integer ? "%,.0f".formatted(amount) : "%.1f".formatted(amount);
    }

    /**
     * Reads the numeric portion of a current-value label for use in goal charts.
     *
     * @param valueLabel label containing a formatted amount and unit
     * @return the current amount
     */
    private double readCurrentAmount(Label valueLabel) {
        if (valueLabel == null) {
            throw new IllegalStateException("Current metric value is not available.");
        }

        String text = valueLabel.getText();
        int unitSeparator = text.indexOf(' ');
        if (unitSeparator < 0) {
            throw new IllegalStateException("Metric value is missing its unit.");
        }

        double value = Double.parseDouble(text.substring(0, unitSeparator).replace(",", ""));
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalStateException("Current metric value must be finite and non-negative.");
        }
        return value;
    }
}
