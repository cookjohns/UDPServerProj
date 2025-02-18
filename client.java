import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

// port #s 10008-10011

// window size 32

class Client {

  public static final int PACKET_SIZE = 512;
  public static final int TIME_OUT = 1000;
  public static final int MAX_SEQ_NUM = 64;

  // for writing to file
  private static boolean readHeader = true;
  private static Writer writer;

  private static int delay_time;

  private static InetAddress ipAddr;
  private static int curPacketSeqNum;

  public static void main(String[] args) throws Exception {
    //Throw Exception if IP address not supplied.
    if (args.length < 1)
      throw new Exception("IP address must be supplied by the command line.");

    //auburn eng tux056
    ipAddr = InetAddress.getByName(args[0]);
    // InetAddress ipAddr = InetAddress.getByName("localhost");

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

     writer = new FileWriter("sampleOut.html");

     DatagramSocket clientSocket = new DatagramSocket();

     byte[] sendData    = new byte[PACKET_SIZE];
     byte[] receiveData = new byte[PACKET_SIZE];

     //String sentence = input.readLine();
     //sendData = sentence.getBytes();

     //byte[] message = Files.readAllBytes(Paths.get("message.txt"));

     String request = "GET TestFileSmall.html HTTP/1.0";
     sendData = request.getBytes();

     DatagramPacket sendPacket = new DatagramPacket(
       sendData, sendData.length, ipAddr, portNumber);

     clientSocket.send(sendPacket);

     curPacketSeqNum = 0;


     while(true) {
       receiveData = new byte[PACKET_SIZE];
       DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
       clientSocket.receive(receivePacket);
       if (receivePacket.getLength() == 1) break;

       try	{
          receiveData = gremlin(lostProb, damageProb, delayProb, receiveData, receivePacket,
            clientSocket, portNumber);
       } catch (InterruptedException e)	{
         // WON'T HAPPEN UNTIL WE IMPLEMENT SOMETHING THAT THROWS AN EXCEPTION
            System.out.print("\nPacket number " + curPacketSeqNum + " is delayed.\n");
       } finally	{
           if (receiveData != null) processPacket(receiveData, receivePacket, clientSocket,
              portNumber);
           else System.out.print("\nPacket number " + curPacketSeqNum + " is lost.\n");
       }

    }
    writer.close();
    clientSocket.close();
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
    byte[] seqNum = ByteBuffer.allocate(4).putInt(curPacketSeqNum).array();
    byte[] ackByte = ByteBuffer.allocate(2).putChar('A').array();
    byte[] array = concat(ackByte, seqNum);
    DatagramPacket ack = new DatagramPacket(array, array.length, ipAddr, portNumber);
    clientSocket.send(ack);
  }

  /* Sends a NAK packet with byte[] length 4 */
  private static void sendNak(DatagramSocket clientSocket, int curPacketSeqNum, InetAddress ipAddr, int portNumber) throws IOException {
    byte[] seqNum = ByteBuffer.allocate(4).putInt(curPacketSeqNum).array();
    byte[] nakByte = ByteBuffer.allocate(2).putChar('N').array();
    byte[] array = concat(nakByte, seqNum);
    DatagramPacket ack = new DatagramPacket(array, array.length, ipAddr, portNumber);
    clientSocket.send(ack);
  }

  private static void writePacketToFile(byte[] data) throws Exception {
    // for (int i = 4; i < data.length; i++) {
    //  saveFile.writeChar(data[i]);
    // }
    writer.write(new String(getMessage(data)));
  }

  private static byte[] concat(byte[] a, byte[] b) {
    byte[] out = new byte[a.length + b.length];
    System.arraycopy(a, 0, out, 0, a.length);
    System.arraycopy(b, 0, out, a.length, b.length);
    return out;
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
      DatagramSocket clientSocket, int portNumber) throws Exception {

		if (packetShouldBeLost(lostProb)) return null;

		if (packetShouldBeDelayed(delayProb)) {
      delayPacket(packet, receivePacket, clientSocket, portNumber);
    }

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

  private static void delayPacket(byte[] packet, DatagramPacket receivePacket,
    DatagramSocket clientSocket, int portNumber) {
    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
	      		@Override
	      		public void run() {
	        		System.out.println("time expired");
	      			timer.cancel();
              try {

                if (packet != null) processPacket(packet, receivePacket, clientSocket,
                   portNumber);
              } catch (Exception e) {
                System.out.print(e);
              }
			}
    }, delay_time*1000);
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

    private static boolean packetShouldBeLost(double lostProb)  {
      // get probability as percentage in range 0-100
      double prob = lostProb * 100;
      // get random int in range 0-100
      Random rand = new Random();
      int val = rand.nextInt(101);

      if (val < prob) return true;
      return false;
    }

   private static boolean packetShouldBeDelayed(double delayProb) {
      // get probability as percentage in range 0-100
        double prob = delayProb * 100;
      // get random int in range 0-100
        Random rand = new Random();
        int val = rand.nextInt(101);

        if (val < prob) return true;
        return false;
     }

  private static boolean packetLost(double lostProb)  {
    // get probability as percentage in range 0-100
    double prob = lostProb * 100;
    // get random int in range 0-100
    Random rand = new Random();
    int val = rand.nextInt(101);

    if (val < prob) return true;
    return false;
  }

  private static boolean packetDelayed(double delayProb)  {
  // get probability as percentage in range 0-100
    double prob = delayProb * 100;
  // get random int in range 0-100
  Random rand = new Random();
  int val = rand.nextInt(101);

  if (val < prob) return true;
  return false;
 }

 private static void processPacket(byte[] receiveData, DatagramPacket receivePacket,
    DatagramSocket clientSocket, int portNumber) throws Exception {
      if (validChecksum(receiveData)) {

        if (isExpectedSeqNum(receiveData, curPacketSeqNum)) {
          curPacketSeqNum ++;
          curPacketSeqNum = curPacketSeqNum % MAX_SEQ_NUM;
          sendAck(clientSocket, curPacketSeqNum, ipAddr, portNumber);
          writePacketToFile(receiveData);

          String modifiedSentence = new String(getMessage(receivePacket.getData()));
          System.out.println("\nFROM SERVER:\n" + modifiedSentence + "\n");
          System.out.println("Recieved in order packet. Sending Ack number " + curPacketSeqNum);
        } else {
          sendAck(clientSocket, curPacketSeqNum, ipAddr, portNumber);
          // System.out.println("Out of order packet " + getActualSeqNum(receiveData) + " recieved, Expecting " + curPacketSeqNum);
          // System.out.println("Resending ACK number " + curPacketSeqNum);
        }
      }
      else {
        sendNak(clientSocket, curPacketSeqNum, ipAddr, portNumber);
        // System.out.println("Error in packet, sending NAK number " + curPacketSeqNum);
      }
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
