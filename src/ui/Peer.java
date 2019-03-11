package ui;

import communication.Receiver;
import communication.Sender;
import protocol.Backup;

import java.net.SocketException;

public class Peer {

    private static Receiver receiver;
    private static Sender sender;
    private static int port;
    private static String ipAddress;
    private static int peerID;
    private static int mcPort;
    private static String mcAddress;
    private static int mdbPort;
    private static String mdbAddress;
    private static int mdrPort;
    private static String mdrAddress;

    public static void main(String args[]){
        int protocolVersion = Integer.parseInt(args[0]);
        peerID = Integer.parseInt(args[1]);
        System.out.println("Peer " + peerID + " has started.");
        splitAP(args[2]);
        mcAddress = args[3];
        mcPort = Integer.parseInt(args[4]);
        mdbAddress = args[5];
        mdbPort = Integer.parseInt(args[6]);
        mdrAddress = args[7];
        mdrPort = Integer.parseInt(args[8]);
    }

    private static void startBackupChannel() throws SocketException {
       // Backup backup = new Backup(mdbAddress, mdbPort);

    }

    private static void startControChannel(){

    }

    private static void startRestoreChannel(){

    }

    public void Peer() throws SocketException {
        startBackupChannel();
        startControChannel();
        startRestoreChannel();
    }

    private static void splitAP(String args){
        if(args.contains(":")){
            String[] output = args.split("\\:");
            port = Integer.parseInt(output[1]);
            if( output[0].length()== 0){
                ipAddress = "localhost";
            }
            else {
                ipAddress = output[0];
            }
        }
        else {
            port = Integer.parseInt(args);
            ipAddress = "localhost";
        }
    }
}
