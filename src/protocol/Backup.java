package protocol;

import ui.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.sql.Timestamp;
import java.util.Date;

public class Backup implements Runnable{

    private int port;
    private InetAddress address;
    private int peerID;
    private Thread thread;
    private Peer peer;

    public Backup(Peer peer){
        System.out.println("Backup protocol called. ");

        this.address = peer.getMdbAddress();
        this.port = peer.getMdbPort();
        this.peerID = peer.getPeerID();
        this.peer = peer;
    }

    @Override
    public void run() {
        byte[] buf = new byte[256];

        try (MulticastSocket socket = new MulticastSocket(port)) {
            socket.joinGroup(address);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
             //   socket.setLoopbackMode(true);
                Date date = new Date();
                /* https://stackoverflow.com/questions/351565/system-currenttimemillis-vs-system-nanotime
                 * NanoTime is more expensive for the CPU.
                 * long startTime = System.nanoTime();
                 * long estimatedTime = System.nanoTime() - startTime;
                 */
                // https://www.mkyong.com/java/how-to-get-current-timestamps-in-java/
                String messageReceived= new String(packet.getData(), 0, packet.getLength());
                System.out.println("TimeStamp: " + new Timestamp(date.getTime()) +" ----- " + messageReceived);
                String[] splitString = messageReceived.trim().split("\\s+"); // Any number of consecutive spaces in the string are split into tokens.
                if (Integer.parseInt(splitString[2]) != this.peerID) {
                    switch (splitString[0]) {
                        case "PUTCHUNK":
                            System.out.println("Backup Message received.");
                            peer.sendStored(splitString[3]);
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
        System.out.println("Backup Thread has started.");
        if (thread == null) {
            thread = new Thread (this, "backupThread");
            thread.start();
            return thread;
        }
        return thread;
    }

}
