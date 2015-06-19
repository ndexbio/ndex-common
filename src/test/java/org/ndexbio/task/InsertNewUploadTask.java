/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.*;
import org.ndexbio.common.models.dao.orientdb.TaskDAO;
import org.ndexbio.common.persistence.orientdb.OrientDBNoTxConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/*
 * this class is responsible for inserting a new file upload task into the orientdb database
 * it can be invoked directly as an application to simply create the new task for a background
 * processor to deal with or it can be invoked as part of a more comprehensive test.
 */

public class InsertNewUploadTask {
	private  final File sourceFile;
	private LocalDataService ds;
	private static final String NETWORK_UPLOAD_PATH = "/opt/ndex/uploaded-networks/";
	
	 private static final Logger logger = LoggerFactory.getLogger(InsertNewUploadTask.class);
	
	public InsertNewUploadTask (String fn) throws NdexException{
		this.sourceFile = new File(fn);
		this.ds = new LocalDataService();		
	}
	
	private void persistFileUploadTask() {
		try {
			String newFile = this.copyFileToConfigArea();
			Task task = this.generateTask(newFile);
			this.ds.createTask(task);
			
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} catch (NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private Task generateTask(String newFile) {
		Task task = new Task();
		task.setResource(newFile);
		task.setStatus(Status.QUEUED);
		
		return task;
	}

	public static void main(String[] args) throws NdexException {
		String testFile = null;
		if(args.length >0 ){
			testFile = args[0];
		} else {
			testFile = "/tmp/small-corpus.xbel";
		}
		logger.info("Scheduling " +testFile +" for loading");
		InsertNewUploadTask test = new InsertNewUploadTask(testFile);
		test.persistFileUploadTask();

	}
	
	private String copyFileToConfigArea () throws IOException{
		String newFile = this.resolveFilename();
		Path uploadFile = Paths.get(new File(newFile).toURI());
		Path inputFile = Paths.get(this.sourceFile.toURI());
		logger.info("Copying " +inputFile +" to " +uploadFile);
		
		Files.copy(inputFile, uploadFile, StandardCopyOption.REPLACE_EXISTING);
		return uploadFile.toString();
		 
	}
	
	private String resolveFilename() {
		StringBuilder sb = new StringBuilder();
		sb.append(NETWORK_UPLOAD_PATH);
		sb.append(this.sourceFile.getName());
		return sb.toString();
	}
	
	class LocalDataService extends OrientDBNoTxConnectionService {
		
		LocalDataService() {
			super();
			
		}
		
		public Task createTask(final Task newTask) throws IllegalArgumentException, NdexException
	    {
	        Preconditions.checkArgument(null!= newTask,"The task to create is empty.");
			
	        

	        try
	        {
	            setupDatabase();
	            
	            
	            final Task task = new Task();
	            task.setStatus(newTask.getStatus());
	            task.setResource(newTask.getResource());
	            task.setProgress(0);
	            task.setTaskType(TaskType.PROCESS_UPLOADED_NETWORK);

	            TaskDAO dao = new TaskDAO(this._ndexDatabase);
	            UUID taskId = dao.createTask(this.getLoggedInUser(), task);
	            this._ndexDatabase.commit();
	            logger.info("file upload task for " + task.getResource() +" created");

	            task.setExternalId(taskId);
	            
	            return task;
	        }
	        catch (Exception e)
	        {
	            logger.error("Failed to create a task : " , e);
	            this._ndexDatabase.rollback(); 
	            throw new NdexException("Failed to create a task.");
	        }
	        finally
	        {
	            teardownDatabase();
	        }
	    }

		private String getLoggedInUser() {
			return "tester";
			
		}
		
		
	}
	


}
