package ui;

import communication.Message;
import communication.Receiver;
import protocol.Backup;
import protocol.Control;

import javax.xml.crypto.Data;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class Peer {

    private static int peerPort;
    private static InetAddress peerAddress;
    private static int peerID;
    private String peerFolder;
    private int mcPort;
    private InetAddress mcAddress;
    private int mdbPort;
    private InetAddress mdbAddress;
    private int mdrPort;
    private InetAddress mdrAddress;

    public static void main(String args[]) throws SocketException, UnknownHostException {
        double protocolVersion = Double.parseDouble(args[0]);

        splitAP(args[2]);
        new Peer(Integer.parseInt(args[1]),args[3],Integer.parseInt(args[4]) ,args[5], Integer.parseInt(args[6]), args[7], Integer.parseInt(args[8]));
    }

    public Peer(int peerID, String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort) throws SocketException, UnknownHostException {
        this.peerID = peerID;
        this.mcAddress =  InetAddress.getByName(mcAddress);
        this.mcPort = mcPort;
        this.mdbAddress =  InetAddress.getByName(mdbAddress);
        this.mdbPort = mdbPort;
        this.mdrAddress =  InetAddress.getByName(mdrAddress);
        this.mdrPort = mdrPort;

        peerFolder = "Peer" + peerID;
        new File(peerFolder).mkdirs();

        System.out.println("Peer " + peerID + " has started and folder Peer " + peerID +" created. Address and Port are: " + peerAddress.getHostName() + ":"+ peerPort);
        Receiver testAppReceiver = new Receiver(this, peerAddress, peerPort);
        testAppReceiver.start();
        startBackupListener();
        startControlListener();
        startRestoreListener();
    }

    public void sendPutchunk(String filename, int repDeg){
        byte[] buf = new byte[64000];
        ArrayList<byte[]> fileToSend = breakFileToSend(filename);
        String hashedFileName = hashEncoder(filename);
        try (MulticastSocket socket = new MulticastSocket(mdbPort)) {
            socket.joinGroup(mdbAddress);
            //  socket.setLoopbackMode(true);
            Message msg = new Message("PUTCHUNK", 1.0,this.getPeerID(), hashedFileName );
            for (int i = 0; i < fileToSend.size() ; i++){
                buf = fileToSend.get(i);
                byte[] msgToSend = msg.createPutchunkMessage(i, repDeg, buf);
                DatagramPacket packet = new DatagramPacket(msgToSend, msgToSend.length, mdbAddress, mdbPort);
                socket.send(packet);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void sendStored(String filename, int chunkNo){
        try (MulticastSocket socket = new MulticastSocket(mcPort)) {
            socket.joinGroup(mcAddress);
          //  socket.setLoopbackMode(true);
            Message msg = new Message("STORED", 1.0,this.getPeerID(), filename);
            String msgToSend = msg.createStoredMessage(chunkNo);
            //DatagramPacket msgPacket = new DatagramPacket(msgToSend.getBytes(), msgToSend.getBytes().length, this.getIp(), this.getPort());
            DatagramPacket msgPacket = new DatagramPacket(msgToSend.getBytes(), msgToSend.getBytes().length, mcAddress, mcPort);
            socket.send(msgPacket);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // https://stackoverflow.com/questions/5531455/how-to-hash-some-string-with-sha256-in-java
    private String hashEncoder (String filename) {
        byte[] hashedName = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hashedName = digest.digest(filename.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return toHexString(hashedName);
    }

    // https://stackoverflow.com/questions/332079/in-java-how-do-i-convert-a-byte-array-to-a-string-of-hex-digits-while-keeping-l
    public static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    //TODO https://netjs.blogspot.com/2017/04/reading-all-files-in-folder-java-program.html

    // https://www.mkyong.com/java/how-to-get-file-size-in-java/
    // https://stackoverflow.com/questions/10864317/how-to-break-a-file-into-pieces-using-java
    private static ArrayList<byte[]> breakFileToSend(String filepath) {
        int partCounter = 1;
        int sizeOfFiles = 64000;
        ArrayList<byte[]> listOfFiles = new ArrayList<>();
        byte[] buffer = new byte[sizeOfFiles];
        File file = new File(filepath);
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                listOfFiles.add(Arrays.copyOf(buffer, bytesAmount));
                String filePartName = String.format("%s Number: %03d", filepath, partCounter++);
                System.out.println(filePartName + " Size: " + bytesAmount);
            }
        } catch (IOException e) {
            System.out.println("File name was incorrect. Check Path or filename.");
            e.printStackTrace();
        }
        if(listOfFiles.get(listOfFiles.size() - 1).length == 64000){
            byte[] lastItem= new byte[0];
            listOfFiles.add(lastItem);
        }
        System.out.println("Total Chunks: " + listOfFiles.size() + ", total size of file " + file.length() + " bytes.");
        return listOfFiles;
    }

    private static void splitAP(String args) throws UnknownHostException {
        String address;
        if(args.contains(":")){
            String[] output = args.split("\\:");
            peerPort = Integer.parseInt(output[1]);
            if( output[0].length()== 0){
                address = "localhost";
            }
            else {
                address = output[0];
            }
        }
        else {
            peerPort = Integer.parseInt(args);
            address = "localhost";
        }
        peerAddress = InetAddress.getByName(address);
        System.out.println("IpAddress: " +  address + " and Port Number is: " + peerPort);
    }

    private void startBackupListener() throws SocketException {
        Backup backup = new Backup(this);
        backup.start();
    }

    private void startControlListener(){
        Control control = new Control(this);
        control.start();
    }

    private void startRestoreListener(){

    }

    public int getPeerID() {
        return peerID;
    }

    public String getPeerFolder(){
        return  peerFolder;
    }

    public int getMcPort() {
        return mcPort;
    }

    public InetAddress getMcAddress() {
        return mcAddress;
    }

    public int getMdbPort() {
        return mdbPort;
    }

    public InetAddress getMdbAddress() {
        return mdbAddress;
    }

    public int getMdrPort() {
        return mdrPort;
    }

    public InetAddress getMdrAddress() {
        return mdrAddress;
    }
}
