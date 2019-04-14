package model;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Chunk {

    private String fileName;
    private int chunkNo;
    private long chunkSize;
    private int desRepDeg;
    private int actualDeg;
    private ArrayList<Integer> peers;

    public Chunk(String fileName, int chunkNo, int desRepDeg) {
        this.fileName = fileName;
        this.chunkNo = chunkNo;
        this.desRepDeg = desRepDeg;
        this.peers = new ArrayList<Integer>();
    }

    public void addPeer(int peerID){
        peers.add(peerID);
        this.actualDeg++;
    }

    public String getFileName() {
        return fileName;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public int getDesRepDeg() {
        return desRepDeg;
    }

    public int getActualDeg() {
        return actualDeg;
    }

    public ArrayList<Integer> getPeers() {
        return peers;
    }
}
