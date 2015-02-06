package org.ndexbio.common.models.dao.orientdb;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Priority;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
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
		
		Date d = doc.field(NdexClasses.Task_P_startTime);
		if (d !=null)
			result.setStartTime(new Timestamp(d.getTime()));
		d = doc.field(NdexClasses.Task_P_endTime);
		if ( d!=null)
			result.setFinishTime(new Timestamp(d.getTime()));
		result.setMessage(NdexClasses.Task_P_message);
		
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
				throw new SecurityException("access denied - " + account.getAccountName() + " is not owner of task " + UUIDString); // message will not be saved
				
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
    
    //TODO: looks like we are having racing conditions here. Need to review the usage and make it thread safe.
    public Task updateTaskStatus(Status status, Task task) throws ObjectNotFoundException, NdexException {
    	ODocument doc = this.getRecordByUUID(task.getExternalId(), NdexClasses.Task);
//    	doc.reload();
    	Status s = Status.valueOf((String)doc.field(NdexClasses.Task_P_status));
    	if ( s != status )
    		doc.fields(NdexClasses.Task_P_status, status,
    				   NdexClasses.ExternalObj_mTime, new Date()).save();
    	task.setStatus(status);
    	return task;
    }

   
    public void flagStagedTaskAsErrors() {
    	db.command( new OCommandSQL("update "+ NdexClasses.Task + " set " + 
    	  NdexClasses.Task_P_status + " = '"+ Status.COMPLETED_WITH_ERRORS + "' where " +
    	  NdexClasses.Task_P_status + " = '"+ Status.STAGED.toString() + "'")).execute();
    }

    public int deleteTask (UUID taskID) throws ObjectNotFoundException, NdexException {
        ODocument d = this.getRecordByUUID(taskID, NdexClasses.Task);
       
   		for	(int retry = 0;	retry <	maxRetries;	++retry)	{
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


}
