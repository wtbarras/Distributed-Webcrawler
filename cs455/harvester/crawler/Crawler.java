package cs455.harvester.crawler;

/*
 *Author: Tiger Barras
 *Crawler.java
 *Crawls webpages looking for links, and slowly build a graph of a domain
 */

import cs455.harvester.threadpool.ThreadPoolManager;
import cs455.harvester.task.*;
import cs455.harvester.communication.ServerThread;
import cs455.harvester.communication.Connection;
import cs455.harvester.communication.ConnectionCache;
import cs455.harvester.wireformats.Event;
import cs455.harvester.wireformats.CrawlerSendsCrawlTask;
import cs455.harvester.wireformats.CrawlerUpdateCompleteStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.MalformedURLException;



public class Crawler{

	//Basic info on where the crawler is operating
	private final int portnum;
	private final int threadPoolSize;
	private final String rootUrl;
	private final String pathToConfigFile;
	private String rootFilePath;

	//The threadpool manager. All tasks go through this guy
	private ThreadPoolManager manager;

	//Objects that handle all the communication with other crawlers
	private ConnectionCache cache = new ConnectionCache();
	//Info from config file
	String[] hostNames = new String[7];
	int[] ports = new int[7];
	String[] domains = new String[7];
	//Count for how many crawl requests are pending
	private Integer pendingCrawlRequests = new Integer(0);

	//This maps a domain to it's complete status
	//When all of these are mapped to true, then it's time to exit
	HashMap<String, Boolean> completeStatus = new HashMap<String, Boolean>();


	public Crawler(int _portnum, int _threadPoolSize, String _rootUrl, String _pathToConfigFile){
		portnum = _portnum;
		threadPoolSize = _threadPoolSize;
		rootUrl = _rootUrl;
		pathToConfigFile = _pathToConfigFile;
	}//End constructor

	public void beginCrawling(){
		CrawlTask firstTask = new CrawlTask(this.rootUrl, 0, this.rootFilePath);
		manager.addTask(firstTask);
	}//End beginCrawling

	//Starts thread pool and server
	public void initialize(){
		this.startServer();
		this.startThreadPoolManager();
		this.createDirectory();
		this.parseConfigFile();
		this.setupConnections();
	}//End initailize

	private void startServer(){
		try{
			ServerThread server = new ServerThread(this.portnum, this);
			server.start();
			// System.out.println("Crawler: Server Created and Running");
		}catch(IOException exception){
			System.out.println("Crawler: Error initializing ServerThread");
			System.out.println(exception);
		}

		//Sleep after everything is set up, give the other crawlers a chance to set up
		try{
			Thread.sleep(10000);
		}catch(InterruptedException exception){
			System.out.println("Crawler: Interrupted from sleeping  *yawn*");
		}
	}//End startServer

	private void startThreadPoolManager(){
		System.out.println("Starting thread pool");
		manager = new ThreadPoolManager(this.threadPoolSize, this);
		Thread managerThread = new Thread(manager);
		managerThread.start();
	}//End startThreadPoolManager

	private void createDirectory(){
		//Turn the url parameter into a absolute file path under /tmp/wbarras
		this.generateRootFilePath();

		//Make sure /tmp/wbarras exists
		this.createUserDir();

		//Make sure the directory specified by rootFilePath exists
		this.createWorkingDirectory();

		//Set up nodes and disjoint subgraphs directories
		this.createSubDirectories();

	}//End createDirectory

