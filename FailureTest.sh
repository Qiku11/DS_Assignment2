./runAGG.sh
aggid=$!

./runPUT.sh

# loop five times starting five different get clients on the background using ./runGET.sh
for i in {1..5}
do
    ./runGET.sh
done

kill -kill $aggid

./runAGG.sh

java -cp Bytecode:.:jar/json.jar PUTClient 127.0.0.1:4567 Weatherdata/2.txt