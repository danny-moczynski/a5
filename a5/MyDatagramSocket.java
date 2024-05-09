/**************************************************
 * CS 391 - Spring 2024 - A5
 *
 * File: MyDatagramSocket.java
 *
 * You may:
 * + modify this class but for testing purposes only
 * + NOT add functionality to it that is needed for
 *   the other classes to work since you will NOT be
 *   including this file in your submission.
 **************************************************/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;

/**
 * A DatagramSocket that injects errors to test the robustness of the rdt 
 * protocol, namely:
 * 1. drop the packet with some probability 
 * 2. corrupt one bit of the packet with some probability
 */
public class MyDatagramSocket extends DatagramSocket
{
    private static final int DEFAULT_ONE_OUT_OF = 0;
    private              int dropOneOutOf       = DEFAULT_ONE_OUT_OF;
    private              int corruptOneOutOf    = DEFAULT_ONE_OUT_OF;
    private              Random rand;

    // sets the socket's random number generator with seed value 391
    // the socket will use an OS-generated port number
    public MyDatagramSocket() throws SocketException
    {
        super();
        rand = new Random(391);
    }

    // sets the socket's random number generator with seed value 391
    // the socket will use the given port number
    public MyDatagramSocket(int port) throws SocketException
    {
        super(port);
        rand = new Random(391);    
    }

    /**
     *  sends packet, just like DatagramSocket would, but:
     * +  with a probability of 1/dropOneOutOf of dropping the packet
     * +  with a probability of 1/corruptOneOutOf of flipping one bit in the 
     *    packet
     */
    @Override
    public void send(DatagramPacket packet) throws IOException
    {
        if (dropOneOutOf > 0 && rand.nextInt(dropOneOutOf) == 0)
        {
            // drop the packet
            if (packet.getLength() > 0)
            {
                String firstBytes = firstBytesAsStr(packet.getData());
                System.out.println("   *** DROPPED packet with first bytes " +
                                   firstBytes); 
            }     
            else
                System.out.println("   *** DROPPED empty packet");
            return;
        }

        DatagramPacket sendPacket = packet; // packet that will be sent
        
        if (corruptOneOutOf > 0 && 
            packet.getLength() > 0 && 
            rand.nextInt(corruptOneOutOf) == 0)
        {
            // pick a random bit within a random byte in the packet
            int len = packet.getLength();
            byte[] corruptBytes = new byte[len],
                   packetBytes = packet.getData();
            System.arraycopy(packetBytes, 0, corruptBytes, 0, len);
            int randByte = rand.nextInt(len),
                randBit = 1 << rand.nextInt(Byte.SIZE);
            // flip the selected bit
            corruptBytes[randByte] = (byte)(corruptBytes[randByte] ^ randBit);
            String firstBytes = firstBytesAsStr(corruptBytes);
            System.out.println("   *** CORRUPTED packet with first bytes = " +
                               firstBytes);
            sendPacket = new DatagramPacket(corruptBytes,
                                            0,
                                            corruptBytes.length,
                                            packet.getAddress(),
                                            packet.getPort());
        }
        
        // Sleep to give the receiver time to catch up
        try { Thread.sleep(100); } catch (Exception e) { }
        super.send(sendPacket);
    }// send

    // returns as a string the first (up to) 3 bytes of the given array
    private String firstBytesAsStr(byte[] array)
    {
        String firstBytes = "";
        int numBytes = Math.min(array.length, 3);
        for (int i = 0; i < numBytes - 1; i++)
            firstBytes += String.format("0x%02x ", array[i]);
        firstBytes += String.format("0x%02x", array[numBytes - 1]);
        return firstBytes;
    }// firstBytesAsStr
        
}// MyDatagramSocket class
