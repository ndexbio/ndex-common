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
package org.ndexbio.common.models.dao.orientdb;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.Priority;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskAttribute;
import org.ndexbio.model.object.TaskType;
import org.ndexbio.model.object.network.FileFormat;

import com.google.common.collect.Lists;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class TaskDocDAO extends OrientdbDAO {


//	private static final Logger logger = Logger.getLogger(TaskDAO.class.getName());
	
	public TaskDocDAO (ODatabaseDocumentTx dbConn) {
		super(dbConn);
	}

	public Task getTaskByUUID(String UUIDStr) throws ObjectNotFoundException, NdexException {
        
        return getTaskFromDocument(getRecordByUUID(UUID.fromString(UUIDStr),NdexClasses.Task));
		
	}
	
	protected static Task getTaskFromDocument(ODocument doc) {
		Task result = new Task();

		Helper.populateExternalObjectFromDoc(result, doc);
    	
		result.setDescription((String)doc.field(NdexClasses.Task_P_description));
		result.setPriority(Priority.valueOf((String)doc.field(NdexClasses.Task_P_priority)));
		result.setProgress((int)doc.field(NdexClasses.Task_P_progress));
		result.setResource((String)doc.field(NdexClasses.Task_P_resource));
		result.setStatus(Status.valueOf((String)doc.field(NdexClasses.Task_P_status)));
		result.setTaskType(TaskType.valueOf((String)doc.field(NdexClasses.Task_P_taskType)));
		
		Object o = doc.field(NdexClasses.Task_P_attributes);
		if ( o != null) {
			Map<String, Object> attr = (Map<String,Object>) o;
			result.setAttributes(attr);
		}
		
		Date d = doc.field(NdexClasses.Task_P_startTime);
		if (d !=null)
			result.setStartTime(new Timestamp(d.getTime()));
		d = doc.field(NdexClasses.Task_P_endTime);
		if ( d!=null)
			result.setFinishTime(new Timestamp(d.getTime()));
		result.setMessage((String)doc.field(NdexClasses.Task_P_message));
		
		ODocument ownerDoc = doc.field("out_"+ NdexClasses.Task_E_owner);
		
		result.setTaskOwnerId(UUID.fromString((String)ownerDoc.field(NdexClasses.ExternalObj_ID)));

		String str = doc.field(NdexClasses.Task_P_fileFormat);
		
		if ( str != null) result.setFormat(FileFormat.valueOf(str));
		
        return result;
	}
	
    public List<Task> stageQueuedTasks()     {
   	 	List<Task> stagedList = Lists.newArrayList();
   	 	List<ODocument> recs = this.getTaskDocumentsByStatus(Status.QUEUED);
     
   	 	for (final ODocument document : recs) {
			stagedList.add(getTaskFromDocument(document));
			document.field(NdexClasses.Task_P_status, Status.STAGED).save();
   	 	}
     
   	 	return stagedList;
   	 
    }
    
    public List<Task> getActiveTasks(){
    	List<Task> taskList = Lists.newArrayList();
    	for ( ODocument d : getTaskDocumentsByStatus(Status.PROCESSING))
    		taskList.add(getTaskFromDocument(d));
    	for ( ODocument d : getTaskDocumentsByStatus(Status.STAGED))
    		taskList.add(getTaskFromDocument(d));
    	
    	return taskList;
       }

    
    private List<ODocument>  getTaskDocumentsByStatus(Status aStatus) {
    	String query = "select from task "
	            + " where status = '" +aStatus +"'";
        return  db.query(new OSQLSynchQuery<ODocument>(query));
    }
    
    // This is the method called by the Task REST Service
    // The User account is passed in so that we can check to be
    // sure that the user requesting the update owns the task
/*    public Task updateTaskStatus(Status status, String UUIDString, User account) throws NdexException{
    	Preconditions.checkArgument(account != null, "Must be logged in to update a task");
    	Preconditions.checkArgument(null != UUIDString, "A Task UUID is required");
    
    	try {
    		ODocument taskDocument = this.getRecordByExternalId(UUID.fromString(UUIDString));
    		OrientVertex vTask = this.graph.getVertex(taskDocument);
    		ODocument userAccount = this.getRecordById(account.getExternalId(), NdexClasses.User);
    	
    		for(Vertex v : vTask.getVertices(Direction.OUT, "ownedBy")) {
				if(((OrientVertex) v).getIdentity().equals(userAccount.getIdentity())) {
					taskDocument.field(NdexClasses.Task_P_status, status).save();
					return this.getTaskByUUID(UUIDString);
				} 
				logger.severe("Account " + account.getAccountName() + " is not owner of task " + UUIDString);
				throw new UnauthorizedOperationException("access denied - " + account.getAccountName() + " is not owner of task " + UUIDString); // message will not be saved
				
    		}
		} catch (SecurityException e){
			throw e;
		} catch (Exception e) {
			logger.severe("Unable to update task. UUID : " +  UUIDString + " error: " + e.toString());
			throw new NdexException("Failed to update the task");
		}
		return null; 
	
    }
*/

    // This is the method called by trusted applications
    // such as the task runner, where we also update
    // status in the Task object passed in.
    /**
     * Update the status of task.status to newStatus. Update will be applied to database and the task object. 
     * @param newStatus
     * @param task
     * @return
     * @throws ObjectNotFoundException
     * @throws NdexException
     */
    public Task updateTaskStatus(Status newStatus, Task task) throws ObjectNotFoundException, NdexException {
    	ODocument doc = this.getRecordByUUID(task.getExternalId(), NdexClasses.Task);
    	Status s = Status.valueOf((String)doc.field(NdexClasses.Task_P_status));
    	if ( s != newStatus ) {
       		for	(int retry = 0;	retry <	NdexDatabase.maxRetries;	++retry)	{
       			try	{
       	    		doc.fields(NdexClasses.Task_P_status, newStatus,
         				   NdexClasses.ExternalObj_mTime, new Date()).save();
      				break;
       			} catch(ONeedRetryException	e)	{
       				doc.reload();
       			}
       		}
    	}
    	task.setStatus(newStatus);
    	return task;
    }

    
	public void saveTaskStatus (String taskID, Status status, String message, String stackTrace) throws NdexException {
			ODocument taskdoc = getRecordByUUIDStr(taskID, NdexClasses.Task);
			
			if ( status == Status.PROCESSING) {
				taskdoc.fields(NdexClasses.Task_P_startTime, new Timestamp(Calendar.getInstance().getTimeInMillis()),
							NdexClasses.Task_P_status, status.toString()).save();
			} else if ( status == Status.COMPLETED || status == Status.COMPLETED_WITH_ERRORS 
						|| status == Status.COMPLETED_WITH_WARNINGS || status == Status.FAILED) {
				taskdoc.fields(NdexClasses.Task_P_endTime, new Timestamp(Calendar.getInstance().getTimeInMillis()),
						NdexClasses.Task_P_status, status.toString(),
						NdexClasses.Task_P_message, message).save();
				if ( stackTrace !=null) {
					Map<String, Object> attributes = taskdoc.field(NdexClasses.Task_P_attributes);
					if ( attributes == null) {
						attributes = new TreeMap<>();
					}
					attributes.put(TaskAttribute.NdexServerStackTrace, stackTrace);
					taskdoc.fields(NdexClasses.Task_P_attributes,attributes).save();
				}
			}
	}

   
    public void flagStagedTaskAsErrors() {
    	db.command( new OCommandSQL("update "+ NdexClasses.Task + " set " + 
    	  NdexClasses.Task_P_status + " = '"+ Status.COMPLETED_WITH_ERRORS + "' where " +
    	  NdexClasses.Task_P_status + " = '"+ Status.STAGED.toString() + "'")).execute();
    }

    public int deleteTask (UUID taskID) throws ObjectNotFoundException, NdexException {
        ODocument d = this.getRecordByUUID(taskID, NdexClasses.Task);
       
   		for	(int retry = 0;	retry <	NdexDatabase.maxRetries;	++retry)	{
   			try	{
   		        d.fields(NdexClasses.ExternalObj_isDeleted,  true, 
   	        		 NdexClasses.ExternalObj_mTime, new Date()).save();
  				break;
   			} catch(ONeedRetryException	e)	{
   				d.reload();
   			}
   		}
   		
    	return 1;		           
    }
    
    public void saveTaskAttributes(String UUIDStr, Map<String, Object> attributes ) throws ObjectNotFoundException, NdexException {
    	ODocument d = this.getRecordByUUID(UUID.fromString(UUIDStr), NdexClasses.Task);
    	d.field(NdexClasses.Task_P_attributes, attributes).save();
    }

    /**
     * Get all the tasks that have status as 'queued' or 'processing' in db. This is a helper function for system startup. 
     * @return
     */
    public Collection<Task> getUnfinishedTasks() { 
    	List<Task> result = new LinkedList<>();
    	OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
  			"SELECT FROM task where ( isDeleted=false) and ( status = 'QUEUED' or status = 'PROCESSING') ");
    	List<ODocument> records = db.command(query).execute();
    	for ( ODocument doc : records ) {
    		result.add(getTaskFromDocument(doc));
    	}
    	return result;
    }
}
