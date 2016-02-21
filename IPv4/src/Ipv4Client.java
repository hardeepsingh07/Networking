import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

public class Ipv4Client {

	public static void main(String[] args) throws Exception {
		Socket socket = new Socket("76.91.123.97", 38003);
		Random r = new Random();
		BufferedReader read = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		OutputStream out = socket.getOutputStream();
		System.out.println("Connection: " + socket);
		int size = 2;
		int packet = 1;
		while (size != 8) {
			System.out.println("Packet Number: " + packet);
			System.out.println("Data Length: " + size);

			// Intalize the Values
			byte version = 4;
			System.out.println(version);
			int iHlength = 5;
			byte hLen = (byte) iHlength;
			byte Tos = 0;
			short tLength = (short) (hLen * 4 + size);
			short iD = 0;
			short flag = 0;
			short fOffset = 1024;
			byte TTL = 50;
			byte protocol = 6;
			short checksum = 0;
			int sourceAddress = ipConvert("192.168.1.8");
			int destAddress = ipConvert("76.91.123.97");

			// Enter the values in the array
			byte[] data = new byte[tLength];
			ByteBuffer wrap = ByteBuffer.wrap(data);
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

			// Make data byte array
			byte[] endData = new byte[size];
			for (int i = 0; i < size; i++) {
				endData[i] = (byte) r.nextInt();
			}
			
			// Attach to the end of the actual "wrap" array
			if (endData != null) {
				wrap.put(endData);
			}

			// Read/write to the server
			out.write(data);
			System.out.println(Arrays.toString(endData));
			System.out.println("Server Message: " + read.readLine());
			System.out.println();
			size = size + size;
			packet++;
		}

	}

	public static int ipConvert(String ip) {
		int result = 0;
		String[] hex = ip.split("\\.");
		for (int i = 0; i < 4; i++) {
			result |= Integer.valueOf(hex[i]) << ((3 - i) * 8);
		}
		return result;
	}

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

}
