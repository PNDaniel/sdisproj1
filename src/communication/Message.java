package communication;

import utils.Support;

public class Message {

    private String protocol;
    private double version;
    private int senderID;
    private String fileID;

    public Message(String protocol, double version, int senderID, String fileID ) {

        this.protocol = protocol;
        this.version = version;
        this.senderID = senderID;
        this.fileID = fileID;
    }

    private String createStandardMessage(){
        StringBuilder sb = new StringBuilder(protocol);
        Support support = new Support();
        sb.append(" ");sb.append(support.getVersion());
        sb.append(" ");sb.append(senderID);
        sb.append(" ");sb.append(fileID);

        return sb.toString();
    }

    public byte[] createPutchunkMessage(int chunkNo, int repDeg, byte[] body){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append(chunkNo);
        sb.append(" ");sb.append(repDeg);
        sb.append(" ");sb.append("\\r\\n\\r\\n");
        byte[] putchunkHeader = sb.toString().getBytes();
        byte[] bytes = new byte[putchunkHeader.length + body.length];

        System.arraycopy(putchunkHeader, 0, bytes, 0, putchunkHeader.length);
        System.arraycopy(body, 0, bytes, putchunkHeader.length, body.length);

        return bytes;
    }

    public String createStoredMessage(int chunkNo){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append(chunkNo);
        sb.append(" ");sb.append("\\r\\n\\r\\n");

        return sb.toString();
    }

    public String createGetChunkMessage(int chunkNo){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append(chunkNo);
        sb.append(" ");sb.append("\\r\\n\\r\\n");

        return sb.toString();
    }

    public byte[] createChunkMessage(int chunkNo, byte[] body){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append(chunkNo);
        sb.append(" ");sb.append("\\r\\n\\r\\n");
        byte[] putchunkHeader = sb.toString().getBytes();
        byte[] bytes = new byte[putchunkHeader.length + body.length];

        System.arraycopy(putchunkHeader, 0, bytes, 0, putchunkHeader.length);
        System.arraycopy(body, 0, bytes, putchunkHeader.length, body.length);

        return bytes;
    }

    public String createDeleteMessage(){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append("\r\n\r\n");
        return sb.toString();
    }

    public String createRemovedMessage(int chunkNo){
        StringBuilder sb = new StringBuilder(createStandardMessage());
        sb.append(" ");sb.append(chunkNo);
        sb.append(" ");sb.append("\r\n\r\n");

        return sb.toString();
    }

    public String getProtocol() {
        return protocol;
    }

    public double getVersion() {
        return version;
    }

    public int getSenderID() {
        return senderID;
    }

    public String getFileID() {
        return fileID;
    }
}
