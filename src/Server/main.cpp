/**
* This is the core of the backend server, which will receive and process
* IPCPRequests from the frontend, interface with the database, then respond
* by sending back an IPCPResponse object.
*
* The program will spawned by the Java process, and will only terminate when
* it receives a SIGTERM signal (see util::term).
*
* @see account.cpp
* @see util.cpp
* @author Lukas Lynch
*/
// Includes
#include <cstdint>
#include <csignal>

#include "../IPCP/server/IPCP.hpp"
#include "../Storage/storage.cpp"
#include "account.cpp"
#include "util.cpp"

#include <nlohmann/json.hpp>

// Definitions
#define STORAGE_UNINITIALIZED -1
#define STORAGE_SQLITEERROR   -2
#define STORAGE_PLACEHOLDER   -3

using namespace std;
using json_t = nlohmann::json;
using enum IPCPRequest::Type;
using enum IPCPResponse::StatusCode;

// Global variables
#define GLOBAL // This is just eye candy
GLOBAL Storage storage;
GLOBAL bool userID_valid = false;

IPCPResponse
fetch(const int32_t userID, const string * entry_date)
{
    // Equivalent to a nested function
    auto err = [](const string & type)
    {
        const string msg = R"({"message": "Failed to retrieve )" + type + R"( data"})";
    
        return IPCPResponse(
            /*version=*/IPCP_VERSION,
            /*status=*/DATA_NOT_FOUND,
            /*body=*/msg
        );
    };

    // ===================================================================== //

    json_t body = json_t::object();
    NutritionData n_data = storage.get_nutrition(userID, *entry_date);
    FitnessData f_data = storage.get_fitness(userID, *entry_date);
    double hours_slept = storage.get_hours_slept(userID, *entry_date);
    vector<string> goals = storage.get_goals(userID);

    // Check NutritionData; -3 is a placeholder value
    if((n_data.calories < 0 && n_data.calories != STORAGE_PLACEHOLDER) ||
      (n_data.protein < 0 && n_data.protein != STORAGE_PLACEHOLDER) ||
      (n_data.carbs < 0 && n_data.carbs != STORAGE_PLACEHOLDER) ||
      (n_data.fat < 0 && n_data.fat != STORAGE_PLACEHOLDER)) return err("nutrition");

    // Check FitnessData; -3 is placeholder value
    if((f_data.steps < 0 && f_data.steps != STORAGE_PLACEHOLDER) ||
      (f_data.lap_time_seconds < 0 && f_data.lap_time_seconds != STORAGE_PLACEHOLDER) ||
      (f_data.mile_time_seconds < 0 && f_data.mile_time_seconds != STORAGE_PLACEHOLDER)) return err("fitness");

    // Check hours slept; -3 is placeholder value
    if(hours_slept < 0 && hours_slept != STORAGE_PLACEHOLDER) return err("sleep");

    // Put all into JSON body
    body["nutritionData"] = {
        {"calories", n_data.calories},
        {"protein", n_data.protein},
        {"carbs", n_data.carbs},
        {"fat", n_data.fat},
    };
    body["fitnessData"] = {
        {"steps", f_data.steps},
        {"lapTimeSeconds", f_data.lap_time_seconds},
        {"mileTimeSeconds", f_data.mile_time_seconds},
    };
    body["hoursSlept"] = hours_slept;
    body["goals"] = json_t::array();
    for(string goal : goals)
        body["goals"].push_back(goal);

    return IPCPResponse(
        /*version=*/IPCP_VERSION,
        /*status=*/OK,
        /*body=*/body
    );
} // IPCPResponse fetch()

