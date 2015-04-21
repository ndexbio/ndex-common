package org.ndexbio.task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.ndexbio.model.object.Task;

public enum NdexServerQueue {
	
	INSTANCE;
	
	private LinkedBlockingDeque<Task> systemTaskQueue;
	private LinkedBlockingDeque<Task> userTaskQueue;
	
	public static final Task endOfQueue = new Task();
	
	private NdexServerQueue () {
		systemTaskQueue = new LinkedBlockingDeque<>();
		userTaskQueue = new LinkedBlockingDeque<>();
    }
	

	public Task takeNextSystemTask () throws InterruptedException {
		return systemTaskQueue.take();
	}

	public Task takeNextUserTask () throws InterruptedException {
		return userTaskQueue.take();
	}
	
	public void addSystemTask (Task task)  {
		systemTaskQueue.add(task);
	}

	public void addFirstSystemTask (Task task)  {
		systemTaskQueue.addFirst(task);
	}
	
	public void addUserTask (Task task)  {
		userTaskQueue.add(task);
	}
	
	public BlockingQueue<Task> getSystemTaskQueue () {
		return systemTaskQueue;
	}
	
	public BlockingQueue<Task> getUserTaskQueue () {
		return userTaskQueue;
	}
	
	public void shutdown () {
		systemTaskQueue.add(endOfQueue);
		userTaskQueue.add(endOfQueue);
		
	}
 }
