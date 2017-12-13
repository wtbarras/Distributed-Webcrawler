package cs455.harvester.task;

/*
 *Author: Tiger Barras
 *Task.java
 *A single unit of work to be done by the Worker
 *Print a message
 */

import cs455.harvester.threadpool.Worker;

import java.util.ArrayList;


public class PrintMessageTask implements Task{

	private final String message;
	private Worker worker;

	public PrintMessageTask(String s){
		message = s;
	}//End constructor


	//This is what the Worker calls
	//All the actual work kicks off right here
	public ArrayList<Task> execute(){
		System.out.println("PrintMessageTask: " + this.message);
		return new ArrayList<Task>();
	}//End execute


	public void setWorker(Worker w){
		this.worker = w;
	}//End setWorker

	public boolean equals(Object o){
			if(!(o instanceof Task)) return false;
			if(o == this) return true;

			int otherTaskHash = ((Task) o).hashCode();

			return otherTaskHash == this.hashCode();
	}//End equals

	public String getType(){
		return "PrintMessageTask";
	}//End getType

	public int hashCode(){
		return this.toString().hashCode();
	}//End getHash

	public String toString(){
		return this.getType() + ":" + this.message;
	}//End toString

}//End class
