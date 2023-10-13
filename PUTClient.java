import java.io.*;
import java.net.*;
import org.json.JSONObject;

import common.LamportClock;

import java.util.ArrayList;



public class PUTClient {
    private static LamportClock myClock = new LamportClock();
    private Socket mySocket = null;
    private BufferedReader myBufferedReader = null;
    private DataOutputStream myOutputStream = null;
    private JSONObject myJSONObject = null;
    private String filePath = null;
    private String address = null;
    private int port = 4567;

    private static ArrayList<JSONObject> openFile(String filepath){
        ArrayList<JSONObject> resultList = new ArrayList<JSONObject>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            
            String line = "";
            String [] tokens;
            while (reader.ready()){
                // per data entry
                JSONObject currentdata = new JSONObject();
                if (!line.isEmpty()){
                    tokens = line.split(":",2);
                    currentdata.put(tokens[0], tokens[1]);
                }
                while (reader.ready()){
                    line = reader.readLine();
                    if (line.isEmpty()){
                        break;
                    }
                    tokens = line.split(":",2);
                    // check if tokens[0] is id
                    if (tokens[0].equals("id") || tokens[0].equals("ID")){
                        break;
                    }
                    currentdata.put(tokens[0], tokens[1]);
                }
                if (currentdata.length() > 0){
                    resultList.add(currentdata);
                }
                
            }
            
            reader.close();
            return resultList;
        } catch (IOException i) {
            System.out.println(i);
        }
        return null;
    }

    public void sendJSONs(ArrayList<JSONObject> jsonList) throws IOException{
        int length = jsonList.size();
        int retries = 0;
        for (int i = 0; i < length; i++){
            mySocket = new Socket(address, port);
            mySocket.setSoTimeout(5000);
            System.out.println("Connected");
            // try to send the packet, if return code is not 200 or 201, wait for 1 second and try again
            // if return code is 200 or 201, send the next packet
            JSONObject currentJSON = jsonList.get(i);
            myClock.tick();
            myClock.log("PUTClient: sending PUT");
            
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(mySocket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
            // construct and send the http PUT request
            String request = "PUT /weather.json HTTP/1.1\r\n" +
            "Content-Type: application/json\r\n" +
            "Content-Length: " + currentJSON.toString().length() + "\r\n" +
            "Lamport-Clock: " + myClock.getValue() + "\r\n";
            writer.write(request);
            writer.write(currentJSON.toString());
            writer.write("\r\n");
            writer.flush();
            // read the response
            try{
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try{
                String ret = reader.readLine();
                String [] split = ret.trim().split(" ", 3);
                int returnCode = Integer.parseInt(split[1]);

                while ((ret = reader.readLine()) != null && !ret.isEmpty()){
                    if (ret.startsWith("Lamport-Clock:")){
                        String [] tokens = ret.split(":", 2);
                        myClock.update(Integer.parseInt(tokens[1].trim()));
                        myClock.log("PUTClient: receive response");
                    }
                }

                if (returnCode != 200 && returnCode != 201){
                    i--;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                i--;
                retries++;
                if (retries > 3){
                    System.out.println("Connection lost");
                    break;
                }
                continue;
            } catch (NullPointerException n) {
                i--;
                retries++;
                if (retries > 3){
                    System.out.println("Connection lost");
                    break;
                }
                continue;
            }
            mySocket.close();
        }

    }

    public PUTClient(String address, int port, String filePath){
        this.address = address;
        this.port = port;
        this.filePath = filePath;
        ArrayList<JSONObject> jsonList = openFile(filePath);
        try {
            sendJSONs(jsonList);
        } catch (IOException i) {
            System.out.println(i);
        }
    }
    public static void main (String args[]){
        if (args.length < 2){
            System.out.println("not enough arguments...");
            return;
        }

        String address = args[0];
        String filepath = args[1];

        //strip the port number from the address URL
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

        PUTClient client = new PUTClient(address, port, filepath);
    }
}
