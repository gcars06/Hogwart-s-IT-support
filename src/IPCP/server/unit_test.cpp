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
    cout << "=== Testing init system ===" << endl << endl;
    assert(IPCP::init() == true);
    cout << endl << "=== Success ===" << endl;

    // Read incoming
    cout << "=== Reading incoming request from client ===" << endl << endl;
    IPCPRequest req = IPCP::read();

    assert(req.type >= IPCPRequest::GET && req.type <= IPCPRequest::DELETE);
    json in_body = json::parse(req.body);

    cout << "Request:" << endl << "-Type: " << static_cast<int>(req.type) << endl << "-UserID: " << req.userID << endl << "-Body: " << in_body.dump(4) << endl;
    cout << endl << "=== Success ===" << endl;

    // Send outgoing
    cout << "=== Creating and sending response object ===" << endl << endl;

    json out_body = R"(
        {
            "message": "Invalid userID provided"
        }
    )"_json;
    IPCPResponse res
    (
        IPCPResponse::USER_NOT_FOUND,
        out_body.dump()
    );
    // Here's another example:
    // IPCPResponse res(IPCPResponse::OK_NO_CONTENT);
    IPCP::write(res);
    cout << "=== Success ===" << endl;

    // Close
    cout << "=== Closing pipes ===" << endl << endl;
    IPCP::close();
    cout << endl << "=== Success ===" << endl;

    return 0;
}