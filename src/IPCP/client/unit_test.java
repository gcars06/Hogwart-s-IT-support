import java.io.IOException;
import org.json.JSONObject;

class unit_test
{
    public static void main(String args[]) throws IOException
    {
        System.out.println("=== Testing init system ===\n");
        if(IPCP.init() == false)
        {
            System.err.println("Failed to initialize IPCP");
            return;
        }
        System.out.println("\n=== Success ===\n");

        JSONObject out_body = new JSONObject("{\"field\": \"food\", \"subfield\": \"calories\", \"value\": \"2000\"}");

        System.out.println("=== Sending request to server ===");
        IPCPRequest req = new IPCPRequest
        (
            IPCPRequest.Type.UPDATE, // type
            12345,                   // userID
            out_body.toString()      // body
        );
        IPCPResponse res = IPCP.send(req);
        System.out.println("=== Success ===\n");

        System.out.println("=== Parsing body contents ===");
        JSONObject in_body = new JSONObject(res.body);
        System.out.println("=== Success ===\n");

        System.out.printf("Response:%n-Code: %s%n-Body: %s%n%n", res.code, in_body.toString());

        System.out.println("=== Closing pipes ===\n");
        IPCP.close();
        System.out.println("\n=== Success ===");
    }
}