import java.io.IOException;

class unit_test
{
    public static void main(String args[]) throws IOException
    {
        if(IPCP.init() == false)
        {
            System.err.println("Failed to initialize IPCP");
            return;
        }

        IPCPRequest req = new IPCPRequest(IPCPRequest.Type.UPDATE, 12345, "capybaras are awesome");
        IPCPResponse res = IPCP.send(req);

        System.out.printf("%nResponse:%n-Code: %s%n-Message: %s%n%n", res.code, res.message);

        IPCP.close();
    }
}