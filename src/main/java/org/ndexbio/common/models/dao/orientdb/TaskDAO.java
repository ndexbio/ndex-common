package org.ndexbio.common.models.dao.orientdb;

import java.util.Date;
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

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class TaskDAO {


	private ODatabaseDocumentTx db;
	private OrientBaseGraph graph;
	private static final Logger logger = Logger.getLogger(TaskDAO.class.getName());
	
	public TaskDAO (ODatabaseDocumentTx db) {
		this.db = db;
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
					NdexClasses.ExternalObj_cDate, newTask.getCreationDate(),
					NdexClasses.ExternalObj_mDate, newTask.getModificationDate(),
					NdexClasses.Task_P_description, newTask.getDescription(),
					NdexClasses.Task_P_status, newTask.getStatus(),
					NdexClasses.Task_P_priority, newTask.getPriority(),
					NdexClasses.Task_P_progress, newTask.getProgress(),
					NdexClasses.Task_P_taskType, newTask.getTaskType(),
					NdexClasses.Task_P_resource, newTask.getResource())
				.save();	
	
		this.graph.getVertex(taskDoc).addEdge(NdexClasses.Task_E_owner, this.graph.getVertex(userDoc));
		return taskUUID;
		
	}

	static Task getTaskFromDocument(ODocument doc) {
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


    public Task updateTaskStatus(Status status, Task task) {
    	ODocument doc = this.getTaskDocByUUID(task.getExternalId().toString());
    	
    	doc.field(NdexClasses.Task_P_status, status).save();
    	task.setStatus(status);
    	return task;
    }
}
