package org.ndexbio.common.models.dao.orientdb;

import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Task;
import org.ndexbio.task.NdexServerQueue;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class TaskDAO extends TaskDocDAO {

	private OrientBaseGraph graph;
	private static final Logger logger = Logger.getLogger(TaskDAO.class.getName());
	
	public TaskDAO (ODatabaseDocumentTx dbConn) {
		super(dbConn);
		this.graph = new OrientGraph(this.db,false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);

	}

	
	public UUID createTask(String accountName, Task newTask) throws ObjectNotFoundException, NdexException {
		
		if (newTask.getExternalId() == null)
			newTask.setExternalId(NdexUUIDFactory.INSTANCE.getNDExUUID());
		
		ODocument taskDoc = new ODocument(NdexClasses.Task)
				.fields(NdexClasses.ExternalObj_ID, newTask.getExternalId().toString(),
					NdexClasses.ExternalObj_cTime, newTask.getCreationTime(),
					NdexClasses.ExternalObj_mTime, newTask.getModificationTime(),
					NdexClasses.Task_P_description, newTask.getDescription(),
					NdexClasses.Task_P_status, newTask.getStatus(),
					NdexClasses.Task_P_priority, newTask.getPriority(),
					NdexClasses.Task_P_progress, newTask.getProgress(),
					NdexClasses.Task_P_taskType, newTask.getTaskType(),
					NdexClasses.Task_P_resource, newTask.getResource(),
					NdexClasses.ExternalObj_isDeleted, false,
					NdexClasses.Task_P_startTime, newTask.getStartTime(),
					NdexClasses.Task_P_endTime, newTask.getFinishTime(),
					NdexClasses.Task_P_message, newTask.getMessage());
	
		if ( newTask.getFormat() != null) 
			taskDoc = taskDoc.field(NdexClasses.Task_P_fileFormat, newTask.getFormat());
		
		taskDoc = taskDoc.save();
		
		if ( accountName != null) {
			OrientVertex userV = this.graph.getVertex(getRecordByAccountName(accountName, NdexClasses.User));
			String userUUID = userV.getRecord().field(NdexClasses.ExternalObj_ID);

			taskDoc = taskDoc.field("ownerUUID", userUUID).save();
			
			OrientVertex taskV = graph.getVertex(taskDoc);
		
			for	(int retry = 0;	retry <	maxRetries;	++retry)	{
				try	{
					taskV.addEdge(NdexClasses.Task_E_owner, userV );
					break;
				} catch(ONeedRetryException	e)	{
					logger.warning("Retry creating task add edge.");
					userV.reload();
					taskV.reload();
				}
			}
			newTask.setTaskOwnerId(UUID.fromString(userUUID));

			try {
				NdexServerQueue.INSTANCE.addUserTask(newTask);
			} catch (InterruptedException e) {
				throw new NdexException ("Interrupted when adding user task to queue.");
			}
		}
		
		return newTask.getExternalId();
		
	}

	
    public int purgeTask (UUID taskID) throws ObjectNotFoundException, NdexException {
        ODocument d = this.getRecordByUUID(taskID, NdexClasses.Task);
        boolean isDeleted = d.field(NdexClasses.ExternalObj_isDeleted);
 
        if ( isDeleted ) {
            OrientVertex v = graph.getVertex(d);
    			   
     		for	(int retry = 0;	retry <	maxRetries;	++retry)	{
     			try	{
   		   			v.remove();
   		   			break;
     			} catch(ONeedRetryException	e)	{
     				logger.warning("Write conflict when deleting task. Error: " + e.getMessage() +
   						"\nRetry ("+ retry + ") deleting task " + taskID.toString() );
     				v.reload();
     			}
     		}
   		
     		return 1;
        } 
        
        throw new NdexException ("Only deleted tasks can be purged.");
        
    }
    

	@Override
	public void close() {
		this.graph.shutdown();
	}
	
	@Override
	public void commit() { this.graph.commit();}
}
