package IPCP.client;

import java.io.IOException;
import org.json.JSONObject;

/** Small manual client/server smoke test for IPCP pipe initialization and request exchange. */
class unit_test
{
    /** Connects to the backend, sends one update request, and closes the client pipes. */
    public static void main(String args[]) throws IOException
    {
        System.out.println("\n=== Testing init system ===");
        if(IPCP.init() == false)
        {
            System.err.println("Failed to initialize IPCP");
            return;
        }
        System.out.println("=== Success ===\n");

        JSONObject out_body = new JSONObject("{\"field\": \"food\", \"subfield\": \"calories\", \"value\": \"2000\"}");

        System.out.println("=== Sending request to server ===");
        IPCPRequest req = new IPCPRequest
        (
            (byte) 1,                // version
            IPCPRequest.Type.UPDATE, // type
            12345,                   // userID
            out_body                 // body
        );
        IPCPResponse res = IPCP.send(req);
        System.out.printf("Response:%n-Version: %d%n-Status: %s%n-Body: %s%n", res.version, res.status, res.body.toString());
        System.out.println("=== Success ===\n");

        System.out.println("=== Closing pipes ===");
        IPCP.close();
        System.out.println("=== Success ===\n");
    }
}