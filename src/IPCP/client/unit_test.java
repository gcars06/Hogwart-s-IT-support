import java.io.IOException;
import org.json.JSONObject;

class unit_test
{
    public static void main(String args[]) throws IOException
    {
        if(IPCP.init() == false)
        {
            System.err.println("Failed to initialize IPCP");
            return;
        }

        JSONObject out_payld = new JSONObject("{\"message\": \"capybaras are awesome\"}");

        IPCPRequest req = new IPCPRequest(IPCPRequest.Type.UPDATE, 12345, out_payld.toString());
        IPCPResponse res = IPCP.send(req);

        System.out.printf("%nResponse:%n-Code: %s%n-Message: %s%n%n", res.code, res.message);

        IPCP.close();
    }
}