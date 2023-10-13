CLASSPATH='Bytecode:.:jar/json.jar'
SERVER=localhost:4567

make stop

make clean

make all

java -cp $CLASSPATH GETClient $SERVER 1

java -cp $CLASSPATH AggregationServer 4567 &
serverpid=$!

java -cp $CLASSPATH PUTClient $SERVER Weatherdata/2.txt &
putpid=$!

java -cp $CLASSPATH GETClient $SERVER IDS60902

make stop