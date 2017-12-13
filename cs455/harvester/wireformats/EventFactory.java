package cs455.harvester.wireformats;
/*
*Author: Tiger Barras
*EventFactory.java
*Creates events out of byte arrays
*/

import cs455.harvester.wireformats.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;

public class EventFactory{

	private static final EventFactory instance = new EventFactory();

	protected EventFactory(){
		//Only exists to defeat instantiation
	}//End constructor

	//This is synchronized because that if statement is not
	  //Thread safe
	public static EventFactory getInstance(){
		return instance;
	}//End getInstance

	public synchronized static Event manufactureEvent(byte[] data, Socket s)throws UnknownHostException{
		Event event = null;

		int type = -1;

		try{
			ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
			DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
			type = din.readInt();
			baInputStream.close();
			din.close();
		}catch(IOException e){
			System.out.println("EventFactor: Error reading event type");
			System.out.println(e);
		}

		//Add logic here to turn the byte stream into an event...homie
		switch(type){
			case 1:  	event = new CrawlerSendsCrawlTask(data);
								break;
			case 2:		event = new CrawlerAcknowledgesCrawlTask(data);
								break;
			case 3:   event = new CrawlerUpdateCompleteStatus(data);
								break;
			default: System.out.println("That message is not even a real message!");
							 System.exit(-1);
		}

		return event;
	}//End manufactureEvent
}//End class
