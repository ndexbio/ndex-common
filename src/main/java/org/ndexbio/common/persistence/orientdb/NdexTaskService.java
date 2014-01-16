package org.ndexbio.common.persistence.orientdb;

import java.util.List;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.data.*;
import org.ndexbio.common.models.object.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.frames.VertexFrame;

/*
 * Represents a collection of methods for interacting with Tasks in the orientdb database
 * Retained in the common ndex-common project to facilitate availability to multiple ndex
 * projects using Tasks
 * 
 * mod 13Jan2014 - use domain objects instead of model objects 
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
     * get a Collection of ITasks based on status value
     */
    
    private List<ITask> getITasksByStatus(Status aStatus) throws NdexException {
    	String query = "select from task "
	            + " where status = '" +aStatus +"'";
    	final List<ITask> foundITasks = Lists.newArrayList();
    	try {
    		if (!this.ndexService.isSetup()) {
				this.ndexService.setupDatabase();
			}
			final List<ODocument> taskDocumentList = this.ndexService._orientDbGraph.getBaseGraph().
					 getRawGraph().query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument document : taskDocumentList) {
				foundITasks.add(this.ndexService._orientDbGraph.getVertex(document, ITask.class));
			}
			
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
            throw new NdexException("Failed to search tasks.");
			
		}finally {
			this.ndexService.teardownDatabase();
		}
    	return foundITasks;
    }
    
    /*
     * get a Collection of Tasks based on Status value
     */
    private List<Task> getTasksByStatus(Status aStatus) throws NdexException {
    	 String query = "select from task "
    	            + " where status = '" +aStatus +"'";
    	 final List<Task> foundTasks = Lists.newArrayList();
    	 try {
    		 if (!this.ndexService.isSetup()) {
 				this.ndexService.setupDatabase();
 			}

			 final List<ODocument> taskDocumentList = this.ndexService._orientDbGraph.getBaseGraph().
					 getRawGraph().query(new OSQLSynchQuery<ODocument>(query));
			 for (final ODocument document : taskDocumentList)
	                foundTasks.add(new Task(this.ndexService._orientDbGraph.getVertex(document, ITask.class)));

	            return foundTasks;
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
	            throw new NdexException("Failed to search tasks.");
		}finally {
			this.ndexService.teardownDatabase();
		}

    }
   
    
    public List<ITask> getInProgressITasks() throws NdexException{
   	 return getITasksByStatus(Status.PROCESSING);
    }
    
    public List<ITask> getActiveITasks() throws NdexException{
    	List<ITask> activeITasks =  getITasksByStatus(Status.PROCESSING);
    	activeITasks.addAll(getITasksByStatus(Status.STAGED));
    	return activeITasks;
       }
    
    public List<ITask> getAllCompletedITasks() throws NdexException {
    	List<ITask> completedITasks = this.getITasksByStatus(Status.COMPLETED);
    	completedITasks.addAll(this.getITasksByStatus(Status.COMPLETED_WITH_ERRORS));
    	completedITasks.addAll(this.getITasksByStatus(Status.COMPLETED_WITH_WARNINGS));
    	return completedITasks;
    }
    
    public List<ITask> getQueuedITasks() throws NdexException {
    	return getITasksByStatus(Status.QUEUED);
    }
    
     public List<Task> getQueuedTasks() throws NdexException {
    	 return getTasksByStatus(Status.QUEUED);
     }
     
     public List<Task> getInProgressTasks() throws NdexException{
    	 return getTasksByStatus(Status.PROCESSING);
     }
     
     
     /*
      * prublic method to query for ITasks with a QUEUED status,
      * update their status to STAGED and return them as a List
      */
     
     public List<ITask> stageQueuedITasks() throws NdexException
     {
    	 List<ITask> stagedList = Lists.newArrayList();
    	 List<ITask> queuedList = this.getQueuedITasks();
    	 for (ITask task : queuedList){
    		 stagedList.add(this.updateTaskStatus(Status.STAGED, this.resolveVertexId(task)));
    	 }
    	 return stagedList;
    	 
     }
    /**************************************************************************
    * Gets a task by ID.
    * 
    * @param taskId
    *            The task ID.
    * @throws IllegalArgumentException
    *            Bad input.
    * @throws SecurityException
    *            The user doesn't own the task.
    * @throws NdexException
    *            Failed to query the database.
    **************************************************************************/
    
    public ITask getITask(final String taskId) throws IllegalArgumentException, SecurityException, NdexException
    {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId),"No task ID was specified.");
           
        try
        {
            final ORID taskRid = IdConverter.toRid(taskId);           
            if (!this.ndexService.isSetup()) {
				this.ndexService.setupDatabase();
			}
			final ITask t = (this.ndexService._orientDbGraph.getVertex(taskRid, ITask.class));
            if ( t == null )
            	throw new ObjectNotFoundException("Task id ", taskId + " not in orientdb");

            return t;
        }
        catch (Exception e)
        {
            logger.error("Failed to get task: " + taskId + ".", e);
            throw new NdexException("Failed to retrieve the task.");
        }
        finally
        {
        	this.ndexService.teardownDatabase();
        }
        
        
    }

    
    protected String resolveVertexId(VertexFrame vf)
    {
        if (null == vf)
            return null;

        return IdConverter.toJid((ORID)vf.asVertex().getId());
    }
    /* public method to update itask status and return updated itask
     * 
     */
	public ITask updateTaskStatus(Status status, String taskId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId), "A task id is required");
		Preconditions.checkArgument(null != status, "A status is required");
		try {
			if (!this.ndexService.isSetup()) {
				this.ndexService.setupDatabase();
			}
			 ORID taskRid =  IdConverter.toRid(taskId);
			 final ITask taskToUpdate = this.ndexService._orientDbGraph.getVertex(taskRid, ITask.class);
	            if (taskToUpdate == null){
	                throw new ObjectNotFoundException("Task", taskId);
	            }

	            taskToUpdate.setStatus(status);
	            this.ndexService._orientDbGraph.getBaseGraph().commit();
	            return taskToUpdate;
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		} finally {
			this.ndexService.teardownDatabase();
		}
		
	}
}
