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
*     IPCPRequest req = new IPCPRequest(IPCPRequest.Type.GET, 12345);
*     IPCPResponse res = IPCP.send(req);
*
* @see IPCP.java
* @see IPCPResponse.java
* @author Lukas Lynch
*/
import java.nio.ByteBuffer;

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

    public IPCPRequest(Type type, int userID)
    {
        this.type = type;
        this.userID = userID;
    }

    public byte[] getBytes()
    {
        // type=1, userID=4
        ByteBuffer buf = ByteBuffer.allocate(1 + 4);

        buf.put((byte) this.type.ordinal());
        buf.putInt(this.userID);

        return buf.array();
    }
}