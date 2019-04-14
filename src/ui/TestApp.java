package ui;

import java.io.*;
import java.net.*;

// Usar para Testes :
// :1923 BACKUP C:\Users\Daniel\Downloads\application-pdf.pdf 1
// :1923 BACKUP C:\Users\Daniel\Desktop\Recrutamento.txt 3
// :1923 BACKUP C:\Users\Daniel\Downloads\Estatutos_2018_2019_Revisto.pdf 3
// :1923 BACKUP C:\Users\Daniel\Downloads\teste.txt 3

public class TestApp {

    private static Peer peer;
    private static int port;
    private static InetAddress ipAddress;
    private static String filename;
    private static DatagramSocket socket;

    public static void main(String args[]) throws SocketException, UnknownHostException {
        System.out.println("TestApp Started.");
        new TestApp(args);

    }

    private TestApp(String args[]) throws SocketException, UnknownHostException {
        testAppStateMachine(args);
    }

    private void testAppStateMachine(String args[]) throws SocketException, UnknownHostException {
        String operation;
        int size;
        int repDegree;
        splitAP(args[0]);
        operation = args[1];
        String initMsg;
        switch(operation.toUpperCase()){
            case "BACKUP":
                System.out.println("Operation was " + operation);
                filename = args[2];
                repDegree =  Integer.parseInt(args[3]);
                initMsg = "BACKUP " + filename + " " + repDegree;
                initiatePeer(initMsg);
                break;
            case "RESTORE":
                System.out.println("Operation was " + operation);
                filename = args[2];
                initMsg = "RESTORE " + filename;
                initiatePeer(initMsg);
                break;
            case "DELETE":
                System.out.println("Operation was " + operation);
                filename = args[2];
                initMsg = "DELETE " + filename;
                initiatePeer(initMsg);
                break;
            case "RECLAIM":
                System.out.println("Operation was " + operation);
                size = Integer.parseInt(args[2]);
                initMsg = "RECLAIM " + size;
                initiatePeer(initMsg);
                break;
            case "STATE":
                System.out.println("Operation was " + operation);
                initMsg = "STATE";
                initiatePeer(initMsg);
                break;
            case "STORAGE":
                System.out.println("Operation was " + operation);
                initMsg = "STORAGE";
                initiatePeer(initMsg);
                break;
            default:
                System.out.println("Wrong operation! - " + operation);
                break;
        }
    }

    private void initiatePeer(String msg) {
        // Open a new DatagramSocket, which will be used to send the data.
        try {
            DatagramSocket serverSocket = new DatagramSocket();
//            serverSocket.joinGroup(this.ipAddress);

            DatagramPacket msgPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, this.ipAddress, this.port);
            serverSocket.send(msgPacket);
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void splitAP(String args) throws UnknownHostException {
        String address;
        if(args.contains(":")){
            String[] output = args.split("\\:");
            port = Integer.parseInt(output[1]);
            if( output[0].length()== 0){
                address = "localhost";
            }
            else {
                address = output[0];
            }
        }
        else {
            port = Integer.parseInt(args);
            address = "localhost";
        }
        ipAddress = InetAddress.getByName(address);
        System.out.println("IpAddress: " +  address + " and Port Number is: " + port);
    }
}