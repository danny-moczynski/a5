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
 * 2nd STUDENT'S FULL NAME GOES HERE
 *
 * 3rd STUDENT'S FULL NAME GOES HERE (DELETE THIS LINE IF NOT NEEDED)
 
 *****************************************************************************/

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
 
 public class RDT {
     private InetAddress peerIpAddress; // IP address of this instance
     private int rcvPortNum; // port # of this instance's receiver
     private int peerRcvPortNum; // port # of peer's receiver
     private Thread senderThread; // sender side of this instance
     private Thread receiverThread; // receiver side of this instance
     private byte[] dataToSend; // buffer for data to be sent
                                 // AKA the "send buffer"
     private boolean dataWaitingToBeSent; // flag indicating that there is data
                                           // yet to be sent in the send buffer and that the app will have to
                                           // wait before being able to send another message
     private byte[] dataReceived; // buffer for data received from below
                                   // AKA the "receive buffer"
     private boolean dataWasReceivedFromBelow; // flag indicating that there is
                                               // data in the receive buffer that has yet to be grabbed by the app
     private String tag; // only for debugging (see handout)
 
     // Do not modify this constructor
     public RDT(String inPeerIP, int inRcvPortNum, int inPeerRcvPortNum, String inTag) throws Exception {
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
     public void sendData(byte[] data) {
         while (dataWaitingToBeSent) {
             // Wait until the data waiting to be sent is sent
             try {
                 Thread.sleep(10); // Adjust sleep time as needed
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
         }
         // Copy data to send buffer
         dataToSend = data.clone();
         dataWaitingToBeSent = true;
     }// sendData
 
     /** The application calls this method to receive a message from its peer.
      *  The RDT instance simply returns this data to the app once it appears 
      *  in its receive buffer.
      */
     public byte[] receiveData() {
         // Wait until data is received from below
         while (!dataWasReceivedFromBelow) {
             try {
                 Thread.sleep(10); // Adjust sleep time as needed
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
         }
         // Copy received data to a new array to return
         byte[] receivedDataCopy = dataReceived.clone();
         dataWasReceivedFromBelow = false; // Reset flag
         return receivedDataCopy;
     }// receiveData
 
     /**
      * Computes and returns the checksum (i.e., XORed byte values) over the 
      * first n bytes of the given array
      */
     private byte checkSum(byte[] array, int n) {
         byte checksum = 0;
         for (int i = 0; i < n; i++) {
             checksum ^= array[i];
         }
         return checksum;
     }// checkSum
 
     /***********************************************************************
      * inner class: Receiver
      ***********************************************************************/
 
     private class Receiver implements Runnable {
         private DatagramSocket rcvSocket; // socket of the receiver
         private DatagramPacket rcvPacket; // received packet
         private byte[] rcvData = // data in the received packet
                 new byte[A5.MAX_MSG_SIZE + 2];
         private int expectedSeqNum = 0; // enough said!
 
         /**
          * Implements the receiver's FSM for RDT 2.2. More precisely: 1) create the
          * receiver's socket with the appropriate port number 2) in an infinite loop:
          * receive data from below and process it adequately according to the FSM, that
          * is: + if bad data is received, resend the "other" ACK + if good data is
          * received, place it in the receive buffer and wait for the app layer to grab
          * it, then send the corresponding ACK. Make sure to keep the state of the
          * receiver up to date at all times.
          */
         @Override
         public void run() {
             try {
                 rcvSocket = new DatagramSocket(rcvPortNum);
                 while (true) {
                     rcvPacket = new DatagramPacket(rcvData, rcvData.length);
                     rcvSocket.receive(rcvPacket);
                     if (dataOK()) {
                         // Place received data in receive buffer
                         dataReceived = rcvData.clone();
                         dataWasReceivedFromBelow = true;
                         // Send ACK
                         sendAck(expectedSeqNum);
                     } else {
                         // Resend "other" ACK
                         sendAck(1 - expectedSeqNum);
                     }
                 }
             } catch (Exception e) {
                 e.printStackTrace();
             } finally {
                 if (rcvSocket != null) {
                     rcvSocket.close();
                 }
             }
         }// run
 
         /**
          * Returns true except when: + the checksum of the received packet is not zero
          * or + the received sequence number does not match the expected one As always,
          * the format of the messages sent to the console is given in the handout.
          */
         private boolean dataOK() {
             byte checksum = checkSum(rcvData, A5.MAX_MSG_SIZE);
             return checksum == 0 && rcvData[A5.MAX_MSG_SIZE] == expectedSeqNum;
         }// dataOK
 
         /**
          * Sends an acknowledgment packet with the given number
          */
         private void sendAck(int number) throws Exception {
             DatagramPacket ackPacket = new DatagramPacket(new byte[] { (byte) number }, 1, peerIpAddress,
                     peerRcvPortNum);
             rcvSocket.send(ackPacket);
         }// sendAck
     }// Receiver
 
     /***********************************************************************
      * inner class: Sender
      ***********************************************************************/
 
     private class Sender implements Runnable {
         private DatagramSocket senderSocket; // socket of the sender
         private DatagramPacket rcvPacket; // received packet
         private byte[] rcvData = new byte[4]; // data in the received packet
         private int curSeqNum = 0; // enough said!
 
         /**
          * Implements the sender's FSM for RDT 3.0. More precisely: 1) create the
          * sender's socket with an OS-generated port number 2) in an infinite loop:
          * send messages from the app to the receiver, that is: + wait for data from
          * above, then send the packet to the peer + start the socket's timer using the
          * call: setSoTimeout(500) and wait for the ACK to come in - if the ACK comes
          * in okay and in good time, go back to the top of the loop - if the ACK is not
          * okay, keep waiting for the next ACK - if the timer goes off, resend the
          * message and keep waiting for the ACK Make sure to keep the state of the sender
          * up to date at all times.
          */
         @Override
         public void run() {
             try {
                 senderSocket = new DatagramSocket();
                 while (true) {
                     // Wait for data from above
                     while (!dataWaitingToBeSent) {
                         try {
                             Thread.sleep(10); // Adjust sleep time as needed
                         } catch (InterruptedException e) {
                             e.printStackTrace();
                         }
                     }
                     // Send packet to peer's receiver
                     sendPacket();
                     // Start socket's timer
                     senderSocket.setSoTimeout(500);
                     // Wait for ACK to come in
                     while (true) {
                         try {
                             // Receive ACK
                             rcvPacket = new DatagramPacket(rcvData, rcvData.length);
                             senderSocket.receive(rcvPacket);
                             if (ackPacketOK()) {
                                 break; // Exit loop if ACK is okay
                             }
                         } catch (Exception e) {
                             // Timer goes off, resend message
                             sendPacket();
                         }
                     }
                 }
             } catch (Exception e) {
                 e.printStackTrace();
             } finally {
                 if (senderSocket != null) {
                     senderSocket.close();
                 }
             }
         }// run
 
         /**
          * Returns true except when: + the checksum of the received packet is not zero
          * or + the received sequence number does not match the expected one As always,
          * the format of the messages sent to the console is given in the handout.
          */
         private boolean ackPacketOK() {
             byte checksum = checkSum(rcvData, 1);
             return checksum == 0 && rcvData[0] == curSeqNum;
         }// ackPacketOK
 
         /**
          * Sends to the peer's receiver a packet containing the data in the send buffer
          */
         private void sendPacket() throws Exception {
             DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, peerIpAddress,
                     peerRcvPortNum);
             senderSocket.send(sendPacket);
         }// sendPacket
     }// Sender
 }// RDT
 

