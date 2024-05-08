/**************************************************
 * CS 391 - Spring 2024 - A5
 *
 * File: A5.java
 *
 * You may NOT modify this class.
 **************************************************/

public class A5
{
    // Application Message Types
    public static final byte MSG_REQUEST_IMG_FILE      = 1;
    public static final byte MSG_FILE_NAME             = 2;
    public static final byte MSG_FILE_DATA             = 3;
    public static final byte MSG_FILE_DONE             = 4;
    public static final byte MSG_NO_IMG_FILE_AVAILABLE = 5;
    
    // Application message maximum sizes
    public static final int MAX_DATA_SIZE              = 8192;
    public static final int MAX_MSG_SIZE               = MAX_DATA_SIZE + 1;
    
    // Receiver port numbers for the client and server
    public static int CLIENT_RCV_PORT_NUM              = 8888;
    public static int CLIENT_PEER_RCV_PORT_NUM         = 7777;
    public static int SERVER_RCV_PORT_NUM              = 7777;
    public static int SERVER_PEER_RCV_PORT_NUM         = 8888;
    
    // Image subfolder
    public static String IMG_SUBFOLDER = "./images/";

    // sends the given message to the console preceded by a time stamp
    // with some indentation if tag == "S"
    public static void print(String tag, String message)
    {
        System.out.printf(
          (tag.equals("S") ? "                         " : "") +
          String.format("[%6.3f]",
                        (System.currentTimeMillis() % 100000)/1000.0)
          + " " + message + "\n");      
    }// print
}// A5
