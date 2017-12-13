package cs455.harvester.task;

/*
 *Author: Tiger Barras
 *Task.java
 *Reads one webpage
 */

import cs455.harvester.threadpool.Worker;
import cs455.harvester.crawler.Parser;
import cs455.harvester.task.PrintMessageTask;
import cs455.harvester.wireformats.Event;
import cs455.harvester.wireformats.CrawlerSendsCrawlTask;


import net.htmlparser.jericho.*;

import java.util.ArrayList;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;


public class CrawlTask implements Task{

	private final int recursionLimit = 1;


	protected String url;
	private final int recursionLevel;
	private final String workingDirectory;
	//This does not get set in constructor, but by separate method
	  //This is because the worker is not known when the task is created
	private Worker worker;

	public CrawlTask(String s, int rl, String _workingDirectory){
		url = s;
		recursionLevel = rl;
		workingDirectory = _workingDirectory;
	}//End constructor




	//This is what the Worker calls
	//All the actual work kicks off right here
	public ArrayList<Task> execute(){
		//Fix URL if it's a redirect
		Source source = this.redirectBaseUrl();
		if(source == null){
			System.out.printf("Hopefully %s is a broken link, since it could not pull a source\n", this.url);
			this.addBrokenLink(this.url);
			return new ArrayList<Task>();
		}

		//Check to see if this is a duplicate
		if(this.checkDuplicateTask()){
			return new ArrayList<Task>();
		}

		//If we get here, that means it isn't a duplicate
		System.out.printf("Crawling %s at recursion level %d\n", this.url
																													 , this.recursionLevel);

		//Create a parser object that will create all our new tasks
		Parser parser = new Parser(this.url, source);
		//FIXED TO THIS POINT

		//Set up directory and files
		this.initDirectory(this.generateNodePath(this.url));

		ArrayList<Task> newTasks = new ArrayList<Task>();

		//This returns a list of all the absolute links on the page from the listed domains
		ArrayList<String> urlStrings = parser.parseFully();

		//System.out.println("Printing urls");
		for(String pageUrl : urlStrings){

			// System.out.println("CrawlTask: Parsing " + pageUrl);

			//Update edges of the graph
			// this.updateGraph(pageUrl);

			//System.out.println(pageUrl);
			//Check if the URL point to somewhere in this domain
			try{
				if(this.checkDomain(pageUrl)){//This means that the URL is in this crawler's domain
					//Only spawn a new task if the recursion level isn't maxxed
					CrawlTask toAdd = new CrawlTask(pageUrl, this.recursionLevel+1, this.workingDirectory);
					boolean addTaskFlag = true;
					if(this.recursionLevel >= this.recursionLimit){
						//Don't add the task if we're already at the recursion limit
						addTaskFlag = false;
					}
					if(worker.checkDuplicateTask(toAdd)){
						//Don't add the task if it's a duplicate of a completed task
						addTaskFlag = false;
					}
					if(pageUrl.endsWith(".doc") || pageUrl.endsWith(".pdf")){
						//Don't add the task if it's not an html type document
						addTaskFlag = false;
					}

					if(addTaskFlag){
						//System.out.println("Adding task");
						newTasks.add(toAdd);
					}
					//Update the graph no matter what the recursion level
					//Add this to the current node's out file
					this.updateOut(pageUrl);
					//Add this to the new edge's in file
					this.updateIn(pageUrl);
				}else{//The URL is in another Crawler's domain, so we have to send the task to it
					//Send task to appropriate domain
					try{
						CrawlerSendsCrawlTask taskEvent = new CrawlerSendsCrawlTask(pageUrl, this.url);
						this.sendTask(taskEvent, pageUrl);
					}catch(MalformedURLException exception){
						System.out.println("CrawlTask: Error while finding reciever domain for sendTask()");
						System.out.println(exception);
					}
					//Update this node's out file
					this.updateOut(pageUrl);

					//Send task to other crawler
					//It will update its own in file
				}
			}catch(MalformedURLException exception){
				System.out.println("CrawlTask: Error checking domain");
				System.out.println(exception);
			}
			// this.worker.addTask(new PrintMessageTask(pageUrl));

			// System.out.println("CrawlTask: Done parsing " + pageUrl);
		}

		//Sleep for 20 seconds after completing crawling
		//Don't sleep if it's a duplicate or a broken link, because that's wasteful
		this.worker.nice(1000);

		return newTasks;
	}//End execute

