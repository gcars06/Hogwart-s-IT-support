/**
* IPCP - Inter-Process Communication Protocol
*
* This is a protocol designed to facilitate communication between the Java
* frontend, and the C++ backend server, using Linux named pipes (FIFO).
*
* On launch, the server must run IPCP::init(), and IPCP::close() needs to run
* before exit. IPCP::read() will wait for a request from the client, then
* construct/return an IPCPRequest object. To send a response, we must create
* an IPCPResponse object, and send that with IPCP::write().
*
* @see IPCPRequest.cpp
* @see IPCPResponse.cpp
* @author Lukas Lynch
*/
#include <cstdint>
#include <iostream>
#include <cerrno>

#include <unistd.h>
#include <sys/stat.h>
#include <filesystem>
#include <fstream>

using namespace std;

#define PIPE_REQ "/tmp/com.hogwarts.health.req"
#define PIPE_RES "/tmp/com.hogwarts.health.res"
#define PIPE_MODE (S_IRUSR | S_IWUSR) // file mode 600

#include "IPCPRequest.cpp"
#include "IPCPResponse.cpp"

namespace IPCP
{
    ifstream req_pipe;
    ofstream res_pipe;

    bool
    init(void)
    {
        cout << "Creating request pipe... " << flush;
        if(mkfifo(PIPE_REQ, PIPE_MODE) == -1 && errno != EEXIST)
        {
            cerr << endl << "Failed to create request pipe at: " << PIPE_REQ << endl;
            return false;
        }
        cout << "Done" << endl;

        cout << "Creating response pipe... " << flush;
        if(mkfifo(PIPE_RES, PIPE_MODE) == -1 && errno != EEXIST)
        {
            cerr << endl << "Failed to create response pipe at: " << PIPE_RES << endl;
            return false;
        }
        cout << "Done" << endl;

        cout << "Opening request pipe... " << flush;
        req_pipe.open(PIPE_REQ);
        if(!req_pipe.is_open())
        {
            cerr << endl << "Failed to open request pipe." << endl;
            return false;
        }
        cout << "Done" << endl;

        cout << "Opening response pipe... " << flush;
        res_pipe.open(PIPE_RES);
        if(!res_pipe.is_open())
        {
            cerr << endl << "Failed to open response pipe." << endl;
            return false;
        }
        cout << "Done" << endl;

        return true;
    }

    void
    close(void)
    {
        req_pipe.close();
        res_pipe.close();

        cout << "Removing request pipe... " << flush;
        try { filesystem::remove(PIPE_REQ); }
        catch(const filesystem::filesystem_error &err)
        {
            cerr << endl << "Failed to remove request pipe at: " << PIPE_REQ << endl <<
            "Error: " << err.what() << endl;
        }
        cout << "Done" << endl;

        cout << "Removing response pipe... " << flush;
        try { filesystem::remove(PIPE_RES); }
        catch(const filesystem::filesystem_error &err)
        {
            cerr << endl << "Failed to remove response pipe at: " << PIPE_RES << endl <<
            "Error: " << err.what() << endl;
        }
        cout << "Done" << endl;
    }

    IPCPRequest
    read(void)
    {
        char header_buf[IPCPRequest::header_size];
        (void) req_pipe.read(header_buf, sizeof(header_buf));

        // Non-header fields handled in constructor
        return IPCPRequest(header_buf, &req_pipe);
    }

    void
    write(IPCPResponse res)
    {
        vector<uint8_t> bytes = res.to_bytes();

        res_pipe.write(reinterpret_cast<const char*>(bytes.data()), bytes.size());
        res_pipe.flush();
    }
}

#undef PIPE_MODE
#undef PIPE_RES
#undef PIPE_REQ