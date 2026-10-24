/**
* IPCP - Inter-Process Communication Protocol
*
* W.I.P. - This class is not complete; attributes will need to be added/changed
*          as project requirements develop.
*
* When receiving responses from the backend server, they will be converted into
* an IPCPResponse object, from which the useful data can be extracted.
*
* @see IPCP.java
* @see IPCPRequest.java
* @author Lukas Lynch
*/
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
* Attributes:
*   - code: enum StatusCode
*   - body: String
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
    static final StatusCode[] statuses = StatusCode.values();

    public StatusCode code;
    public String body;

    public IPCPResponse(byte[] data)
    {
        // Next free position in data buffer
        int datap = 0;

        try
        {
            // First byte from server should contain code.
            this.code = statuses[data[datap++]];

            // Next 4 bytes are the message's length.
            int bodyLength = ByteBuffer.wrap(data, datap, 4).getInt();
            datap += 4; // ints are 4 bytes.

            body = new String
            (
                data,                  // Data buffer to read from
                datap,                 // Position of body in data buffer
                bodyLength,            // Length of body
                StandardCharsets.UTF_8 // Charset - UTF-8
            );
            datap += bodyLength;
        }
        catch(Exception _ignored)
        {
            this.code = StatusCode.BAD_RESPONSE;
            this.body = "{\"message\": \"Bad response from server.\"}";
        }
    }
}