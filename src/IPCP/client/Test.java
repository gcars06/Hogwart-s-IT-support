import java.io.*;

public class Test
{
    public static void main(String[] args) throws IOException
    {
        IPCP.init();

        IPCPResponse res = IPCP.send
        (
            new IPCPRequest(IPCPRequest.Type.GET, 12345)
        );

        System.out.println("Response:\nStatus: " + res.code + "\nMessage: " + res.message);

        IPCP.close();
    }
}