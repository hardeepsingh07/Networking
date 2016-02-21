// by Hardeep Singh

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import javax.xml.bind.DatatypeConverter;

public class TcpClient {

	public static InputStream in;
	public static OutputStream out;
	public static BufferedReader packetRead;
	public static Random r = new Random();
	public static int pNo = 0;
	public static int size;
	public static int sNumber = 7;
	public static int ackNumber = 0;
	public static int sNumIncrement = 1;
	public static boolean ack = false;
	public static boolean fin = false;
	public static int sourceAddress = ipConvert("192.168.1.8");
	public static int destAddress = ipConvert("76.91.123.97");

	public static void main(String[] args) throws Exception {
		Socket socket = new Socket("45.50.5.238", 38006);
		System.out.println(socket);
		in = socket.getInputStream();
		out = socket.getOutputStream();
		packetRead = new BufferedReader(new InputStreamReader(in));
		
		System.out.println("-----Initiating Hand-Shake-----");
		size = 0;							//Size 0 for handshake
		synTCP();							//SYN Call
		sNumber++;							//Incremet Sequence by 1
		ackNumber = readTcpHeader();		//Read TCP
		ack = true;							//ack flag set
		synTCP();							//ACK Call
		System.out.println("-----Hand-Shake Complete-----");
		
		
		System.out.println("\n-----Initiating TCPwData Tramission-----");
		size = 2;
		while(size != 8192)	{
			sNumber += sNumIncrement;		//Increment Sequence
			//System.out.println("sNumber = " + sNumber);
			ackNumber++;					//Increment Acknowledgment
			pNo++;							//Packet Number Increment
			synTCP();						//Send TCP with Data
			size += size;					//Increment Data
			sNumIncrement += sNumIncrement;	//Get Increment for Sequence
		}
		System.out.println("-----TCPwData Transmission Complete-----");
		
		
		System.out.println("\n-----Initiating Connection Teardown-----");
		size = 0;
		ack = false;
		fin = true;
		synTCP();
		
		int temp = readTcpHeader();		
		System.out.println("Server> Packet 1 Received!");
		int temp1 = readTcpHeader();
		System.out.println("Server> Packet 2 Received!");
		
		fin = false;
		ack = true;
		synTCP();
		System.out.println("-----Connection Teardown Complete-----");

	}
	public static void synTCP()	throws Exception {
		byte[] data = new byte[size];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) r.nextInt();
		}
		
		boolean CWR, ECE, URG, ACK, PSH, RST, SYN, FIN;
		CWR = ECE = URG = ACK = PSH = RST = SYN = FIN = false;
		SYN = true;
		ACK = ack;
		FIN = fin;
		byte dataOffset = (byte) 5;
		int dataOffsetFlags;
		dataOffsetFlags = dataOffset << 12;		//add dataoffset/reserved
		dataOffsetFlags |= (CWR ? 0x80 : 0);	//Start adding flags
		dataOffsetFlags |= (ECE ? 0x40 : 0);	
		dataOffsetFlags |= (URG ? 0x20 : 0);	
		dataOffsetFlags |= (ACK ? 0x10 : 0);
		dataOffsetFlags |= (PSH ? 0x08 : 0);
		dataOffsetFlags |= (RST ? 0x04 : 0);
		dataOffsetFlags |= (SYN ? 0x02 : 0);
		dataOffsetFlags |= (FIN ? 0x01 : 0);	//End adding flags

		byte version = 4;
		byte hLen = (byte) 5;
		byte Tos = 0;
		short tLength = (short) ((hLen * 4) + (20 + data.length)); 
		short iD = 0;
		short flag = 0;
		short fOffset = 1024;
		byte TTL = 50;
		byte protocol = 6;
		short checksum = 0;

		
		ByteBuffer pheader = ByteBuffer.allocate(12 + (20 + data.length)); 
		pheader.putShort((short) 54689);
		pheader.putShort((short) 38006);
		pheader.putInt(sNumber);
		pheader.putInt(ackNumber);
		pheader.putShort((short) dataOffsetFlags);
		pheader.putShort((short) 0); // Window Size
		pheader.putShort((short) 0); // Checksum
		pheader.putShort((short) 0); // Urgent pointer
		if (size != 0) { pheader.put(data);	}

		
		byte[] tcp = new byte[20 + data.length];
		ByteBuffer bTcp = ByteBuffer.wrap(tcp);
		bTcp.putShort((short) 54689);					//Source Port
		bTcp.putShort((short) 38006);					//Dest Port
		bTcp.putInt(sNumber);				
		bTcp.putInt(ackNumber);
		bTcp.putShort((short) dataOffsetFlags);			//DataOffSet/Flags
		bTcp.putShort((short) 0); 						// Window Size
		bTcp.putShort(TcpChecksum(tcp.length, pheader)); 
		bTcp.putShort((short) 0);					  	// Urgent pointer
		if (size != 0) { bTcp.put(data); }

		
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
		wrap.put(tcp);
		out.write(ipv4);
		byte[] array = new byte[4];
		in.read(array);
		if(size != 0)	{
			System.out
			.println("Server> Packet" + pNo + ": " + 
			        DatatypeConverter.printHexBinary(array));
		} else {
		System.out.println("Server> " + DatatypeConverter.printHexBinary(array));
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
	
	public static int readTcpHeader() throws Exception {
		int result;
		byte[] serverTCP = new byte[20];
		in.read(serverTCP);
		byte[] serverSEQ = { serverTCP[4], serverTCP[5], serverTCP[6],
				serverTCP[7] };
		ByteBuffer n = ByteBuffer.wrap(serverSEQ);
		result = n.getInt() + 1;
		return result;
	}
	
	public static short TcpChecksum(int length, ByteBuffer pheader) {
		int n = 0;
		pheader.rewind();
		n += ((sourceAddress >> 16) & 0xffff) + (sourceAddress & 0xffff);
		n += ((destAddress >> 16) & 0xffff) + (destAddress & 0xffff);
		n += (byte) 6 & 0xffff;
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
