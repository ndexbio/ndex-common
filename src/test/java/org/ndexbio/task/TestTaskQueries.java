/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
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
