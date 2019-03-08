package communication;

import javax.sound.sampled.Port;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Date;

public class Sender implements Runnable {

    private Thread thread;
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private boolean senderIsRunning;

    public Sender(){
        System.out.println("Sender prepared.");
    }

    @Override
    public void run() {
        senderIsRunning = true;
        byte[] buf;
        String ipAddress = "localhost";
        buf = "cenas".getBytes();
        while (senderIsRunning) {
            try {
                address = InetAddress.getByName(ipAddress);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                Date date = new Date();
                socket.send(packet);
                System.out.println(new Timestamp(date.getTime()) + " Já tá.");
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println(received);
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public Thread start() {
        System.out.println("LOG: Sender Runnable has started.");
        if (thread == null) {
            thread = new Thread (this, "threadSender");
            thread.start();
            return thread;
        }
        return thread;
    }
}
