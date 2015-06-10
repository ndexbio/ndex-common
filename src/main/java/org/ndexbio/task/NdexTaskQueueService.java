/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
