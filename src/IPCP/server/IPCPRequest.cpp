/**
* IPCP - Inter-Process Communication Protocol
*
* W.I.P. - This class is not complete; attributes will need to be added/changed
*          as project requirements develop.
*
* When IPCP::read() receives a data stream from the client, it will call on the
* constructor to create an IPCPRequest object, from which we can extract
* useful information.
*
* Example:
*     IPCPRequest req = IPCP::read();
*     if(req.type == ICPCRequest::GET) {...}
*
* @see IPCP.cpp
* @see IPCPResponse.cpp
* @author Lukas Lynch
*/
#include <cstdint>
#include <cstring>

// For ntohl
#include <arpa/inet.h>

class IPCPRequest
{
    public:
    const static size_t protocol_size = sizeof(uint8_t) + sizeof(int32_t);
    
    enum Type: uint8_t
    {
        GET=0,
        PUT=1,
        UPDATE=2,
        DELETE=3
    };

    Type type;
    int32_t userID;


    IPCPRequest(const char data[protocol_size])
    {
        uint32_t byte_counter = 0;

        this->type = (Type) (uint8_t) data[byte_counter];
        byte_counter += sizeof(Type);

        uint32_t id;
        (void) memcpy(&id, data+byte_counter, sizeof(uint32_t));
        this->userID = (int32_t) ntohl(id);
        byte_counter += sizeof(int32_t);
    }
};