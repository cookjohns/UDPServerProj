import java.net.*;
import java.io.*;

// port #s 10008-10011

class Client {

  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    
    InetAddress ipAddr = InetAddress.getByName("localhost");
    int portNumber = 10008;
    
    DatagramSocket clientSocket = new DatagramSocket();

    byte[] sendData    = new byte[1024];
    byte[] receiveData = new byte[1024];

    String sentence = input.readLine();

    sendData = sentence.getBytes();
    DatagramPacket sendPacket = new DatagramPacket(
        sendData, sendData.length, ipAddr, portNumber);

    clientSocket.send(sendPacket);

    DatagramPacket receivePacket = new DatagramPacket(
              receiveData, receiveData.length);


    clientSocket.receive(receivePacket);
    String modifiedSentence = new String(receivePacket.getData());

    System.out.println("FROM SERVER:" + modifiedSentence);
    clientSocket.close();

  }

  private int computeChecksum() {
    return 0;
  }

}
