#include <cstdint>
#include <cstdlib>

#include "../IPCP/server/IPCP.hpp"

using namespace std;
using enum IPCPRequest::Type;

int32_t
listen(void)
{
    // We'll jump out when ready to end.
    while(true)
    {
        IPCPRequest req = IPCP::read();

        switch(req.type)
        {
            case GET:
                // Do something
                break;
            case PUT:
                // Do something
                break;
            case UPDATE:
                // Do something
                break;
            case DELETE:
                // Do something
                break;
            case LOGIN:
                // Do something
                break;
            case LOGOUT:
                goto done;
        }
    }

    done:
    return EXIT_SUCCESS;
}

int32_t
main(void)
{
    if(!IPCP::init())
    {
        cerr << "Server: Failed to initialize IPCP" << endl;
        return EXIT_FAILURE;
    }

    int32_t ret = listen();

    IPCP::close();
    return ret;
}