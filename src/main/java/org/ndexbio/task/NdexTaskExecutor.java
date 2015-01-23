package org.ndexbio.task;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;
import org.ndexbio.model.object.network.FileFormat;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;
import org.slf4j.*;

import java.util.concurrent.Future;

import org.ndexbio.common.exceptions.ObjectNotFoundException;

/*
 * Represents a class responsible for pulling itasks off the task queue,
 * instantiating and invoking the appropriate type of NdexTask subclass,
 * and updating the the completion status of the task. The NdexTask instances 
 * were formally the level of application concurrency. This implementation has
 * been retained to minimize refactoring but the concurrency level is fixed a one.
 * The NdexTaskExecutor is now where concurrency is supported. The callable will
 * run until the task queue is empty and return the number of tasks completed.
 * 
 */
public class NdexTaskExecutor implements Callable<Integer> {

	private Integer completionCount = 0;
	private static final Logger logger = LoggerFactory
			.getLogger(NdexTaskExecutor.class);
	private  final CompletionService<Task> taskCompletionService;
	private final Integer MAX_THREADS = 1;
	private final NdexTaskService taskService;
	private final Integer threadIdentifier;
	private final ExecutorService taskExecutor;
	private NdexDatabase db;

	public NdexTaskExecutor(Integer id, NdexDatabase db) {
		 taskExecutor = Executors
				.newFixedThreadPool(MAX_THREADS);
		this.taskCompletionService = new ExecutorCompletionService<>(
				taskExecutor);
		this.taskService = new NdexTaskService();
		this.threadIdentifier = id;
		this.db = db;
	}
	
	public Integer getThreadIdentifier() { return this.threadIdentifier;}

	@Override
	public Integer call() throws Exception {
		logger.info("Executor " + this.getThreadIdentifier() +" invoked");
		while (!NdexTaskQueueService.INSTANCE.isTaskQueueEmpty()) {
			try {
				Task itask = NdexTaskQueueService.INSTANCE.getNextTask();
				NdexTask ndexTask = getNdexTaskByTaskType(itask);
				this.taskCompletionService.submit(ndexTask);
				// now wait for completion of child task
				logger.info("Invoking Ndextask type: " + ndexTask.getClass().getName()
						+" for task id: " +ndexTask.getTask().getExternalId());
				Future<Task> result = taskCompletionService.take();
				// post completion status
				this.postTaskCompletion(result.get());
				this.incrementCompletionCount();
				result.cancel(true);

			} catch (  IllegalArgumentException
					| SecurityException
					| NdexException | ExecutionException e) {
				
				logger.error(e.getMessage());
				e.printStackTrace();
				throw new Exception ("Error occured when executing task. " + e.getMessage());
			} catch (InterruptedException e){
				logger.info("Thread is interrupted");
				return this.getCompletionCount();
			}

		}
		logger.info("Executor " +this.getThreadIdentifier() +" completed.");
		this.taskExecutor.shutdownNow();
		return this.getCompletionCount();
	}

	private void postTaskCompletion(Task completedTask) throws 
	IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException {
	
		taskService.updateTaskStatus( completedTask.getStatus(),completedTask);
		logger.info("Completion status for task id: " +completedTask.getExternalId() +
				"is " +completedTask.getStatus().toString());
	
}

	Integer getCompletionCount() {
		return completionCount;
	}

	private void incrementCompletionCount() {
		this.completionCount++;
	}
	
	
	private NdexTask getNdexTaskByTaskType(Task task) throws NdexException{
		
		try {
			if( task.getTaskType() == TaskType.PROCESS_UPLOADED_NETWORK) {
				return new FileUploadTask(task, db);
			}
			if( task.getTaskType() == TaskType.EXPORT_NETWORK_TO_FILE) {
				if ( task.getFormat() == FileFormat.XBEL)
					return new XbelExporterTask(task);
				else if ( task.getFormat() == FileFormat.XGMML) {
					return new XGMMLExporterTask(task);
				}
				
				throw new NdexException ("Only XBEL exporter is implemented.");
			}
			throw new IllegalArgumentException("Task type: " +task.getType() +" is not supported");
		} catch (IllegalArgumentException | SecurityException | NdexException e) {
			e.printStackTrace();
			throw new NdexException ("Error occurred when creating task. " + e.getMessage());
		} 
	}


}
