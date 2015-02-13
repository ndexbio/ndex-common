package org.ndexbio.task;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.TaskDAO;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;

import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;

public class SystemTaskProcessor extends NdexTaskProcessor {

    private Logger logger = Logger.getLogger(SystemTaskProcessor.class.getSimpleName());
	
	public SystemTaskProcessor () {
		super();
	}
	
	@Override
	public void run() {
		while ( !shutdown) {
			Task task = null;
			try {
				task = NdexServerQueue.INSTANCE.takeNextSystemTask();
			} catch (InterruptedException e) {
				logger.info("takeNextSystemTask Interrupted.");
				break;
			}

			TaskType type = task.getTaskType();
			if ( type == TaskType.SYSTEM_DELETE_NETWORK) {
				try {
				    cleanupDeletedNetwork(task);
				} catch (NdexException e) {
					logger.severe("Error when executing system task: " + e);
					e.printStackTrace();
				} 
			} else if ( type == TaskType.SYSTEM_DATABASE_BACKUP ) {
				try {
					backupDatabase(task);
				} catch (NdexException e) {
					// TODO Auto-generated catch block
					logger.severe("Error when export backup system task: " + e);
					e.printStackTrace();
				}
			} else {
					logger.severe("Unsupported system task type " + type + ". Task ignored.");
			}
		}
	}
	
	
	private void cleanupDeletedNetwork (Task task) throws NdexException  {
		logger.info( "Cleanup deleted network " + task.getResource());

		task.setStartTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
		try (NetworkDAO networkDao = new NetworkDAO(NdexDatabase.getInstance().getAConnection()); ) {
			int cnt = networkDao.cleanupDeleteNetwork(task.getResource());
			networkDao.commit();
			task.setFinishTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
			task.setStatus(Status.COMPLETED);
			if ( cnt >=0 ) {
				logger.info("Network " + task.getResource() + " cleanup finished.");
				task.setMessage(cnt + " vertices deleted.");
			} else {  // only partially deleted.
				String message ="cleanup stoppped after " +( -1 * cnt )+ " vertices deleted.";
				logger.info("Network " + task.getResource() + message);
				task.setMessage(message);
			}
			try (TaskDAO taskdao = new TaskDAO (NdexDatabase.getInstance().getAConnection())) {
				taskdao.createTask(null, task);
				taskdao.commit();
			}
			if ( cnt < 0 ) {
				task.setExternalId(NdexUUIDFactory.INSTANCE.getNDExUUID());
				NdexServerQueue.INSTANCE.addSystemTask(task); // add the task back to the queue.
			}
		}	
	}
	
	private void backupDatabase (Task task) throws NdexException {

		task.setStartTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
   		 String ndexRoot = Configuration.getInstance().getNdexRoot();
   		 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
   		 String strDate = sdf.format(Calendar.getInstance().getTime());

   		 try (ODatabaseDocumentTx db = NdexDatabase.getInstance().getAConnection()){
		
       		 String exportFile = ndexRoot + "/dbbackups/db_"+ strDate + ".export";

       		 logger.info("Backing up database to " + exportFile);
       		 
       		 try{
       			  OCommandOutputListener listener = new OCommandOutputListener() {
       			    @Override
       			    public void onMessage(String iText) {
       			      logger.info(iText);
       			    }
       			  };

       			  ODatabaseExport export = new ODatabaseExport(db, exportFile, listener);
       			  export.setIncludeIndexDefinitions(false);
       			  export.exportDatabase();
       			  export.close();
       			  task.setFinishTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
       			  task.setStatus(Status.COMPLETED);
       			  task.setMessage("Db exported to " + exportFile);
           		 logger.info("Database back up fininished succefully.");
       			} catch (Exception e) {
       				task.setMessage(e.getMessage());
       				task.setStatus(Status.FAILED);
					logger.severe("IO exception when backing up database. " + e.getMessage());
					e.printStackTrace();
				} 

 			try (TaskDAO taskdao = new TaskDAO (NdexDatabase.getInstance().getAConnection())) {
 				taskdao.createTask(null, task);
 				taskdao.commit();
 				taskdao.close();
 			}

       	 } 	
	}
}
