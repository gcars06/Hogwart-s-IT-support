/**
* IPCP - Inter-Process Communication Protocol
*
* When IPCP::read() is called, it reads the protocol header (containing the
* type and userID), then passes that header, as a byte array, to the
* constructor. It also passes the request pipe stream, so we can decode the
* string containing the JSON body.
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

#include <nlohmann/json.hpp>

using namespace std;
using json = nlohmann::json;

/**
* Attributes:
*   - type: enum Type(uint8_t)
*   - userID: int32_t
*   - body: json
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
    json body;

    IPCPRequest(const char header[header_size], ifstream * req_pipe)
    {
        // We'll use this to step through the buffer without mutating it.
        const char * headerp = header;
    
        this->type = (Type) *(uint8_t*) headerp;
        headerp += sizeof(Type);

        // Use ntohl to convert big-endian to the system's endianness.
        this->userID = ntohl(*(int32_t*) headerp);
        headerp += sizeof(int32_t);

        this->body = decode_json(req_pipe);
    } // IPCPRequest()

    /**
    * JSON objects are encoded as strings when sent through the pipes. The
    * strings are prepended with their length (int32_t).
    */
    private:
    static json
    decode_json(ifstream * req_pipe)
    {
        /*
        * Reads 4 bytes from pipe, which should contain a 32-bit integer
        * denoting the length of the string. Convert from big-endian to
        * the system's endianness.
        */
        char len_buf[sizeof(int32_t)];
        req_pipe->read(len_buf, sizeof(int32_t));
        const int32_t len = ntohl(*(int32_t*) len_buf);

        if(len == 0)
            return json::object(); // Empty object

        char str_buf[len];
        req_pipe->read(str_buf, len);
        const string str(str_buf, len);

        return json::parse(str);
    } // json decode_json()
};