import java.io.*;

public class Test
{
    public static void main(String[] args) throws IOException
    {
        IPCP.init();

        IPCPResponse res = IPCP.send
        (
            new IPCPRequest(IPCPRequest.Type.UPDATE, 12345)
        );

        IPCP.close();
    }
}