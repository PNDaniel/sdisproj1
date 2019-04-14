package protocol;

import ui.Peer;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

public class Backup implements Runnable{

    private int port;
    private InetAddress address;
    private int peerID;
    private Thread thread;
    private Peer peer;

    public Backup(Peer peer){
        this.address = peer.getMdbAddress();
        this.port = peer.getMdbPort();
        this.peerID = peer.getPeerID();
        this.peer = peer;
    }

    @Override
    public void run() {
        byte[] buf = new byte[65000];

        try (MulticastSocket socket = new MulticastSocket(port)) {
            socket.joinGroup(address);
            while (true) {
                Date date = new Date();
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                //   socket.setLoopbackMode(true);
                socket.receive(packet);
                /* https://stackoverflow.com/questions/351565/system-currenttimemillis-vs-system-nanotime
                 * NanoTime is more expensive for the CPU.
                 * long startTime = System.nanoTime();
                 * long estimatedTime = System.nanoTime() - startTime;
                 */
                // https://www.mkyong.com/java/how-to-get-current-timestamps-in-java/

                String messageReceived = new String(packet.getData(), 0, packet.getLength());
                int delimiter = messageReceived.indexOf("\\r\\n\\r\\n") + 8;
                String[] splitString = messageReceived.trim().split("\\s+"); // Any number of consecutive spaces in the string are split into tokens.
                byte[] body = Arrays.copyOfRange(packet.getData(), delimiter, packet.getLength());
                if (Integer.parseInt(splitString[2]) != this.peerID) {
                    if(splitString[1].equals("1.0")){
                        switch (splitString[0]) {
                            case "PUTCHUNK":
                                System.out.println(new Timestamp(date.getTime())  + " - Backup Message received at " + address + ":" + port + " and it was: " + messageReceived.substring(0,84));
                                if((peer.getCurrentFolderSize() + body.length) <= peer.getFolderSize()) {
                                    if(createChunk(splitString[3],Integer.parseInt(splitString[4]),body)){
                                        peer.sendStored(splitString[3], Integer.parseInt(splitString[4]), Integer.parseInt(splitString[5]));
                                    } else {
                                        System.out.println("Create chunk failed for " + splitString[3] + "_" + splitString[4]);
                                    }
                                } else {
                                    System.out.println("Peer doesn't have space.");
                                }
                                break;
                            default:
                                System.out.println("Unknown Message in MDB.\n" + messageReceived);
                                break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Thread start() {
        System.out.println("Backup protocol called (at " + address +":"+ port + ") and its Thread has started. ");
        if (thread == null) {
            thread = new Thread (this, "backupThread");
            thread.start();
            return thread;
        }
        return thread;
    }

    public boolean  createChunk(String fileID, int chunkNo, byte[] data) {
        String filename = peer.getPeerFolder()+ "/" + fileID + "_" + chunkNo;
        File chunkfile = new File(filename);
        if (chunkfile.exists()) {
            return false;
        } else {
            try (FileOutputStream file = new FileOutputStream(filename)){
                file.write(data);
                file.close();
                return true;
            }  catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}
