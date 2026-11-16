/**
* IPCP - Inter-Process Communication Protocol
*
* When sending a response to the client, we must construct an IPCPResponse
* object. The object's attributes will be converted into a byte vector and
* sent through the response pipe, where the client will reconstruct it into an
* object from which useful data can be extracted.
*
* Example:
*     IPCPResponse res(1, IPCPResponse::OK, R"({"message": "All is well!"})"_json);
*     IPCP::write(res);
*
* @see IPCP.cpp
* @see IPCP.hpp
* @see IPCPRequest.cpp
* @author Lukas Lynch
*/
#include <string>
#include <vector>
#include <cstdint>
#include <cstring>

#include <arpa/inet.h>

#include <nlohmann/json.hpp>
using json_t = nlohmann::json;

/**
* Attributes:
*   - version: int8_t
*   - status: StatusCode(int8_t)
*   - body: json_t
*
* Methods:
*   - to_bytes(): vector<char>
*/
class IPCPResponse
{
    public:
    enum StatusCode: int8_t
    {
        OK=0,
        OK_NO_CONTENT=1,
        BAD_REQUEST=2,
        USER_NOT_FOUND=3,
        DATA_NOT_FOUND=4,
        SERVER_ERROR=5,
        BAD_RESPONSE=6,
        UNSUPPORTED_VERSION=7
    };

    // Header
    int8_t version;
    StatusCode status;
    const static size_t header_size =
        sizeof(version) +
        sizeof(status);

    // Body
    json_t body;

    IPCPResponse(
        const int8_t v,
        const StatusCode s=BAD_RESPONSE,
        const json_t & b=json_t::object()
    ): version(v), status(s), body(b) {}

    std::vector<char>
    to_bytes(void)
    {
        const std::string body_s = this->body.empty() ? std::string() : this->body.dump(); // body_s(erialized)
        const int32_t body_len = htonl(body_s.size()); // Network byte order

        std::vector<char> buf(header_size + sizeof(body_len) + body_s.size());

        buf[0] = this->version;
        buf[1] = this->status;
        (void) memcpy( // Append body length
            buf.data() + header_size,
            &body_len,
            sizeof(body_len)
        );
        (void) memcpy( // Append body
            buf.data() + header_size + sizeof(body_len),
            body_s.data(),
            body_s.size()
        );

        return buf;
    } // vector<char> to_bytes()
};