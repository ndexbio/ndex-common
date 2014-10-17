package org.ndexbio.common.models.dao.orientdb;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.Priority;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.network.FileFormat;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class TaskDAO extends OrientdbDAO {


	private OrientBaseGraph graph;
	private static final Logger logger = Logger.getLogger(TaskDAO.class.getName());
	
	public TaskDAO (ODatabaseDocumentTx dbConn) {
		super(dbConn);
		this.graph = new OrientGraph(this.db,false);
	}

	public Task getTaskByUUID(String UUID) {
        
        return getTaskFromDocument(getTaskDocByUUID(UUID));
		
	}
	
	private ODocument getTaskDocByUUID(String uuid) {
		String query = "select * from " + NdexClasses.Task + " where " + NdexClasses.ExternalObj_ID
				 + " ='" + uuid + "'";
       final List<ODocument> tasks = db.query(new OSQLSynchQuery<ODocument>(query));
       
       if (tasks.isEmpty())
	        return null;
       
	   return tasks.get(0);	
	}
	
	public UUID createTask(String accountName, Task newTask) throws ObjectNotFoundException, NdexException {
		UserDAO udao = new UserDAO (db);
		ODocument userDoc = udao.getRecordByAccountName(accountName, NdexClasses.User);
		
		UUID taskUUID = NdexUUIDFactory.INSTANCE.getNDExUUID();
		ODocument taskDoc = new ODocument(NdexClasses.Task)
				.fields(NdexClasses.ExternalObj_ID, taskUUID.toString(),
					NdexClasses.ExternalObj_cTime, newTask.getCreationTime(),
					NdexClasses.ExternalObj_mTime, newTask.getModificationTime(),
					NdexClasses.Task_P_description, newTask.getDescription(),
					NdexClasses.Task_P_status, newTask.getStatus(),
					NdexClasses.Task_P_priority, newTask.getPriority(),
					NdexClasses.Task_P_progress, newTask.getProgress(),
					NdexClasses.Task_P_taskType, newTask.getTaskType(),
					NdexClasses.Task_P_resource, newTask.getResource());
	
		if ( newTask.getFormat() != null) 
			taskDoc = taskDoc.field(NdexClasses.Task_P_fileFormat, newTask.getFormat());
		
		taskDoc = taskDoc.save();
		
		this.graph.getVertex(taskDoc).addEdge(NdexClasses.Task_E_owner, this.graph.getVertex(userDoc));
		return taskUUID;
		
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
    public Task updateTaskStatus(Status status, String UUIDString, User account) throws NdexException{
    	Preconditions.checkArgument(account != null, "Must be logged in to update a task");
    	Preconditions.checkArgument(null != UUIDString, "A Task UUID is required");
    
    	try {
    		ODocument taskDocument = this.getTaskDocByUUID(UUIDString);
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


    // This is the method called by trusted applications
    // such as the task runner, where we also update
    // status in the Task object passed in.
    public Task updateTaskStatus(Status status, Task task) {
    	String UUIDString = task.getExternalId().toString();
    	ODocument doc = this.getTaskDocByUUID(UUIDString);
    	doc.field(NdexClasses.Task_P_status, status).save();
    	task.setStatus(status);
    	return task;
    }
    
    public int deleteTask (UUID taskID) throws ObjectNotFoundException, NdexException {
        ODocument d = this.getRecordById(taskID,NdexClasses.Task);
        
        OrientVertex v = graph.getVertex(d);
   		v.remove();
    			           
    	return 1;		           
    }
    
    public void flagStagedTaskAsErrors() {
    	db.command( new OCommandSQL("update "+ NdexClasses.Task + " set " + 
    	  NdexClasses.Task_P_status + " = '"+ Status.COMPLETED_WITH_ERRORS + "' where " +
    	  NdexClasses.Task_P_status + " = '"+ Status.STAGED.toString() + "'")).execute();
    }
}
