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

    public StatusCode code;
    public String message;

    public IPCPResponse(byte[] data)
    {
        final StatusCode[] statuses = StatusCode.values();
        int byteCounter = 0;

        try
        {
            // First byte from server should contain code.
            this.code = statuses[data[byteCounter]];
            byteCounter += 1; // Only one byte used for code

            // Next 4 bytes are the message's length.
            int msgLength = ByteBuffer.wrap(data, byteCounter, 4).getInt();
            byteCounter += 4; // ints are 4 bytes.

            message = new String
            (
                data,
                byteCounter,
                msgLength,
                StandardCharsets.UTF_8
            );
            byteCounter += msgLength;
        }
        catch(Exception _ignored)
        {
            this.code = StatusCode.BAD_RESPONSE;
            this.message = "Bad response received from server.";
        }
    }
}