IPCPResponse
fetch_wrapper(const IPCPRequest & req)
{
    if(GLOBAL userID_valid)
    {
        return IPCPResponse(
            /*version=*/IPCP_VERSION,
            /*status=*/USER_NOT_FOUND,
            /*body=*/R"({"message": "Invalid userID provided"})"_json
        );
    }

    const string * entry_date = req.body.at("entryDate").get_ptr<const string *>();
    if(!entry_date)
    {
        return IPCPResponse(
            /*version=*/IPCP_VERSION,
            /*status=*/BAD_REQUEST,
            /*body=*/R"({"message": "No entryDate specified in request"})"_json
        );
    }

    return fetch(req.userID, entry_date);
} // IPCPResponse fetch_wrapper()

IPCPResponse
put(const int32_t userID, const string * entry_date, const json_t & body)
{
    // Equivalent to a nested function
    auto err = [](const string & type)
    {
        const string msg = R"({"message": "Failed to upload )" + type + R"( data to the database"})";
    
        return IPCPResponse(
            /*version=*/IPCP_VERSION,
            /*status=*/SERVER_ERROR,
            /*body=*/msg
        );
    };

    // ===================================================================== //

    if(!storage.save_nutrition(
        /*user_id=*/userID,
        /*entry_date=*/*entry_date,
        /*calories=*/body.at("nutritionData").at("calories").get<double>(),
        /*protein=*/body.at("nutritionData").at("protein").get<double>(),
        /*carbs=*/body.at("nutritionData").at("carbs").get<double>(),
        /*fat=*/body.at("nutritionData").at("fat").get<double>()
    )) return err("nutrition");

    if((!storage.save_steps(userID, *entry_date, body.at("fitnessData").at("steps").get<double>())) ||
       (!storage.save_lap_time(userID, *entry_date, body.at("fitnessData").at("lapTimeSeconds").get<double>())) ||
       (!storage.save_mile_time(userID, *entry_date, body.at("fitnessData").at("mileTimeSeconds").get<double>()))
    ) return err("fitness");

    if(!storage.save_hours_slept(userID, *entry_date, body.at("hoursSlept").get<double>()))
        return err("sleep");

    if(!storage.save_goal(userID, body.at("goal").get_ref<const string &>()))
        return err("goal");

    return IPCPResponse(
        /*version=*/IPCP_VERSION,
        /*type=*/OK_NO_CONTENT
    );
} // IPCPResponse put()

IPCPResponse
put_wrapper(const IPCPRequest & req)
{
    // PUT/UPDATE request with no body creates a new account
    if(req.body.empty())
        return account::create(req.userID);

    const string * entry_date = req.body.at("entryDate").get_ptr<const string *>();
    if(!entry_date)
    {
        return IPCPResponse(
            /*version=*/IPCP_VERSION,
            /*status=*/BAD_REQUEST,
            /*body=*/R"({"message": "No entryDate specified in request"})"_json
        );
    }

    return put(req.userID, entry_date, req.body);

} // IPCPResponse put_wrapper()

[[noreturn]] void
listen(void)
{
    while(true)
    {
        IPCPRequest req = IPCP::read();
        IPCPResponse res(IPCP_VERSION, SERVER_ERROR); // Default value

        if(req.version != IPCP_VERSION)
        {
            const string msg = R"({"message": "Server and Client have mismatched version numbers"})"_json;
            res = IPCPResponse(
                /*version=*/IPCP_VERSION,
                /*status=*/UNSUPPORTED_VERSION,
                /*body=*/msg
            );

            util::die(msg);
        }

        switch(req.type)
        {
            case GET:
                res = fetch_wrapper(req);
                break;
            case PUT: // Fallthrough to UPDATE
            case UPDATE:
                res = put_wrapper(req);
                break;
            case DELETE:
                // Still need to implement
                break;
            case LOGIN:
                res = account::login(req);
                break;
            case LOGOUT:
                res = account::logout();
        }

        IPCP::write(res);
    }
} // void listen()

[[noreturn]] int32_t
main(void)
{
    signal(SIGTERM, util::term);

    if(!IPCP::init())
        util::die("Server: Failed to initialize IPCP");
    if(!storage.initialize())
        util::die("Server: Failed to initialize database");

    listen();
} // int32_t main()