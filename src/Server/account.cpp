/**
* This module is responsible for the processing portion of account management.
* It mostly just manages the state of the `userID_valid` global variable, but
* it does also handl eaccount creation.
*
* @author Lukas Lynch
*/
#include "../IPCP/server/IPCP.hpp"
#include "../Storage/storage.h"

#include <nlohmann/json.hpp>
using json_t = nlohmann::json;

extern Storage storage;
extern bool userID_valid;

namespace account
{
    IPCPResponse
    create(const int32_t userID)
    {
        return storage.save_user_id(userID) ?
            IPCPResponse(
                /*version=*/IPCP_VERSION,
                /*status=*/IPCPResponse::StatusCode::OK_NO_CONTENT
            ) :
            IPCPResponse(
                /*version=*/IPCP_VERSION,
                /*status=*/IPCPResponse::StatusCode::SERVER_ERROR,
                /*body=*/R"({"message": "Failed to add new userID to database"})"_json
            );
    } // IPCPResponse create()

    IPCPResponse
    login(const IPCPRequest & req)
    {
        if(!storage.check_user_id(req.userID))
        {
            return IPCPResponse(
                /*version=*/IPCP_VERSION,
                /*status=*/IPCPResponse::StatusCode::USER_NOT_FOUND,
                /*body=*/R"({"message": "Invalid userID provided"})"_json
            );
        }

        userID_valid = true;
        return IPCPResponse(
            /*version=*/IPCP_VERSION,
            /*status=*/IPCPResponse::StatusCode::OK_NO_CONTENT
        );
    } // IPCPResponse login()

    IPCPResponse
    logout(void)
    {
        userID_valid = false;
        return IPCPResponse(
            /*version=*/IPCP_VERSION,
            /*status=*/IPCPResponse::StatusCode::OK_NO_CONTENT
        );
    } // IPCPResponse logout()
}