//by Hardeep Singh

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

import javax.xml.bind.DatatypeConverter;

public class UdpClient {
	public static InputStream in;
	public static OutputStream out;
	public static byte[] array;
	public static int sourceAddress;
	public static int destAddress;
	public static Random r = new Random();
	public static int port;
	public static double time = 0;

	public static void main(String[] args) throws Exception {
		Socket socket = new Socket("45.50.5.238", 38008);
		in = socket.getInputStream();
		out = socket.getOutputStream();
		System.out.println("Connection: " + socket);
		sourceAddress = ipConvert("192.168.1.8");
		destAddress = ipConvert("76.91.123.97");
		//ipv4Packet();
		int size = 2;
		int packet = 1;
		while (size != 8192) {
			System.out.println("Packet: " + packet + "\nData Size: " + size);
			udpPacket(size);
			size = size + size;
			packet++;
		}
		System.out.println("Average Round Trip Time: " + time);
	}

	// ************************************************************************************
	public static void ipv4Packet() throws Exception {
		byte version = 4;
		byte hLen = (byte) 5;
		byte Tos = 0;
		short tLength = (short) (20 + 4); // 4 is size of the data
		short iD = 0;
		short flag = 0;
		short fOffset = 1024;
		byte TTL = 50;
		byte protocol = 17;
		short checksum = 0;

		// Enter the values in the array
		byte[] ipv4 = new byte[tLength];
		ByteBuffer wrap = ByteBuffer.wrap(ipv4);
		wrap.put((byte) (((version & 0xf) << 4) | (hLen & 0xf)));
		wrap.put(Tos);
		wrap.putShort(tLength);
		wrap.putShort(iD);
		wrap.putShort((short) (((flag & 0x7) << 13) | (fOffset & 0x1ffff) << 4));
		wrap.put(TTL);
		wrap.put(protocol);
		wrap.putShort(checksum);
		wrap.putInt(sourceAddress);
		wrap.putInt(destAddress);
		checksum = CheckSum(wrap, hLen);
		wrap.putInt((int) 0xDEADBEEF);
		out.write(ipv4);
		array = new byte[4];
		in.read(array);
		port = ((int) (array[0] << 8) + (array[1] & 0xff));
		System.out
				.println("Server: " + DatatypeConverter.printHexBinary(array));
	}

	// ************************************************************************************
	public static void udpPacket(int size) throws Exception {
		// data
		double timeAverage = 0;
		double start;
		double stop;
		int length = 8 + size;
		byte[] data = new byte[size];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) r.nextInt();
		}

		byte version = 4;
		byte hLen = (byte) 5;
		byte Tos = 0;
		short tLength = (short) ((hLen * 4) + (8 + data.length));
		short iD = 0;
		short flag = 0;
		short fOffset = 1024;
		byte TTL = 50;
		byte protocol = 17;
		int sourceAddress = ipConvert("192.168.1.8");
		int destAddress = ipConvert("76.91.123.97");
		short checksum = 0;

		// *******************PsuedoHeader******************
		ByteBuffer pheader = ByteBuffer.allocate(length);
		pheader.putShort((short) 51248);
		pheader.putShort((short) 38003);
		pheader.putShort((short) length);
		pheader.putShort((short) 0);
		pheader.put(data);

		// **********Make UDP*******************************
		byte[] udp = new byte[8 + data.length];
		ByteBuffer udpBuff = ByteBuffer.wrap(udp);
		udpBuff.putShort((short) 51248); // dest port
		udpBuff.putShort((short) 38003); // dest port
		udpBuff.putShort((short) udp.length);
		udpBuff.putShort(udpChecksum(udp.length, data, pheader));
		udpBuff.put(data);

		// ********************wrap it in ipv4**************
		byte[] udpIPv4 = new byte[tLength];
		ByteBuffer wrap = ByteBuffer.wrap(udpIPv4);
		wrap.put((byte) (((version & 0xf) << 4) | (hLen & 0xf)));
		wrap.put(Tos);
		wrap.putShort(tLength);
		wrap.putShort(iD);
		wrap.putShort((short) (((flag & 0x7) << 13) | (fOffset & 0x1ffff) << 4));
		wrap.put(TTL);
		wrap.put(protocol);
		wrap.putShort(checksum);
		wrap.putInt(sourceAddress);
		wrap.putInt(destAddress);
		checksum = CheckSum(wrap, hLen);
		wrap.put(udp);
		out.write(udpIPv4);
		start = System.currentTimeMillis();
		byte[] array1 = new byte[4];
		in.read(array1);
		stop = System.currentTimeMillis();
		System.out.println("Server: "
				+ DatatypeConverter.printHexBinary(array1));
		time += stop - start;

	}

	// ************************************************************************************
	public static int ipConvert(String ip) {
		int result = 0;
		String[] hex = ip.split("\\.");
		for (int i = 0; i < 4; i++) {
			result |= Integer.valueOf(hex[i]) << ((3 - i) * 8);
		}
		return result;
	}

	// ************************************************************************************
	public static short CheckSum(ByteBuffer wrap, int hLen) {
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

	// ************************************************************************************
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
	// ************************************************************************************
}
