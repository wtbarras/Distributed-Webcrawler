package cs455.harvester.wireformats;
/*
 *Author: Tiger Barras
 *Event.java
 *Parent interface for all message wireformats
 */


public interface Event{

	public int getType(); //Returns the integer ID for this message

	public byte[] getBytes(); //Returns the bytes of this event, for unmarshalling

	public String toString();

}
