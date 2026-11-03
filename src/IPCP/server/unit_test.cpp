#include "IPCP.cpp"

#include <iostream>
#include <cassert>
#include <cstdint>

#include <nlohmann/json.hpp>
using json = nlohmann::json;

using namespace std;


int32_t
main(void)
{
    // Init
    cout << endl << "=== Testing init system ===" << endl;
    assert(IPCP::init() == true);
    cout << "=== Success ===" << endl << endl;

    // Read incoming
    cout << "=== Reading incoming request from client ===" << endl;
    IPCPRequest req = IPCP::read();

    assert(req.type >= IPCPRequest::GET && req.type <= IPCPRequest::DELETE);

    cout << "Request:" << endl << "-Type: " << static_cast<int>(req.type) << endl << "-UserID: " << req.userID << endl << "-Body: " << req.body.dump(4) << endl;
    cout << "=== Success ===" << endl << endl;

    // Send outgoing
    cout << "=== Creating and sending response object ===" << endl;

    json out_body = R"(
        {
            "message": "Invalid userID provided"
        }
    )"_json;
    IPCPResponse res
    (
        IPCPResponse::USER_NOT_FOUND,
        out_body
    );
    // Here's another example:
    // IPCPResponse res(IPCPResponse::OK_NO_CONTENT);
    IPCP::write(res);
    cout << "=== Success ===" << endl << endl;

    // Close
    cout << "=== Closing pipes ===" << endl;
    IPCP::close();
    cout << "=== Success ===" << endl << endl;

    return 0;
}