/**************************************************
 * CS 391 - Spring 2024 - A5
 *
 * File: Server.java
 *
 **************************************************/

 import java.io.File;
import java.io.FileInputStream;
 
 /**
  * The server program for the application-level protocol
  */
 public class Server
 {
     private RDT rdt;     // the server-side rdt instance
 
     /**
      * Constructor: creates the rdt instance with its IP address and the port
      *              number of its receiver, as well as the port number of its
      *              peer's receiver; then the thread sleeps of 0.1 second.
      */
     public Server(String ipAddress,
                   int rcvPortNum,
                   int peerRcvPortNum) throws Exception
     {
         rdt = new RDT(ipAddress, rcvPortNum, peerRcvPortNum, "SERVER");
         Thread.sleep(100);
     }// constructor
 
     /**
      * Implements the server-side of the application-level protocol as
      * described in the handout. The messages to be sent to the console are
      * also specified in the traces given in the handout, namely, the lines
      * starting with the string "SERVER ".
      *
      * When the server is done, make the thread wait for two seconds before
      * terminating the program.
      */
     public void run() throws Exception
     {
         // To be completed
         getRequest();
         String fileName = getRandomImageFile();
         sendFileName(fileName);
         FileInputStream fileInputStream = new FileInputStream(new File(A5.IMG_SUBFOLDER + fileName));
         sendFile(fileInputStream);
         rdt.sendData("DONE".getBytes());
         Thread.sleep(2000); // Wait for 2 seconds
     }// run
 
     // Do not modify this method
     private String getRandomImageFile()
     {
         File dir = new File(A5.IMG_SUBFOLDER);
         String[] entireFileList = dir.list();
         String[] imgFileList = new String[entireFileList.length];
         int imgCount = 0;
 
         for (int i = 0; i < entireFileList.length; i++)
         {
             String filename = entireFileList[i].toLowerCase();
             if (filename.endsWith(".jpg") ||
                 filename.endsWith(".jpeg") ||
                 filename.endsWith(".gif") ||
                 filename.endsWith(".png"))
                 imgFileList[imgCount++] = entireFileList[i];
         }
         if (imgCount == 0)
             return null;
         int index = new java.util.Random().nextInt(imgCount);
         return imgFileList[index];
     }// getRandomImageFile
 
     // Do not modify this method
     public static void main(String[] args)
     {
         try
         {
             new Server(args.length != 1 ? null : args[0],
                 A5.SERVER_RCV_PORT_NUM,
                 A5.SERVER_PEER_RCV_PORT_NUM).run();
         }
         catch (Exception e)
         {
             A5.print("S", "SERVER closing due to error in main: "
                 + e);
         }
     }// main
 
     /******************************************************************
      *   private methods
      ******************************************************************/
 
     // sends to the client, the name of the image file given as parameter
     private void sendFileName(String inFileName)
     {
         byte[] fileNameBytes = inFileName.getBytes();
         rdt.sendData(fileNameBytes);
     }// sendFileName
 
     // sends to the client the chunk(s) of the file to be read from the given
     // input stream
     private void sendFile(FileInputStream in) throws Exception
     {
         byte[] buffer = new byte[4096];
         int bytesRead;
         while ((bytesRead = in.read(buffer)) != -1) {
             byte[] dataToSend = new byte[bytesRead];
             System.arraycopy(buffer, 0, dataToSend, 0, bytesRead);
             rdt.sendData(dataToSend);
         }
         in.close();
     }// sendFile
 
     // waits for the file request from the client.
     // As explained in the handout, uses a flag-based loop that causes the
     // thread to yield until a MSG_REQUEST_IMG_FILE sent by the client is
     // received
     private void getRequest()
     {
         boolean received = false;
         while (!received) {
             byte[] data = rdt.receiveData();
             if (data != null && new String(data).equals("REQUEST")) {
                 received = true;
             }
         }
     }// getRequest
 
 }// Server
 