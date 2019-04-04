package communication;

import ui.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.sql.Timestamp;
import java.util.Date;

public class Receiver implements Runnable {

    private Thread thread;
    private Peer peer;
    private DatagramSocket socket;
    private boolean receiverIsRunning;

    public Receiver(Peer peer , InetAddress address, int port) throws SocketException {
        socket = new DatagramSocket(port);
        this.peer = peer;
    }

    @Override
    public void run() {
        receiverIsRunning = true;
        while (receiverIsRunning) {
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
                Date date = new Date();
                String messageReceived= new String(packet.getData(), 0, packet.getLength());
                System.out.println("TimeStamp: " + new Timestamp(date.getTime()) + " |TestAPP Order: " + messageReceived);
                String[] splitString = messageReceived.trim().split("\\s+"); // Any number of consecutive spaces in the string are split into tokens.
                byte[] returnString;
                DatagramPacket returnPacket;
                switch (splitString[0])
                {
                    case "BACKUP":
                        System.out.println("Backup Message received.");
                        returnString = "Backup, gz.".getBytes();
                        peer.sendPutchunk(splitString[1], Integer.parseInt(splitString[2]));
                        break;
                    case "RESTORE":
                        System.out.println("Restore Message received.");
                        returnString = "Restore, gz.".getBytes();
                        break;
                    case "DELETE":
                        System.out.println("Delete Message received.");
                        returnString = "Delete, gz.".getBytes();
                        break;
                    case "RECLAIM":
                        System.out.println("Reclaim Message received.");
                        returnString = "Reclaim, gz.".getBytes();
                        break;
                    default:
                        System.out.println("Unknown Message.");
                        returnString = "Wrong Message Format.".getBytes();
                        break;
                }
                returnPacket = new DatagramPacket(returnString, returnString.length, packet.getAddress(), packet.getPort());
             //   socket.send(returnPacket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        socket.close();
    }

    public Thread start() {
        System.out.println("Receiver Runnable opened and started.");
        if (thread == null) {
            thread = new Thread (this, "threadReceiver");
            thread.start();
            return thread;
        }
        return thread;
    }

    public String concatenateName(String[] splitString){
        if (splitString.length == 3)
            return splitString[2];
        else {
            //StringBuilder fullString = new StringBuilder(splitString[2]);
            String fullString = splitString[2];
            for (int i = 3; i < splitString.length; i++){
                fullString = fullString + " " + splitString[i];
                //fullString.append(" ").append(splitString[i]);
            }
            return fullString;
        }
    }
}
