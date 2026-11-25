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
* @see IPCP.hpp
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

#include "IPCPRequest.cpp"
#include "IPCPResponse.cpp"

#define PIPE_REQ "/tmp/com.hogwarts.health.req"
#define PIPE_RES "/tmp/com.hogwarts.health.res"
#define PIPE_MODE (S_IRUSR | S_IWUSR) // file mode 600

namespace IPCP
{
    // Prototypes
    bool init(void);
    void close(void);
    IPCPRequest read(void);
    void write(IPCPResponse&);

    std::ifstream req_pipe;
    std::ofstream res_pipe;

    bool
    init(void)
    {
        // Start: Create pipes
        if(mkfifo(PIPE_REQ, PIPE_MODE) == -1 && errno != EEXIST)
        {
            std::cerr << "IPCP: Failed to create request pipe at: " << PIPE_REQ << std::endl;
            return false;
        }

        if(mkfifo(PIPE_RES, PIPE_MODE) == -1 && errno != EEXIST)
        {
            std::cerr << "IPCP: Failed to create response pipe at: " << PIPE_RES << std::endl;
            IPCP::close();
            return false;
        }
        // End: Create pipes

        // Start: Open pipes
        req_pipe.open(PIPE_REQ);
        if(!req_pipe.is_open())
        {
            std::cerr << "IPCP: Failed to open request pipe." << std::endl;
            IPCP::close();
            return false;
        }

        res_pipe.open(PIPE_RES);
        if(!res_pipe.is_open())
        {
            std::cerr << "IPCP: Failed to open response pipe." << std::endl;
            IPCP::close();
            return false;
        }
        // End: Open pipes

        return true;
    } // bool init()

    void
    close(void)
    {
        req_pipe.close();
        res_pipe.close();

        try { std::filesystem::remove(PIPE_REQ); }
        catch(const std::filesystem::filesystem_error &err)
        {
            std::cerr << "IPCP: Failed to remove request pipe at: " << PIPE_REQ << std::endl <<
            "Error: " << err.what() << std::endl;
        }

        try { std::filesystem::remove(PIPE_RES); }
        catch(const std::filesystem::filesystem_error &err)
        {
            std::cerr << "IPCP: Failed to remove response pipe at: " << PIPE_RES << std::endl <<
            "Error: " << err.what() << std::endl;
        }
    } // void close()

    IPCPRequest
    read(void)
    {
        char header_buf[IPCPRequest::header_size];
        (void) req_pipe.read(header_buf, sizeof(header_buf));

        // Non-header fields read in constructor
        return IPCPRequest(header_buf, &req_pipe);
    } // IPCPRequest read()

    void
    write(IPCPResponse & res)
    {
        std::vector<char> bytes = res.to_bytes();

        res_pipe.write(bytes.data(), bytes.size());
        res_pipe.flush();
    } // void write()
}

#undef PIPE_MODE
#undef PIPE_RES
#undef PIPE_REQ