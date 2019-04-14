package ui;

import communication.Message;
import communication.Receiver;
import model.Chunk;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Peer {

    private static int FOLDER_SIZE = 1000000;
    private int actualFolderSize;
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
    private final int TIME_INTERVAL = 1;
    private final int ATTEMPTS = 5;
 //   private ConcurrentHashMap<Integer, Integer> chunksSavedByPeers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Chunk> chunksSendingToPeers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Chunk> chunksSavedByPeers1 = new ConcurrentHashMap<>();
    public ArrayList<String> listOfChunksSendByPeers = new ArrayList<>();

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

    public void sendPutchunk(String filename, int repDeg) {
        AtomicInteger delay = new AtomicInteger(1);
        String hashedFileName = hashEncoder(filename);
        ConcurrentHashMap<Integer, byte[]> fileToSend = breakFileToSend2(filename);
        try (MulticastSocket socket = new MulticastSocket(mdbPort)) {
            socket.joinGroup(mdbAddress);
            Message msg = new Message("PUTCHUNK", 1.0, this.getPeerID(), hashedFileName);
            for (int chunkIterator = 0; chunkIterator < fileToSend.size(); chunkIterator++) {
                Chunk chunk = new Chunk(filename,chunkIterator, repDeg);
                addChunkToMap(chunk);
                int j = 0;
                if(chunksSavedByPeers1.get(chunkIterator).getActualDeg() <= chunksSavedByPeers1.get(chunkIterator).getDesRepDeg() ){
                    while (j < ATTEMPTS) {
                        //  System.out.println(j + " - Trying to send chunkNumber " + chunkIterator);
                        if (!getChunkStored(chunkIterator)) {
                            byte[] buf = fileToSend.get(chunkIterator);
                            byte[] msgToSend = msg.createPutchunkMessage(chunkIterator, repDeg, buf);
                            DatagramPacket packet = new DatagramPacket(msgToSend, msgToSend.length, mdbAddress, mdbPort);
                            ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(10);
                            ScheduledFuture future = scheduledThreadPoolExecutor.schedule(() -> {
                                try {
                                    socket.send(packet);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }, TIME_INTERVAL * delay.get(), TimeUnit.SECONDS);
                            future.get();
                        } else{
                            System.out.println("Peer already received stored for " + chunkIterator);
                            delay.updateAndGet(v -> v * 2);
                            break;
                        }
                        j++;
                    }
                    delay.set(1);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        File file = new File(filename);
        db.addFileToDatabase(filename, fileToSend.size(), file.length(), hashedFileName, repDeg);

     //   System.out.println(chunksSavedByPeers1);
        for (Map.Entry<Integer, Chunk> entry : chunksSavedByPeers1.entrySet()) {
            String key = entry.getKey().toString();
            Chunk value = entry.getValue();
            System.out.print("key, " + key + " Name: " + value.getFileName()
                    + " Chunk: " + value.getChunkNo()
                    + " ActDeg: " + value.getActualDeg()
                    + " Size: " + value.getChunkSize()
                    + " RepDeg: " + value.getDesRepDeg() + "\n"
                    + " Peer: ");
            for (int i = 0; i < value.getPeers().size() ; i++){
                System.out.print(value.getPeers().get(i) + " | ");
            }
            System.out.println("\n");
        }
    }

    // Executor Threads examples:
    // https://www.mkyong.com/java/java-scheduledexecutorservice-examples/
    // https://www.baeldung.com/java-executor-service-tutorial
    public void sendStored(String filename, int chunkNo){
        Message msg = new Message("STORED", 1.0,this.getPeerID(), filename);
        String msgToSend = msg.createStoredMessage(chunkNo);
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(5);
        Runnable task2 = () -> {
            try (MulticastSocket socket = new MulticastSocket(mcPort)) {
                socket.joinGroup(mcAddress);
                //DatagramPacket msgPacket = new DatagramPacket(msgToSend.getBytes(), msgToSend.getBytes().length, this.getIp(), this.getPort());
                DatagramPacket msgPacket = new DatagramPacket(msgToSend.getBytes(), msgToSend.getBytes().length, mcAddress, mcPort);
                socket.send(msgPacket);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        };
        ses.schedule(task2, new Random().nextInt(401), TimeUnit.MILLISECONDS);
        ses.shutdown();
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
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        Runnable task3 = () -> {
            if (!listOfChunksSendByPeers.contains(chunkNo)) {
                try (MulticastSocket socket = new MulticastSocket(mcPort)) {
                    socket.joinGroup(mcAddress);
                    Message msg = new Message("CHUNK", 1.0, this.getPeerID(), filename);
                    String filePath = this.getPeerFolder()+ "/" + filename + "_" + chunkNo;
                    if(new File(filePath).exists()){
                        byte[] buf = readChunk(filePath);
                        byte[] msgToSend = msg.createChunkMessage(chunkNo, buf);
                        DatagramPacket packet = new DatagramPacket(msgToSend, msgToSend.length, mdrAddress, mdrPort);
                        socket.send(packet);
                    } else {
                        System.out.println("I don't have this chunk. " + filename + " " + chunkNo);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                System.out.println("Estou aqui?");
            }
        };
        ses.schedule(task3, new Random().nextInt(401), TimeUnit.MILLISECONDS);
        ses.shutdown();
    }

    private byte[] readChunk(String filePath) throws IOException{
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

    public void reclaim(int space){
        File filepath = new File(this.getPeerFolder());
        File[] folder = filepath.listFiles();
        System.out.println(space);
        if (space < FOLDER_SIZE)
        {
            FOLDER_SIZE -= space;
            int tempFolderSize = getCurrentFolderSize();
            int i = 0;
            while (FOLDER_SIZE < tempFolderSize ){
                try (MulticastSocket socket = new MulticastSocket(mcPort)) {
                    if (i > folder.length) {
                        break;
                    }
                    socket.joinGroup(mcAddress);
                    String chunkName = folder[i].getName();
                    String[] splitString = chunkName.trim().split("_");
                    Message msg = new Message("REMOVED", 1.0,this.getPeerID(), splitString[0]);
                    String msgToSend =  msg.createRemovedMessage(Integer.parseInt(splitString[1]));
                    DatagramPacket msgPacket = new DatagramPacket(msgToSend.getBytes(), msgToSend.getBytes().length, mcAddress, mcPort);
                    System.out.println("Going to remove " + chunkName);
                    folder[i].delete();
                    socket.send(msgPacket);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                i++;
                tempFolderSize = getCurrentFolderSize();
            }
//            if (FOLDER_SIZE < tempFolderSize){
//                int i = 0;
//                // vai ficar com 14 itens
//                while (tempFolderSize > folder.length){
//                    try (MulticastSocket socket = new MulticastSocket(mcPort)) {
//                        socket.joinGroup(mcAddress);
//                        String chunkName = folder[i].getName();
//                        String[] splitString = chunkName.trim().split("_");
//                        Message msg = new Message("REMOVED", 1.0,this.getPeerID(), splitString[0]);
//                        String msgToSend =  msg.createRemovedMessage(Integer.parseInt(splitString[1]));
//                        DatagramPacket msgPacket = new DatagramPacket(msgToSend.getBytes(), msgToSend.getBytes().length, mcAddress, mcPort);
//                        System.out.println("Going to remove " + chunkName);
//                        folder[i].delete();
//                        socket.send(msgPacket);
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                    }
//                    i++;
//                    tempFolderSize = getCurrentFolderSize();
//                }
        }else {
            System.out.println("Reclaim number bigger than folder size.");
        }
        System.out.println(getCurrentFolderSize());
    }

    public int getCurrentFolderSize(){
        File folder = new File(this.getPeerFolder());
        File[] listOfFiles = folder.listFiles();
        int space = 0;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                space += listOfFiles[i].length();
            }
        }
        this.actualFolderSize = space;
        return actualFolderSize;
    }

    public void setStorage(int newStorage){
        FOLDER_SIZE = newStorage;
    }

    // https://netjs.blogspot.com/2017/04/reading-all-files-in-folder-java-program.html
    public void state(){
        File filepath = new File(this.getPeerFolder());
        File[] folder = filepath.listFiles();
        int folderSize = 0;
        for (File file : folder){
            System.out.println(file.getName() + " - Size in disk: " + file.length());
            folderSize += file.length();
        }
        System.out.println("Folder Size: " + folderSize + ", in " + folder.length + " files.") ;
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

    private static ConcurrentHashMap<Integer, byte[]>  breakFileToSend2(String filepath) {
        int partCounter = 1;
        int sizeOfFiles = 64000;
        ConcurrentHashMap<Integer, byte[]>  chunkList = new ConcurrentHashMap<>();
        byte[] buffer = new byte[sizeOfFiles];
        File file = new File(filepath);
        int count = 0;
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            if (fis.getChannel().size() % 64000 == 0){
                int bytesAmount = 0;
                while ((bytesAmount = bis.read(buffer)) > 0) {
                    chunkList.put(count, Arrays.copyOf(buffer,bytesAmount));
                    count++;
                    String filePartName = String.format("%s Number: %03d", filepath, partCounter++);
                    System.out.println(filePartName + " Size: " + bytesAmount);
                }
                byte[] lastItem= new byte[0];
                chunkList.put(++count, lastItem);

            } else {
                int bytesAmount = 0;
                while ((bytesAmount = bis.read(buffer)) > 0) {
                    chunkList.put(count, Arrays.copyOf(buffer,bytesAmount));
                    count++;
                    String filePartName = String.format("%s Number: %03d", filepath, partCounter++);
                    System.out.println(filePartName + " Size: " + bytesAmount);
                }
            }
        } catch (IOException e) {
            System.out.println("File name was incorrect. Check Path or filename.");
            e.printStackTrace();
        }
        System.out.println("Total Chunks: " + chunkList.size() + ", total size of file " + file.length() + " bytes.");
        return chunkList;
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

    public void addChunkToMap(Chunk chunk){
        Iterator<Integer> it = chunksSendingToPeers.keySet().iterator();
        if (chunksSendingToPeers.isEmpty()){
            chunksSendingToPeers.put(chunk.getChunkNo(), chunk);
            chunksSavedByPeers1.putAll(chunksSendingToPeers);
        } else {
            while(it.hasNext()){
                int key = it.next();
                if(! (key == chunk.getChunkNo())){
                    chunksSendingToPeers.put(chunk.getChunkNo(), chunk);
                    System.out.println("Criado o " + chunk.getChunkNo());
                    chunksSavedByPeers1.putAll(chunksSendingToPeers);
                    return;
                } else {
                    System.out.println("Ja tenho o " + chunk.getChunkNo());
                }
            }
        }

    }

    public void addChunkStored(int peer, int chunkNo){
        Chunk chunk = chunksSendingToPeers.get(chunkNo);
        chunk.addPeer(peer);
        //chunksSavedByPeers1.put(chunkNo, chunk);
        chunksSavedByPeers1.replace(chunkNo, chunk);
    }

//    public void addChunkStored1(int peer, int chunkNo){
//        Iterator<Integer> it = chunksSavedByPeers.keySet().iterator();
//        if (chunksSavedByPeers.isEmpty()){
//            chunksSavedByPeers.put(chunkNo, peer);
//        } else {
//            while(it.hasNext()){
//                int key = it.next();
//                if(! (key == chunkNo)){
//                    chunksSavedByPeers.put(chunkNo, peer);
//                    System.out.println("Peer " + peer + " guardou o " + chunkNo);
//                    return;
//                } else {
//                    System.out.println("Ja tenho o " + chunkNo + " no peer " + peer);
//                }
//            }
//        }
//    }

    private boolean getChunkStored(int chunkNo){
       // return chunksSavedByPeers1.containsKey(chunkNo);
        if (chunksSavedByPeers1.containsKey(chunkNo)){
            if (chunksSavedByPeers1.get(chunkNo).getPeers().size() != 0){
                return true;
            }
            else return false;
        }
        return false;
        // return chunksSavedByPeers1.containsKey(chunkNo);
    }

//    private boolean getChunkStored1(int chunkNo){
//        return chunksSavedByPeers.containsKey(chunkNo);
//    }
}
