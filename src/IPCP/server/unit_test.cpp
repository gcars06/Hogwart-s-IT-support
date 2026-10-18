#include "IPCP.cpp"

#include <iostream>
#include <cassert>
#include <cstdint>

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
    cout << "Request:" << endl << "-Type: " << static_cast<int>(req.type) << endl << "-userID: " << req.userID << endl << "-demo_field: " << req.demo_field << endl;
    cout << endl << "=== Success ===" << endl;

    // Send outgoing
    cout << "=== Creating and sending response object ===" << endl << endl;
    IPCPResponse res = (IPCPResponse)
    {
        IPCPResponse::USER_NOT_FOUND,
        "Provided userID not associated with any known user."
    };
    IPCP::write(res);
    cout << "=== Success ===" << endl;

    // Close
    cout << "=== Closing pipes ===" << endl << endl;
    IPCP::close();
    cout << endl << "=== Success ===" << endl;

    return 0;
}