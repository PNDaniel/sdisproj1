package protocol;

import ui.Peer;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.sql.Timestamp;
import java.util.Date;

public class Control implements Runnable {

    private int port;
    private InetAddress address;
    private int peerID;
    private Thread thread;
    private Peer peer;

    public Control(Peer peer){
        this.peer = peer;
        this.address = peer.getMcAddress();
        this.port = peer.getMcPort();
        this.peerID = peer.getPeerID();
    }

    @Override
    public void run() {
        byte[] buf = new byte[256];
        try (MulticastSocket socket = new MulticastSocket(port)) {
            socket.joinGroup(address);
         //   socket.setLoopbackMode(true);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                Date date = new Date();
                String messageReceived= new String(packet.getData(), 0, packet.getLength());
                String[] splitString = messageReceived.trim().split("\\s+"); // Any number of consecutive spaces in the string are split into tokens.
                if (Integer.parseInt(splitString[2]) != this.peerID) {
                    switch (splitString[0]) {
                        case "STORED":
                            System.out.println(new Timestamp(date.getTime())  + " - Store Message received at " + address  + ":" + port + " and it was :\n" + messageReceived.trim());
                            break;
                        case "DELETE":
                            System.out.println(messageReceived.trim());
                            checkFile(splitString[3]);
                            break;
                        default:
                            System.out.println("Unknown Message.\n" + messageReceived);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkFile(String fileID) {
        File[] listFiles = new File(peer.getPeerFolder()).listFiles();
        for (File listFile : listFiles) {
            if (listFile.isFile()) {
                String fileName = listFile.getName();
                if (fileName.startsWith(fileID)) {
                    System.out.println("found file" + " " + fileName);
                    File file = new File(peer.getPeerFolder()+"\\" + fileName);
                    System.out.println(file.getAbsolutePath());
                    file.delete();
                }
            }
        }
    }

    public Thread start() {
        System.out.println("Control protocol called (at " + address +":"+ port + ") and its Thread has started. ");
        if (thread == null) {
            thread = new Thread (this, "controlThread");
            thread.start();
            return thread;
        }
        return thread;
    }

}
