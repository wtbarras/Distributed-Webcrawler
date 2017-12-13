package cs455.harvester.communication;
//Author: Tiger Barras
//ConnectionCache.java
//Holds a collection of Connections.
//This will be inherited by two classes:
//  RegisterConnectionCache
//  NodeConnectionCache
//These will have error/sanity checking
//  e.g. NodeConnectionCache will no let you have more that four connections

//This is SHORT TERM storage. If a connection is valid, it will be added to the
  //node's routing table for long term storage/retrieval

import cs455.harvester.communication.Connection;

import java.util.HashMap;

public class ConnectionCache{

	HashMap<String, Connection> cache = new HashMap<String, Connection>();

	public void add(String index, Connection c){
		synchronized(cache){
			cache.put(index,c);
		}
	}//End add

	public Connection get(String index){
		synchronized(cache){
			return cache.get(index);
		}
	}//End get

	public int size(){
		synchronized(cache){
			return cache.size();
		}
	}//End size

}//End class
