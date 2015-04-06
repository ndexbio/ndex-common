package org.ndexbio.task;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ndexbio.model.object.Task;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;

/*
 * a singleton implemented as an enum to make the task queue avilable to
 * multiple threads within the application
 * Access is limited to classes within the same package
 */

enum NdexTaskQueueService {
	INSTANCE;
	
	private final ConcurrentLinkedQueue<Task> taskQueue =
			Queues.newConcurrentLinkedQueue();
	
	void addCollection(Collection<Task> iTasks){
		Preconditions.checkArgument(null != iTasks,
				"a collection if ITasks is required");
		this.taskQueue.addAll(iTasks);
	}
	
	/*
	 * encapsulate direct access to queue
	 */
	Task getNextTask() {
		return this.taskQueue.poll();
	}
	
	boolean isTaskQueueEmpty() {
		return this.taskQueue.isEmpty();
	}

	int getTaskQueueSize() {
		return this.taskQueue.size();
	}
}
