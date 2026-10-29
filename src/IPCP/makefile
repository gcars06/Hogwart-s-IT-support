CPP_CC   = g++
JAVA_CC  = javac
JAVA_INT = java

JAVA_FLAGS = -cp ".:client:../../lib/json-java/json.jar"

comp:
	$(CPP_CC) -o test_server -I ../../lib/json-cpp/include server/unit_test.cpp
	$(JAVA_CC) $(JAVA_FLAGS) -d . client/unit_test.java

server: comp
	./test_server &
	$(JAVA_INT) $(JAVA_FLAGS) unit_test 1>/dev/null

client: comp
	./test_server 1>/dev/null &
	$(JAVA_INT) $(JAVA_FLAGS) unit_test

clean:
	rm -f *.class client/*.class test_server