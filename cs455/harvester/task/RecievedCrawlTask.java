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
import cs455.harvester.wireformats.CrawlerAcknowledgesCrawlTask;

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


public class RecievedCrawlTask extends CrawlTask{

	private final String sentFrom;

	public RecievedCrawlTask(String s, int rl, String _workingDirectory, String _sentFrom){
		super(s, rl, _workingDirectory);
		sentFrom = _sentFrom;
	}//End constructor

	public ArrayList<Task> execute(){
		ArrayList<Task> newTasks = super.execute();

		this.addEdgeFromOtherDomain(this.sentFrom);

		try{
			super.sendTask(new CrawlerAcknowledgesCrawlTask(1), this.sentFrom);
		}catch(MalformedURLException exception){
			System.out.println("RecievedCrawlTask: Error while finding reciever domain for sendTask()");
			System.out.println(exception);
		}

		return newTasks;
	}//End execute


	private void addEdgeFromOtherDomain(String otherDomainUrl){
		String pathToInFile = super.generateNodePath(super.url)+"/in";
		try{
			//System.out.printf("Adding url %s to file %s\n", this.url, pathToInFile);
			PrintWriter outFileWriter = new PrintWriter(new BufferedWriter(new FileWriter(pathToInFile,true)));
			outFileWriter.println(otherDomainUrl);
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
	}//End addEdgeFromAnotherDomain
}
