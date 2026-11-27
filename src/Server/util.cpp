/**
* Just some supporting functions that would otherwise clutter the main source
* file.
*
* @author Lukas Lynch
*/
#include <iostream>
#include <cstdlib>
#include "../IPCP/server/IPCP.hpp"

namespace util
{
    [[noreturn]] void
    die(const std::string & msg)
    {
        IPCP::close();
        std::cerr << msg << std::endl;
        std::_Exit(EXIT_FAILURE);
    } // void die()

    [[noreturn]] void
    term(int32_t /*unused*/)
    {
        IPCP::close();
        std::_Exit(EXIT_SUCCESS);
    } // void term()
}