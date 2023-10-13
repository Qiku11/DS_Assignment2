import java.io.*;
import java.net.Socket;

import org.json.JSONObject;

import common.LamportClock;

public class GETClient {
    private static LamportClock myClock = new LamportClock();
// this method sends a request to the server and returns the response as a JSONObject
    private static JSONObject sendRequest(String address, int port, String request){
        int retries = 0;
        JSONObject result = null;
        while (true){
            try {
                Socket socket = new Socket(address, port);
                socket.setSoTimeout(5000);
                System.out.println("Connected");

                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                myClock.tick();
                myClock.log("GETClient: sending request");
                
                outputStream.writeUTF(request);

                String currentLine;
                if ((currentLine = input.readLine()) != null && !currentLine.isEmpty()) {
                    String [] split = currentLine.trim().split(" ", 3);
                    int returnCode = Integer.parseInt(split[1]);
                    if (returnCode == 200){
                        result = new JSONObject();
                        while ((currentLine = input.readLine()) != null && !currentLine.isEmpty()){
                            if (currentLine.startsWith("Lamport-Clock:")){
                                String [] tokens = currentLine.split(":", 2);
                                myClock.update(Integer.parseInt(tokens[1].trim()));
                                myClock.log("GETClient: receive response");
                                break;
                            }
                        }
                        while ((currentLine = input.readLine()) != null && !currentLine.isEmpty()){
                            if (currentLine.startsWith("{")){
                                JSONObject currentData = new JSONObject(currentLine);
                                
                                result = currentData;
                                break;
                            }
                        }
                        socket.close();
                        outputStream.close();
                        input.close();
                        break;
                    } else if (returnCode == 404){
                        System.out.println("File not found");
                        socket.close();
                        outputStream.close();
                        input.close();
                        return null;
                    } else {
                        System.out.println("Unknown error");
                        retries ++;
                        continue;
                    }
                } else {
                    retries ++;
                    continue;
                }
            } catch (IOException i) {
                System.out.println(i);
                retries++;
                if (retries > 3){
                    System.out.println("Connection failed");
                    return null;
                }
            }
        }
        return result;
    }
// this method prints a given json object to the console.
    public static void printjson(JSONObject json, String stationID){
        if (!stationID.equals("NULL")){
            // loop through all objects and check the id field of each object
            // if the id field matches the stationID, print the object

            JSONObject target = new JSONObject();
            for (String key : json.keySet()){
                if (((JSONObject) json.get(key)).has("id") && ((String) ((JSONObject) json.get(key)).get("id")).contains(stationID)){
                    target = (JSONObject) json.get(key);
                }
            }
            System.out.println(target.toString(4));
            return;
        }
        for (String key : json.keySet()){
            System.out.println((String)json.getJSONObject(key).toString(4));
            
        }
    }
// this method is the main method of the client, it takes in the address and stationID as arguments, the station ID is optional.
// GET client will read input from command line on start up, then send a GET request to aggregation server and display the retrieved data.
// if a station ID is specified, it will only filter out the corresponding data.
    public static void main (String args[]){
        if (args.length < 1){
            System.out.println("not enough arguments...");
            return;
        }

        String address = args[0];

        int port = 4567;
        if (address.contains(":")){
            String[] addressParts = address.split(":");
            if (addressParts.length < 3){
                address = addressParts[0];
                port = Integer.parseInt(addressParts[1]);
            } else if (addressParts.length == 3){
                address = addressParts[0] + ":" + addressParts[1];
                port = Integer.parseInt(addressParts[2]);
            } else {
                System.out.println("invalid address");
                return;
            }

        } else {
            System.out.println("invalid address");
            return;
        }
        String stationID = "NULL";
        if (args.length > 1){
            stationID = args[1];
        }

        String request = "GET /weather.json? HTTP/1.1\r\nLamport-Clock: " + myClock.getValue() + "\r\n\r\n";

        try{
            JSONObject response = sendRequest(address, port, request);
            if (response.length() == 0){
                System.out.println("failed to retrieve data");
                return;
            }
            printjson(response, stationID);
        } catch (Exception e){
            System.out.println("failed to retrieve data");
            System.out.println(e);
            return;
        }
    }
}
