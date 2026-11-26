#include <cstdint>
#include <cstdlib>

#include "../IPCP/server/IPCP.hpp"
#include "../Storage/storage.cpp"

#define STORAGE_UNINITIALIZED -1
#define STORAGE_SQLITEERROR   -2
#define STORAGE_PLACEHOLDER   -3

#include <nlohmann/json.hpp>
using json_t = nlohmann::json;

using namespace std;
using enum IPCPRequest::Type;
using enum IPCPResponse::StatusCode;

#define GLOBAL
GLOBAL Storage storage;
GLOBAL bool userID_valid = false;

IPCPResponse
fetch(const int32_t userID, const string * entry_date)
{
    json_t body = json_t::object();
    NutritionData n_data = storage.get_nutrition(userID, *entry_date);
    FitnessData f_data = storage.get_fitness(userID, *entry_date);
    double hours_slept = storage.get_hours_slept(userID, *entry_date);
    vector<string> goals = storage.get_goals(userID);

    // Check NutritionData; -3 is a placeholder value
    if((n_data.calories < 0 && n_data.calories != STORAGE_PLACEHOLDER) ||
      (n_data.protein < 0 && n_data.protein != STORAGE_PLACEHOLDER) ||
      (n_data.carbs < 0 && n_data.carbs != STORAGE_PLACEHOLDER) ||
      (n_data.fat < 0 && n_data.fat != STORAGE_PLACEHOLDER)) goto fetch_bad_n;

    // Check FitnessData; -3 is placeholder value
    if((f_data.steps < 0 && f_data.steps != STORAGE_PLACEHOLDER) ||
      (f_data.lap_time_seconds < 0 && f_data.lap_time_seconds != STORAGE_PLACEHOLDER) ||
      (f_data.mile_time_seconds < 0 && f_data.mile_time_seconds != STORAGE_PLACEHOLDER)) goto fetch_bad_f;

    // Check hours slept; -3 is placeholder value
    if(hours_slept < 0 && hours_slept != STORAGE_PLACEHOLDER) goto fetch_bad_s;

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
        /*version=*/1,
        /*status=*/OK,
        /*body=*/body
    );

    // ===================================================================== //

    fetch_bad_n:
    return IPCPResponse(
        /*version=*/1,
        /*status=*/DATA_NOT_FOUND,
        /*body=*/R"({"message": "Failed to retrieve nutrition data"})"_json
    );
    fetch_bad_f:
    return IPCPResponse(
        /*version=*/1,
        /*status=*/DATA_NOT_FOUND,
        /*body=*/R"({"message": "Failed to retrieve fitness data"})"_json
    );
    fetch_bad_s:
    return IPCPResponse(
        /*version=*/1,
        /*status=*/DATA_NOT_FOUND,
        /*body=*/R"({"message": "Failed to retrieve sleep data"})"_json
    );
} // IPCPResponse fetch()

IPCPResponse
fetch_wrapper(const IPCPRequest & req)
{
    if(GLOBAL userID_valid)
    {
        return IPCPResponse(
            /*version=*/1,
            /*status=*/USER_NOT_FOUND,
            /*body=*/R"({"message": "Invalid userID provided"})"_json
        );
    }

    const string * entry_date = req.body.at("entryDate").get_ptr<const string *>();
    if(!entry_date)
    {
        return IPCPResponse(
            /*version=*/1,
            /*status=*/BAD_REQUEST,
            /*body=*/R"({"message": "No entryDate specified in request"})"_json
        );
    }

    return fetch(req.userID, entry_date);
} // IPCPResponse fetch_wrapper()

IPCPResponse
login(const IPCPRequest & req)
{
    if(!storage.check_user_id(req.userID))
    {
        return IPCPResponse(
            /*version=*/1,
            /*status=*/USER_NOT_FOUND,
            /*body=*/R"({"message": "Invalid userID provided"})"_json
        );
    }

    GLOBAL userID_valid = true;
    return IPCPResponse(
        /*version=*/1,
        /*status=*/OK_NO_CONTENT
    );
} // IPCPResponse login()

int32_t
listen(void)
{
    // We'll jump out when ready to end.
    while(true)
    {
        IPCPRequest req = IPCP::read();
        IPCPResponse res(1, SERVER_ERROR); // Default value

        if(req.version != 1)
        {
            res = IPCPResponse(
                /*version=*/1,
                /*status=*/UNSUPPORTED_VERSION,
                /*body=*/R"({"message": "Server and Client have mismatched version numbers"})"_json
            );
            goto listen_exit;
        }

        switch(req.type)
        {
            case GET:
                res = fetch_wrapper(req);
                break;
            case PUT: // Fallthrough to UPDATE
            case UPDATE:
                // Do something
                break;
            case DELETE:
                // Do something
                break;
            case LOGIN:
                res = login(req);
                break;
            case LOGOUT:
                goto listen_exit;
        }

        IPCP::write(res);
    }

    listen_exit:
    return EXIT_SUCCESS;
}

int32_t
main(void)
{
    if(!IPCP::init())
    {
        IPCP::close();
        cerr << "Server: Failed to initialize IPCP" << endl;
        return EXIT_FAILURE;
    }

    if(!storage.initialize())
    {
        cerr << "Server: Failed to initialize database" << endl;
        return EXIT_FAILURE;
    }

    int32_t ret = listen();

    IPCP::close();
    return ret;
}