package utils;

public class Support {

    public enum Protocol {BACKUP, RESTORE, DELETE, RECLAIM}
    public static final double VERSION = 1.0;

    public double getVersion(){
        return VERSION;
    }

}