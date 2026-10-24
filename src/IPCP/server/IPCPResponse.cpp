/**
* IPCP - Inter-Process Communication Protocol
*
* W.I.P. - This class is not complete; attributes will need to be added/changed
*          as project requirements develop.
*
* When sending a response to the client, we must construct an IPCPResponse
* object. The object's attributes will be converted into a byte vector and
* sent through the response pipe, where the client will reconstruct it into an
* object from which useful data can be extracted.
*
* Example:
*     IPCPResponse res(IPCPResponse::OK, "All is well!");
*     IPCP::write(res);
*
* @see IPCP.cpp
* @see IPCPRequest.cpp
* @author Lukas Lynch
*/
#include <cstdint>
#include <cstdlib>
#include <cstring>

#include <string>
#include <vector>

#include <nlohmann/json.hpp>

using namespace std;
using json = nlohmann::json;

/**
* Attributes:
*   - code: enum StatusCode(uint8_t)
*   - body: string
*
* Methods:
*   - to_bytes(): vector<uint8_t>
*/
class IPCPResponse
{
    public:
    enum StatusCode: uint8_t
    {
        OK=0,
        OK_NO_CONTENT=1,
        BAD_REQUEST=2,
        USER_NOT_FOUND=3,
        DATA_NOT_FOUND=4,
        SERVER_ERROR=5,
        BAD_RESPONSE=6
    };

    StatusCode code;
    string body;

    IPCPResponse(const StatusCode c=BAD_RESPONSE, const string & b=""): code(c), body(b) {}
    IPCPResponse(const StatusCode c, const json & j): IPCPResponse(c, j.dump()) {}

    vector<uint8_t>
    to_bytes(void)
    {
        vector<uint8_t> buf;
        buf.reserve(sizeof(uint8_t) + sizeof(int32_t) + this->body.size());

        // Status code
        buf.push_back(code);

        if(this->body.size() == 0)
        {
            // Client will expect 4 bytes
            buf.push_back(0);
            buf.push_back(0);
            buf.push_back(0);
            buf.push_back(0);
            return buf;
        }

        // Payload length
        // Break up into individual bytes and convert to big-endian 
        uint32_t len = static_cast<uint32_t>(this->body.size());
        buf.push_back(static_cast<uint8_t>((len >> 24) & 0xFF));
        buf.push_back(static_cast<uint8_t>((len >> 16) & 0xFF));
        buf.push_back(static_cast<uint8_t>((len >>  8) & 0xFF));
        buf.push_back(static_cast<uint8_t>( len        & 0xFF));

        // Payload
        buf.insert
        (
            buf.end(),
            this->body.begin(),
            this->body.end()
        );
        return buf;
    }
};