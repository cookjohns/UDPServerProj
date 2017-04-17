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

		while (true) {
			DatagramPacket receivePacket = new DatagramPacket(
					receiveData, receiveData.length);
			serverSocket.receive(receivePacket);

			String sentence = new String(receivePacket.getData());

			System.out.println("Message recieved: " + sentence);

			InetAddress ipAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();

			byte[] data = Files.readAllBytes(Paths.get("TestFile.html"));
			byte[] message = concat(httpHeader(data.length), data);
			sendMessage(message, ipAddress, clientPort, serverSocket);
		}
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

	private static byte[] nextPacket(byte[] message, int readHead) {
		byte[] packet = new byte[PACKET_SIZE];

		for (int i = 4; i < packet.length; i++) {
			if (readHead + i == message.length) break;
			packet[i] = message[readHead + i];
		}
		// add checksum to packet
		int checksum = sumBytesInPacket(packet);
		byte[] checksumByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(checksum).array();
		for (int j = 0; j < 4; j++) packet[j] = checksumByteArray[j];
		return packet;
	}

	private static void sendMessage(byte[] message, InetAddress ipAddr, int port, DatagramSocket socket) throws Exception {
		HashMap<Integer, DatagramPacket> packetStore = new HashMap<Integer, DatagramPacket>(); // verbose to avoid warning
		int readHead     = -4;
		int packetSeqNum = 0;

	 	while (readHead < message.length+1) {
	    	byte[] packet = nextPacket(message, readHead);
     		DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, ipAddr, port);
	    	socket.send(sendPacket);
	    	readHead += PACKET_SIZE - 4;
	  	}
	 	byte[] nullPacket  = new byte[1];
	 	nullPacket[0] = 0;
		int packet_id = 0;
	 	DatagramPacket sendPacket = new DatagramPacket(nullPacket, 1, ipAddr, port);
		packetStore.put(packet_id, sendPacket); // should overwrite reused seq numbers
	 	socket.send(sendPacket);

		// increment packet sequence number
		if (packetSeqNum < MAX_SEQ_NUM) packetSeqNum++;
		else packetSeqNum = 0;
	}

	private static int sumBytesInPacket(byte[] packet) {
		int total = 0;
		for (byte b : packet) total += b;
		return total;
	}
}
