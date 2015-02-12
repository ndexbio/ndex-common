package org.ndexbio.task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import org.ndexbio.model.object.Task;

public enum NdexServerQueue {
	
	INSTANCE;
	
	private LinkedBlockingDeque<Task> systemTaskQueue;
	private LinkedBlockingQueue<Task> userTaskQueue;
	
	
	private NdexServerQueue () {
		systemTaskQueue = new LinkedBlockingDeque<>();
		userTaskQueue = new LinkedBlockingQueue<>();
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
	
}
