package ui;

public class TestApp {

    private static int port;
    private static String ipAddress;
    private static String filename;

    public static void main(String args[]){
        System.out.println("TestApp Started.");
        testAppStateMachine(args);
    }

    private static void testAppStateMachine(String args[]){
        String operation;
        int size;
        int repDegree;
        splitAP(args[0]);
        operation = args[1];
        switch(operation.toUpperCase()){
            case "BACKUP":
                System.out.println("Operation was " + operation);
                filename = args[2];
                repDegree =  Integer.parseInt(args[3]);
                break;
            case "RESTORE":
                System.out.println("Operation was " + operation);
                filename = args[2];
                break;
            case "DELETE":
                System.out.println("Operation was " + operation);
                filename = args[2];
                break;
            case "RECLAIM":
                System.out.println("Operation was " + operation);
                size = Integer.parseInt(args[2]);
                break;
            case "STATE":
                System.out.println("Operation was " + operation);
                break;
            default:
                System.out.println("Wrong operation! - " + operation);
                break;
        }
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
