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

        JSONObject out_body = new JSONObject("{\"field\": \"food\", \"subfield\": \"calories\", \"value\": \"2000\"}");

        IPCPRequest req = new IPCPRequest(IPCPRequest.Type.UPDATE, 12345, out_body.toString());
        IPCPResponse res = IPCP.send(req);

        System.out.printf("%nResponse:%n-Code: %s%n-Body: %s%n%n", res.code, res.body);

        IPCP.close();
    }
}