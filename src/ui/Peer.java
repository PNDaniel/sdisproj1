package ui;

import communication.Message;
import communication.Receiver;
import communication.Sender;
import protocol.Backup;

import java.io.IOException;
import java.net.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

public class Peer {

    private static Receiver receiver;
    private static Sender sender;
    private static int port;
    private static InetAddress ipAddress;
    private static int peerID;
    private int mcPort;
    private InetAddress mcAddress;
    private int mdbPort;
    private InetAddress mdbAddress;
    private int mdrPort;
    private InetAddress mdrAddress;
    private DatagramSocket socket;

    public static void main(String args[]) throws SocketException, UnknownHostException {
        double protocolVersion = Double.parseDouble(args[0]);

        splitAP(args[2]);
        Peer peer = new Peer(Integer.parseInt(args[1]),args[3],Integer.parseInt(args[4]) ,args[5], Integer.parseInt(args[6]), args[7], Integer.parseInt(args[8]));
    }

    private void startBackupChannel() throws SocketException {
        Backup backup = new Backup(mdbAddress, mdbPort);
        backup.start();
    }

    private void startControChannel(){

    }

    private void startRestoreChannel(){

    }

    public Peer(int peerID, String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort) throws SocketException, UnknownHostException {
        this.peerID = peerID;
        this.mcAddress =  InetAddress.getByName(mcAddress);
        this.mcPort = mcPort;
        this.mdbAddress =  InetAddress.getByName(mdbAddress);
        this.mdbPort = mdbPort;
        this.mdrAddress =  InetAddress.getByName(mdrAddress);
        this.mdrPort = mdrPort;

        System.out.println("Peer " + peerID + " has started.");

        startBackupChannel();
        startControChannel();
        startRestoreChannel();
      //  run();
//        Receiver receiver = new Receiver(mcAddress, mcPort);
//        receiver.start();
    }

//    public void backup(ArrayList<byte[]> listOfFiles, int repDeg) throws SocketException {
//        try (MulticastSocket socket = new MulticastSocket(port)) {
//            byte[] buf = new byte[65000];
//            InetAddress address = InetAddress.getByName(ipAddress);
//            socket.joinGroup(address);
//            for(int i = 0; i < listOfFiles.size(); i++) {
//                DatagramPacket packet = new DatagramPacket(listOfFiles.get(i), listOfFiles.get(i).length, address, port);
//                Date date = new Date();
//                socket.send(packet);
//                System.out.println(new Timestamp(date.getTime()) + " chunk was sent.");
//            }
//
//            DatagramPacket packet = new DatagramPacket(buf, buf.length);
//            socket.receive(packet);
//            String received = new String(packet.getData(), 0, packet.getLength());
//            System.out.println(received);
//        } catch (UnknownHostException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

    public void run() {
        System.out.println("I'm listening on " + ipAddress + ":" + port);
        // Create a buffer of bytes, which will be used to store
        // the incoming bytes containing the information from the server.
        // Since the message is small here, 256 bytes should be enough.
        byte[] buf = new byte[256];

        // Create a new Multicast socket (that will allow other sockets/programs
        // to join it as well.
        try (MulticastSocket clientSocket = new MulticastSocket(port)) {

            //Joint the Multicast group.
            clientSocket.joinGroup(ipAddress);

            while (true) {
                // Receive the information and print it.
                DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
                clientSocket.receive(msgPacket);

                String msg = new String(buf, 0, buf.length);
                msg = msg.replace("\r\n\r\n", " ");

                String[] msg_parts = msg.split(" ");

                if (!msg_parts[0].matches("BACKUP|RESTORE|DELETE|RECLAIM")) {
                    System.out.println("Received invalid order: " + msg_parts[0]);
                } else if (msg_parts[0].equals("BACKUP")) {
                    System.out.println(msg);
                    //this.getPeer().backup(msg_parts[1], Integer.parseInt(msg_parts[2]));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void splitAP(String args) throws UnknownHostException {
        String address;
        if(args.contains(":")){
            String[] output = args.split("\\:");
            port = Integer.parseInt(output[1]);
            if( output[0].length()== 0){
                address = "localhost";
            }
            else {
                address = output[0];
            }
        }
        else {
            port = Integer.parseInt(args);
            address = "localhost";
        }
        ipAddress = InetAddress.getByName(address);
        System.out.println("IpAddress: " +  address + " and Port Number is: " + port);
    }
}
