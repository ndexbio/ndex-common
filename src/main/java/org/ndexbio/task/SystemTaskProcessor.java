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
				if ( task == NdexServerQueue.endOfQueue) {
					logger.info("End of queue signal received. Shutdown processor.");
					return;
				}
			} catch (InterruptedException e) {
				logger.info("takeNextSystemTask Interrupted.");
				return;
			}

			TaskType type = task.getTaskType();
			if ( type == TaskType.SYSTEM_DELETE_NETWORK) {
				try {
				    cleanupDeletedNetwork(task);
				} catch (NdexException e) {
					logger.severe("Error when executing system task: " + e.getMessage());
					e.printStackTrace();
				} catch ( Exception e2) {
					logger.severe("Error when executing system task: " + e2.getMessage());
					e2.printStackTrace();
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
				task.setExternalId(NdexUUIDFactory.INSTANCE.createNewNDExUUID());
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
