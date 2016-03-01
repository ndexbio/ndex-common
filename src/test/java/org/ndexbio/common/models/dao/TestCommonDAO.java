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
package org.ndexbio.common.models.dao;

import java.util.Date;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;
import org.ndexbio.model.object.Priority;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/*
 * Represents a set of integration tests for the Common DAO implementations
 */

public class TestCommonDAO {
	private static final String userId = "C31R0"; // default userid for tests
	private static final String smallCorpusId = "C25R1308";
	
	public TestCommonDAO() {
		
	}
	
	private void performTests() {
		//this.testTaskDAO();
	}
/*
	private void testTaskDAO() {
		TaskDAO dao =
				DAOFactorySupplier.INSTANCE.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE)
				.get().getTaskDAO();
		Task task = this.testCreateTask(dao);
	    task = this.getTask(dao,task.getId());
	    task = this.changeTaskStatus(task.getId(), dao, task, Status.FAILED.toString());
	    System.out.println("New status = " +task.getStatus().toString());
	    // change priority to HIGH
	    task.setPriority(Priority.HIGH);
	    this.updateTask(dao, task);
	    // this should fail
	    try {
			Task nullTask = this.changeTaskStatus(task.getId(), dao, task, "INVALID_STATUS");
		} catch (Exception e1) {
			System.out.println(e1.getMessage());
		}
		this.deleteTask(dao, task);
		// this should fail
		try {
			this.deleteTask(dao, new Task());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		this.createXBELExportNetworkTask(dao, smallCorpusId, userId);
	}
	
	private void updateTask(TaskDAO dao, Task task) {
		System.out.println("Updating task id " +task.getId());
		 try {
			dao.updateTask(task, userId);
			System.out.println("Task id " +task.getId() +" updated");
		} catch (IllegalArgumentException 
				| SecurityException | NdexException e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	private Task createXBELExportNetworkTask(TaskDAO dao, String networkId, String userId){
		System.out.println("Creating export task");
		Task task = new Task();
		try {
			 task = dao.createXBELExportNetworkTask(networkId, userId);
			System.out.println("Export task id " + task.getId() +" created");
			
		} catch (IllegalArgumentException | SecurityException | NdexException e) {
			System.out.println(e.getMessage());
		}
		return task;
	}
	
	private Task changeTaskStatus(String taskId, TaskDAO dao, Task task, String newStatus){
		System.out.println("Change status of task id " +taskId +" to " +newStatus);
		Task newTask;
		try {
			newTask = dao.setTaskStatus(taskId, newStatus, userId);
			System.out.println("New status = " + newTask.getStatus());
			return newTask;
		} catch (NdexException e) {
			System.out.println(e.getMessage());
		}
		return new Task();
	}
	
	private Task getTask(TaskDAO dao, String taskId){
		Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId), "getTask requires a task id");
		try {
			Task task = dao.getTask(taskId, userId);
			System.out.println("Retrieved task id= " +task.getId());
			return task;
		} catch (IllegalArgumentException | SecurityException | NdexException e) {
			System.out.println(e.getMessage());
		}
		return new Task();
	}
	
	private void deleteTask(TaskDAO dao, Task task){
		Preconditions.checkArgument(!Strings.isNullOrEmpty(task.getId()), "deleteTask requires a task id");
			try {
				dao.deleteTask(task.getId(), userId);
				System.out.println("Task id " +task.getId() +" deleted");
			} catch (IllegalArgumentException | SecurityException
					| NdexException e) {
				System.out.println(e.getMessage());
				
			}
		
	}
	
	private Task testCreateTask(TaskDAO dao) {
		Task task = new Task();
		task.setCreatedDate(new Date());
		task.setDescription("Test task");
		task.setId("");  
		task.setPriority(Priority.LOW);
		task.setProgress(0);
		task.setResource(smallCorpusId);
		task.setStatus(Status.COMPLETED);
		task.setType(TaskType.EXPORT_NETWORK_TO_FILE);
		
		try {
			task.setId(dao.createTask(task, userId).getId());
			System.out.println("New Task created id = " +task.getId());
		} catch (IllegalArgumentException | NdexException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return task;
		
	}
	
	public static void main(String[] args) {
	 TestCommonDAO test = new TestCommonDAO();
	 test.performTests();

	}
 */
}
