import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.HashSet;

// port #s 10008-10011

// window size 32

class Client {

  public static final int PACKET_SIZE = 512;

	// for writing to file
	private static boolean readHeader = true;
	private static DataOutputStream saveFile;

  public static void main(String[] args) throws Exception {
    // get damage probability
    double damageProb = 0.0;
    do {
      System.out.print("Enter damage probability (in range 0-1): ");
      BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
      damageProb = Double.parseDouble(input.readLine());
    } while (damageProb < 0 || damageProb > 1);

	  //auburn eng tux056
	  //InetAddress ipAddr = InetAddress.getByName("131.204.14.56");
    InetAddress ipAddr = InetAddress.getByName("localhost");
    int portNumber = 10008;

	  FileOutputStream filestream = new FileOutputStream("sampleOut.html");
	  saveFile = new DataOutputStream(filestream);

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

    int curPacketSeqNum = 0;

    while(true) {
      System.out.print("Started client loop");
      receiveData = new byte[PACKET_SIZE];
      DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
      clientSocket.receive(receivePacket);
      if (receivePacket.getLength() == 1) break;

      receiveData = gremlin(damageProb, receiveData);

      if (isExpectedSeqNum(receiveData, curPacketSeqNum++) && vaivalidChecksum(receiveData, curPacketSeqNum)) {
        sendAck(clientSocket, curPacketSeqNum, ipAddr, portNumber);
        writePacketToFile(receiveData);

        String modifiedSentence = new String(receivePacket.getData());
        System.out.println("\nFROM SERVER:\n" + modifiedSentence);
      }
      else sendNak();
    }
    saveFile.close();
    filestream.close();
    clientSocket.close();
  }

  /* Sends an ACK packet with byte[] length 4 */
  private static void sendAck(DatagramSocket clientSocket, int curPacketSeqNum, InetAddress ipAddr, int portNumber) throws IOException {
    byte[] array = ByteBuffer.allocate(4).putInt(curPacketSeqNum).array();
    DatagramPacket ack = new DatagramPacket(array, array.length, ipAddr, portNumber);
    clientSocket.send(ack);
  }

  private static void sendNak() {

  }

	private static void writePacketToFile(byte[] data) throws Exception {
		for (int i = 4; i < data.length; i++) {
			saveFile.writeByte(data[i]);
		}
	}

  private static boolean validChecksum(byte[] packet, int curPacketSeqNum) {
    // calculate checksum
    int checksum   = getChecksum(packet);
    int messageSum = sumBytesInMessage(packet);
    // print message if packet is damaged
    if (checksum != messageSum) {
      System.out.print("\nPacket number " + curPacketSeqNum
        + " contains an error.\n");
        return false;
    }
    System.out.print("Checksum pass");
    return true;
  }

  public static boolean isExpectedSeqNum(byte[] packet, int curPacketSeqNum) {
    int actualPacketSeqNum = getActualSeqNum(packet);
    System.out.print("SeqNum pass");
    return actualPacketSeqNum == curPacketSeqNum;
  }

  private static byte[] gremlin(double damageProb, byte[] packet) {
    if (packetShouldBeDamaged(damageProb)) {
      int numBytesToDamage = determineNumBytesToBeDamaged();
      HashSet<Integer> listOfDamagedBytes = new HashSet<Integer>(); // type in instantiation to quash the warning
      int index = 0;

      while (numBytesToDamage > 0) {
 	      // reset and scan through again if necessary
        if (index == PACKET_SIZE) index = 0;
        if (!listOfDamagedBytes.contains(index) && byteShouldBeDamaged()) {
          packet[index] -= 1; // damage packet
          numBytesToDamage--;
          listOfDamagedBytes.add(index);
        }
		    index ++;
      }
      return packet;
    }
    // else packet should not be damaged
    return packet;
  }

  private static boolean packetShouldBeDamaged(double damageProb) {
    // get probability as percentage in range 0-100
    double prob = damageProb * 100;
    // get random int in range 0-100
    Random rand = new Random();
    int val = rand.nextInt(101);

    if (val <= prob) return true;
    return false;
  }

  private static boolean byteShouldBeDamaged() {
    Random rand = new Random();
    int val = rand.nextInt(2);
    // .5 prob byte is damaged
    if (val == 0) return true;
    return false;
  }

  private static int determineNumBytesToBeDamaged() {
    Random rand = new Random();
    return rand.nextInt(PACKET_SIZE);
  }

  private static int getChecksum(byte[] packet) {
    byte[] ba = new byte[4];
    for (int i = 0; i < 4; i++) ba[i] = packet[i];
    int checksum = ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN).getInt();
    return checksum;
  }

  private static int getActualSeqNum(byte[] packet) {
    byte[] ba = new byte[4];
    for (int i = 4; i < 8; i++) ba[i - 4] = packet[i];
    int seqNum = ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN).getInt();
    return seqNum;
  }

  private static int sumBytesInMessage(byte[] packet) {
		int total = 0;
		for (int i = 4; i < PACKET_SIZE; i++) total += packet[i];
		return total;
	}
}
