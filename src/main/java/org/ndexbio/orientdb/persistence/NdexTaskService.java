package org.ndexbio.orientdb.persistence;

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


public class NdexTaskService extends OrientDBConnectionService
{
    private static final Logger logger = LoggerFactory.getLogger(NdexTaskService.class);
    
    public NdexTaskService()
    {
        super();
    }
    
    /*
     * get a Collection of Tasks based on Status value
     */
    private List<Task> getTasksByStatus(Status aStatus) throws NdexException {
    	 String query = "select from task "
    	            + " where status = '" +aStatus +"'";
    	 final List<Task> foundTasks = Lists.newArrayList();
    	 try {
			setupDatabase();

			 final List<ODocument> taskDocumentList = _orientDbGraph.getBaseGraph().
					 getRawGraph().query(new OSQLSynchQuery<ODocument>(query));
			 for (final ODocument document : taskDocumentList)
	                foundTasks.add(new Task(_orientDbGraph.getVertex(document, ITask.class)));

	            return foundTasks;
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
	            throw new NdexException("Failed to search tasks.");
		}finally {
			teardownDatabase();
		}

    }
    // only specific status states are exposed 
     public List<Task> getQueuedTasks() throws NdexException {
    	 return getTasksByStatus(Status.QUEUED);
     }
     
     public List<Task> getInProgressTasks() throws NdexException{
    	 return getTasksByStatus(Status.PROCESSING);
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
    
    public Task getITask(final String taskId) throws IllegalArgumentException, SecurityException, NdexException
    {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId),"No task ID was specified.");
           
        try
        {
            final ORID taskRid = IdConverter.toRid(taskId);           
            setupDatabase();          
            final Task t = new Task(_orientDbGraph.getVertex(taskRid, ITask.class));
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
            teardownDatabase();
        }
        
        
    }

    /**************************************************************************
    * Updates a task.
    * 
    * @param updatedTask
    *            The updated request.
    * @throws IllegalArgumentException
    *            Bad input.
    * @throws ObjectNotFoundException
    *            The task doesn't exist.
    * @throws SecurityException
    *            The user doesn't own the task.
    * @throws NdexException
    *            Failed to update the task in the database.
    **************************************************************************/
  
    public void updateTask(final Task updatedTask) throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
    	Preconditions.checkNotNull(updatedTask,"The task to update is empty.");
        
        ORID taskRid = IdConverter.toRid(updatedTask.getId());

        try
        {
        	setupDatabase();
            final ITask taskToUpdate = _orientDbGraph.getVertex(taskRid, ITask.class);
            if (taskToUpdate == null){
                throw new ObjectNotFoundException("Task", updatedTask.getId());
            }

            taskToUpdate.setStartTime(updatedTask.getCreatedDate());
            taskToUpdate.setStatus(updatedTask.getStatus());

            _orientDbGraph.getBaseGraph().commit();
        }
        catch (Exception e)
        {
            logger.error("Failed to update task: " + updatedTask.getId() + ".", e);
            _orientDbGraph.getBaseGraph().rollback(); 
            throw new NdexException("Failed to update task: " + updatedTask.getId() + ".");
        }
        finally
        {
            teardownDatabase();
        }
    }
}
