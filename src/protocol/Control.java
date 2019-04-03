package protocol;

import ui.Peer;

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

    public Control(Peer peer){
        System.out.println("Control protocol called. ");

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
                System.out.println("TimeStamp: " + new Timestamp(date.getTime()) +" ----- " + messageReceived);
                String[] splitString = messageReceived.trim().split("\\s+"); // Any number of consecutive spaces in the string are split into tokens.
                if (Integer.parseInt(splitString[2]) != this.peerID) {
                    switch (splitString[0]) {
                        case "STORED":
                            System.out.println("Store Message received.");
                            break;
                        default:
                            System.out.println("Unknown Message.");
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Thread start() {
        System.out.println("Control Thread has started.");
        if (thread == null) {
            thread = new Thread (this, "controlThread");
            thread.start();
            return thread;
        }
        return thread;
    }

}
