# DS_Assignment2
Assignment 2 of Distributed Systems at the University of Adelaide. A system consisted of content servers, get clients and an aggregation server


# Usage:
make all
    this compiles the code

make clean
    this deletes all bytecode files

make stop
    this kills all running java processes


all compiled codes are in Bytecode/
the imported external json library is in jar/

PUTClientTest.sh GETClientTest.sh and AGGTest.sh are the three integration testing scripts I wrote.



# components and their respective behaviours

# aggregation server
everytime it receives new data, it saves all information to hot.txt and then do a replicate backup in cold.txt

on start up, it checks if hot.txt and/or cold.txt exist and is valid. If so, recover saved state of the server from the newer file that is valid. (there would at least be one valid file)

a dedicated thread is allocated to scan all saved data every 30 seconds and purge all data that's outdated.

a new thread is created whenever a new connection request is made to the aggregation server. (can be PUT or GET)

# GET client

the get client receives command-line input on start up of the server information.

It then sends a single get request to the aggregation server and wait for its response.

If this process fails, it waits for 1 second and retries, until it can receive a 200 ok response.

It would then parse and display the received data. The specific behaviour depends on whether the user provided a station ID on startup. if not, all data is to be displayed. If there is a stationID, data of that specific weather station is fitered and displayed.

# PUT client (content server)

the put client starts up with a command line argument providing a file path for it to retrive data from.

It then open, read, and parse the data from that file, change it to json format and send over to aggregation server.

Every entry takes one independent connection. it doesn't send multiple weather data in one go.

it is multi-threaded, a dedicated thread is spawned on start up to answer heart-beat packets from the aggregation server.

when package sending fails, it waits for 5 seconds and tries again. until the retry limit of 3 times is reached, then it prints an error message and quits.

# Common/Lamport clock

this file implements the lamport clock behaviour, which is used across all three entities in my system.
the lamport clock class implements crucial distributed mutexes for tie-breaking when concurrency issues arise.


# Testing structure

Since these programs only work when connected to each other, there is not much I can do in unit testing.

runAGG.sh runGET.sh runPUT.sh allow you to start up the servers and clients quickly.

The purpose of these script is to verify the interaction inside these programs. corresponding logs are to be generated for in depth inspection inside folder logs/

# logging in lamport clock class

since debugging is heavily related to lamport clocks. I added some logging functionalities to the lamport clock class such that each time the class gets called I can use it to generate a new entry into a log file.

Also, each time a new lamport clock object is to be created, a unique log file is generated in the process, this way, throughout my testing I will have many logs to collect and debug with.

My logs are in the folder "logs" and they are in the following format: eventname + lamport value.

the entities will each have a log.