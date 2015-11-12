package org.ndexbio.task;

import org.ndexbio.common.models.dao.orientdb.SingleNetworkDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttachNamespacefilesTask extends NdexTask {

	private static final Logger logger = LoggerFactory
			.getLogger(AttachNamespacefilesTask.class);
	
	
	public AttachNamespacefilesTask(Task itask) throws NdexException {
		super(itask);
		
	}

	@Override
	public Task call() throws Exception  {
			logger.info("[start: attaching namespace files in network ='{}']", this.getTask().getResource());
			Status taskStatus = Status.PROCESSING;
			this.startTask();
			
			try (SingleNetworkDAO dao = new SingleNetworkDAO(this.getTask().getResource()); ) {
				dao.attachNamespaceFiles();
				dao.commit();
			}
			this.updateTaskStatus(taskStatus);
			logger.info("[end: attaching namespace files in network ='{}']", this.getTask().getResource());
	        return this.getTask();
	        
	}

}
