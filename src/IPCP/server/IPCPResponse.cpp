/**
* IPCP - Inter-Process Communication Protocol
*
* W.I.P. - This class is not complete; attributes will need to be added/changed
*          as project requirements develop.
*
* When sending a response to the client, we must construct an IPCPResponse
* object. The object's attributes will be converted into a byte vector and
* sent through the reponse pipe, where the client will reconstruct it into an
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

using namespace std;

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
    string message;

    IPCPResponse(const StatusCode code, const string message="")
    {
        this->code = code;
        this->message = message;
    }

    vector<uint8_t>
    to_bytes(void)
    {
        vector<uint8_t> buf;
        buf.reserve(sizeof(uint8_t) + sizeof(uint32_t) + message.size());
        buf.push_back(code);

        uint32_t len = static_cast<uint32_t>(message.size());
        buf.push_back(static_cast<uint8_t>((len >> 24) & 0xFF));
        buf.push_back(static_cast<uint8_t>((len >> 16) & 0xFF));
        buf.push_back(static_cast<uint8_t>((len >>  8) & 0xFF));
        buf.push_back(static_cast<uint8_t>( len        & 0xFF));

        buf.insert(buf.end(), message.begin(), message.end());
        return buf;
    }
};