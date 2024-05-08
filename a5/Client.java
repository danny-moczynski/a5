/**************************************************
 * CS 391 - Spring 2024 - A5
 *
 * File: Client.java
 *
 **************************************************/

import java.awt.BorderLayout;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * The client program for the application-level protocol
 */
public class Client
{
    private RDT rdt;                   // the client-side rdt instance 
    private String fileName;           // name of the image file to be received
    private JFrame frame;              // window to display the image
    private ByteArrayOutputStream out; // stream used to store the data bytes in
      // the image file into a byte array to be displayed in the frame above

    /**
     * Constructor: creates the rdt instance with its IP address and the port
     *              number of its receiver, as well as the port number of its
     *              peer's receiver; then the thread sleeps of 0.1 second.
     */
    public Client(String ipAddress, 
                  int rcvPortNum,
                  int peerRcvPortNum) throws Exception
    {
        // To be completed
    }// constructor

    /**
     * Implements the client-side of the application-level protocol as 
     * described in the handout. The messages to be sent to the console are
     * also specified in the traces given in the handout, namely, the lines
     * starting with the string "CLIENT ".
     *
     * If the server has an image file:
     * 1) loop on the file chunks, writing them to disk (and sending 
     *    appropriate messages to the console), making sure to have the thread
     *    yield after each block.
     * 2) display the image for two seconds (using the Thread.sleep method)
     * 3) close the frame before exiting the program.
     */
    public void run() throws Exception
    {
        // To be completed
    }// run

    // Do not modify this method
    public static void main(String[] args)
    {
        try
        {
            new Client(args.length != 1 ? null : args[0],
                       A5.CLIENT_RCV_PORT_NUM,
                       A5.CLIENT_PEER_RCV_PORT_NUM).run();
        }
        catch (Exception e)
        {
            A5.print("","CLIENT closing with error: " + e);
        }
    }// main

    /******************************************************************
     *   private methods
     ******************************************************************/

    // do not modify this method
    private void displayImage(byte[] fileData)
    {
        try
        {
            Image image = null;
            ByteArrayInputStream bin = new ByteArrayInputStream(fileData);
            image = ImageIO.read(bin);
            image = image.getScaledInstance(500, -1, Image.SCALE_DEFAULT);
            
            frame = new JFrame();
            JLabel label = new JLabel(new ImageIcon(image));
            frame.getContentPane().add(label, BorderLayout.CENTER);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }
        catch (Exception e)
        {
            System.err.println("CLIENT display image error: " + e);
        }
    }// displayImage

    /**
     * Sends the MSG_REQUEST_IMG_FILE message
     */
    private void sendFileRequest()
    {
        // To be completed
    }// sendFileRequest

    /**
     * Receives one of two messages from the server and returns true if and
     * only if the message received is MSG_FILE_NAME, in which case it updates
     * the appropriate instance variable.
     */    
    private boolean getFileName()
    {
        // To be completed
        
        return true;  // only to satisfy the compiler
    }// getFileName

}// Client
