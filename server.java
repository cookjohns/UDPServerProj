import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Arrays;

// port #s 10008-10011

// window size 32

class Server {

	public static final int PACKET_SIZE = 512;
	public static final int WINDOW_SIZE = 32; // 1/2 MAX_SEQ_NUM
	public static final int MAX_SEQ_NUM = 64;


	public static void main(String[] args) throws Exception {
		int portNumber = 10008;
		DatagramSocket serverSocket = new DatagramSocket(portNumber);

		byte[] receiveData = new byte[1024];
		byte[] sendData    = new byte[1024];

		System.out.print("Started server loop\n");
		while (true) {
			DatagramPacket receivePacket = new DatagramPacket(
					receiveData, receiveData.length);
			serverSocket.receive(receivePacket);

			if (receivePacket.getLength() == 6) {
				System.out.println("Ack/Nack recieved: seq number " + ackNum(receivePacket.getData()));
			} else {
				String sentence = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
				System.out.println("Message recieved: " + sentence);
		
					
				InetAddress ipAddress = receivePacket.getAddress();
				int clientPort = receivePacket.getPort();

				byte[] data = Files.readAllBytes(Paths.get("TestFile.html"));
				byte[] message = concat(httpHeader(data.length), data);
				goBackN(message, ipAddress, clientPort, serverSocket);
			}
		}
	}


	private static int nextNum(int seqNum) {
		return (seqNum + 1) % MAX_SEQ_NUM;
	}

	private static Boolean isAck(byte[] data) {
		byte[] ackByte = Arrays.copyOfRange(data, 0, 2);
		return (new String(ackByte).contains("A"));
	}	

	private static int ackNum(byte[] data) {
		byte[] seqNum = Arrays.copyOfRange(data, 2, 6);
		return ByteBuffer.wrap(seqNum).getInt();
	}

	private static void goBackN(byte[] message, InetAddress ipAddr, int port, DatagramSocket socket) throws Exception {
		int readHead = 0;
		int windowStart = 0;
		int nextSeqNum = 0;

		byte[] receiveData = new byte[1024];

		while (readHead < message.length + 1) {
			sendWindow(message, ipAddr, port, socket, readHead, windowStart);
			nextSeqNum += WINDOW_SIZE;
			nextSeqNum = nextSeqNum % MAX_SEQ_NUM;
			while(windowStart != nextSeqNum) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				socket.receive(receivePacket);
				byte[] data = receivePacket.getData();
				int ackNum = ackNum(data);
				if (isAck(data)) { // is ACK
					System.out.println("Ack recieved: seq number " + ackNum);
					if (ackNum == nextNum(windowStart)) {
						windowStart = nextNum(windowStart); //advance window by one when next packet is ack
						System.out.println("Moving window up to " + windowStart);
						readHead += PACKET_SIZE - 8;
						//set timeout to next packet
						if (readHead >= message.length) {
							break;
						}
					}
				} else { //is NAK
					System.out.println("Nack recieved: seq number " + ackNum);
					nextSeqNum = windowStart; //go back to last successful ACK
					break;
				} 
			}
		}
		System.out.println("Sending null packet");
		byte[] nullPacket = new byte[1];
		nullPacket[0] = 0;
		DatagramPacket sendPacket = new DatagramPacket(nullPacket, 1, ipAddr, port);
		socket.send(sendPacket);
	}

	private static byte[] httpHeader(int size) {
		String out = "HTTP/1.0 Document Follows \r\n";
		out += "Content-Type: text/plain\r\n";
		out += "Content-Length: " + size + "\r\n\r\n";
		return out.getBytes();
	}

	private static byte[] concat(byte[] a, byte[] b) {
		byte[] out = new byte[a.length + b.length];
		System.arraycopy(a, 0, out, 0, a.length);
		System.arraycopy(b, 0, out, a.length, b.length);
		return out;
	}

	private static byte[] nextPacket(byte[] message, int readHead, int packetSeqNum) {
		byte[] packet = new byte[PACKET_SIZE];

		for (int i = 8; i < packet.length; i++) {
			if (readHead + i >= message.length) break;
			packet[i] = message[readHead + i];
		}
		// add checksum to packet
		int checksum = sumBytesInPacket(packet);
		byte[] checksumByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(checksum).array();
		for (int j = 0; j < 4; j++) packet[j] = checksumByteArray[j];
		// add seq num to packet
		int seqNumMod64 = packetSeqNum % 64;
	  	byte[] packetSeqNumArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(seqNumMod64).array();
		for (int k = 4; k < 8; k++) packet[k] = packetSeqNumArray[k - 4];
		return packet;
	}

	private static void sendWindow(byte[] message, InetAddress ipAddr, int port, DatagramSocket socket, int offset, int windowStart) throws Exception {
		int packetSeqNum = windowStart;
		int readHead = offset - 8;

		int counter = 0;
		System.out.println("Sending window " + windowStart + " to " + (windowStart + WINDOW_SIZE) % MAX_SEQ_NUM);
		while (readHead < (message.length + 1 - 8) && counter < WINDOW_SIZE) {
			counter ++;
			byte[] packet = nextPacket(message, readHead, packetSeqNum++);
			DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, ipAddr, port);
			socket.send(sendPacket);
			readHead += PACKET_SIZE - 8;
		}
	}

	private static void sendMessage(byte[] message, InetAddress ipAddr, int port, DatagramSocket socket) throws Exception {
		HashMap<Integer, DatagramPacket> packetStore = new HashMap<Integer, DatagramPacket>(); // verbose to avoid warning
		int readHead     = -8;
		int packetSeqNum = 0;

	 	while (readHead < message.length+1) {
	    	byte[] packet = nextPacket(message, readHead, packetSeqNum++);
     		DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, ipAddr, port);
	    	socket.send(sendPacket);
	    	packetStore.put(packetSeqNum, sendPacket); // should overwrite reused seq numbers
	    	readHead += PACKET_SIZE - 8;
	  	}
	 	byte[] nullPacket  = new byte[1];
	 	nullPacket[0] = 0;
	 	DatagramPacket sendPacket = new DatagramPacket(nullPacket, 1, ipAddr, port);
	 	socket.send(sendPacket);

		// increment packet sequence number
		// if (packetSeqNum < MAX_SEQ_NUM) packetSeqNum++;
		// else packetSeqNum = 0;
	}

	private static int sumBytesInPacket(byte[] packet) {
		int total = 0;
		for (byte b : packet) total += b;
		return total;
	}
}
