package ui;

import java.io.*;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;


// Usar para Testes : :1923 BACKUP C:\Users\Daniel\Desktop\Recrutamento.txt 3
public class TestApp {

    private static int port;
    private static String ipAddress;
    private static String filename;

    public static void main(String args[]){
        System.out.println("TestApp Started.");
        testAppStateMachine(args);
    }

    private static void testAppStateMachine(String args[]){
        String operation;
        int size;
        int repDegree;
        splitAP(args[0]);
        operation = args[1];
        switch(operation.toUpperCase()){
            case "BACKUP":
                System.out.println("Operation was " + operation);
                filename = args[2];
                repDegree =  Integer.parseInt(args[3]);
             //   breakFileToSend(filename);
                sendFile(breakFileToSend(filename));
                break;
            case "RESTORE":
                System.out.println("Operation was " + operation);
                filename = args[2];
                break;
            case "DELETE":
                System.out.println("Operation was " + operation);
                filename = args[2];
                break;
            case "RECLAIM":
                System.out.println("Operation was " + operation);
                size = Integer.parseInt(args[2]);
                break;
            case "STATE":
                System.out.println("Operation was " + operation);
                break;
            default:
                System.out.println("Wrong operation! - " + operation);
                break;
        }
    }
    static DatagramSocket socket;
    private static void sendFile(ArrayList<byte[]> listOfFiles){
        try {

            InetAddress address = InetAddress.getByName(ipAddress);
            for(int i = 0; i < listOfFiles.size(); i++) {
                DatagramPacket packet = new DatagramPacket(listOfFiles.get(i), listOfFiles.get(i).length, address, port);
                Date date = new Date();
                socket.send(packet);
                System.out.println(new Timestamp(date.getTime()) + " Já tá.");
            }

//            packet = new DatagramPacket(buf, buf.length);
//            socket.receive(packet);
//            String received = new String(packet.getData(), 0, packet.getLength());
//            System.out.println(received);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void splitAP(String args){
        if(args.contains(":")){
            String[] output = args.split("\\:");
            port = Integer.parseInt(output[1]);
            if( output[0].length()== 0){
                ipAddress = "localhost";
            }
            else {
                ipAddress = output[0];
            }
        }
        else {
            port = Integer.parseInt(args);
            ipAddress = "localhost";
        }
    }

    //TODO https://netjs.blogspot.com/2017/04/reading-all-files-in-folder-java-program.html

    // https://www.mkyong.com/java/how-to-get-file-size-in-java/
    // https://stackoverflow.com/questions/10864317/how-to-break-a-file-into-pieces-using-java
    private static ArrayList<byte[]> breakFileToSend(String filepath) {
        int partCounter = 1;//I like to name parts from 001, 002, 003, ...
        //you can change it to 0 if you want 000, 001, ...
        int sizeOfFiles = 64000;
        ArrayList<byte[]> listOfFiles = new ArrayList<>();
        byte[] buffer = new byte[sizeOfFiles];
        File file = new File(filepath);
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                listOfFiles.add(buffer);
                //write each chunk of data into separate file with different number in name
                String filePartName = String.format("%s Number: %03d", filepath, partCounter++);
                System.out.println(filePartName + " Size: " + bytesAmount);
            //    File newFile = new File(file.getParent(), filePartName);
//                try (FileOutputStream out = new FileOutputStream(newFile)) {
//                    out.write(buffer, 0, bytesAmount);
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(listOfFiles.get(listOfFiles.size() - 1).length == 64000){
            byte[] lastItem= new byte[0];
            listOfFiles.add(lastItem);
        }
        System.out.println("Ora bem: " + listOfFiles.size());
        return listOfFiles;
    }
}
