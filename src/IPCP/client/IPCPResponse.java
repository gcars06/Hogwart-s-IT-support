package IPCP.client;

/**
* IPCP - Inter-Process Communication Protocol
*
* When receiving responses from the backend server, they will be converted into
* an IPCPResponse object, from which the useful data can be extracted.
*
* @see IPCP.java
* @see IPCPRequest.java
* @author Lukas Lynch
*/
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
* Attributes:
*   - code: enum StatusCode
*   - body: JSONObject
*
* Methods:
*   - N/A
*/
class IPCPResponse
{
    public enum StatusCode
    {
        OK,             // 0
        OK_NO_CONTENT,  // 1
        BAD_REQUEST,    // 2
        USER_NOT_FOUND, // 3
        DATA_NOT_FOUND, // 4
        SERVER_ERROR,   // 5
        BAD_RESPONSE    // 6
    }
    public static final StatusCode[] statuses = StatusCode.values();

    public StatusCode code;
    public JSONObject body;

    public IPCPResponse(InputStream in)
    {
        try
        {
            // Next free position in header buffer
            int headerp = 0;

            // 1=StatusCode, 4=Body length
            byte[] header = new byte[1 + 4];
            in.read(header, headerp, header.length);
    
            // First byte from server should contain code.
            this.code = statuses[header[headerp++]];

            // Next 4 bytes are the message's length.
            int bodyLength = ByteBuffer.wrap(header, headerp, 4).getInt();
            headerp += 4;

            byte[] body_s = new byte[bodyLength]; // body_s(erial)
            in.read(body_s, 0, bodyLength);
            body = new JSONObject(new String
            (
                body_s,                // Data buffer to read from
                0,                     // Position of body in data buffer
                bodyLength,            // Length of body
                StandardCharsets.UTF_8 // Charset - UTF-8
            ));
        }
        catch(Exception _ignored)
        {
            this.code = StatusCode.BAD_RESPONSE;
            this.body = new JSONObject("{\"message\": \"Bad response from server.\"}");
        }
    } // IPCPResponse
}