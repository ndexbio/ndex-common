package org.ndexbio.orientdb.persistence;

import java.util.List;

import org.ndexbio.orientdb.domain.*;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.service.helpers.RidConverter;
import org.ndexbio.rest.models.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.id.ORID;


public class NdexTaskService extends OrientDBConnectionService
{
    private static final Logger _logger = LoggerFactory.getLogger(NdexTaskService.class);
    
    
    
    public NdexTaskService()
    {
        super();
    }
    
    /*
     * get a Collection of Tasks that are pending file uploads
     */
    
     public List<Task> getPendingFileUploads() {
    	 return null;
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
            final ORID taskRid = RidConverter.convertToRid(taskId);           
            setupDatabase();          
            final Task t = new Task(_orientDbGraph.getVertex(taskRid, ITask.class));
            if ( t == null ){
            	throw new ObjectNotFoundException("Task id ", taskId +" not in orientdb");
            }
            return t;
        }
        catch (Exception e)
        {
            _logger.error("Failed to get task: " + taskId + ".", e);
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
        
        ORID taskRid = RidConverter.convertToRid(updatedTask.getId());

        try
        {
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
            _logger.error("Failed to update task: " + updatedTask.getId() + ".", e);
            _orientDbGraph.getBaseGraph().rollback(); 
            throw new NdexException("Failed to update task: " + updatedTask.getId() + ".");
        }
        finally
        {
            teardownDatabase();
        }
    }
}
