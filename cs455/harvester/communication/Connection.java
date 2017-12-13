package cs455.harvester.communication;
/*
*Author: Tiger Barras
*Connection.java
*Holds pairs of senders and receiver threads. These will be stored in the cache
*/

import cs455.harvester.crawler.Crawler;
//import cs455.harvester.node.MessageNode;
//import cs455.harvester.node.Registry;
import cs455.harvester.communication.RecieverThread;
import cs455.harvester.communication.Sender;
//import cs455.harvester.wireformats.*;
import java.net.Socket;

public class Connection{

	private Crawler crawler;
	private Socket socket;
	private RecieverThread reciever;
	private Sender sender;

	public Connection(Crawler c, Socket s){
		crawler = c;
		socket = s;
		this.reciever = new RecieverThread(this.crawler, s);
		reciever.start();
		this.sender = new Sender(s);
	}

	public void write(byte[] data){
		sender.setMessage(data);
		Thread thread = new Thread(sender);
		thread.start();
	}

	public RecieverThread getReciever(){
		return this.reciever;
	}

	public Sender getSender(){
		return this.sender;
	}

}
