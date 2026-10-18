CPP_CC   = g++
JAVA_CC  = javac
JAVA_INT = java

comp:
	$(CPP_CC) -o test_server server/unit_test.cpp
	$(JAVA_CC) -d . -cp client client/unit_test.java

run: comp
	./test_server &
	$(JAVA_INT) unit_test 1>/dev/null

clean:
	rm -f *.class test_server