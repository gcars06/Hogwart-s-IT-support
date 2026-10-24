# IPCP - Inter-Process Communication Protocol
This is a wire protocol to allow the Java frontend to communicate with the C++ backend.

## Basic usage - Java
```java
import org.java.JSONObject;

IPCP.init(); // Returns true on success, false on failure

IPCPRequest req = new IPCPRequest(
    IPCPRequest.Type.GET,     // type
    12345,                    // userID
    "{\"field\": \"login\"}"  // body (JSON structure not determined yet; you can also pass a JSONObject instead of a string)
);

IPCPResponse res = IPCP.send(req);
JSONObject res_body = new JSONObject(res.body);
System.out.println("Response code: " + res.code);
System.out.println("Response: " + res_body.toString());

IPCP.close();
```

## Basic usage - C++
```cpp
#include "path/to/IPCP.cpp"
#include <nlohmann/json>

IPCP::init(); // Returns true on success, false on failure

IPCPRequest req = IPCP::read();
nlohmann::json req_body = json::parse(req.body);
cout << "Request type: " << static_cast<int>(req.type) << endl;
cout << "Request: " << req_body.dump(4) << endl; // dump(n); n=# of spaces/indent

IPCPResponse res(OK_NO_CONTENT);
IPCP::write(res);

IPCP::close();
```

## Parameters - IPCPRequest
- **type** - The type of request: (GET, PUT, UPDATE, DELETE)
- **userID** - The userID of the relevant user
- **body** - JSON object detailing requested data; structure not determined yet

## Parameters - IPCPResponse
- **code** - Status code from server: (OK, OK_NO_CONTENT, BAD_REQUEST, USER_NOT_FOUND, DATA_NOT_FOUND, SERVER_ERROR, BAD_RESPONSE)
- **body** - JSON object containing requested data; structure not determined yet