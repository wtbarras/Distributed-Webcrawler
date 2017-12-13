package cs455.harvester.communication;
/*
 *Author: Tiger Barras
 *Sender.java
 *Object that will send messages over a socket connection
 */

import java.net.Socket;
import java.io.*;

public class Sender implements Runnable{

	Socket socket; //Socket Sender will talk in to
	DataOutputStream dout;
	byte[] message;

	public Sender(Socket s){
		socket = s;
		try{//Open printwriter on the output stream of the socket
			dout = new DataOutputStream(socket.getOutputStream());
		}catch(IOException e){
			System.out.println("Sender: Error opening Printwriter");
			System.out.println(e);
		}
	}//End constructor

	public synchronized void setMessage(byte[] data){
		message = data;
	}

	public synchronized void run(){
		int dataLength = message.length;
		try{
			dout.writeInt(dataLength);
			dout.write(message, 0, dataLength);
			dout.flush();
		}catch(IOException e){
			System.out.println("Sender: Error writing to DataOutputStream");
			System.out.println(e);
		}
	}//End write

}//End class
