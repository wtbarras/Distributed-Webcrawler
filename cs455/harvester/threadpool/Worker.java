package cs455.harvester.threadpool;

/*
 *Author: Tiger Barras
 *Worker.java
 *Assigned tasks by the ThreadPoolManager
 */

import cs455.harvester.crawler.Crawler;
import cs455.harvester.wireformats.Event;
import cs455.harvester.task.Task;
import cs455.harvester.threadpool.ThreadPoolManager;

import java.util.NoSuchElementException;
import java.util.ArrayList;

public class Worker implements Runnable{

	//The thread that is running this worker
	Thread wrapperThread = null;

	private final Crawler crawler;

	/*
	 *This is the manager that created this Worker
	 *Need a reference to this so we can pass back new tasks
	 */
	private final ThreadPoolManager manager;

	/*
	 *Don't ever modify this. It should only be wait/notify-ed on
	 *Wait when there's nothing to take out of it.
	 *Maybe notify if adding is implemented in this class(That might all be done in the manager)
	 */
	private final ArrayList<Task> tasksToComplete;

	private final String workerName;

	public Worker(ThreadPoolManager _manager, ArrayList<Task> _tasksToComplete, String name, Crawler _crawler){
		manager = _manager;
		tasksToComplete = _tasksToComplete;
		workerName = name;
		this.crawler = _crawler;
	}//End constructor


	//Runs until it is given the task to kill itself
	public void run(){
		System.out.println("Worker " + this.workerName+ " Starting");
		while(true){
			Task currentTask = null;

			//Only one Worker can be looking at tasksToComplete at a time
			//Otherwise, isEmpty could end up in an inconsistent state
			synchronized(tasksToComplete){
				System.out.println("Worker " + this.workerName + " entering synchronized block");
				//The only time we should hit this statement is if we get woken up by
				  //the manager
				if(!tasksToComplete.isEmpty()){
					//System.out.println("Queue not empty. Gon grab meself anotter task mon");


					//Grab a task from the manager
					//This will be null if the list is actually empty
					currentTask = this.requestTask();


				}else{
					try{
						//System.out.println("Queue empty. Adding self back to pool");
						//Niceness measure. Sleep for a bit before returning to pool
						this.manager.returnWorkerToPool(this.wrapperThread);
						tasksToComplete.wait();
					}catch(InterruptedException exception){
						System.out.println("Worker: Interrupted");
						System.out.println(exception);
					}finally{
						this.manager.removeWorkerFromPool(this.wrapperThread);
					}
				}
			}//End sychronized block

			//Make sure a task was actually returned
			//If currenTask is null, that means no task was returned from the manager
			//The continue statement puts is back at the top of the while,
			//  and we'll wait on the task list again
			if(currentTask == null) continue;

			System.out.println("Worker " + this.workerName + ": Recieved Task");

			//Set up and execute the task
			currentTask.setWorker(this);
			ArrayList<Task> newTasks = currentTask.execute();

			//Place current task in finished list, add others to taskQueue
			this.organizeTasks(currentTask, newTasks);

		}
	}//End run


	public void setWrapperThread(Thread t){
		//Can't be final, but we don't want anyone resetting the thread
		if(this.wrapperThread == null){
			wrapperThread = t;
		}else{
			System.out.println("Worker: Error, wrapperThread already set");
		}
	}//End setWrapperThread

	private Task requestTask(){
		Task newTask = null;
		try{
			synchronized(this.manager){
				newTask = this.manager.getTask();
			}
		}catch(NoSuchElementException exception){
			//Hopefully this branch is never ever reached
			//If it is, then a concurrency issue is causing workers to think the task
			// list has elements when it's actually empty!
			System.out.println("Worker: No task to recieve from ThreadPoolManager");
			System.out.println(exception);
		}
		return newTask;
	}//End requestTask

	private void addTask(Task task){
			this.manager.addTask(task);
	}//End addTask

	//Adds current task to completed tasks, adds the new tasks to the task list
	private void organizeTasks(Task currentTask, ArrayList<Task> newTasks){
		this.manager.addCompletedTask(currentTask);

		for(Task taskToAdd : newTasks){
			this.addTask(taskToAdd);
		}
	}//End organizeTasks

	//Checks to see if a task has already been done
	public boolean checkDuplicateTask(Task task){
		synchronized(this.manager){
			return this.manager.checkDuplicateTask(task);
		}
	}//End checkDuplicateTask

	//The task is marshalled into an Event that gets sent over the wire
	public void sendTask(Event event, String recievingDomain){
		this.crawler.sendTask(event,recievingDomain);
	}//End sendTask

	public void nice(int time){
		try{
			System.out.printf("Worker %s going to sleep before retrieving new task\n", this.workerName);
			this.wrapperThread.sleep(time);
		}catch(InterruptedException exception){
			System.out.println("Worker: Interrupted");
			System.out.println(exception);
		}
	}

}//End class
