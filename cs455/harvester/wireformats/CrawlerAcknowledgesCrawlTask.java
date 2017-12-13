package cs455.harvester.wireformats;

/*
 *Author: Tiger Barras
 *Task.java
 *Reads one webpage
 */

import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class CrawlerAcknowledgesCrawlTask implements Event{

	private byte[] message;
	private final int messageType = 2;
	private int successStatus;

	public CrawlerAcknowledgesCrawlTask(byte[] data){
		this.message = data;

		try{
			ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
			DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));

			din.readInt();//Read past messageType, since that's already set
			this.successStatus = din.readInt();

			baInputStream.close();
			din.close();
		}catch(IOException e){
			System.out.println("RRRS: Error Unmarshalling");
			System.out.println(e);
		}
	}//End unmarshall constructor

	public CrawlerAcknowledgesCrawlTask(int ss){
		this.successStatus = ss;

		try{
			message = null;
			ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
			DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

			dout.writeInt(this.messageType);
			dout.writeInt(this.successStatus);

			dout.flush();
			message = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();
		}catch(IOException e){
			System.out.println("ONSR: Error Marshalling");
			System.out.println(e);
		}
	}//End marshall constructor


	public int getSuccessStatus(){
		return this.successStatus;
	}//End getSuccessStatus

	public int getType(){
		return this.messageType;
	}//End getType

	public byte[] getBytes(){
		return this.message;
	}//end getBytes

	public String toString(){
		return "CrawlerAcknowledgesCrawlTask\n Success Status: "+this.successStatus;
	}//End toString

}
