import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Scanner;


public class server {
	// toggle showBytes to display the bytes being sent
	   static boolean showBytes = false;
	   static String input = "";
	   static int payloadSize;

	   public static void main(String args[]) throws Exception 
	   {
	      try
	      {
	    	 Socket socket = new Socket("76.91.123.97", 38003);
	    	 readFromServer rfs = new readFromServer(socket);
	    	 new Thread(rfs).start();
	         while(true)
	         {
	            // connect to the server
	            System.out.println("\n-----------------------------------------------------------------");
	            System.out.println("Connected: " + socket);
	            System.out.println("-----------------------------------------------------------------");

	            // ask user for size of data & wait for exit command
	            System.out.println("Enter a payload/data size or type exit to stop sending packets.");
	            Scanner sc = new Scanner(System.in);
	            input = sc.nextLine();
	            try { payloadSize = Integer.parseInt(input); }
	            catch(NumberFormatException e)
	            {
	               if(!input.equals("exit"))
	                  System.out.println("ERROR: You did not enter a valid input. Please reload the program.");

	               System.exit(0);
	            }

	            //creates and fill payload data
	            byte[] payloadData = new byte[payloadSize];
	            Random rand = new Random();
	            for(int i = 0; i < payloadSize; i++) { payloadData[i] = (byte) rand.nextInt(); }

	            // create values
	            byte ipVersion = 4;
	            int Ipv4HeaderLength = 5;
	            byte hLength = (byte) Ipv4HeaderLength;
	            byte tos = 0;
	            short totalLength = (short) (hLength * 4 + payloadData.length);
	            short id = 0;
	            byte flags = 0;
	            short fragmentOffset = 1024;
	            byte ttl = 50;
	            byte protocol = 6;
	            short checksum = 0;
	            int sourceAddress = toIpv4Address(InetAddress.getLocalHost().getHostAddress()); 
	            int destinationAddress = toIpv4Address("76.91.123.97");

	            // put values into byte buffer
	            byte[] b = new byte[totalLength];
	            ByteBuffer buf = ByteBuffer.wrap(b);
	            buf.put((byte) (((ipVersion & 0xf) << 4) | (hLength & 0xf)));
	            buf.put(tos);
	            buf.putShort(totalLength);
	            buf.putShort(id);
	            buf.putShort((short) (((flags & 0x7) << 13) | (fragmentOffset & 0x1fff) << 4));
	            buf.put(ttl);
	            buf.put(protocol);
	            buf.putShort(checksum);
	            buf.putInt(sourceAddress);
	            buf.putInt(destinationAddress);
	            calculateChecksum(buf, hLength, checksum);
	            if(payloadData != null) buf.put(payloadData);

	            if(showBytes == true) printByteData(b);

	            // send bytes
	            OutputStream os = socket.getOutputStream();
	            DataInputStream is = new DataInputStream(socket.getInputStream());
	            os.write(b);

	            // wait a second before restarting loop. this gives time for server's response
	            try { Thread.sleep(1000); }
	            catch (InterruptedException iex) {}
	         }
	      }
	      catch (IOException e) {}
	   }
	//----------------------------------------------------------------------------------------------------
	   public static int toIpv4Address(String ipAddress)
	   {
	      if (ipAddress == null) throw new IllegalArgumentException("format incorrect.");
	      String[] octets = ipAddress.split("\\.");
	      if (octets.length != 4) throw new IllegalArgumentException("format incorrect.");

	      int result = 0;
	      for (int i = 0; i < 4; ++i) { result |= Integer.valueOf(octets[i]) << ((3-i)*8); }
	      System.out.println(result);
	      return result;
	   }
	//----------------------------------------------------------------------------------------------------
	   public static void calculateChecksum(ByteBuffer buf, byte hLength, short checksum)
	   {
	      if (checksum == 0)
	      {
	         buf.rewind();
	         int x = 0;
	         for (int i = 0; i < hLength * 2; ++i) { x += 0xffff & buf.getShort(); }
	         x = ((x >> 16) & 0xffff) + (x & 0xffff);
	         checksum = (short) (~x & 0xffff);
	         buf.putShort(10, checksum);
	      }
	   }
	//----------------------------------------------------------------------------------------------------
	   public static void printByteData(byte[] b)
	   {
	      System.out.print("\nbytes sent: ");
	      for ( byte testing : b ) System.out.print(testing + " ");
	      System.out.println();
	   }
	//----------------------------------------------------------------------------------------------------
	}// end of Ipv4Client class
	// Reads text from the server, displays to console.
	class readFromServer implements Runnable
	{
	   Socket socket;
	   readFromServer(Socket s) { socket = s; }

	   public void run()
	   {
	      try
	      {
	         BufferedReader streamIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	         String line;
	         while((line = streamIn.readLine()) != null) System.out.println(line);
	      }
	      catch (IOException e) {}
	   }
	}// end of readFromServer class
	//--------------------------------------------------------

