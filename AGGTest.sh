CLASSPATH='Bytecode:.:jar/json.jar'
SERVER=localhost:4567

make stop

make clean

make all

java -cp $CLASSPATH AggregationServer 4567 &
agpid=$!

java -cp $CLASSPATH PUTClient $SERVER Weatherdata/2.txt &
putpid=$!

kill -k $agpid

echo: "server down!"

java -cp $CLASSPATH GETClient $SERVER IDS60902

java -cp $CLASSPATH AggregationServer 4567 &

echo "server has recovered"

java -cp $CLASSPATH GETClient $SERVER
