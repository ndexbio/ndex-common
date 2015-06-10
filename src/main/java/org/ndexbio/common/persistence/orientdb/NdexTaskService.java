/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.common.persistence.orientdb;

import java.util.List;

import org.ndexbio.common.models.dao.orientdb.TaskDAO;
import org.ndexbio.common.models.dao.orientdb.TaskDocDAO;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

/*
 * Represents a collection of methods for interacting with Tasks in the orientdb database
 * Retained in the common ndex-common project to facilitate availability to multiple ndex
 * projects using Tasks
 * 
 * mod 13Jan2014 - use domain objects instead of model objects 
 * mod 01Apr2014 - add public method to delete task entities
 */

public class NdexTaskService 
{
    private static final Logger logger = LoggerFactory.getLogger(NdexTaskService.class);
    private OrientDBNoTxConnectionService ndexService;
    
    public NdexTaskService()
    {
    	ndexService = new OrientDBNoTxConnectionService();  
    }
    
    
    /*
     * public method to delete Task entities that have a status of
     * QUEUED_FOR_DELETION
     */
    public void deleteTasksQueuedForDeletion() throws NdexException {
    	String query = "select from task "
	            + " where status = '" +Status.QUEUED_FOR_DELETION.toString() +"'";
    	
    	try {
    		
			this.ndexService.setupDatabase();
			
			final List<ODocument> taskDocumentList = this.ndexService._ndexDatabase.
					query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument document : taskDocumentList) {
				this.ndexService.getGraph().getVertex(document).remove();
			}
			
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
            throw new NdexException("Failed to search tasks.");
			
		}finally {
			this.ndexService.teardownDatabase();
		}
    	
    }
  
    /*
    public Task getTask(String taskUUID) throws ObjectNotFoundException, NdexException {
    	TaskDAO dao = new TaskDAO(NdexAOrientDBConnectionPool.getInstance().acquire());
    	return dao.getTaskByUUID(taskUUID);
    }
*/
    
    
    public List<Task> stageQueuedTasks() throws NdexException
    {
    	try {
    		
			this.ndexService.setupDatabase();
			TaskDAO dao = new TaskDAO(this.ndexService._ndexDatabase);
			List<Task> taskList = dao.stageQueuedTasks();
			this.ndexService._ndexDatabase.commit();
			return taskList;
			
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
            throw new NdexException("Failed to search tasks.");
			
		}finally {
			this.ndexService.teardownDatabase();
		}
   	 
    }

    public List<Task> getActiveTasks() throws NdexException
    {
    	try {
    		
			this.ndexService.setupDatabase();
			TaskDAO dao = new TaskDAO(this.ndexService._ndexDatabase);
			List<Task> taskList = dao.getActiveTasks();
			return taskList;
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
            throw new NdexException("Failed to search tasks.");
			
		}finally {
			this.ndexService.teardownDatabase();
		}
   	 
    }

    public Task updateTaskStatus(Status status, Task task) throws NdexException {
    	
    	try {
    		
			this.ndexService.setupDatabase();
			TaskDocDAO dao = new TaskDocDAO(this.ndexService._ndexDatabase);
			logger.info("Updating status of tasks " + task.getExternalId() + " from " +
			   task.getStatus() + " to " + status);
			dao.updateTaskStatus(status, task);
			this.ndexService._ndexDatabase.commit();
			return task;
		} catch (Exception e) {
			logger.error("Failed to update task satus : " + e.getMessage(), e);
			throw new NdexException("Failed to update status of task: " + task.getExternalId() + " to " + status);
			
		}finally {
			this.ndexService.teardownDatabase();
		}
    	
    }

    public void addTaskAttribute(String uuidStr, String name, Object value) throws NdexException {
    	try {
    		
			this.ndexService.setupDatabase();
			TaskDocDAO dao = new TaskDocDAO(this.ndexService._ndexDatabase);
			Task t = dao.getTaskByUUID(uuidStr);
			t.getAttributes().put(name,value);
			dao.saveTaskAttributes(uuidStr,t.getAttributes());
			this.ndexService._ndexDatabase.commit();
			return;
		} finally {
			this.ndexService.teardownDatabase();
		}
    	
    	
    }
    
    public String getTaskOwnerAccount(Task task) throws NdexException {
    	try {
    		
			this.ndexService.setupDatabase();
			UserDAO dao = new UserDAO(this.ndexService._ndexDatabase);
			return dao.getUserById(task.getTaskOwnerId()).getAccountName();
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
			throw new NdexException("Failed to search tasks." + e.getMessage());
			
		}finally {
			this.ndexService.teardownDatabase();
		}
    	
    }
   
}
