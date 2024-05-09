/*****************************************************************************
 * CS 391 - Spring 2024 - A5
 *
 * File: RDT.java
 *
 * Classes: RDT
 *          Sender (inner class)
 *          Receiver (inner class)
 *
 * Danny Moczynski
 *
 * Alex Ceithamer  
 *
 * Alden Sprackling
 
 *****************************************************************************/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**  
 * Reliable Data Transfer potocol implemented on top of MyDatagramSocket.
 *
 * Important note: this class is NOT aware of (and thus cannot reference) the
 * client/server processes that will be using it. In fact, any client-server
 * application should be able to use this class.
 *
 * The handout explains how an RDT instance's sender thread talks to its
 * peer's receiver thread, and vice-versa.
 */

public class RDT
{
    private InetAddress peerIpAddress;   // IP address of this instance
    private int rcvPortNum;              // port # of this instance's receiver
    private int peerRcvPortNum;          // port # of peer's receiver
    private Thread senderThread;         // sender side of this instance
    private Thread receiverThread;       // receiver side of this instance
    private byte[] dataToSend;           // buffer for data to be sent
                                         // AKA the "send buffer"
    private boolean dataWaitingToBeSent; // flag indicating that there is data
         // yet to be sent in the send buffer and that the app will have to
         // wait before being able to send another message
    private byte[] dataReceived;         // buffer for data received from below
                                         // AKA the "receive buffer"    
    private boolean dataWasReceivedFromBelow; // flag indicating that there is
         // data in the receive buffer that has yet to be grabbed by the app
    private String tag;                  // only for debugging (see handout)

    private final Object sendLock = new Object();
    private final Object receiveLock = new Object();
    
    // Do not modify this constructor
    public RDT(String inPeerIP, 
               int inRcvPortNum, 
               int inPeerRcvPortNum,
               String inTag) throws Exception
    {
        rcvPortNum = inRcvPortNum;
        peerRcvPortNum = inPeerRcvPortNum;
        tag = inTag;
        if (inPeerIP == null)
            peerIpAddress = InetAddress.getLoopbackAddress();
        else
            peerIpAddress = InetAddress.getByName(inPeerIP);
        dataWaitingToBeSent = false;
        dataWasReceivedFromBelow = false;
        senderThread = new Thread(new Sender());
        receiverThread = new Thread(new Receiver());
        senderThread.start();
        receiverThread.start();
    }// constructor
      
