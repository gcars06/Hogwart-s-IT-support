package IPCP.client;

/**
* IPCP - Inter-Process Communication Protocol
*
* This is a protocol designed to facilitate communication between the Java
* frontend, and the C++ backend server, using Linux named pipes (FIFO).
*
* The backend server should be run first, which will initialize the pipes.
* There are two pipes: one for requests and the other for responses; this is
* done to avoid from constant disconnect/reconnections to a single pipe for
* bidirectional communication.
*
* The IPCP class is meant to be used statically. During program initialization,
* run IPCP.init(), then run IPCP.close() before exiting. IPCP.send() is to be
* used for sending an IPCPRequest object, and will return an IPCPResponse
* object.
*
* @see IPCPRequest.java
* @see IPCPResponse.java
* @author Lukas Lynch
*/
import java.io.*;

public class IPCP
{
    private static final String PIPE_REQ = "/tmp/com.hogwarts.health.req";
    private static final String PIPE_RES = "/tmp/com.hogwarts.health.res";
    private static OutputStream out = null;
    private static InputStream in = null;

    // IPCP should only be used statically
    private IPCP() {}

    public static boolean init()
    {
        if(out != null && in != null)
            return true;

        // Check if pipes exist
        File pipe_out = new File(PIPE_REQ);
        File pipe_in = new File(PIPE_RES);
        if(!pipe_out.exists() || !pipe_in.exists())
        {
            System.err.println("IPCP: Failed to initialize IPCP.\nPipes don't exist; make sure server is running.");
            return false;
        }

        try
        {
            out = new FileOutputStream(PIPE_REQ);
            in = new FileInputStream(PIPE_RES);

            return true;
        }
        catch(IOException e)
        {
            System.err.printf("IPCP: Failed to initialize IPCP.%n%s%n", e);
            close();
            return false;
        }
    } // boolean init()

    public static void close()
    {
        try { if(out != null) out.close(); } catch(IOException _ignored) {}
        try { if(in != null) in.close(); } catch(IOException _ignored) {}

        out = null;
        in = null;
    } // void close()

    public static IPCPResponse send(IPCPRequest req) throws IOException
    {
        out.write(req.getBytes());
        out.flush();

        byte[] res_header = new byte[IPCPResponse.header_size];
        in.read(res_header, /*offset=*/0, IPCPResponse.header_size);
        return new IPCPResponse(res_header, in);
    } // IPCPResonse send()
}