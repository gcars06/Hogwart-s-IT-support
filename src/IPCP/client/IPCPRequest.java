package IPCP.client;

/**
* IPCP - Inter-Process Communication Protocol
*
* When requesting information from the backend server, you will create a
* request object using this class.
*
* Example:
*     JSONObject obj = new JSONObject(...);
*     IPCPRequest req = new IPCPRequest(1, IPCPRequest.Type.GET, 12345, obj);
*     IPCPResponse res = IPCP.send(req);
*
* @see IPCP.java
* @see IPCPResponse.java
* @author Lukas Lynch
*/
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

/**
* Attributes:
*   - version: byte
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

    // Header
    public byte version;
    public Type type;
    public int userID;
    public static final int header_size = 1 + 1 + 4;

    // Body
    public JSONObject body;

    public IPCPRequest(byte version, Type type, int userID, JSONObject body)
    {
        this.version = version;
        this.type = type;
        this.userID = userID;
        this.body = body;
    } // IPCPRequest()

    public IPCPRequest(byte version, Type type, int userID, String body)
    {
        this(version, type, userID, new JSONObject(body));
    } // IPCPRequest()

    private static final int INT_SIZE = 4;
    public byte[] getBytes()
    {
        String body_s = this.body.isEmpty() ? null : this.body.toString(); // body_s(erialized)
        int body_len = body_s == null ? 0 : body_s.length();

        byte[] buf = new byte[this.header_size + INT_SIZE + body_len];
        int bufp = 0; // Next free position in buf

        // Header
        buf[bufp++] = version;
        buf[bufp++] = (byte) this.type.ordinal();
        buf[bufp++] = (byte) (this.userID >> 24); // MSB
        buf[bufp++] = (byte) (this.userID >> 16);
        buf[bufp++] = (byte) (this.userID >> 8);
        buf[bufp++] = (byte) this.userID;         // LSB

        // Body length
        buf[bufp++] = (byte) (body_len >> 24); // MSB
        buf[bufp++] = (byte) (body_len >> 16);
        buf[bufp++] = (byte) (body_len >> 8);
        buf[bufp++] = (byte) body_len;         // LSB

        // Body
        if(body_s != null)
        {
            byte[] body_bytes = body_s.getBytes(StandardCharsets.UTF_8);
            for(byte b : body_bytes)
                buf[bufp++] = b;
        }

        return buf;
    } // byte[] getBytes()
}