package utils;

import model.BackedFile;

import java.io.*;
import java.util.ArrayList;

public class Database implements java.io.Serializable{

    ArrayList<BackedFile> backedFileList;

    public Database(){
        backedFileList =  new ArrayList<>();
//        if  (!loadDatabase()){
//            backedFileList =  new ArrayList<>();
//        }
//        System.out.println(backedFileList.size());

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
        BackedFile backedFile = new BackedFile(fileName, chunks, fileSize, hashedFileName, desRepDeg);
        backedFileList.add(backedFile);
    //    saveDatabase(backedFile);
    }

    private void saveDatabase(BackedFile obj){
        File file = new File("C:\\temp\\database.ser");
        try {
            FileOutputStream fileOut = new FileOutputStream("C:\\temp\\database.ser",true);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(obj);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in database.ser");
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    private boolean loadDatabase(){
        File f =  new File("C:\\temp\\database.ser");
        if (!f.exists()){
            return false;
        }
        try {
            FileInputStream fis = new FileInputStream("C:\\temp\\database.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            backedFileList = (ArrayList) ois.readObject();
            ois.close();
            fis.close();
            return true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
            return false;
        }
    }
}
