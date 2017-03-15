import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.Random;

// port #s 10008-10011

class Client {

  public static final int PACKET_SIZE = 128;

  public static void main(String[] args) throws Exception {

    // get damage probability
    double damageProb = 0.0;
    do {
      System.out.print("Enter damage probability (in range 0-1): ");
      BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
      damageProb = Double.parseDouble(input.readLine());
    } while (damageProb < 0 || damageProb > 1);

    InetAddress ipAddr = InetAddress.getByName("localhost");
    int portNumber = 10008;

    DatagramSocket clientSocket = new DatagramSocket();

    byte[] sendData    = new byte[1024];
    byte[] receiveData = new byte[1024];

    //String sentence = input.readLine();
    //sendData = sentence.getBytes();

    byte[] message = Files.readAllBytes(Paths.get("message.txt"));

    // DatagramPacket sendPacket = new DatagramPacket(
    //     sendData, sendData.length, ipAddr, portNumber);

    // clientSocket.send(sendPacket);


    sendMessage(message, ipAddr, portNumber, clientSocket);



    DatagramPacket receivePacket = new DatagramPacket(
              receiveData, receiveData.length);


    clientSocket.receive(receivePacket);


    String modifiedSentence = new String(receivePacket.getData());
    System.out.println("FROM SERVER:" + modifiedSentence);

    clientSocket.close();

  }

  private static byte[] nextPacket(byte[] message, int readHead) {
    byte[] packet;
    if (message.length <= readHead + PACKET_SIZE) {
      packet = new byte[message.length - readHead];
    } else {
      packet = new byte[PACKET_SIZE];
    }
    for (int i = 0; i < packet.length; i++) {
      packet[i] = message[readHead + i];
    }
    return packet;
  }

  private static void sendMessage(byte[] message, InetAddress ipAddr, int port, DatagramSocket socket) throws Exception {
    int readHead = 0;
    while (readHead < message.length) {
      byte[] packet = nextPacket(message, readHead);
      DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, ipAddr, port);
      socket.send(sendPacket);
      readHead += PACKET_SIZE;
    }
  }

  private int errorCheck() {
    return 0;
  }

  private byte[] gremlin(double damageProb, byte[] packet) {
    if (packetIsDamaged(damageProb)) {
      int numBytesDamaged = determineNumBytesDamaged();
    }
    return packet;
  }

  private boolean packetIsDamaged(double damageProb) {
    // get probability as percentage in range 0-100
    double prob = damageProb * 100;
    // get random int in range 0-100
    Random rand = new Random();
    int val = rand.nextInt(101);

    if (val <= prob) return true;
    return false;
  }

  private int determineNumBytesDamaged() {
    Random rand = new Random();
    int val = rand.nextInt(11);

    if (val <= 5) return 1;
    else if (val <= 8) return 2;
    else return 3;
  }
}
