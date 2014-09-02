package org.ndexbio.common.persistence.orientdb;

import java.util.List;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.dao.orientdb.TaskDAO;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Vertex;

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
    
    public NdexTaskService() throws NdexException
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
    
    public Task getTask(String taskUUID) {
    	TaskDAO dao = new TaskDAO(ndexService._ndexDatabase);
    	return dao.getTaskByUUID(taskUUID);
    }

    
    
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
			TaskDAO dao = new TaskDAO(this.ndexService._ndexDatabase);
			dao.updateTaskStatus(status, task);
			this.ndexService._ndexDatabase.commit();
			return task;
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
			throw new NdexException("Failed to search tasks.");
			
		}finally {
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
