import java.io.*;
import java.net.*;
import java.nio.file.*;

import org.json.JSONObject;

import common.LamportClock;

//aggregation server is the centre of the distributed system. It receives data from PUTClient (content servers) and GETClient simultaneously.
//it also does most of the synchronization as it is the bridge between the other two entities.
// it backs up the data to disk very frequently so it is robust to server failure.
public class AggregationServer {
    private int port = 4567;
    private static Path hotFile = Paths.get("hot.txt");
    private static Path coldFile = Paths.get("cold.txt");
    private static JSONObject storage = new JSONObject();
    private static JSONObject lastConneciton = new JSONObject();
    private static final Object lock = new Object();
    private LamportClock myClock = new LamportClock();
    ServerSocket serverSocekt = null;
    
//constructor of agg server class, sets port
    public AggregationServer(int port){
        this.port = port;
    }

// helper function to verify if a string is a valid json object
    public int checkValid(String data){
        
        try {
            JSONObject json = new JSONObject(data);
            if (json.has("id")){
                return 1;
            } else {
                return 0;
            }
        } catch (Exception e){
            // System.out.println(e);
            return 0;
        }

    }
// function to check if there is leftover data on the disk, and recover it if so.
    public void recover(){
        if (Files.isReadable(hotFile) && Files.isReadable(coldFile)){
            try{
                long hotTime = Files.getLastModifiedTime(hotFile).toMillis();
                long coldTime = Files.getLastModifiedTime(coldFile).toMillis();

                if (hotTime > coldTime){
                    String data = new String(Files.readAllBytes(hotFile));
                    if (checkValid(data) == 1){
                        Files.copy(hotFile, coldFile,StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        if (checkValid(new String(Files.readAllBytes(coldFile))) == 1){
                            Files.copy(coldFile, hotFile,StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.delete(hotFile);
                            Files.delete(coldFile);
                            Files.createFile(hotFile);
                            Files.createFile(coldFile);
                            return;
                        }
                    }
                    storage = new JSONObject(data);
                } else {
                    String data = new String(Files.readAllBytes(coldFile));
                    if (checkValid(data) == 1){
                        Files.copy(coldFile, hotFile,StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        if (checkValid(new String(Files.readAllBytes(hotFile))) == 1){
                            Files.copy(hotFile, coldFile,StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.delete(hotFile);
                            Files.delete(coldFile);
                            Files.createFile(hotFile);
                            Files.createFile(coldFile);
                            return;
                        }
                    }
                    storage = new JSONObject(data);
                }
            } catch (IOException i){
            }

        } else if (Files.isReadable(hotFile)){
            try{
                String data = new String(Files.readAllBytes(hotFile));
                if (checkValid(data) == 1){
                    try {
                        Files.copy(hotFile, coldFile);
                        this.storage = new JSONObject(data);
                    } catch (IOException i){
                        //System.out.println(i);
                    }
                } else {
                    try {
                        Files.delete(hotFile);
                    } catch (IOException i){
                        //System.out.println(i);
                    }
                }
            } catch (IOException i){
                //System.out.println(i);
            }

        } else if (Files.isReadable(coldFile)){
            try{
                String data = new String(Files.readAllBytes(coldFile));
                if (checkValid(data) == 1){
                    try {
                        Files.copy(coldFile, hotFile);
                        this.storage = new JSONObject(data);
                    } catch (IOException i){
                        //System.out.println(i);
                    }
                } else {
                    try {
                        Files.delete(coldFile);
                    } catch (IOException i){
                        //System.out.println(i);
                    }
                }
            } catch (IOException i){
                //System.out.println(i);
            }
        } else {
            System.out.println("No previous file");
        }
        try {
            Files.createFile(hotFile);
            Files.createFile(coldFile);
        } catch (IOException i){
            System.out.println(i);
        }
    }
// mutex function to save data into the critical variables.
    public synchronized void storeData(JSONObject data){
        synchronized (storage){
            storage.put(data.getString("id"), data);
            synchronized (lastConneciton){
                lastConneciton.put(data.getString("id"), System.currentTimeMillis());
            }
        }
    }
// mutex function to copy the content of hotfile to cold file (double back up in case server failure during file IO)
    public synchronized void bakcup(){
        // copy the content of hotfile to cold file
        try {
            Files.copy(hotFile, coldFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException i){
            System.out.println(i);
        }
    }
// mutex function to save the content of storage to hotfile
    public synchronized void save(){
        // save the content of storage to hotfile
        try {
            Files.write(hotFile, this.storage.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException i){
            System.out.println(i);
        }
    }
// function to start the server
    public void start() throws IOException {
        serverSocekt = new ServerSocket(port);
        System.out.println("Server started on port: " + port);

        while (true){
            Socket clientSocket = null;
            try {
                clientSocket = serverSocekt.accept();
                System.out.println("A new client is connected: " + clientSocket);
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                System.out.println("Assigning new thread for this client");
                Thread t = new ClientHandler(clientSocket, input, output, this);
                t.start();
            } catch (Exception e){
                clientSocket.close();
                e.printStackTrace();
            }
        }
    }
// independent function to periodically check and purge outdated data.
    private static void janitor() {
        while (true){
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // remove all the data that's older than 5 minutes
            long currentTime = System.currentTimeMillis();
            // a json array called loser

            synchronized (storage) {
                for (String key : lastConneciton.keySet()) {
                    if (currentTime - lastConneciton.getLong(key) > 30000) {
                        storage.remove(key);
                        synchronized(lastConneciton){
                            lastConneciton.remove(key);
                        }
                    }
                }
            }
        }
        //sleep for 10 seconds
        
    }
// worker function for multi connection handling.
    private static class ClientHandler extends Thread {
        private Socket mySocket;
        private AggregationServer server;
        private BufferedReader input;
        private BufferedWriter output;

        public ClientHandler(Socket clientSocket, BufferedReader input, BufferedWriter output, AggregationServer server){
            this.mySocket = clientSocket;
            this.server = server;
            this.input = input;
            this.output = output;
        }

        @Override
        public void run(){
            int retries = 0;
            try {
                String currentLine;
                if ((currentLine = input.readLine()) != null && !currentLine.isEmpty()) {
                    String [] split = currentLine.trim().split(" ", 3);
                    String method = split[0];
                    if (method.matches(".*GET.*")){
                        JSONObject result = new JSONObject();
                        while ((currentLine = input.readLine()) != null && !currentLine.isEmpty()){
                            if (currentLine.startsWith("Lamport-Clock:")){
                                String [] tokens = currentLine.split(":", 2);
                                server.myClock.update(Integer.parseInt(tokens[1].trim()));
                                server.myClock.log("Agg: receive GET");
                                break;
                            }
                        }
                        result = AggregationServer.storage;
                        server.myClock.tick();
                        server.myClock.log("Agg: send GET response");
                        output.write("HTTP/1.1 200 OK\r\n");
                        output.write("Content-Type: application/json\r\n");
                        output.write("Content-Length: " + result.toString().length() + "\r\n");
                        output.write("Lamport-Clock: " + server.myClock.getValue() + "\r\n");
                        output.write(result.toString());
                        output.write("\r\n");
                        output.flush();
                        mySocket.close();
                        output.close();
                        input.close();
                    } else if (method.matches(".*PUT.*")){
                        int code = 204;

                        while ((currentLine = input.readLine()) != null && !currentLine.isEmpty()){
                            if (currentLine.startsWith("Lamport-Clock:")){
                                String [] tokens = currentLine.split(":", 2);
                                server.myClock.update(Integer.parseInt(tokens[1].trim()));
                                server.myClock.log("Agg: receive PUT");
                                break;
                            }
                        }
                        JSONObject currentData = new JSONObject();
                        while ((currentLine = input.readLine()) != null && !currentLine.isEmpty()){
                            if (currentLine.startsWith("{")){
                                code = 200;
                                currentData = new JSONObject(currentLine);
                                if (currentData.length() > 1){
                                    if (!AggregationServer.storage.has(currentData.getString("id"))){
                                        code = 201;
                                    }
                                    server.storeData(currentData);
                                    break;
                                }
                            }
                        }
                        server.myClock.tick();
                        server.myClock.log("Agg: send PUT response");
                        output.write("HTTP/1.1 " + code +" OK\r\n");
                        output.write("Content-Type: application/json\r\n");
                        output.write("Content-Length: " + currentData.toString().length() + "\r\n");
                        output.write("Lamport-Clock: " + server.myClock.getValue() + "\r\n");
                        output.write("\r\n");
                        output.write(currentData.toString());
                        output.write("\r\n");
                        output.flush();
                        server.save();
                        server.bakcup();
                    } else {
                        server.myClock.tick();
                        server.myClock.log("Agg: send 400 response");
                        output.write("HTTP/1.1 400 Bad Request\r\n");
                        output.write("\r\n");
                        output.flush();
                    }

                } else {
                    server.myClock.tick();
                    server.myClock.log("Agg: send 400 response");
                    output.write("HTTP/1.1 400 Bad Request\r\n");
                    output.write("\r\n");
                    output.flush();
                }
                
            } catch (IOException i) {
                System.out.println(i);
            }
        }
    }
// main function to start the server
    public static void main (String args[]){
        int port = 4567;
        if (args.length > 0){
            port = Integer.parseInt(args[0]);
        }
        AggregationServer server = new AggregationServer(port);

        server.recover();
        Thread janitor = new Thread(new Runnable(){
            @Override
            public void run(){
                janitor();
            }
        });
        janitor.start();
        try {
            server.start();
        } catch (IOException i){
            System.out.println(i);
        }

    }
}