	private boolean checkDomain(String pageUrl)throws MalformedURLException{
		boolean sameDomain = new URL(pageUrl).getHost().equals(new URL(this.url).getHost());
		if(!sameDomain){
			String psychDomain = "http://www.colostate.edu/Depts/Psychology";
			sameDomain = (pageUrl.startsWith(psychDomain) && this.url.startsWith(psychDomain));
		}

		return sameDomain;
	}//End checkDomain


	private Source redirectBaseUrl(){
		try{
			HttpURLConnection con = (HttpURLConnection)(new URL(url).openConnection());
			con.connect();
			InputStream is = con.getInputStream();
		  // this is the actual url, the page is redirected to (if there is a redirect).
		  url = con.getURL().toString();
			// instead of passing the URL, pass the input stream.
		  Source source = new Source(is);

			return source;
		}catch(MalformedURLException exception){
			System.out.println("CrawlTask: Error creating source from url");
			System.out.println(exception);
		}catch(IOException exception){
			System.out.println("CrawlTask: Error creating source from url");
			System.out.println("Most likely broken URL");
			System.out.println(exception);
		}

		return null;
	}//End redirectBaseUrl

	private boolean checkDuplicateTask(){
		//Check to see if the redirected URL has already been crawled
		boolean duplicate = this.worker.checkDuplicateTask(new CrawlTask(this.url
																										, this.recursionLevel
																										, this.workingDirectory));

		if(duplicate){
			System.out.println("Not crawling duplicate: " + this.url);
			return true;
		}

		return false;
	}//End checkDuplicateTask

	//Set this before execution of any task
	//This is what allows a task to talk back to the worker
	public void setWorker(Worker _worker){
		worker = _worker;
	}//End setWorker

	public boolean equals(Object o){
		//System.out.println("equals");

		if(!(o instanceof Task)) return false;
		if(o == this) return true;

		int otherTaskHash = ((Task) o).hashCode();
		return otherTaskHash == this.hashCode();
	}//End equals

	public String getType(){
		return "CrawlTask";
	}//End getType

	//Punt hashCode to toString
	public int hashCode(){
		//System.out.println("hashCode");
		return this.toString().hashCode();
	}//End getHash

	private static String urlToPath(String s){
		//Chop of leading http://
		if(s.startsWith("http://")){
			s = s.substring(7,s.length());
		}
		//Remove trailing '/'
		if(s.substring(s.length()-1, s.length()).equals("/")){
			s = s.substring(0,s.length()-1);
		}
		//Remove any '/'s and replace them with periods.
		//This should only do anything to the psych domain
		s = s.replaceAll("/", "-");

		return s;
	}//End urlToPath

	protected String generateNodePath(String nodeUrl){
		String cleanedUrl = urlToPath(nodeUrl);

		return this.workingDirectory + "/nodes/" + cleanedUrl;
	}//End GenerateNodePath

	private void initDirectory(String path){
		File dir = new File(path);
		boolean created = false;
		try{
			created = dir.mkdir();
		}catch(SecurityException exception){
			System.out.println("Crawler: Error creating directory. Security Exception");
			System.out.println(exception);
		}

		if(created || dir.exists()){
			//System.out.println("Directory at " + dir + " exists or was created");
		}else{
			System.out.println("Directory not created");
			System.out.println(dir);
			System.out.println("Check to make sure /tmp/cs455-wbarras exists");
			System.exit(-1);
		}

		File in = new File(path + "/in");
		File out = new File(path + "/out");

		try{
			in.createNewFile();
			out.createNewFile();
		}catch(SecurityException exception){
			System.out.println("Crawler: Error creating file. Security Exception");
			System.out.println(exception);
		}catch(IOException exception){
			System.out.println("CrawlTask: Error creating file");
			System.out.println(exception);
		}
	}//End initDirectory