    /** The application calls this method to send a message to its peer.
     *  The RDT instance simply "copies" this data into its send buffer, but
     *  only after waiting for the data currently in that buffer (if any)
     *  has been sent.
     */
    public void sendData(byte[] data)
    {
        synchronized (sendLock) {
            while (dataWaitingToBeSent) {
                try {
                    sendLock.wait(); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); 
                }
            }
            dataToSend = data;
            dataWaitingToBeSent = true;
            sendLock.notifyAll(); 
        }
        
    }// sendData

    /** The application calls this method to receive a message from its peer.
     *  The RDT instance simply returns this data to the app once it appears 
     *  in its receive buffer.
     */
    public byte[] receiveData()
    {
        synchronized (receiveLock) {
            while (!dataWasReceivedFromBelow) {
                try {
                    receiveLock.wait(); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); 
                }
            }
            byte[] data = dataReceived;
            dataWasReceivedFromBelow = false;
            receiveLock.notifyAll(); 
            return data;
        }
    }// receiveData

    /**
     * Computes and returns the checksum (i.e., XORed byte values) over the 
     * first n bytes of the given array
     */
    private byte checkSum(byte[] array, int n)
    {
        byte sum = 0;
        for (int i = 0; i < n; i++) {
            sum ^= array[i]; 
        }
        return sum;
    }// checkSum

    /***********************************************************************
     * inner class: Receiver
     ***********************************************************************/
    
    private class Receiver implements Runnable
    {
        private MyDatagramSocket rcvSocket;   // socket of the receiver
        private DatagramPacket rcvPacket;     // received packet
        private byte[] rcvData =              // data in the received packet
                new byte[A5.MAX_MSG_SIZE + 2];
        private int expectedSeqNum = 0;       // enough said!

        /**
         * Implements the receiver's FSM for RDT 2.2. More precisely:
         * 1) create the receiver's socket with the appropriate port number
         * 2) in an infinite loop: receive data from below and process it 
         *    adequately according to the FSM, that is:
         *    + if bad data is received, resend the "other" ACK
         *    + if good data is received, place it in the receive buffer and
         *      wait for the app layer to grab it, then send the corresponding
         *      ACK.
         *    Make sure to keep the state of the receiver up to date at all 
         *    times.
         */
        @Override
        public void run()
        {
            try {
                rcvSocket = new MyDatagramSocket(rcvPortNum);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        
            while (true) {
                DatagramPacket packet = new DatagramPacket(rcvData, rcvData.length);
                try {
                    rcvSocket.receive(packet);
                    if (packet.getLength() > 2 && dataOK()) {
                        synchronized (receiveLock) {
                            if (packet.getLength() > 1) { 
                                dataReceived = Arrays.copyOfRange(rcvData, 1, packet.getLength() - 1);
                            } else {
                                continue; 
                            }
                            dataWasReceivedFromBelow = true;
                            receiveLock.notifyAll();
                        }
                        sendAck(expectedSeqNum);
                        expectedSeqNum ^= 1;
                    } else {
                        sendAck(expectedSeqNum ^ 1); 
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }// run

        /**
         * Returns true except when:
         * + the checksum of the received packet is not zero
         *   or
         * + the received sequence number does not match the expected one
         * As always, the format of the messages sent to the console is given
         * in the handout.
         */
        private boolean dataOK() 
        {
            byte receivedSeqNum = rcvData[0];
            byte receivedCheckSum = rcvData[rcvData.length - 1];
            byte calculatedCheckSum = checkSum(rcvData, rcvData.length - 1);

            if (receivedCheckSum == calculatedCheckSum && receivedSeqNum == expectedSeqNum) {
                expectedSeqNum ^= 1;  
                return true;
            } else {
                // If checksum is correct but sequence number is wrong, it might be a duplicate packet,
                // so ACK with the last expected sequence number to confirm receipt of the last correct packet
                return false;
            }
        }

        /**
         * Sends an acknowledgment packet with the given number
         */
        private void sendAck(int number)
        {
            byte[] ackPacket = {(byte) number};
            DatagramPacket packet = new DatagramPacket(ackPacket, ackPacket.length, peerIpAddress, peerRcvPortNum);
            try {
                rcvSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }// sendAck
    }// Receiver

    /***********************************************************************
     * inner class: Sender
     ***********************************************************************/

    private class Sender implements Runnable
    {
        private MyDatagramSocket senderSocket;  // socket of the sender
        private DatagramPacket rcvPacket;       // received packet
        private byte[] rcvData = new byte[4];   // data in the received packet
        private int curSeqNum = 0;              // enough said!

        /**
         * Implements the sender's FSM for RDT 3.0. More precisely:
         * 1) create the sender's socket with an OS-generated port number
         * 2) in an infinite loop: send messages from the app to the receiver,
         *    that is:
         *    + wait for data from above, then send the packet to the peer
         *    + start the socket's timer using the call: setSoTimeout(500)
         *      and wait for the ACK to come in
         *      - if the ACK comes in okay and in good time, go back to the top
         *        of the loop
         *      - if the ACK is not okay, keep waiting for the next ACK
         *      - if the timer goes off, resend the message and keep waiting
         *        for the ACK
         *    Make sure to keep the state of the sender up to date at all 
         *    times.
         */
        @Override
        public void run()
        {
            try {
                senderSocket = new MyDatagramSocket(); 
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        
            while (true) {
                synchronized (sendLock) {
                    while (!dataWaitingToBeSent) {
                        try {
                            sendLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    try {
                        sendPacket();
                        senderSocket.setSoTimeout(500); 
                        DatagramPacket ackPacket = new DatagramPacket(rcvData, rcvData.length);
                        senderSocket.receive(ackPacket); 
                        if (ackPacketOK()) {
                            curSeqNum ^= 1; 
                            dataWaitingToBeSent = false;
                            sendLock.notifyAll();
                        } else {
                        }
                    } catch (SocketTimeoutException e) {
                        try {
                            sendPacket();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        } 
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }// run

        /**
         * Returns true except when:
         * + the checksum of the received packet is not zero
         *   or
         * + the received sequence number does not match the expected one
         * As always, the format of the messages sent to the console is given
         * in the handout.
         */
        private boolean ackPacketOK()
        {
            return rcvData[0] == curSeqNum;

            
        }// ackPacketOK

        /**
         * Sends to the peer's receiver a packet containing the data in the 
         * send buffer
         */     
        private void sendPacket() throws Exception
        {
            byte[] sendDataWithHeader = new byte[dataToSend.length + 2];
            sendDataWithHeader[0] = (byte) curSeqNum;
            System.arraycopy(dataToSend, 0, sendDataWithHeader, 1, dataToSend.length);
            sendDataWithHeader[sendDataWithHeader.length - 1] = checkSum(sendDataWithHeader, sendDataWithHeader.length - 1);
            DatagramPacket packet = new DatagramPacket(sendDataWithHeader, sendDataWithHeader.length, peerIpAddress, peerRcvPortNum);
            senderSocket.send(packet);
        }// sendPacket
    }// Sender
}// RDT


