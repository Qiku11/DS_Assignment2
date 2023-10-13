CLASSPATH='Bytecode:.:jar/json.jar'
SERVER=localhost:4567

make stop

make clean

make all

java -cp $CLASSPATH PUTClient 127.0.0.1:4567 Weatherdata/1.txt

java -cp $CLASSPATH AggregationServer 4567 &
serverpid=$!

java -cp $CLASSPATH PUTClient $SERVER Weatherdata/3.txt &
putpid=$!

make stop