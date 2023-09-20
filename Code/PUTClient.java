package Code;

import java.io.*;
import java.net.*;
import org.json.JSONObject;

public class PUTClient {
    private Socket mySocket = null;
    // private DataInputStream myInputStream = null;
    private BufferedReader myBufferedReader = null;
    private DataOutputStream myOutputStream = null;
    private JSONObject myJSONObject = null;

    public PUTClient(String address, int port){
        try {
            mySocket = new Socket(address, port);
            System.out.println("Connected");

            // myInputStream = new DataInputStream(System.in);
            myBufferedReader = new BufferedReader(new InputStreamReader(System.in));

            myOutputStream = new DataOutputStream(mySocket.getOutputStream());
        } catch(UnknownHostException u) {
            System.out.println(u);
        } catch(IOException i) {
            System.out.println(i);
        }

        String line = "";

        while(!line.equals("Exit")){
            try {
                // line = myInputStream.readLine();
                line = myBufferedReader.readLine();
                myJSONObject = new JSONObject(line);
                // myOutputStream.writeUTF(line);
            } catch (IOException i) {
                System.out.println(i);
            }
        }

        try {
            myBufferedReader.close();
            myOutputStream.close();
            mySocket.close();
        } catch(IOException i) {
            System.out.println(i);
        }
    }
    public static void main (String args[]){
        PUTClient client = new PUTClient("127.0.0.1", 5099);
    }
}
