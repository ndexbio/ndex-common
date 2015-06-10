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

import java.util.List;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Task;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.id.ORID;

public class TestTaskQueries {
	private final NdexTaskService taskService ;
	private static final Logger logger = LoggerFactory.getLogger(TestTaskQueries.class);
	
	public TestTaskQueries() {
		taskService = new NdexTaskService();
	}
	
	private void runTests() {
		this.determineInProgressTasks();
//			this.determineQueuedTasks();
//			this.determineCompletedTasks();
		
	}
	
	public static void main(String[] args) throws NdexException {
		TestTaskQueries test = new TestTaskQueries();
		test.runTests();

	}

	private  Integer determineInProgressTasks() {
/*		List<Task> taskList = taskService.getInProgressTasks();
		Integer activeTasks = taskList.size();
		logger.info("There are " +taskList.size() +" in progress tasks");
		for ( Task task : taskList){
			
			logger.info("Task id " +resolveVertexId(task) +" is in progress owner = " +task.getOwner().getUsername());
		}
		return activeTasks; */
		return null;
	}
/*	
	private  Integer determineQueuedTasks() throws NdexException {
		List<ITask> taskList = taskService.getQueuedITasks();
		Integer activeTasks = taskList.size();
		logger.info("There are " +taskList.size() +" in queued tasks");
		for ( ITask task : taskList){
			 
			logger.info("Task id " +resolveVertexId(task) +" is queued owner "
					+task.getOwner().getUsername());
		}
		return activeTasks;
	}
	
	private  Integer determineCompletedTasks() throws NdexException {
		List<ITask> taskList = taskService.getAllCompletedITasks();
		Integer activeTasks = taskList.size();
		logger.info("There are " +taskList.size() +" completed tasks");
		for ( ITask task : taskList){
			 
			logger.info("Task id " +resolveVertexId(task) +" is completed owner "
					+task.getOwner().getUsername() +" completion status = " +task.getStatus().toString());
		}
		return activeTasks;
	}
	
	protected String resolveVertexId(VertexFrame vf)
    {
        if (null == vf)
            return null;

        return IdConverter.toJid((ORID)vf.asVertex().getId());
    }
	*/
}
