all: Code/PUTClient.class Code/AggregationServer.class

Code/PUTClient.class: Code/PUTClient.java
	javac Code/PUTClient.java

Code/AggregationServer.class: Code/AggregationServer.java
	javac Code/AggregationServer.java

clean:
	rm -f Code/*.class

run: Code/PUTClient.class Code/AggregationServer.class
	java Code.AggregationServer &
	java Code.PUTClient

# test: Code/PUTClient.class Code/AggregationServer.class
# 	./autotesting.sh