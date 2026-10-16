#include <iostream>
#include <cstdint>

#include "IPCP.cpp"

int32_t
main(void)
{
    IPCP::init();

    IPCPRequest req = IPCP::read();
    cout << "Type: " << static_cast<int>(req.type) << endl << "userID: " << req.userID << endl;

    IPCPResponse res(IPCPResponse::OK, "hello!");
    IPCP::write(res);

    IPCP::close();
}