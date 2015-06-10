package org.ndexbio.task;

import java.util.concurrent.Callable;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.Status;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;


public abstract class NdexTask implements Callable<Task> {
	
	private final NdexTaskService taskService;
	private  Task task;
	private String taskOwnerAccount;
	
	public NdexTask(Task itask) throws NdexException {
		this.taskService = new NdexTaskService();
		this.task = itask;
		this.taskOwnerAccount = taskService.getTaskOwnerAccount(itask);
	}

	protected Task getTask() { return this.task;}
	
	protected final void startTask() throws IllegalArgumentException, 
		ObjectNotFoundException, SecurityException, NdexException{
		this.updateTaskStatus(Status.PROCESSING);
		
	}
	
	/*
	 * update the actual itask in the task service which is responsible for database connections
	 * refresh the itask instancce to reflect the updated status
	 * do not set the status directly since the database connection may be closed
	 * 
	 */
	protected final void updateTaskStatus(Status status) throws IllegalArgumentException, 
		ObjectNotFoundException, SecurityException, NdexException{
		this.task = this.taskService.updateTaskStatus(status, this.task);
	}
	
	protected final void addTaskAttribute(String attributeName, Object value) throws NdexException {
	   task.getAttributes().put(attributeName, value);
	   this.taskService.addTaskAttribute(task.getExternalId().toString(), attributeName, value);
	}
	

	@Override
	public abstract Task call() throws Exception;
	
	protected String getTaskOwnerAccount() {return this.taskOwnerAccount;}

}
