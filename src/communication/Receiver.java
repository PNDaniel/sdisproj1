package communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.sql.Timestamp;
import java.util.Date;

public class Receiver implements Runnable {

    private Thread thread;
    private DatagramSocket socket;
    private boolean receiverIsRunning;

    public Receiver(String address, int port) throws SocketException {
        System.out.println("Receiver opened.");
        socket = new DatagramSocket(port);
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
                /* https://stackoverflow.com/questions/351565/system-currenttimemillis-vs-system-nanotime
                 * NanoTime is more expensive for the CPU.
                 * long startTime = System.nanoTime();
                 * long estimatedTime = System.nanoTime() - startTime;
                 */
                // https://www.mkyong.com/java/how-to-get-current-timestamps-in-java/
                String messageReceived= new String(packet.getData(), 0, packet.getLength());
                System.out.println("TimeStamp: " + new Timestamp(date.getTime()) +" |Full String: " + messageReceived);
                String[] splitString = messageReceived.trim().split("\\s+"); // Any number of consecutive spaces in the string are split into tokens.
                // https://stackoverflow.com/questions/2220400/how-do-i-make-my-string-comparison-case-insensitive
                System.out.println("Full Name: " + concatenateName(splitString));
                byte[] returnString;
                DatagramPacket returnPacket;
                switch (splitString[0])
                {
                    case "BACKUP":
                        System.out.println("Backup Message received.");
                        returnString = "Backup, gz.".getBytes();
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
                socket.send(returnPacket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        socket.close();
    }

    public Thread start() {
        System.out.println("LOG: Receiver Runnable has started.");
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
