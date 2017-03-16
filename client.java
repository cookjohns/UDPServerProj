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

    byte[] sendData    = new byte[PACKET_SIZE];
    byte[] receiveData = new byte[PACKET_SIZE];

    //String sentence = input.readLine();
    //sendData = sentence.getBytes();

    //byte[] message = Files.readAllBytes(Paths.get("message.txt"));

	String request = "GET TestFile.html HTTP/1.0";
	sendData = request.getBytes(); 

    DatagramPacket sendPacket = new DatagramPacket(
         sendData, sendData.length, ipAddr, portNumber);

    clientSocket.send(sendPacket);

	while(true) {
		receiveData = new byte[PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(
              receiveData, receiveData.length);
    		clientSocket.receive(receivePacket);
		if (receivePacket.getLength ()== 1) break;
    
		String modifiedSentence = new String(receivePacket.getData());
   	System.out.println("FROM SERVER:" + modifiedSentence);
	}
    
	clientSocket.close();

  }
  
	private int errorCheck() {
    return 0;
  }

  private byte[] gremlin(double damageProb, byte[] packet) {
    if (packetIsDamaged(damageProb)) {
      int numBytesDamaged = determineNumBytesDamaged();
      for (byte b : packet) {
        if (byteIsDamaged()) {
          b -= 1;
        }
      }
      return packet;
    }
    // else packet is not damaged
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

  private boolean byteIsDamaged() {
    Random rand = new Random();
    int val = rand.nextInt(2);

    // .5 prob byte is damaged
    if (val == 0) return true;
    return false;
  }

  private int determineNumBytesDamaged() {
    Random rand = new Random();
    int val = rand.nextInt(11);

    // .5 prob 1 packet damaged
    if (val <= 5) return 1;
    // .3 prob 1 packet damaged
    else if (val <= 8) return 2;
    // .2 prob 1 packet damaged
    return 3;
  }
}
