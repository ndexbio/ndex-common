package org.ndexbio.task;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.TaskDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.network.FileFormat;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;



public class ClientTaskProcessor extends NdexTaskProcessor {

	static Logger logger = LoggerFactory.getLogger(ClientTaskProcessor.class);
	
	public ClientTaskProcessor () {
		super();
	}
	
	@Override
	public void run() {
		while ( !shutdown) {
			Task task = null;
			try {
				task = NdexServerQueue.INSTANCE.takeNextUserTask();
				if ( task == NdexServerQueue.endOfQueue) {
					logger.info("End of queue signal received. Shutdown processor.");
					return;
				}
			} catch (InterruptedException e) {
				logger.info("takeNextUserTask Interrupted.");
				return;
			}
			
			try {		        
		        MDC.put("RequestsUniqueId", (String)task.getAttribute("RequestsUniqueId") );
				logger.info("[start: starting task]");
				
				NdexTask t = getNdexTask(task);
				saveTaskStatus(task.getExternalId().toString(), Status.PROCESSING, null);
				Task taskObj = t.call();
				saveTaskStatus(task.getExternalId().toString(), Status.COMPLETED, taskObj.getMessage());

				logger.info("[end: task completed]");

			} catch (Exception e) {
				logger.error("Error occured when executing task " + task.getExternalId());
				e.printStackTrace();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);     
				try {
					saveTaskStatus(task.getExternalId().toString(), Status.FAILED, e.getMessage() + "\n\n"
							+ sw.toString());
				} catch (NdexException e1) {
					logger.error("Error occured when saving task " + e1);
				}
				
			} 
		}
	}
	
	private static NdexTask getNdexTask(Task task) throws NdexException{
		
		try {
			switch ( task.getTaskType()) { 
				case PROCESS_UPLOADED_NETWORK: 
					return new FileUploadTask(task, NdexDatabase.getInstance());
				case EXPORT_NETWORK_TO_FILE: 
					
					if ( task.getFormat() == FileFormat.XBEL)
						return new XbelExporterTask(task);
					else if ( task.getFormat() == FileFormat.XGMML) {
						return new XGMMLExporterTask(task);
					} if ( task.getFormat() == FileFormat.BIOPAX) {
						return new BioPAXExporterTask(task);
					} if ( task.getFormat() == FileFormat.SIF) {
						return new SIFExporterTask(task);
					} 
				
					throw new NdexException ("Only XBEL, XGMML and BIOPAX exporters are implemented.");
				case CREATE_NETWORK_CACHE: 
					return new AddNetworkToCacheTask(task);
				case DELETE_NETWORK_CACHE:
					return new RemoveNetworkFromCacheTask(task);
				default:
					throw new NdexException("Task type: " +task.getType() +" is not supported");
			}		
		} catch (IllegalArgumentException | SecurityException | NdexException e) {
			e.printStackTrace();
			throw new NdexException ("Error occurred when creating task. " + e.getMessage());
		} 
	}


	private static  void saveTaskStatus (String taskID, Status status, String message) throws NdexException {
		try (TaskDocDAO dao = new TaskDocDAO (NdexDatabase.getInstance().getAConnection());) {
			dao.saveTaskStatus(taskID, status, message);
			dao.commit();
		}
	}
	
	
}
