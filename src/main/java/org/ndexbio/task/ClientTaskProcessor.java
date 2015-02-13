package org.ndexbio.task;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.TaskDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;
import org.ndexbio.model.object.network.FileFormat;


public class ClientTaskProcessor extends NdexTaskProcessor {

    private Logger logger = Logger.getLogger(ClientTaskProcessor.class.getSimpleName());
	
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
				NdexTask t = getNdexTask(task);
				saveTaskStatus(task.getExternalId().toString(), Status.PROCESSING, null);
				t.call();
				saveTaskStatus(task.getExternalId().toString(), Status.COMPLETED, null);
				
			} catch (Exception e) {
				logger.severe("Error occured when executing task " + task.getExternalId());
				e.printStackTrace();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);     
				try {
					saveTaskStatus(task.getExternalId().toString(), Status.FAILED, e.getMessage() + "\n\n"
							+ sw.toString());
				} catch (NdexException e1) {
					logger.severe("Error occured when saving task " + e1);
				}
				
			} 
		}
	}
	
	private static NdexTask getNdexTask(Task task) throws NdexException{
		
		try {
			if( task.getTaskType() == TaskType.PROCESS_UPLOADED_NETWORK) {
				return new FileUploadTask(task, NdexDatabase.getInstance());
			}
			if( task.getTaskType() == TaskType.EXPORT_NETWORK_TO_FILE) {
				if ( task.getFormat() == FileFormat.XBEL)
					return new XbelExporterTask(task);
				else if ( task.getFormat() == FileFormat.XGMML) {
					return new XGMMLExporterTask(task);
				} if ( task.getFormat() == FileFormat.BIOPAX) {
					return new BioPAXExporterTask(task);
				}
				
				throw new NdexException ("Only XBEL, XGMML and BIOPAX exporters are implemented.");
			}
			throw new IllegalArgumentException("Task type: " +task.getType() +" is not supported");
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
