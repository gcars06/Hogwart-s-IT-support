# IPCP - Inter-Process Communication Protocol
This is a wire protocol to dictate communication between the Java frontend and the C++ backend. This protocol uses network byte order, so multi-byte integers are big-endian.

## IPCPRequest packet envelope:
+---------+------+-----------+
|        1|     1|          4|-Bytes
+---------+------+-----------+
| version | type |    userID |-Header
+---------+------+-----------+
| body_len(i32) + body(json) |
+----------------------------+

## IPCPResponse packet envelope:
+---------+------------------+
|        1|                 1|-Bytes
+---------+------------------+
| version |           status |-Header
+---------+------------------+
| body_len(i32) + body(json) |
+----------------------------+

## Example sequence
+----------+   +------+             +---------+
| Frontend |   | IPCP |             | Backend |
+----------+   +------+             +---------+
     ||           ||                    ||
     ||Login      ||                    ||
     ||==========>||                    ||
     ||userID     ||IPCPRequest         ||
     ||---------->||===================>||
     ||           ||type:LOGIN          ||
     ||           ||userID              ||
     ||           ||body:{}             ||
     ||           ||------------------->||
     ||           ||                    ||
     ||           ||        IPCPResponse||
     ||           ||<===================||
     ||           ||status:OK_NO_CONTENT||
     ||           ||             body:{}||
     ||  Logged in||<-------------------||
     ||<==========||
     ||
     \/