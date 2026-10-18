/**
* IPCP - Inter-Process Communication Protocol
*
* W.I.P. - This class is not complete; attributes will need to be added/changed
*          as project requirements develop.
*
* When requesting information from the backend server, you will create a
* request object using this class. `type` and `userID` are required for all
* requests, and other attributes may be optional depending on the type.
*
* Example:
*     IPCPRequest req = new IPCPRequest(IPCPRequest.Type.GET, 12345, "something");
*     IPCPResponse res = IPCP.send(req);
*
* @see IPCP.java
* @see IPCPResponse.java
* @author Lukas Lynch
*/
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
* Attributes:
*   - type: enum Type
*   - userID: int
*   - demoField: String
*
* Methods:
*   - getBytes(): byte[]
*/
public class IPCPRequest
{
    public enum Type
    {
        GET,    // 0
        PUT,    // 1
        UPDATE, // 2
        DELETE  // 3
    };

    public Type type;
    public int userID;
    public String demoField;

    public IPCPRequest(Type type, int userID, String demoField)
    {
        this.type = type;
        this.userID = userID;
        this.demoField = demoField;
    }

    public byte[] getBytes()
    {
        /*
        * type = 1byte
        * userID = 4bytes
        *
        * All string fields need 4 bytes for the length (int) + the length of the string
        */
        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + 4 + demoField.length());

        buf.put((byte) this.type.ordinal());
        buf.putInt(this.userID);
        inputString(buf, encode(demoField));

        return buf.array();
    }

    private static byte[] encode(String source)
    {
        return (source == null) ? null : source.getBytes(StandardCharsets.UTF_8);
    }

    private static void inputString(ByteBuffer buf, byte[] source)
    {
        if(source == null)
            buf.putInt(0);
        else
        {
            buf.putInt(source.length);
            buf.put(source);
        }
    }
}