	private void parseConfigFile(){
		File configFile = new File(this.pathToConfigFile);

		FileReader reader = null;
		try{
			reader = new FileReader(configFile);
		}catch(FileNotFoundException exception){
			System.out.println("Crawler: Error opening reader on config file");
			System.out.println(exception);
		}

		BufferedReader bufferedReader = new BufferedReader(reader);

		int count = 0;
		boolean selfFlag = false;
		try{
			String line = bufferedReader.readLine();
			while(line != null){//Read to end of file
				//The delimiter between the hostname and the port number
				int colonDelimiter = line.indexOf(":");
				//The delimiter between the portnumber and the domain to crawl
				int commaDelimiter = line.indexOf(",");
				String host = line.substring(0,colonDelimiter);
				int port = Integer.valueOf(line.substring(colonDelimiter+1,commaDelimiter));
				String domain = line.substring(commaDelimiter+1);

				//Set the whole map to false
				completeStatus.put(domain, false);

				//Check to see if this entry is the one for this crawler
				//We don't want to be connecting to it later if it is
				if(this.rootUrl.equals(domain)){
					selfFlag = true;
					line = bufferedReader.readLine();
					//count++;
					continue;
				}

				hostNames[count] = host+".cs.colostate.edu";
				ports[count] = port;
				domains[count] = domain;

				line = bufferedReader.readLine();

				count++;
			}
		}catch(IOException exception){
			System.out.println("Crawler: Error reading config file");
			System.out.println(exception);
		}

		if(selfFlag == false){
			System.out.println("Crawler: Url assigned to this crawler did not match the config file");
		}

		if(count != 7){
			System.out.println("Crawler: Did not find eight machines in config file");
			System.exit(-1);
		}else{
			// System.out.println(Arrays.toString(hostNames));
			// System.out.println(Arrays.toString(ports));
			// System.out.println(Arrays.toString(domains));
		}

	}//End parseConfigFile

	private void setupConnections(){
		for(int i = 0; i < this.hostNames.length; i++){
			String hostName = hostNames[i];
			int port = ports[i];
			String domain = domains[i];

			try{
				domain = new URL(domain).getHost();
				//Psych is a special flower
				//Deal with their bullshit
				if(domain.equals("http://www.colostate.edu")){
					domain = "http://www.colostate.edu/Depts/Psychology";
				}

				domains[i] = domain;
			}catch(MalformedURLException exception){
				System.out.println("Crawler: Error normalizing domain");
				System.out.println(exception);
			}

			System.out.println("Setting up connection to " + hostName + " on port " + port);

			if(port != this.portnum){
				System.out.println("Something is fucky with the ports");
			}

			try{
				InetAddress hostAddress = InetAddress.getByName(hostName);

				Socket socket = new Socket(hostName, port);

				Connection connection = new Connection(this, socket);
				this.cache.add(domain, connection);
			}catch(UnknownHostException exception){
				System.out.println("Crawler: Error creating InetAddress from host name");
				System.out.println(exception);
			}catch(IOException exception){
				System.out.println("Crawler: Error creating socket to " + hostName);
				System.out.println(exception);
			}

		}
	}

	public void sendTask(Event event, String recievingDomain){
		//If we're sending a crawl request, increment pendingCrawlRequests
		if(event.getType() == 1){
			synchronized(this.pendingCrawlRequests){
				this.pendingCrawlRequests++;
			}
		}

		//System.out.println("Sending task to domain " + recievingDomain);
		//Snag the connection that was set up to the recieving domain
		synchronized(this.cache){
			Connection connection = this.cache.get(recievingDomain);
			//Send it the message contained in the event
			connection.write(event.getBytes());
		}

	}//End sendTask

	//Create the /tmp/wbarras dir
	private void createUserDir(){
		File userDir = new File("/tmp/cs455-wbarras");

		boolean created = false;
		try{
			created = userDir.mkdir();
		}catch(SecurityException exception){
			System.out.println("Crawler: Error creating directory. Security Exception");
			System.out.println(exception);
		}

		if(created || userDir.exists()){
			System.out.println("Directory at " + userDir + " exists or was created");
		}else{
			System.out.println("Directory not created");
			System.out.println(userDir);
			System.out.println("Check to make sure /tmp exists");
			System.exit(-1);
		}
	}//End createUserDir

	private void createWorkingDirectory(){
		File parentDirectory = new File(this.rootFilePath);
		boolean created = false;
		try{
			created = parentDirectory.mkdir();
		}catch(SecurityException exception){
			System.out.println("Crawler: Error creating directory. Security Exception");
			System.out.println(exception);
		}

		if(created || parentDirectory.exists()){
			System.out.println("Directory at " + this.rootFilePath + " exists or was created");
		}else{
			System.out.println("Directory not created");
			System.out.println(this.rootFilePath);
			System.out.println("Check to make sure /tmp/cs455-wbarras exists");
			System.exit(-1);
		}
	}//End createWorkingDirectory

