package communication;

import utils.Support;

import java.util.ArrayList;

public class Message {

    private Support.Protocol protocol;
    private double version;
    private int senderID;
    private String fileID;

    public Message(Support.Protocol protocol, double version, int senderID, String fileID ) {

        this.protocol = protocol;
        this.version = version;
        this.senderID = senderID;
        this.fileID = fileID;
    }

    private String createStandardMessage(){
        StringBuilder sb = new StringBuilder(protocol.toString());
        Support support = new Support();
        sb.append(" ");sb.append(support.getVersion());
        sb.append(" ");sb.append(senderID);
        sb.append(" ");sb.append(fileID);

        return sb.toString();
    }

    public String createPutchunkMessage(int chunkNo, int repDeg, byte[] body){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append(chunkNo);
        sb.append(" ");sb.append(repDeg);
        sb.append(" ");sb.append("\r\n\r\n");
        sb.append(" ");sb.append(body);


        return sb.toString();
    }

    private String createStoredMessage(int chunkNo){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append(chunkNo);
        sb.append(" ");sb.append("\r\n\r\n");

        return sb.toString();
    }

    private String createGetChunkMessage(int chunkNo){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append(chunkNo);
        sb.append(" ");sb.append("\r\n\r\n");

        return sb.toString();
    }

    private String createChunkMessage(int chunkNo, byte[] body){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append(chunkNo);
        sb.append(" ");sb.append("\r\n\r\n");
        sb.append(" ");sb.append(body);

        return sb.toString();
    }

    private String createDeleteMessage(){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append("\r\n\r\n");
        return sb.toString();
    }

    private String createRemovedMessage(int chunkNo){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append(chunkNo);
        sb.append(" ");sb.append("\r\n\r\n");

        return sb.toString();
    }

}
