import java.net.*;
import java.io.*;

// port #s 10008-10011

class Client {

  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    DatagramSocket clientSocket = new DatagramSocket();
    InetAddress ipAddr = InetAddress.getByName("hostname");

    byte[] sendData    = new byte91024];
    byte[] receiveData = new byte91024];

    String sentence = input.readLine();

    sendData = sentence.getBytes();
  }

  private int computeChecksum() {
    return 0;
  }
}
