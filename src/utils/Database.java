package utils;

import model.BackedFile;

import java.io.File;
import java.util.ArrayList;

public class Database {

    ArrayList<BackedFile> backedFileList;

    public Database(){
        backedFileList =  new ArrayList<BackedFile>();
    }

    int number = -1;
    public int getFileChunksNumber(String fileName){
        backedFileList.stream().filter(o -> o.getFileName().equals(fileName)).forEach(
                o -> {
                    number = o.getChunks();
                }
        );
        return number;
    }

    public void addFileToDatabase(String fileName, int chunks, long fileSize, String hashedFileName, int desRepDeg){
        backedFileList.add(new BackedFile(fileName, chunks, fileSize, hashedFileName, desRepDeg));
    }
}
