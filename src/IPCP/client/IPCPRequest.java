package IPCP.client;

/**
* IPCP - Inter-Process Communication Protocol
*
* When requesting information from the backend server, you will create a
* request object using this class. `type` and `userID` are required for all
* requests, and other attributes may be optional depending on the type.
*
* Example:
*     JSONObject obj = new JSONObject(...);
*     IPCPRequest req = new IPCPRequest(IPCPRequest.Type.GET, 12345, obj);
*     IPCPResponse res = IPCP.send(req);
*
* @see IPCP.java
* @see IPCPResponse.java
* @author Lukas Lynch
*/
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
* Attributes:
*   - type: enum Type
*   - userID: int
*   - body: JSONObject
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
        DELETE, // 3
        LOGIN,  // 4
        LOGOUT  // 5
    };

    public Type type;
    public int userID;
    public JSONObject body;

    public IPCPRequest(Type type, int userID, JSONObject body)
    {
        this.type = type;
        this.userID = userID;
        this.body = body;
    } // IPCPRequest

    public IPCPRequest(Type type, int userID, String body)
    {
        this(type, userID, new JSONObject(body));
    } // IPCPRequest

    public byte[] getBytes()
    {
        /*
        * type = 1byte
        * userID = 4bytes
        *
        * All string fields need 4 bytes for the length (int) + the length of the string
        */
        String body_s = this.body.toString(); // body_s(erialized)
        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + 4 + body_s.length());

        buf.put((byte) this.type.ordinal());
        buf.putInt(this.userID);
        inputString(buf, encode(body_s));

        return buf.array();
    } // byte[] getBytes()

    private static byte[] encode(String source)
    {
        return (source == null) ? null : source.getBytes(StandardCharsets.UTF_8);
    } // byte[] encode()

    private static void inputString(ByteBuffer buf, byte[] source)
    {
        if(source == null)
            buf.putInt(0);
        else
        {
            buf.putInt(source.length);
            buf.put(source);
        }
    } // void inputString()
}