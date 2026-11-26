CPP_CC   = g++
JAVA_CC  = javac
JAVA_INT = java
JAVA_RELEASE ?= 25

JAVA_FLAGS = -cp ".:../../lib/json-java/json.jar"

comp:
	$(CPP_CC) -o test_server -I ../../lib/json-cpp/include server/unit_test.cpp
	$(JAVA_CC) --release $(JAVA_RELEASE) $(JAVA_FLAGS) -d . client/*.java

server: comp
	./test_server &
	$(JAVA_INT) $(JAVA_FLAGS) IPCP.client.unit_test 1>/dev/null

client: comp
	./test_server 1>/dev/null &
	$(JAVA_INT) $(JAVA_FLAGS) IPCP.client.unit_test

clean:
	rm -f *.class client/*.class test_server
	rm -f IPCP/client/*.class