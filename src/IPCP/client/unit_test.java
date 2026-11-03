import java.io.IOException;
import org.json.JSONObject;

class unit_test
{
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
            IPCPRequest.Type.UPDATE, // type
            12345,                   // userID
            out_body                 // body
        );
        IPCPResponse res = IPCP.send(req);
        System.out.printf("Response:%n-Code: %s%n-Body: %s%n", res.code, res.body.toString());
        System.out.println("=== Success ===\n");

        System.out.println("=== Closing pipes ===");
        IPCP.close();
        System.out.println("=== Success ===\n");
    }
}