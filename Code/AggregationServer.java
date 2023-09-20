package Code;

import java.io.*;
import java.net.*;

public class AggregationServer {
    private Socket mySocket = null;
    private ServerSocket server = null;
    // private DataInputStream myInputStream = null;
    private DataInputStream myBufferedReader = null;
    // private DataOutputStream myOutputStream = null;

    public AggregationServer(int port){
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");

            System.out.println("Waiting for a client ...");

            mySocket = server.accept();
            System.out.println("Client accepted");

            myBufferedReader = new DataInputStream(new BufferedInputStream(mySocket.getInputStream()));

            String line = "";

            while(!line.equals("Exit")){
                try {
                    // line = myInputStream.readUTF();
                    line = myBufferedReader.readUTF();
                    System.out.println(line);
                } catch(IOException i) {
                    System.out.println(i);
                }
            }

            System.out.println("Closing connection");

            // myInputStream.close();
            myBufferedReader.close();
            mySocket.close();
        } catch(IOException i) {
            System.out.println(i);
        }
    }
    public static void main (String args[]){
        AggregationServer server = new AggregationServer(5099);
    }
}