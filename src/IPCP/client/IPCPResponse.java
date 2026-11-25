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
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
* Attributes:
*   - version: byte
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
        OK,                 // 0
        OK_NO_CONENT,       // 1
        BAD_REQUEST,        // 2
        USER_NOT_FOUND,     // 3
        DATA_NOT_FOUND,     // 4
        SERVER_ERROR,       // 5
        BAD_RESPONSE,       // 6
        UNSUPPORTED_VERSION // 7
    }

    // Header
    public byte version;
    public StatusCode status;
    public static final int header_size = 1 + 1;

    // Body
    public JSONObject body;

    private static final int INT_SIZE = 4;
    private static final StatusCode[] statuses = StatusCode.values();
    public IPCPResponse(byte[] header, InputStream res_pipe)
    {
        try
        {
            this.version = header[0];
            this.status = statuses[header[1]];
            this.body = deserialize(res_pipe);
        }
        catch(Exception _ignored)
        {
            this.status = StatusCode.BAD_RESPONSE;
            this.body = new JSONObject("{\"message\": \"Bad response from server.\"}");
        }
    } // IPCPResponse()

    private static JSONObject
    deserialize(InputStream res_pipe) throws Exception
    {
        byte[] len_buf = new byte[INT_SIZE];
        res_pipe.read(len_buf, /*offset=*/0, INT_SIZE);
        int len = ((len_buf[0] & 0xFF) << 24) | // MSB
                  ((len_buf[1] & 0xFF) << 16) |
                  ((len_buf[2] & 0xFF) <<  8) |
                   (len_buf[3] & 0xFF);         // LSB

        if(len == 0)
            return new JSONObject("{}");

        byte[] body_s = new byte[len]; // body_s(erialized)
        res_pipe.read(body_s, /*offset=*/0, len);
        return new JSONObject(new String
        (
            /*source=*/body_s,
            /*offset=*/0,
            /*length=*/len,
            /*charset=*/StandardCharsets.UTF_8
        ));
    } // JSONObject deserialize()
}