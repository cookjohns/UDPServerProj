import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

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

			if (receivePacket.getLength() == 4) {
				System.out.println("Ack/Nack recieved: seq number " + ByteBuffer.wrap(receivePacket.getData()).getInt());
			} else {
				String sentence = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
				System.out.println("Message recieved: " + sentence);
			}
			
			
			int windowHead = 0;


			InetAddress ipAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();

			byte[] data = Files.readAllBytes(Paths.get("TestFile.html"));
			byte[] message = concat(httpHeader(data.length), data);
			sendMessage(message, ipAddress, clientPort, serverSocket);
		}
	}

	private static void goBackN(byte[] message, InetAddress ipAddr, int port, DatagramSocket socket) throws Exception {
		int readHead = 0;
		int windowStart = 0;
		int nextSeqNum = 0;

		byte[] receiveData = new byte[1024];

		outerloop:
		while (readHead < message.length + 1) {
			sendWindow(message, ipAddr, port, socket, readHead, windowStart);
			nextSeqNum += WINDOW_SIZE;
			while(windowStart != nextSeqNum) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				socket.receive(receivePacket);
				if (receivePacket.getLength() == 4) { // is ACK
					int ackNum = ByteBuffer.wrap(receivePacket.getData()).getInt();
					System.out.println("Ack recieved: seq number " + ackNum);
					if (ackNum == windowStart + 1) {
						windowStart ++; //advance window by one when next packet is ack
						readHead += PACKET_SIZE - 8;
						//set timeout to next packet
					}
				} else if (receivePacket.getLength() == 4) { //is NAK
					nextSeqNum = windowStart; //go back to last successful ACK
					continue outerloop;
				} 
			}
		}
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
			if (readHead + i == message.length) break;
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

		while (readHead < message.length + 1) {
			byte[] packet = nextPacket(message, readHead, packetSeqNum++);
			DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, ipAddr, port);
			socket.send(sendPacket);
			readHead += PACKET_SIZE - 8;
		}
		byte[] nullPacket = new byte[1];
		nullPacket[0] = 0;
		DatagramPacket sendPacket = new DatagramPacket(nullPacket, 1, ipAddr, port);
		socket.send(sendPacket);

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
