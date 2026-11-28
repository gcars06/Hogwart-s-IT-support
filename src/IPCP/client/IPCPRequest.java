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

    /** Creates a request from its protocol header fields and JSON body. */
    public IPCPRequest(byte version, Type type, int userID, JSONObject body)
    {
        this.version = version;
        this.type = type;
        this.userID = userID;
        this.body = body;
    } // IPCPRequest()

    /** Parses a serialized JSON body and delegates to the main constructor. */
    public IPCPRequest(byte version, Type type, int userID, String body)
    {
        this(version, type, userID, new JSONObject(body));
    } // IPCPRequest()

    private static final int INT_SIZE = 4;
    /** Encodes the header and UTF-8 JSON body using IPCP's big-endian lengths. */
    public byte[] getBytes()
    {
        // The protocol length is bytes on the wire, not Java UTF-16 characters.
        byte[] body_bytes = this.body.isEmpty()
                ? new byte[0]
                : this.body.toString().getBytes(StandardCharsets.UTF_8);
        int body_len = body_bytes.length;

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
        System.arraycopy(body_bytes, 0, buf, bufp, body_len);

        return buf;
    } // byte[] getBytes()
}