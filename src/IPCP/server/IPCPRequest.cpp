/**
* IPCP - Inter-Process Communication Protocol
*
* When IPCP::read() is called, it reads the protocol header (containing the
* version, type, and userID), then passes that header, as a byte array, to the
* constructor. It also passes the request pipe stream, so we can decode the
* string containing the JSON body.
*
* Example:
*     IPCPRequest req = IPCP::read();
*     if(req.type == IPCPRequest::GET) {...}
*
* @see IPCP.cpp
* @see IPCP.hpp
* @see IPCPResponse.cpp
* @author Lukas Lynch
*/
#include <string>
#include <cstdint>
#include <cstdlib>

#include <arpa/inet.h>
#include <fstream>

#include <nlohmann/json.hpp>
using json_t = nlohmann::json;

/**
* Attributes:
*   - version: int8_t
*   - type: Type(int8_t)
*   - userID: int32_t
*   - body: json_t
*
* Methods:
*   - N/A
*/
class IPCPRequest
{
    public:
    enum Type: int8_t
    {
        GET=0,
        PUT=1,
        UPDATE=2,
        DELETE=3,
        LOGIN=4,
        LOGOUT=5
    };
    
    // Header
    int8_t version;
    Type type;
    int32_t userID;
    const static size_t header_size =
        sizeof(version) +
        sizeof(type) +
        sizeof(userID);

    // Body
    json_t body;

    IPCPRequest(char header[header_size], std::ifstream * req_pipe)
    {
        this->version = *(int8_t*) header++;
        this->type = (Type) *(int8_t*) header++;
        this->userID = ntohl(*(int32_t*) header); // Convert big-endian to system's endianness
        this->body = deserialize(req_pipe);
    } // IPCPRequest()

    private:
    static json_t
    deserialize(std::ifstream * req_pipe)
    {
        char len_buf[sizeof(int32_t)];
        (void) req_pipe->read(len_buf, sizeof(len_buf));
        int32_t len = ntohl(*(int32_t*) len_buf); // Convert big-endian to system's endianness
        if(!len)
            return json_t::object(); // Empty object

        char * json_s = (char*) calloc(len + 1, sizeof(char)); // json_s(erialized)
        (void) req_pipe->read(json_s, len);
        json_t obj = json_t::parse(json_s);
        free(json_s);

        return obj;
    } // json_t deserialize()
};