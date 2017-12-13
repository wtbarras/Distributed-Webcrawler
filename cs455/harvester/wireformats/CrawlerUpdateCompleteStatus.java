package cs455.harvester.wireformats;

/*
 *Author: Tiger Barras
 *Event.java
 *Let all other crawlers know whether or not you're finished
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



public class CrawlerUpdateCompleteStatus implements Event{

	private byte[] message;
	private final int messageType = 3;
	private int length;
	private byte[] domainBytes;
	private String domainString;
	private int completeStatus;

	public CrawlerUpdateCompleteStatus(byte[] data){
		this.message = data;

		try{
			ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
			DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
			din.readInt();//Read past messageType, since that's already set

			length = din.readInt();
			//System.out.println(status);
			domainBytes = new byte[length];
			din.readFully(domainBytes);
			this.completeStatus = din.readInt();

			try{
				domainString = new String(domainBytes, "US-ASCII");
			}catch(UnsupportedEncodingException e){
				System.out.println("RRRS: Error, US-ASCII not supported");
				System.out.println(e);
			}
			baInputStream.close();
			din.close();
		}catch(IOException e){
			System.out.println("RRRS: Error Unmarshalling");
			System.out.println(e);
		}
	}//End constructor

	public CrawlerUpdateCompleteStatus(String domain, int status){
		this.domainString = domain;
		this.completeStatus = status;

		try{
			domainBytes = domainString.getBytes("US-ASCII");
		}catch(UnsupportedEncodingException e){
			System.out.println("RRRS: Error, US-ASCII not supported");
			System.out.println(e);
		}

		length = domainBytes.length;

		try{
			message = null;
			ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
			DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));
			dout.writeInt(messageType);
			dout.writeInt(length);
			dout.write(domainBytes);
			dout.writeInt(completeStatus);
			dout.flush();
			message = baOutputStream.toByteArray();
			baOutputStream.close();
			dout.close();
		}catch(IOException e){
			System.out.println("ONSR: Error Marshalling");
			System.out.println(e);
		}
	}//End constructor


	public int getType(){
		return this.messageType;
	}//End getType

	public byte[] getBytes(){
		return this.message;
	}//end getbytes

	public String toString(){
		return "CrawlerUpdateCompleteStatus: \n" + domainString + " is " + completeStatus;
	}//end toString

	public String getDomain(){
		return this.domainString;
	}

	public int getStatus(){
		return this.completeStatus;
	}

}//End class
