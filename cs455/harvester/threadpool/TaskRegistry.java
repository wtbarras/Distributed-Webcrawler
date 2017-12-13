package cs455.harvester.threadpool;

/*
 *Author: Tiger Barras
 *TaskRegistry.java
 *Holds on to a collection of tasks that have already been done
 */

import cs455.harvester.task.Task;
import cs455.harvester.task.CrawlTask;

import java.util.ArrayList;


public class TaskRegistry{

  private ArrayList<Task> completedTasks = new ArrayList<Task>();

  public boolean contains(Task task){
    synchronized(completedTasks){
      boolean contains = completedTasks.contains(task);
      //System.out.println("Does reg contain " + task);
      //System.out.println(task.hashCode());
      //System.out.println(contains);
      return contains;
    }
  }//End contains

  public boolean add(Task task){
    boolean urlAdded;
    synchronized(completedTasks){
      urlAdded = !this.contains(task);
      if(urlAdded){
        completedTasks.add(task);
      }
    }

    return urlAdded;
  }//End add


  public static void main(String args[]){
    // CrawlTask taskOne = new CrawlTask("string", 0);
    // CrawlTask taskTwo = new CrawlTask("string", 0);

    // TaskRegistry reg = new TaskRegistry();
    //
    // reg.add(taskOne);
    // System.out.println(taskTwo==null ? taskOne==null : taskTwo.equals(taskOne));
    // System.out.println(reg.contains(taskOne));
    // System.out.println(reg.contains(taskTwo));
  }//End main

}//End class