	private void createSubDirectories(){
		File nodes = new File(this.rootFilePath + "/nodes");
		File subgraphs = new File(this.rootFilePath + "/disjoint-subgraphs");

		try{
			nodes.mkdir();
			subgraphs.mkdir();
		}catch(SecurityException exception){
			System.out.println("Crawler: Error creating directory. Security Exception");
			System.out.println(exception);
		}

		if(nodes.exists() && subgraphs.exists()){
			System.out.println("Directory at " + this.rootFilePath + "/nodes exists or was created");
			System.out.println("Directory at " + this.rootFilePath + "/disjoint-subgraphs exists or was created");
		}else{
			System.out.println("Directory not created");
			System.out.println(this.rootFilePath);
			System.out.println("Check to make sure /tmp/cs455-wbarras exists");
			System.exit(-1);
		}
	}//End createSubDirectories

	private void generateRootFilePath(){
		//Clean up the rootUrl so there are no '/'s
		String cleanUrl = rootUrl;
		//Chop of leading http://
		if(cleanUrl.startsWith("http://")){
			cleanUrl = cleanUrl.substring(7,cleanUrl.length());
		}
		//Remove trailing '/'
		if(cleanUrl.substring(cleanUrl.length()-1, cleanUrl.length()).equals("/")){
			cleanUrl = cleanUrl.substring(0,cleanUrl.length()-1);
		}
		//Remove any '/'s and replace them with periods.
		//This should only do anything to the psych domain
		cleanUrl = cleanUrl.replaceAll("/", "-");

		this.rootFilePath = "/tmp/cs455-wbarras/"+cleanUrl;
	}//End generateRootFilePath

	public ConnectionCache getConnectionCache(){
		return this.cache;
	}//End getConnectionCache

	public void setDone(boolean done){
		boolean update = true;
		//Onlu update if the value is actually new
		if(done == this.completeStatus.get(this.rootUrl)){
			update = false;
		}

		this.completeStatus.put(this.rootUrl, done);

		if(update){
			int doneint = 1;
			if(!done) doneint = 0;

			if(this.pendingCrawlRequests > 0) doneint = 0;

			for(String domain : this.domains){
				sendTask(new CrawlerUpdateCompleteStatus(this.rootUrl,doneint), domain);
			}
		}

	}//End setDone

	public boolean allDone(){
		//If it contains no zeros,then everyone is done
		return !this.completeStatus.containsValue(false);
	}

	public void onEvent(Event event){
		//System.out.println("crawler.onEvent()");
		//System.out.println(event);

		int type = event.getType();

		switch(type){
			case 1:		this.eventOne(new CrawlerSendsCrawlTask(event.getBytes()));
								break;
			case 2:		synchronized(this.pendingCrawlRequests){
									this.pendingCrawlRequests--;
								}
								break;
			case 3:		this.eventThree(new CrawlerUpdateCompleteStatus(event.getBytes()));
								break;
			default: System.out.println("This doesn't seem to be a handled event");
								break;
		}
	}//End onEvent

	private void eventOne(CrawlerSendsCrawlTask csct){
		String crawlAddress = csct.getCrawlAddress();
		String sendingAddress = csct.getSendingAddress();

		CrawlTask newTask = new RecievedCrawlTask(crawlAddress, 0, this.rootFilePath,sendingAddress);
		manager.addTask(newTask);
	}//End eventOne

	private void eventThree(CrawlerUpdateCompleteStatus cucs){
		String domain = cucs.getDomain();
		int status = cucs.getStatus();

		boolean flag = true;
		if(status == 0) flag = false;

		this.completeStatus.put(domain, flag);
	}//Ends eventThree


	//Anything past here is static and only used in main()--------------->>><<<

	public static void main(String args[]){

		//Sanity check input
		if(!checkArgs(args)){
			System.exit(-1);
		}

		//pull variables
		int port = Integer.valueOf(args[0]);
		int pool = Integer.valueOf(args[1]);
		//Build Crawler
		Crawler crawler = new Crawler(port, pool, args[2], args[3]);

		//Initialize tpm and server
		crawler.initialize();



		crawler.beginCrawling();

	}//End main


	private static boolean checkArgs(String[] args){
		if(args.length != 4){
			System.out.println("Error: Incorrect number of command line arguments");
			System.out.println("Usage: Crawler <portnum> <thread pool size> <root url> <path to config file>");
			return false;
		}else{
			return true;
		}
	}//Check args
}//End class
