package org.ndexbio.task;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NdexQueuedTaskProcessor {
	
	/*
	 * Represents a java application responsible for processing Ndex tasks
	 * persisted in the database with a status of QUEUED. The application
	 * is intended to be invoked on a scheduled basis as a UNIX/Linx cron task.
	 * Each invocation of this application will select all queued tasks from the
	 * orientdb database. The status of these tasks will be updated to STAGED and 
	 * the task ids placed in FIFO queue. The application will process all the STAGED
	 * entries, the level of concurrency will be set by a parameter value. As each task
	 * is removed from the queue, an appropriate task handler will be invoked.
	 * The individual task handlers are responsible for update the task status in the
	 * orientdb database from STAGED to PROCESSING and then to a final completion status.
	 * 
	 * Because a number of tasks may be processed by a single cron invocation of this
	 * application. The application is responsible for checking if any tasks in the database
	 * have a status of STAGED or PROVESSING. If so, that invocation will log the tasks found
	 * with those statuses and terminate. The intent is to have only one invocation running at a time.
	 * 
	 * New tasks queued to the database while this application is running will not
	 * be processed until the next cron invocation of the application.
	 * 
	 */
	private static final Logger logger = LoggerFactory.getLogger(NdexQueuedTaskProcessor.class);
	private static final Integer MAX_THREADS = 1;
	private final NdexTaskService taskService ;
	private   final CompletionService<Integer> taskCompletionService;
	private final ExecutorService taskExecutor;
	private NdexDatabase db ;
	
	
	public NdexQueuedTaskProcessor(NdexDatabase db) {
		 this.taskService = new NdexTaskService();
		 taskExecutor = Executors.newFixedThreadPool(MAX_THREADS);
	       this.taskCompletionService =
	           new ExecutorCompletionService<>(taskExecutor);  
	     this.db = db;   
	}
	
	
	
	/*
	 * private method to process all the tasks current persisted with a 
	 * QUEUED status
	 */
	//TODO: need to handle stale tasks 
	private void processQueuedTasks() {
		List<Task> stagedTasks = null;
		try {
			/*
			 * obtain a List of queued tasks and update their status to staged
			 */
			stagedTasks = taskService.stageQueuedTasks();
		} catch (NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return;
		}

		if(!stagedTasks.isEmpty()){
				NdexTaskQueueService.INSTANCE.addCollection(stagedTasks);		
				logger.info("The task queue contains" +NdexTaskQueueService.INSTANCE.getTaskQueueSize()
						+" staged tasks");
			
				int threadCount = Math.min(NdexTaskQueueService.INSTANCE.getTaskQueueSize(), 
						MAX_THREADS);
				int startedThreads = 0;
				for ( int i = 0 ; i < threadCount ; i++ ){
					
					NdexTaskExecutor executor  = new NdexTaskExecutor(startedThreads, db);
					this.taskCompletionService.submit(executor);
					logger.info("A NdexTaskExecutor thread started");
					startedThreads++;
					
				}
				// monitor submitted jobs until completion
				Integer totalCompletedTasks = 0;
				for(int threadsCompletd=0;threadsCompletd<startedThreads;threadsCompletd++){
			        try {
			            logger.debug("trying to take from Completion service");
			            Future<Integer> result = taskCompletionService.take();
			            totalCompletedTasks += result.get();
			            
			            System.out.println("Thread completed " +result.get() +" tasks" 
			            		+" Total tasks completed = " +totalCompletedTasks);
			            result.cancel(true);
			            
			        } catch (InterruptedException e) {
			            
			            logger.error("Error Interrupted exception");
			            e.printStackTrace();
			        } catch (ExecutionException e) {
			            // Something went wrong with the result
			            e.printStackTrace();
			            logger.error("Error get() threw exception");
			        }
			        
			    }
				// clear the stage flags in case error occurs.
			   	try {
	//				Thread.sleep(5000);
					for ( Task t : taskService.getActiveTasks()) {
						taskService.updateTaskStatus ( Status.COMPLETED_WITH_ERRORS, t);
						logger.error ("Setting status of stale task " + t.getExternalId() + " from " + t.getStatus()
							+" to "	+ Status.COMPLETED_WITH_ERRORS);
					}
				} catch (NdexException  e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logger.error("Failed to cleanup the unfinished tasks: "  + e.getMessage());
				} 
		 
				
		}
			
	}
	
	private void shutdown() {
		this.taskExecutor.shutdownNow();
	}
	
	/*
	 * method to determine the number of tasks that are currently marked
	 * as STAGED or PROCESSING. If >0, it's an indication that an previous 
	 * invocation of this application is still running
	 */
	private  Integer determineActiveTasks() throws NdexException {
		List<Task> taskList = taskService.getActiveTasks();
		Integer activeTasks = taskList.size();
		// for now just print out the in progress tasks
		for ( Task task : taskList){
			//TODO check running time and terminate job if necessary
			// decrement activeTasks 
			logger.info("Task id " + task.getExternalId() +" has an active status of " 
					+task.getStatus());
		}
		return activeTasks;
	}
	

/*	public static void main(String[] args) {
		
		try {
			processAll();
		} catch (NdexException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			System.exit(-1);
		} 
	} */
	

	public void processAll () throws NdexException {
			logger.info("Task processer invoked");

			if (determineActiveTasks() < 1){
				processQueuedTasks();
			}

			logger.info(this.getClass().getSimpleName() + " completed.");
		
	}

}
