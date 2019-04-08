package ui;

import communication.Message;
import communication.Receiver;
import protocol.Backup;
import protocol.Control;
import protocol.Restore;
import utils.Database;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private Database db;

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

        db = new Database();
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
            File file = new File(filename);
            db.addFileToDatabase(filename,fileToSend.size(),file.length(),hashedFileName, repDeg);
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

    private int chunksToRestore;
    private String fileToRestore;
    private int initiatorPeer;

    public void setInitiatorPeer(int initiatorPeer) {
        this.initiatorPeer = initiatorPeer;
    }

    public int getInitiatorPeer() {
        return initiatorPeer;
    }


    public void sendGetChunk(String filename){
        try (MulticastSocket socket = new MulticastSocket(mcPort)) {
            socket.joinGroup(mcAddress);
            //  socket.setLoopbackMode(true);
            String hashedFileName = hashEncoder(filename);
            chunksToRestore = db.getFileChunksNumber(filename);
       //     chunkList = new ArrayList<>(chunksToRestore);
       //     list1 = new CopyOnWriteArrayList<>(chunkList);
            fileToRestore = filename;
            setInitiatorPeer(this.getPeerID());
            Message msg = new Message("GETCHUNK", 1.0,this.getPeerID(), hashedFileName);
            for (int i = 0; i < chunksToRestore; i++) {
                String msgToSend = msg.createGetChunkMessage(i);
                DatagramPacket msgPacket = new DatagramPacket(msgToSend.getBytes(), msgToSend.getBytes().length, mcAddress, mcPort);
                socket.send(msgPacket);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void sendChunk(String filename, int chunkNo){
        byte[] buf;
        try (MulticastSocket socket = new MulticastSocket(mcPort)) {
            socket.joinGroup(mcAddress);
            //  socket.setLoopbackMode(true);
            Message msg = new Message("CHUNK", 1.0,this.getPeerID(), filename);
            buf = readChunk(filename, chunkNo);
            byte[] msgToSend = msg.createChunkMessage(chunkNo, buf);
            DatagramPacket packet = new DatagramPacket(msgToSend, msgToSend.length, mdrAddress, mdrPort);
            socket.send(packet);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private byte[] readChunk(String filename, int chunkNo) throws IOException{
        String filePath = this.getPeerFolder()+ "/" + filename + "_" + chunkNo;
        File file = new File(filePath);
        byte[] body =  new byte[(int )file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(body);
        fis.close();
        return body;
    }

    private ConcurrentHashMap<Integer, byte[]> chunkList = new ConcurrentHashMap<>();
    public void buildFile(byte[] receivedBody, int chunkNo) throws IOException {
        if(chunkList.size() == 0){
            chunkList.put(chunkNo, receivedBody);
        }
        Iterator<Integer> it = chunkList.keySet().iterator();
        while(it.hasNext()){
            int key = it.next();
            if(! (key == chunkNo)){
                chunkList.put(chunkNo, receivedBody);
            }
        }
        if(chunkList.size() == chunksToRestore){
            System.out.println("Initiator Peer has all the chunks, restoring file. " + chunkList.size());
            FileOutputStream out = new FileOutputStream(fileToRestore);
            it = chunkList.keySet().iterator();
            while(it.hasNext()){
                int key = it.next();
                System.out.println("Writing chunk " + key);
                out.write(chunkList.get(key));
            }
            out.close();
            chunkList.clear();
        }
    }

    //   private ArrayList<byte[]> chunkList;
    // private List<byte[]> list1 = new CopyOnWriteArrayList<>();
    //   private CopyOnWriteArrayList<byte[]> list1 = new CopyOnWriteArrayList<>();
//    public void buildFile(byte[] receivedBody, int chunkNo){
//        if(!list1.contains(receivedBody)){
//            list1.addIfAbsent(receivedBody);
//            System.out.println("Added number " + chunkNo);
//        }else {
//            System.out.println("Already received chunk number " + chunkNo);
//        }
//        if (chunksToRestore == list1.size()){
//            FileOutputStream fos;
//            try {
//                fos = new FileOutputStream(fileToRestore,true);
//                for (int i = 0; i < chunksToRestore; i++){
//                    System.out.println("Writing chunk " + i);
//                    fos.write(list1.get(i));
//                    fos.flush();
//                }
//                fos.close();
//            }catch (Exception exception){
//                exception.printStackTrace();
//            }
//        }
//    }

//    public void buildFile(byte[] receivedBody, int chunkNo){
//
////        synchronized(list) {
////            for (byte[] o : list)
////            {
////
////            }
////        }
//        if(list1.contains(receivedBody)){
//            list1.add(chunkNo,receivedBody);
//        }else {
//            System.out.println("Already received chunk number " + chunkNo);
//        }
////        if(!chunkList.contains(receivedBody)){
////            chunkList.add(chunkNo,receivedBody);
////        } else {
////            System.out.println("Already received chunk number " + chunkNo);
////        }
//        if (chunksToRestore == chunkList.size()){
//            FileOutputStream fos;
//            try {
//                fos = new FileOutputStream(fileToRestore,true);
//                for (int i = 0; i < chunksToRestore; i++){
//                    System.out.println("Writing chunk " + i);
//                    fos.write(chunkList.get(i));
//                    fos.flush();
//                }
//                fos.close();
//            }catch (Exception exception){
//                exception.printStackTrace();
//            }
//        }
//    }

    public void delete(String filename){
        try (MulticastSocket socket = new MulticastSocket(mcPort)) {
            socket.joinGroup(mcAddress);
            //  socket.setLoopbackMode(true);
            String hashedFileName = hashEncoder(filename);
            Message msg = new Message("DELETE", 1.0,this.getPeerID(), hashedFileName);
            String msgToSend =  msg.createDeleteMessage();
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
        Restore restore = new Restore(this);
        restore.start();
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
