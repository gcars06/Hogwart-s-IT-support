/**
* IPCP - Inter-Process Communication Protocol
*
* W.I.P. - This class is not complete; attributes will need to be added/changed
*          as project requirements develop.
*
* When IPCP::read() is called, it reads the protocol header (containing the
* type and userID), then passes that header, as a byte array, to the
* constructor. It also passes the request pipe stream, so we can get the string
* field values.
*
* Example:
*     IPCPRequest req = IPCP::read();
*     if(req.type == IPCPRequest::GET) {...}
*
* @see IPCP.cpp
* @see IPCPResponse.cpp
* @author Lukas Lynch
*/
#include <cstdint>
#include <cstring>
#include <string>

#include <arpa/inet.h> // For ntohl
#include <fstream>

using namespace std;

/**
* Attributes:
*   - type: enum Type(uint8_t)
*   - userID: int32_t
*   - demo_field: string
*
* Methods:
*   - N/A
*/
class IPCPRequest
{
    public:
    const static size_t header_size = sizeof(uint8_t) + sizeof(int32_t);
    
    enum Type: uint8_t
    {
        GET=0,
        PUT=1,
        UPDATE=2,
        DELETE=3
    };

    Type type;
    int32_t userID;
    string demo_field;

    IPCPRequest(const char header[header_size], ifstream* req_pipe)
    {
        // Next free position in header buffer
        uint32_t headerp = 0;

        this->type = (Type) (uint8_t) header[headerp];
        headerp += sizeof(Type);

        this->userID = ntohl(*(int32_t*)(header + headerp));
        headerp += sizeof(int32_t);

        this->demo_field = decode_str(req_pipe);
    }

    /**
    * String fields are encoded with the string's length (int32_t), followed by
    * the string in UTF-8. Call this method for every string field.
    */
    static string
    decode_str(ifstream* req_pipe)
    {
        char len_buf[sizeof(int32_t)];
        req_pipe->read(len_buf, sizeof(int32_t));
        int32_t len = ntohl(*(int32_t*)len_buf);

        if(len == 0)
            return "";

        char str_buf[len];
        req_pipe->read(str_buf, len);
        string str(str_buf, len);

        return str;
    }
};