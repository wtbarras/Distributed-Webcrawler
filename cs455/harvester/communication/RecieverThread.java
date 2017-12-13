package cs455.harvester.communication;
/*
*Author: Tiger Barras
*RecieverThread.java
*Object that will send messages over a socket connection
*/

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cs455.harvester.crawler.Crawler;
import cs455.harvester.wireformats.*;
// import cs455.overlay.node.MessageNode;
// import cs455.overlay.node.Registry;


public class RecieverThread extends Thread{

	Crawler crawler;
	Socket socket; //Socket that Reciever will listen to
	DataInputStream din;
	//Lock dataStreamLock = new ReentrantLock();

	public RecieverThread(Crawler c, Socket s){
		crawler = c;

		socket = s;
		try{
			din = new DataInputStream(socket.getInputStream());
		}catch(IOException e){
			System.out.println("RecieverThread: Error opening DataInputStream");
			System.out.println(e);
		}
	}//End constructor

	public synchronized void run(){
		while(socket != null){
			try{
				//Critical section
				//Don't want multiple threads messing with the dataStream
				//dataStreamLock.lock();
				if(din.available() != 0){

					int dataLength = din.readInt(); //Number of bytes to read
					byte[] data = new byte[dataLength];
					din.readFully(data, 0, dataLength);//Full datagram from sender

					//Grab the instance of EventFactory
					//System.out.println("RecieverThread: About to get instance");
					EventFactory eventFactory = EventFactory.getInstance();
					try{
						//Turn the data into a wireformat event
						//System.out.println("RecieverThread: About to manufacture event");
						Event event = eventFactory.manufactureEvent(data, socket);
						//System.out.println("RecieverThread: About to call onEvent");
						//Pass that back to the node
						this.crawler.onEvent(event);
						//System.out.println("RecieverThread: Returned from onEvent");
					}catch(UnknownHostException e){
						System.out.println("Receiver: Error constructing Event");
						System.out.println(e);
					}
				}
			}catch(IOException e){
				System.out.println("Error reading from BufferedReader");
				System.out.println(e);
			}

			//Release the lock so another thread can read stuff
			//dataStreamLock.unlock();
		}
	}

}//End class
