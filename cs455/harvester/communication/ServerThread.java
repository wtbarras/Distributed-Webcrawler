package cs455.harvester.communication;
//Author: Tiger Barras
//ServerThread.java
//Waits for incoming connections, then opens a socket with them

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import cs455.harvester.communication.RecieverThread;
import cs455.harvester.crawler.Crawler;

//I think this needs to run on port 0, which will make it find an acceptable port

public class ServerThread extends Thread{

	Crawler crawler; //The node that started this ServerThread
	ServerSocket serverSocket;
	int portNum; //Port that the ServerThread object will listen on
	ConnectionCache cache; //Shared object with the node that holds connections

	//These are just used for messageWithId() to give some context to the message
	long id;
	String name;

	public ServerThread(int pn, Crawler c)throws IOException{ //Port number to listen to, cache to add sockets to
		crawler = c;
		serverSocket = new ServerSocket(pn);
		//Get the port number from the socket. If the user specifies port zero,
		  //the socket will pick a usable port on its own
		this.portNum = serverSocket.getLocalPort();
		System.out.println("ServerThread port: " + this.portNum);
		//node.setPortNum(this.portNum);//Report back to node where you are
		cache = crawler.getConnectionCache();

		id = this.getId();
		name = this.getName();
	}//End constructor

	//This is where execution begins when this thread is created
	public void run(){
		Socket socket;
		try{ //Listen at port portNum, and open socket to an incoming connection
			while(true){
				System.out.println("ServerThread: Waiting for connection");
				//messageWithId("Ready to connect. . .");
				socket = serverSocket.accept();
				System.out.println("ServerThread: Accepted connection");
				//messageWithId("Socket Generated");
				Connection connection = new Connection(crawler, socket);
				//messageWithId("Connection Generated");
				//Key is the address of the sender
				//This will probably break if you have more than one node running on a machine
				String index = socket.getInetAddress().getHostAddress();
				///(TO REMOVE)index = index.concat(String.valueOf(socket.getPort()));
				System.out.println("ServerThread: Adding connection w/ key: " + index);
				cache.add(index, connection);
				//messageWithId("Connection added to ConnectionCache");
				//Open up new Connection
			}
		}catch(IOException e){
			messageWithId("Error opening Server Socket");
			messageWithId("Error: " + e);
			System.exit(-1);
		}//End try/catch
	}//End run

	public int getPortNum(){
		return portNum;
	}//End getPortNum

	private void messageWithId(String message){
		System.out.printf("Thread (%s:%d): %s\n", name, id, message);
	}//End messageWithId

}//End class
