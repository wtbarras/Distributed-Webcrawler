package cs455.harvester.task;

/*
 *Author: Tiger Barras
 *Task.java
 *A single unit of work to be done by the Worker
 *Implemented to package different tasks
 */

import cs455.harvester.threadpool.Worker;

import java.util.ArrayList;


public interface Task{

  //This is what the Worker calls
  //All the actual work kicks off right here
  public ArrayList<Task> execute();

  //Set this before execution of any task
  //This is what allows a task to talk back to the worker
  public void setWorker(Worker worker);

  //This needs to be implemented so .contains() works
  public boolean equals(Object o);

  //Returns a string with the class name
  public String getType();

  //Returns getType():<some payload value>
  // e.g. CrawlTask:<UrlToParse>
  //      PrintMessageTask:<messageToPrint>
  public int hashCode();

  public String toString();

}//End interface
