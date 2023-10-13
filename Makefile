all: Bytecode/PUTClient.class Bytecode/AggregationServer.class Bytecode/GETClient.class

Bytecode/PUTClient.class: PUTClient.java
	javac -d Bytecode -cp .:jar/json.jar PUTClient.java

Bytecode/AggregationServer.class: AggregationServer.java
	javac -d Bytecode -cp .:jar/json.jar AggregationServer.java

Bytecode/GETClient.class: GETClient.java
	javac -d Bytecode -cp .:jar/json.jar GETClient.java

clean:
	rm -f Bytecode/*.class
	rm -f Bytecode/common/*.class
	rm -f hot.txt cold.txt
	rm -f logs/*.*

testGET: Bytecode/GETClient.class Bytecode/AggregationServer.class
	./GETClientTest.sh

testPUT: Bytecode/PUTClient.class Bytecode/AggregationServer.class
	./PUTClientTest.sh

testAGG: Bytecode/AggregationServer.class
	./AggregationServerTest.sh

get:
	java -cp Bytecode:.:jar/json.jar GETClient 127.0.0.1:4567

put:
	java -cp Bytecode:.:jar/json.jar PUTClient 127.0.0.1:4567 Weatherdata/3.txt

agg:
	java -cp Bytecode:.:jar/json.jar AggregationServer

stop:
	killall java

# test: Code/PUTClient.class Code/AggregationServer.class
# 	./autotesting.sh