package ui;

import communication.Receiver;
import communication.Sender;

public class Peer {

    private static Receiver receiver;
    private static Sender sender;
    private static int port;
    private static String ipAddress;
    private static int peerID;
    public static int mcPort;
    public static String mcAddress;
    public static int mdbPort;
    public static String mdbAddress;
    public static int mdrPort;
    public static String mdrAddress;

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

    public static void startReceiver(){
        receiver = new Receiver();
        receiver.start();
    }

    public static void startSender(){
        sender = new Sender();
        sender.start();
    }

    private void Peer(){
        startReceiver();
        startSender();
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
