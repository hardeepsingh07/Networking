import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class Ipv6Client {

	public static void main(String[] args) throws Exception {
		Socket socket = new Socket("76.91.123.97", 38004);
		InputStream is = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		System.out.println("Connection: " + socket);
		int size = 2;
		int packet = 1;
		while (size != 8192) {
			System.out.println("Packet: " + packet);
			System.out.println("Data Size: " + size);
			Random r = new Random();
			byte[] data = new byte[size];
			for (int i = 0; i < data.length; i++) {
				data[i] = (byte) r.nextInt();
			}
			byte version = 6;
			byte hTC = 0; // temp to fill zeros
			byte trafficClass = 0;
			int firstPartFT = 0;
			short flowLabel = 0;
			short pLlength = (short) size;
			byte nHeader = 17;
			byte hopLimit = 20;

			// fill the array
			byte[] send = new byte[40 + size];
			ByteBuffer b = ByteBuffer.wrap(send);
			b.put((byte) (((version & 0xf) << 4) | (hTC & 0xf))); 
			b.put((byte) (((trafficClass & 0xf) << 4) | (firstPartFT & 0xf))); 
			b.putShort(flowLabel); // flowtable
			b.putShort(pLlength); // payload length
			b.put(nHeader); // header
			b.put(hopLimit); // hoplimit
			// Start of Source Address
			b.putLong((long) 0x0); // 0.0.0.0.0.0.0.0
			b.putInt((int) 0x0000ffff); // 0.0.255.255
			b.putInt((int) 0x42d7c3c1); // 66.215.195.193
			// Start of destination Address
			b.putLong((long) 0x0); // 0.0.0.0.0.0.0.0
			b.putInt((int) 0x0000ffff); // 0.0.255.255
			b.putInt((int) 0x4c5b7b61); // 76.91.123.97
			b.put(data); // Random Data
			out.write(send);
			byte[] array = new byte[4];
			is.read(array);
			System.out.println("Server: "
					+ DatatypeConverter.printHexBinary(array));
			size = size + size;
			packet++;
		}
	}
}
