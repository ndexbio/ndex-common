package org.ndexbio.task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.ndexbio.model.object.Task;

public enum NdexServerQueue {
	
	INSTANCE;
	
	private LinkedBlockingQueue<Task> systemTaskQueue;
	private LinkedBlockingQueue<Task> userTaskQueue;
	
	
	private NdexServerQueue () {
		systemTaskQueue = new LinkedBlockingQueue<>();
		userTaskQueue = new LinkedBlockingQueue<>();
    }
	

	public Task takeNextSystemTask () throws InterruptedException {
		return systemTaskQueue.take();
	}

	public Task takeNextUserTask () throws InterruptedException {
		return systemTaskQueue.take();
	}
	
	public void addSystemTask (Task task) throws InterruptedException {
		systemTaskQueue.put(task);
	}

	public void addUserTask (Task task) throws InterruptedException {
		userTaskQueue.put(task);
	}
	
	public BlockingQueue<Task> getSystemTaskQueue () {
		return systemTaskQueue;
	}
	
	public BlockingQueue<Task> getUserTaskQueue () {
		return userTaskQueue;
	}
	
}