	private void updateGraph(String newEdge){
			//Add this to the current node's out file
			this.updateOut(newEdge);
			//Add this to the new edge's in file
			this.updateIn(newEdge);
	}//End updateGraphs

	private void updateOut(String newEdge){
		//Make sure the directories for the other node are in place
		this.initDirectory(this.generateNodePath(newEdge));

		//Add URL to this page's out file
		String pathToOutFile = this.generateNodePath(this.url)+"/out";
		try{
			//System.out.printf("Adding url %s to file %s\n", newEdge, pathToOutFile);
			PrintWriter outFileWriter = new PrintWriter(new BufferedWriter(new FileWriter(pathToOutFile,true)));
			outFileWriter.println(newEdge);
			outFileWriter.flush();
			outFileWriter.close();
		}catch(FileNotFoundException exception){
			System.out.println("CrawlTask.updateOut: Error creating out file");
			System.out.println(exception);
			System.exit(-1);
		}catch(SecurityException exception){
			System.out.println("CrawlTask.updateOut: Error creating out file");
			System.out.println(exception);
			System.exit(-1);
		}catch(IOException exception){
			System.out.println("CrawlTask.updateOut: Error creating out file");
			System.out.println(exception);
			System.exit(-1);
		}
	}//End updateOut

	private void updateIn(String newEdge){
		String pathToInFile = this.generateNodePath(newEdge)+"/in";
		try{
			//System.out.printf("Adding url %s to file %s\n", this.url, pathToInFile);
			PrintWriter outFileWriter = new PrintWriter(new BufferedWriter(new FileWriter(pathToInFile,true)));
			outFileWriter.println(this.url);
			outFileWriter.flush();
		}catch(FileNotFoundException exception){
			System.out.println("CrawlTask: Error creating in file");
			System.out.println(exception);
			System.exit(-1);
		}catch(SecurityException exception){
			System.out.println("CrawlTask: Error creating in file");
			System.out.println(exception);
			System.exit(-1);
		}catch(IOException exception){
			System.out.println("CrawlTask: Error creating in file");
			System.out.println(exception);
			System.exit(-1);
		}
	}



	protected void sendTask(Event event, String otherDomainUrl)throws MalformedURLException{

		//Figure out what domain you're sending it to
		String domain = new URL(otherDomainUrl).getHost();
		//Psych is a special flower
		//Deal with their bullshit
		if(domain.equals("http://www.colostate.edu")){
			domain = "http://www.colostate.edu/Depts/Psychology";
		}

		this.worker.sendTask(event, domain);
	}//End sendTask

	private void addBrokenLink(String brokenLink){
		String pathToBrokenLinkFile = this.workingDirectory + "/broken-links";

		try{
			//System.out.printf("Adding url %s to file %s\n", this.url, pathToInFile);
			PrintWriter outFileWriter = new PrintWriter(new BufferedWriter(new FileWriter(pathToBrokenLinkFile,true)));
			outFileWriter.println(brokenLink);
			outFileWriter.flush();
		}catch(FileNotFoundException exception){
			System.out.println("CrawlTask: Error accessing broken-links file");
			System.out.println(exception);
			System.exit(-1);
		}catch(SecurityException exception){
			System.out.println("CrawlTask: Error accessing broken-links file");
			System.out.println(exception);
			System.exit(-1);
		}catch(IOException exception){
			System.out.println("CrawlTask: Error accessing broken-links file");
			System.out.println(exception);
			System.exit(-1);
		}
	}//End addBrokenLink

	public String toString(){
		return "CrawlTask:"+url;
	}//End toString

}//End class
