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
  public static int delay_time;

	// for writing to file
	private static boolean readHeader = true;
	private static DataOutputStream saveFile;

  private static InetAddress ipAddr;
  private static int curPacketSeqNum;

  public static void main(String[] args) throws Exception {
 		//Throw Exception if IP address not supplied.
 		if (args.length < 1)
 			throw new Exception("IP address must be supplied by the command line.");

 		//auburn eng tux056
 		ipAddr = InetAddress.getByName(args[0]);
    // InetAddress ipAddr = InetAddress.getByName("localhost");
		
 		(new Client()).start();
  }

  public void run() {
    try {
      BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
      // get damage probability
 	    double damageProb = 0.0;
 	    do {
 	      System.out.print("Enter damage probability (in range 0-1): ");
 	      damageProb = Double.parseDouble(input.readLine());
 	    } while (damageProb < 0 || damageProb > 1);
      // get lost packet probability
 	    double lostProb = 0.0;
 		  do {
 			    System.out.print("Enter lost packet probability (in range 0-1): ");
 			    lostProb = Double.parseDouble(input.readLine());
 		   } while (lostProb < 0 || lostProb > 1);
       // get delayed packed probability
 		   double delayProb = 0.0;
 		   do	{
 			     System.out.print("Enter delayed packet probability (in range 0-1): ");
 			     delayProb = Double.parseDouble(input.readLine());
 		   } while (delayProb < 0 || delayProb > 1);
			// get delay time
			do	{
				System.out.print("Enter delay time (in milliseconds): ");
				delay_time = Integer.parseInt(input.readLine());
			} while (delay_time < 0);
       input.close();

       //auburn eng tux056
	     //InetAddress ipAddr = InetAddress.getByName("131.204.14.56");
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

       curPacketSeqNum = 0;

       System.out.print("Started client loop\n");
       while(true) {
         Thread timer = new Thread();
         timer.start();

         receiveData = new byte[PACKET_SIZE];
         DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
         clientSocket.receive(receivePacket);
         if (receivePacket.getLength() == 1) break;

         try	{
 	    	    receiveData = gremlin(lostProb, damageProb, delayProb, receiveData, receivePacket, timer);
 			   } catch (InterruptedException e)	{
 				      System.out.print("\nPacket number " + curPacketSeqNum + " is delayed.\n");
 				 } finally	{
 				     if (receiveData != null) processPacket(receiveData, receivePacket, clientSocket,
                portNumber);
 				     else System.out.print("\nPacket number " + curPacketSeqNum + " is lost.\n");
 				 }
       }
        saveFile.close();
        filestream.close();
        clientSocket.close();
     } catch(Exception e)	{
         System.out.println(e);
     }

  }

  private static byte[] getMessage(byte[] packetBytes) {
    byte[] out = new byte[packetBytes.length - 8];
    for (int i = 0; i < out.length; i++) {
      out[i] = packetBytes[8 + i];
    } 
    return out;
  }

  /* Sends an ACK packet with byte[] length 4 */
  private static void sendAck(DatagramSocket clientSocket, int curPacketSeqNum, InetAddress ipAddr, int portNumber) throws IOException {
    System.out.println("Sending ack number " + curPacketSeqNum);
    byte[] array = ByteBuffer.allocate(4).putInt(curPacketSeqNum).array();
    DatagramPacket ack = new DatagramPacket(array, array.length, ipAddr, portNumber);
    clientSocket.send(ack);
  }

  /* Sends a NAK packet with byte[] length 4 */
  private static void sendNak(DatagramSocket clientSocket, int curPacketSeqNum, InetAddress ipAddr, int portNumber) throws IOException {
    System.out.println("Sending nack number " + curPacketSeqNum);
    byte[] array = ByteBuffer.allocate(4).putInt(curPacketSeqNum).array();
    DatagramPacket ack = new DatagramPacket(array, array.length, ipAddr, portNumber);
    clientSocket.send(ack);
  }

	private static void writePacketToFile(byte[] data) throws Exception {
		for (int i = 4; i < data.length; i++) {
			saveFile.writeByte(data[i]);
		}
	}

  private static boolean validChecksum(byte[] packet) {
    // calculate checksum
    int checksum   = getChecksum(packet);
    int messageSum = sumBytesInMessage(packet);
    // print message if packet is damaged
    if (checksum != messageSum) {
      //System.out.print("\nPacket number " + curPacketSeqNum
      //  + " contains an error.\n");
        return false;
    }
    //System.out.print("Checksum pass");
    return true;
  }

  public static boolean isExpectedSeqNum(byte[] packet, int curPacketSeqNum) {
    int actualPacketSeqNum = getActualSeqNum(packet);
    //System.out.print("SeqNum pass");
    return actualPacketSeqNum == curPacketSeqNum;
  }

  private static byte[] gremlin(double lostProb, double damageProb,
			double delayProb, byte[] packet, DatagramPacket receivePacket,
			Thread timer) throws Exception {
		if (packetShouldBeLost(lostProb)) return null;

		if (packetShouldBeDelayed(delayProb)) delayPacket(packet, receivePacket, timer);

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

    if (val < prob) return true;
    return false;
  }

  private static boolean packetShouldBeLost(double lostProb)	{
    // get probability as percentage in range 0-100
 	  double prob = lostProb * 100;
  	// get random int in range 0-100
    Random rand = new Random();
		int val = rand.nextInt(101);

		if (val < prob) return true;
		return false;
	}

  private static boolean packetShouldBeDelayed(double delayProb)	{
 	// get probability as percentage in range 0-100
  	double prob = delayProb * 100;
 	// get random int in range 0-100
 		Random rand = new Random();
 		int val = rand.nextInt(101);

 		if (val < prob) return true;
 		return false;
 }

 private static void delayPacket(byte[] packet, DatagramPacket receivePacket, Thread timer) throws Exception {
		Thread.sleep(delay_time); 		
		Thread.currentThread().interrupt();
 }

 private static void processPacket(byte[] receiveData, DatagramPacket receivePacket,
    DatagramSocket clientSocket, int portNumber) throws Exception {
    System.out.println("Recieving packet " + getActualSeqNum(receiveData) + ",expecting " + curPacketSeqNum +"\n");
      if (validChecksum(receiveData)) {

        if (isExpectedSeqNum(receiveData, curPacketSeqNum)) {
          curPacketSeqNum ++;
          sendAck(clientSocket, curPacketSeqNum, ipAddr, portNumber);
          writePacketToFile(receiveData);

          String modifiedSentence = new String(getMessage(receivePacket.getData()));
          System.out.println("\nFROM SERVER:\n" + modifiedSentence + "\n");  
        } else {
          System.out.println("Out of order packet " + getActualSeqNum(receiveData) + " recieved, Expecting " + curPacketSeqNum);
        }
        
      }
      else sendNak(clientSocket, curPacketSeqNum+1, ipAddr, portNumber);
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
		for (int i = 8; i < PACKET_SIZE; i++) total += packet[i];
		return total;
	}
}
