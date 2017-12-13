package cs455.harvester.threadpool;

/*
 *Author: Tiger Barras
 *ThreadPoolManager.java
 *Maintains a queue of worker threads and a queue of tasks, and assigns the tasks to the workers
 */

import cs455.harvester.crawler.Crawler;
import cs455.harvester.threadpool.Worker;
import cs455.harvester.task.Task;
import cs455.harvester.task.PrintMessageTask;
import cs455.harvester.task.CrawlTask;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.ArrayList;



public class ThreadPoolManager implements Runnable{

  private final Crawler crawler;
  private final int numberOfThreads;

  //This holds references to the Threads running available workers
  //When there is a task to be done, the worker grabs it, and removes itself from the queue
  //When it completes that task, it places itself back at the end of the queue
  private final ArrayList<Thread> availableWorkers = new ArrayList<Thread>();

  //Hold references to tasks waiting to be completed
  //When there is a task to be done, it is grabbed by the next available worker
  private final ArrayList<Task> tasksToComplete = new ArrayList<Task>();

  private final TaskRegistry completedTasks = new TaskRegistry();

  //Use this to see if it's the first time for all of the threads in the pool
  //They all end up there at oncat the very begining, and we need to ignore it
  private boolean firstTime = true;

  public ThreadPoolManager(int _numberOfThreads, Crawler _crawler){
    numberOfThreads = _numberOfThreads;
    this.crawler = _crawler;
  }//End constructor


  public void run(){
    //Fill availableWorkers with workerThreads
    this.initWorkers(this.numberOfThreads);
    //Start all the workers
    this.startWorkers();

    //Want to just sit in here until the thread gets killed
    while(true){
      //System.out.println("Loop");
    //  synchronized(tasksToComplete){
    //
    //  }
    }
  }//End run


  //Initializes the proper amount of Workers, and assigns each one to a Thread
  //The Threads are placed in the availableWorkers Queue
  private void initWorkers(int threadCount){
    //Add the appropriate number of Workers to the Queue
    for(int i = 0; i < threadCount; i++){
      String workerName = "Worker:" + i;//Generate a unique name for each thread
      System.out.println("Generating Worker");
      Worker worker = new Worker(this, tasksToComplete, workerName, this.crawler);
      System.out.println("Generating thread");
      Thread workerThread = workerThread = new Thread(worker, workerName);
      worker.setWrapperThread(workerThread);
      System.out.println("Adding workerThread " + workerThread.getName());
      this.availableWorkers.add(workerThread);
    }

    //Queue should now contain the proper numbers of threads, but they have not yet been started
  }//End initWorkers

  //Starts each of the Workers in the queue
  private void startWorkers(){
    synchronized(availableWorkers){
      //Have to move all the threads to another array so I can clear this one
        //before I start the threads in it
      ArrayList<Thread> copy = new ArrayList<Thread>();
      for(Thread copyThread : availableWorkers){
        copy.add(copyThread);
      }
      availableWorkers.clear();
      for(Thread workerThreadToStart : copy){
        workerThreadToStart.start();
      }
    }
  }//End startWorkers

  //I...I don't remember what this is for
  public boolean checkTaskRegistry(){
    return true;
  }//End checkTaskRegistry

  public void addTask(Task task){
    synchronized(tasksToComplete){
      //Only add the task if we haven't already done it
      if(!this.completedTasks.contains(task) || !this.tasksToComplete.contains(task)){
        //System.out.println("Size: " + this.tasksToComplete.size());
        //System.out.println("Manager: Adding task " + task);
        this.tasksToComplete.add(task);
        tasksToComplete.notify();
        this.checkDone(false);
      }
    }
  }//End addTask

  public void addCompletedTask(Task task){
    synchronized(completedTasks){
      System.out.println("Manager: Adding completed task " + task);
      this.completedTasks.add(task);
      //this.crawler.setDone(false);
    }
  }//End addCompletedTask

  public boolean checkDuplicateTask(Task task){
    synchronized(completedTasks){
      return this.completedTasks.contains(task);
    }
  }//End checkDuplicateTask

  public Task getTask()throws NoSuchElementException{
    //This needs to be synchronized
    //If not, then two Workers could both recieve the last Task in the Queue
    synchronized(tasksToComplete){
      return tasksToComplete.remove(0);
    }
  }//End getTask

  public void returnWorkerToPool(Thread workerThread){
    synchronized(availableWorkers){
      System.out.println("Returning thread " + workerThread.getName() + " to pool");
      this.availableWorkers.add(workerThread);
      //System.out.println("Available Workers: " + availableWorkers.size());
      if(availableWorkers.size() == this.numberOfThreads){
        this.checkDone(true);
        System.out.println("If I sit here for a while, I'm done");
      }
    }
  }//End returnToQueue

  public void removeWorkerFromPool(Thread workerThread){
    synchronized(availableWorkers){
      System.out.println("Removing thread " + workerThread.getName() + " from pool");
      this.availableWorkers.remove(workerThread);
      //System.out.println("Available Workers: " + availableWorkers.size());
      this.checkDone(false);
    }
  }

  private void checkDone(boolean done){
    if(firstTime){
      firstTime = false;
      return;
    }

    //Else
    this.crawler.setDone(done);
    if(this.crawler.allDone()){
      System.out.println("Crawler finished. Reciever Finished messages from all other crawlers");
      System.out.println("Exiting...");
      System.exit(1);
    }
  }


  //This main is for the purposes of testing this class only
  //ThreadPoolManager should always be used as an object inside something else
  public static void main(String args[]){


    // ThreadPoolManager tpm = new ThreadPoolManager(4);
    // for(int i = 0; i < 4; i++){
    //   tpm.addTask(new AddTaskTask(new PrintMessageTask("Hello" + i)));
    // }

    // tpm.addTask(new CrawlTask("http://www.cs.colostate.edu/", 0));

    // Thread managerThread = new Thread(tpm);
    // managerThread.start();
  }

}//End class
