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

import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
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
			newTask.setExternalId(NdexUUIDFactory.INSTANCE.createNewNDExUUID());
		
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
		
		if ( newTask.getAttributes() != null ) {
			taskDoc = taskDoc.field(NdexClasses.Task_P_attributes, newTask.getAttributes());
		}
		
		taskDoc = taskDoc.save();
		
		if ( accountName != null) {
			ODocument userDoc = getRecordByAccountName(accountName, NdexClasses.User);
			
			String userUUID = userDoc.field(NdexClasses.ExternalObj_ID);

			taskDoc = taskDoc.field("ownerUUID", userUUID).save();
			
			OrientVertex taskV = graph.getVertex(taskDoc);
			
			OrientVertex userV = this.graph.getVertex(userDoc.reload());
			for	(int retry = 0;	retry <	NdexDatabase.maxRetries;	++retry)	{
				try	{
					taskV.addEdge(NdexClasses.Task_E_owner, userV );

					break;
				} catch(ONeedRetryException	e)	{
					logger.warning("Retry creating task add edge.");
					taskV.reload();
					taskV.getRecord().removeField("out_"+ NdexClasses.Task_E_owner);
					userV.reload();
				}
			}
			newTask.setTaskOwnerId(UUID.fromString(userUUID));
			graph.commit();
			NdexServerQueue.INSTANCE.addUserTask(newTask);
		}
		
		return newTask.getExternalId();
		
	}

	
    public int purgeTask (UUID taskID) throws ObjectNotFoundException, NdexException {
        ODocument d = this.getRecordByUUID(taskID, NdexClasses.Task);
        boolean isDeleted = d.field(NdexClasses.ExternalObj_isDeleted);
 
        if ( isDeleted ) {
            OrientVertex v = graph.getVertex(d);
    			   
     		for	(int retry = 0;	retry <	NdexDatabase.maxRetries;	++retry)	{
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
