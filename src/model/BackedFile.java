package model;

public class BackedFile {

    private String fileName;
    private int chunks;
    private long fileSize;
    private String hashedFileName;
    private int desRepDeg;

    public BackedFile(String fileName, int chunks, long fileSize, String hashedFileName, int desRepDeg) {
        this.fileName = fileName;
        this.chunks = chunks;
        this.fileSize = fileSize;
        this.hashedFileName = hashedFileName;
        this.desRepDeg = desRepDeg;
    }

    public String getFileName() {
        return fileName;
    }

    public int getDesRepDeg() {
        return desRepDeg;
    }

    public int getChunks() {
        return chunks;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getHashedFileName() {
        return hashedFileName;
    }
}
