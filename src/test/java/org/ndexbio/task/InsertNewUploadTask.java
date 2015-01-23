package org.ndexbio.task;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.UUID;

import org.ndexbio.common.exceptions.*;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.*;
import org.ndexbio.common.models.dao.orientdb.TaskDAO;
import org.ndexbio.common.persistence.orientdb.OrientDBNoTxConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.id.ORID;


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
