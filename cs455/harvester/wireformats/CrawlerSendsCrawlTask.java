package cs455.harvester.wireformats;
/*
 *Author: Tiger Barras
 *CrawlerSendsCrawlTask.java
 *Sent from one crawler to another to pass off a out of domain task
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



public class CrawlerSendsCrawlTask implements Event{

	byte[] message;
	int messageType = 1;
	int crawlAddressLength;
	byte[] crawlAddressBytes;
	String crawlAddressString;
	int sendingAddressLength;
	byte[] sendingAddressBytes;
	String sendingAddressString;

	public CrawlerSendsCrawlTask(byte[] data){
		this.message = data;

		try{
    	ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
      DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
      din.readInt();//Read past messageType, since that's already set
      crawlAddressLength = din.readInt();
      //System.out.println(status);
      crawlAddressBytes = new byte[crawlAddressLength];
      din.readFully(crawlAddressBytes);

      sendingAddressLength = din.readInt();
      sendingAddressBytes = new byte[sendingAddressLength];
      din.readFully(sendingAddressBytes);
      try{
      	crawlAddressString = new String(crawlAddressBytes, "US-ASCII");
      	sendingAddressString = new String(sendingAddressBytes, "US-ASCII");
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

	}//End unmarshall constructor

	public CrawlerSendsCrawlTask(String crawlAddress, String sendingAddress){
		crawlAddressString = crawlAddress;
		sendingAddressString = sendingAddress;

    try{
      crawlAddressBytes = crawlAddressString.getBytes("US-ASCII");
      sendingAddressBytes = sendingAddressString.getBytes("US-ASCII");
    }catch(UnsupportedEncodingException e){
      System.out.println("RRRS: Error, US-ASCII not supported");
      System.out.println(e);
    }

		crawlAddressLength = crawlAddressBytes.length;
		sendingAddressLength = sendingAddressBytes.length;

    try{
	    message = null;
	    ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
	    DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));
	    dout.writeInt(messageType);
	    dout.writeInt(crawlAddressLength);
	    dout.write(crawlAddressBytes);
	    dout.writeInt(sendingAddressLength);
	    dout.write(sendingAddressBytes);
	    dout.flush();
	    message = baOutputStream.toByteArray();
    	baOutputStream.close();
      dout.close();
    }catch(IOException e){
    	System.out.println("ONSR: Error Marshalling");
      System.out.println(e);
    }

	}//End marshall constructor

	public String getCrawlAddress(){
		return this.crawlAddressString;
	}//End getCrawlAddress

	public String getSendingAddress(){
		return this.sendingAddressString;
	}//End getSendingAddress

	public int getType(){
		return this.messageType;
	}//End messageType

	public byte[] getBytes(){
		return this.message;
	}//End getBytes

	public String toString(){
		String toReturn = "CrawlerSendsCrawlTask:\n";
		toReturn = toReturn.concat(this.crawlAddressString + "\n");
		toReturn = toReturn.concat(this.sendingAddressString + "\n");
		return toReturn;
	}

}
