package protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.sql.Timestamp;
import java.util.Date;

public class Backup implements Runnable{

    private int port;
    private InetAddress address;
    private Thread thread;

    public Backup(InetAddress address, int port){
        System.out.println("Backup protocol called.");

        this.address = address;
        this.port = port;
    }

    @Override
    public void run() {
           byte[] buf = new byte[256];

        try (MulticastSocket socket = new MulticastSocket(port)) {
            socket.joinGroup(address);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                Date date = new Date();
                /* https://stackoverflow.com/questions/351565/system-currenttimemillis-vs-system-nanotime
                 * NanoTime is more expensive for the CPU.
                 * long startTime = System.nanoTime();
                 * long estimatedTime = System.nanoTime() - startTime;
                 */
                // https://www.mkyong.com/java/how-to-get-current-timestamps-in-java/
                String messageReceived= new String(packet.getData(), 0, packet.getLength());
                System.out.println("TimeStamp: " + new Timestamp(date.getTime()) +" |Full String: " + messageReceived);
                String[] splitString = messageReceived.trim().split("\\s+"); // Any number of consecutive spaces in the string are split into tokens.
                byte[] returnString;
                DatagramPacket returnPacket;
                switch (splitString[0])
                {
                    case "BACKUP":
                        System.out.println("Backup Message received.");
                        returnString = "Backup, gz.".getBytes();
                        break;
                    default:
                        System.out.println("Unknown Message.");
                        returnString = "Wrong Message Format.".getBytes();
                        break;
                }
                returnPacket = new DatagramPacket(returnString, returnString.length, packet.getAddress(), packet.getPort());
                socket.send(returnPacket);
            }
            //socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Thread start() {
        System.out.println("LOG: Backup Thread has started.");
        if (thread == null) {
            thread = new Thread (this, "backupThread");
            thread.start();
            return thread;
        }
        return thread;
    }

}
