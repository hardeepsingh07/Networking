import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.xml.bind.DatatypeConverter;

public class CrytoClient {
	public static InputStream in;
	public static OutputStream out;
	public static Random r = new Random();
	public static byte[] sessionKey;
	public static byte[] cipherText;
	public static byte[] eIpv4;
	public static boolean firstTime = true;
	public static int dataSize = 2;
	public static int packet = 1;
	public static double time = 0;
	public static double timeAverage = 0;
	public static double start; 
	public static double stop;
	public static int sourceAddress = ipConvert("192.168.1.8");
	public static int destAddress = ipConvert("76.91.123.97");

	@SuppressWarnings({ "resource", "unused" })
	public static void main(String[] args) throws Exception {
		Socket socket = new Socket("45.50.5.238", 38008);
		System.out.println("Connection: " + socket);
		in = socket.getInputStream();
		out = socket.getOutputStream();

		// Read key from file
		String file = "public.bin";
		ObjectInputStream rsa = new ObjectInputStream(new FileInputStream(file));
		RSAPublicKey publicKey = (RSAPublicKey) rsa.readObject();
		Cipher publicCipher = Cipher.getInstance("RSA");
		Cipher privateCipher = Cipher.getInstance("AES");
		rsa.close();

		// Generate a private session key and serialize
		Key sKey = KeyGenerator.getInstance("AES").generateKey();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream serialKey = new ObjectOutputStream(baos);
		serialKey.writeObject(sKey);
		sessionKey = baos.toByteArray();

		// Encrypt the session key
		publicCipher.init(Cipher.ENCRYPT_MODE, publicKey);
		privateCipher.init(Cipher.ENCRYPT_MODE, sKey);
		cipherText = publicCipher.doFinal(sessionKey);

		// Send in session key data packet
		System.out.println("\n-----Sending Encrypted Session Key-----");
		out.write(udpPacket(cipherText.length));
		readFromServer();
		System.out.println("------Encrypted Session Received-------");

		// Send 12 encrypted packets
		System.out.println("\n-------Sending Encrypted UDPwDATA------");
		firstTime = false;
		while (dataSize != 2048) {
			eIpv4 = privateCipher.doFinal(udpPacket(dataSize));
			start = System.currentTimeMillis();
			out.write(eIpv4);
			readFromServer();
			dataSize += dataSize;
			packet++;
		}
		System.out.println("---------Encrypted UDPwDATA Sent-------");
		System.out.printf("   Average Transmission Time: %.2fms", (time/10) );
	}

	public static void readFromServer() throws Exception {
		double tempTime;
		byte[] read = new byte[4];
		in.read(read);
		if(firstTime) { 
			System.out.printf("Server> Packet%2d: %8s\n",
	                packet, DatatypeConverter.printHexBinary(read));
		} else {
			stop = System.currentTimeMillis();
			tempTime = stop - start;
			time += (stop - start);
			System.out.printf("Server> Packet%2d: %8s  Time: %2.0fms\n",
                packet, DatatypeConverter.printHexBinary(read), tempTime);
		}
	}

	public static byte[] udpPacket(int size) throws Exception {
		// make data
		byte[] data = new byte[size];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) r.nextInt();
		}

		int length = 8 + size;
		byte version = 4;
		byte hLen = 5;
		byte Tos = 0;
		short tLength = (short) ((hLen * 4) + (8 + size));
		short iD = 0;
		short flag = 0;
		short offSet = 1024;
		byte TTL = 50;
		byte protocol = 17;
		short checksum = 0;

		// make Psuedo Header
		ByteBuffer psuedoHeader = ByteBuffer.allocate(length);
		psuedoHeader.putShort((short) 51742);
		psuedoHeader.putShort((short) 38008);
		psuedoHeader.putShort((short) length);
		psuedoHeader.putShort((short) 0);
		if (firstTime) {
			psuedoHeader.put(cipherText);
		} else {
			psuedoHeader.put(data);
		}

		// make UDP Header
		byte[] udp = new byte[length];
		ByteBuffer udpWrap = ByteBuffer.wrap(udp);
		udpWrap.putShort((short) 51742);
		udpWrap.putShort((short) 38008);
		udpWrap.putShort((short) length);
		if (firstTime) {
			udpWrap.putShort(udpChecksum(length, cipherText, psuedoHeader));
			udpWrap.put(cipherText);
		} else {
			udpWrap.putShort(udpChecksum(length, data, psuedoHeader));
			udpWrap.put(data);
		}

		// make IPV4 Header
		byte[] ipv4 = new byte[tLength];
		ByteBuffer wrap = ByteBuffer.wrap(ipv4);
		wrap.put((byte) (((version & 0xf) << 4) | (hLen & 0xf)));
		wrap.put(Tos);
		wrap.putShort(tLength);
		wrap.putShort(iD);
		wrap.putShort((short) (((flag & 0x7) << 13) | (offSet & 0x1ffff) << 4));
		wrap.put(TTL);
		wrap.put(protocol);
		wrap.putShort(checksum);
		wrap.putInt(sourceAddress);
		wrap.putInt(destAddress);
		checksum = Checksum(wrap, hLen);
		wrap.put(udp);

		return ipv4;
	}

	// convert ip address
	public static int ipConvert(String ip) {
		int result = 0;
		String[] hex = ip.split("\\.");
		for (int i = 0; i < 4; i++) {
			result |= Integer.valueOf(hex[i]) << ((3 - i) * 8);
		}
		return result;
	}

	// checksum for ipv4
	public static short Checksum(ByteBuffer wrap, int hLen) {
		short result;
		wrap.rewind();
		int n = 0;
		for (int i = 0; i < hLen * 2; ++i) {
			n += 0xffff & wrap.getShort();
		}
		n = ((n >> 16) & 0xffff) + (n & 0xffff);
		result = (short) (~n & 0xffff);
		wrap.putShort(10, result);
		return result;
	}

	// checksum for udp
	public static short udpChecksum(int length, byte[] data, ByteBuffer pheader) {
		// udp header, 8 bytes
		int n = 0;
		pheader.rewind();
		n += ((sourceAddress >> 16) & 0xffff) + (sourceAddress & 0xffff);
		n += ((destAddress >> 16) & 0xffff) + (destAddress & 0xffff);
		n += (byte) 17 & 0xffff;
		n += length & 0xffff;
		for (int i = 0; i < length / 2; ++i) {
			n += 0xffff & pheader.getShort();
		}
		if (length % 2 > 0) {
			n += (pheader.get() & 0xff) << 8;
		}
		n = ((n >> 16) & 0xffff) + (n & 0xffff);
		short result = (short) (~n & 0xffff);
		return result;
	}
}