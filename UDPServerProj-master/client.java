import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.HashSet;
import java.lang.Thread;

// port #s 10008-10011

// window size 32

class Client extends Thread {

  public static final int PACKET_SIZE = 512;
	public static final int TIME_OUT = 0;		// Thread.sleep

	// for writing to file
	private static boolean readHeader = true;
	private static DataOutputStream saveFile;

	private static int curPacketSeqNum = 0;
	
	private static InetAddress ipAddr = null;

	public void run() {
		try	{
			DatagramSocket clientSocket = new DatagramSocket();

		  byte[] sendData    = new byte[PACKET_SIZE];
		  int portNumber = 10008;

		  //String sentence = input.readLine();
		  //sendData = sentence.getBytes();

		  //byte[] message = Files.readAllBytes(Paths.get("message.txt"));

			FileOutputStream filestream = new FileOutputStream("sampleOut.html");
			saveFile = new DataOutputStream(filestream);

			String request = "GET TestFile.html HTTP/1.0";
			sendData = request.getBytes();

		  DatagramPacket sendPacket = new DatagramPacket(
		       sendData, sendData.length, ipAddr, portNumber);

		  clientSocket.send(sendPacket);

			BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

			// get lost packet probability
			double lostProb = 0.0;
			do	{
				System.out.print("Enter lost packet probability (in range 0-1): ");
				lostProb = Double.parseDouble(input.readLine());
			} while (lostProb < 0 || lostProb > 1);
		
		  // get damage probability
		  double damageProb = 0.0;
		  do {
		    System.out.print("Enter damage probability (in range 0-1): ");
		    damageProb = Double.parseDouble(input.readLine());
		  } while (damageProb < 0 || damageProb > 1);

			// get delayed packet probability
			double delayProb = 0.0;
			do	{
				System.out.print("Enter delayed packet probability (in range 0-1): ");
				delayProb = Double.parseDouble(input.readLine());
			} while (delayProb < 0 || delayProb > 1);

			input.close();

			while(true) {
				Thread timer = new Thread();
				timer.start();

		    byte[] receiveData = new byte[PACKET_SIZE];
		    DatagramPacket receivePacket = new DatagramPacket(receiveData,
		      receiveData.length);
		    clientSocket.receive(receivePacket);
		    if (receivePacket.getLength() == 1) break;

		    receiveData = gremlin(lostProb, damageProb, delayProb, receiveData, receivePacket);
				if (receiveData != null)	{
					errorCheck(receiveData, curPacketSeqNum);
					writePacketToFile(receiveData);

					String modifiedSentence = new String(receivePacket.getData());
		 			System.out.println("\nFROM SERVER:\n" + modifiedSentence);
		 			curPacketSeqNum++;
				}

				timer.stop();
		  }

			saveFile.close();
		  filestream.close();
		  clientSocket.close();
		} catch(Exception e)	{
			System.out.println("Error");
		}
	}

  public static void main(String[] args) throws Exception {
		//Throw Exception if IP address not supplied.		
		if (args.length < 1)
			throw new Exception("IP address must be supplied by the command line.");
		
		//auburn eng tux056
		ipAddr = InetAddress.getByName(args[0]);
    //InetAddress ipAddr = InetAddress.getByName("localhost");

		(new Client()).start();
  }

	private static void writePacketToFile(byte[] data) throws Exception {
		for (int i = 4; i < data.length; i++) {
			saveFile.writeByte(data[i]);
		}
	}

  private static void errorCheck(byte[] packet, int curPacketSeqNum) {
    // calculate checksum
    int checksum   = getChecksum(packet);
    int messageSum = sumBytesInMessage(packet);
    // print message if packet is damaged
    if (checksum != messageSum) {
      System.out.print("\nPacket number " + curPacketSeqNum
        + " contains an error.\n");
    }
  }

  private static byte[] gremlin(double lostProb, double damageProb, 
			double delayProb, byte[] packet, DatagramPacket receivePacket) throws Exception {
		if (packetLost(lostProb))
			return null;

		if (packetDelayed(delayProb))
			delayPacket(packet, receivePacket);

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

	private static boolean packetLost(double lostProb)	{
		// get probability as percentage in range 0-100
		double prob = lostProb * 100;
		// get random int in range 0-100
		Random rand = new Random();
		int val = rand.nextInt(101);
	
		if (val <= prob) return true;
		return false;
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

	private static boolean packetDelayed(double delayProb)	{
		// get probability as percentage in range 0-100
		double prob = delayProb * 100;
		// get random int in range 0-100
		Random rand = new Random();
		int val = rand.nextInt(101);

		if (val <= prob) return true;
		return false;
	}

	private static void delayPacket(byte[] packet, DatagramPacket receivePacket) throws Exception {
		Thread.sleep(TIME_OUT);
	}

	private static void processPacket(byte[] packet, DatagramPacket receivePacket)	{

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

  private static int sumBytesInMessage(byte[] packet) {
		int total = 0;
		for (int i = 4; i < PACKET_SIZE; i++) total += packet[i];
		return total;
	}
}
