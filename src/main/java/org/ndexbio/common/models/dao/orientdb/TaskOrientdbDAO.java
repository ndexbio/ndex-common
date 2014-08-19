/**
 * 
 */
package org.ndexbio.common.models.dao.orientdb;

import java.util.Arrays;
import java.util.Date;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.common.models.object.TaskType;
import org.ndexbio.model.object.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;

/**
 * @author fcriscuo
 *
 */
@Deprecated
public class TaskOrientdbDAO extends OrientdbDAO  {
	 private static final Logger _logger = LoggerFactory.getLogger(TaskOrientdbDAO.class);
	
	private TaskOrientdbDAO(){
		super();
	}
	
	static TaskOrientdbDAO createInstance() { return new TaskOrientdbDAO() ;}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.TaskDAO#createTask(org.ndexbio.common.models.object.Task)
	 */
	public Task createTask(Task newTask, String userId) throws IllegalArgumentException,
			NdexException {
		Preconditions.checkArgument(null != newTask, 
    			" A task object is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");
        
        String username = "unknown";

        try
        {
            
           
            setupDatabase();
     /*       final IUser taskOwner = this.findIuserById(userId);
            username = taskOwner.getUsername();
            final ITask task = _orientDbGraph.addVertex("class:task", ITask.class);
            task.setDescription(newTask.getDescription());
            task.setOwner(taskOwner);
         //   task.setPriority(newTask.getPriority());
            task.setProgress(newTask.getProgress());
            task.setResource(newTask.getResource());
//            task.setStatus(newTask.getStatus());
//            task.setStartTime(newTask.getCreatedDate());
 //           task.setType(newTask.getType());
 //           newTask.setId(IdConverter.toJid((ORID) task.asVertex().getId()));
            return newTask; */
            return null;
        }
        catch (Exception e)
        {
            _logger.error("Error creating task for: " + username + ".", e);
            throw new NdexException("Error creating a task.");
        }
        finally
        {
            teardownDatabase();
        }
	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.TaskDAO#deleteTask(java.lang.String)
	 */
	
	public void deleteTask(String taskId, String userId) throws IllegalArgumentException,
			ObjectNotFoundException, SecurityException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId), 
    			"A task id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");
		
        final ORID taskRid = new ORecordId(taskId);

        try
        {
        	
            setupDatabase();
     /*       String userName = this.findIuserById(userId).getUsername();
            final ITask taskToDelete = _orientDbGraph.getVertex(taskRid, ITask.class);
            if (taskToDelete == null)
                throw new ObjectNotFoundException("Task", taskId);
            else if (!taskToDelete.getOwner().getUsername().equals(userName))
                throw new SecurityException("You cannot delete a task you don't own.");
    
            _orientDbGraph.removeVertex(taskToDelete.asVertex());
         */  
        }
        catch (SecurityException | ObjectNotFoundException onfe)
        {
            throw onfe;
        }
        catch (Exception e)
        {
            if (e.getMessage().indexOf("cluster: null") > -1){
                throw new ObjectNotFoundException("Task", taskId);
            }
            
            _logger.error("Failed to delete task: " + taskId + ".", e);
        
            throw new NdexException("Failed to delete a task.");
        }
        finally
        {
            teardownDatabase();
        }

	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.TaskDAO#getTask(java.lang.String)
	 */
	
	public Task getTask(String taskId, String userId) throws IllegalArgumentException,
			SecurityException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId),"A task id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");
	        try
	        {
	            final ORID taskRid = new ORecordId(taskId);
	           
	            setupDatabase();
	     //       String userName = this.findIuserById(userId).getUsername();
	   /*         final ITask task = _orientDbGraph.getVertex(taskRid, ITask.class);
	            if (task != null)
	            {
	                if (!task.getOwner().getUsername().equals(userName))
	                    throw new SecurityException("Access denied.");
	                else
	                    return new Task();
	            } */
	        }
	        catch (SecurityException se)
	        {
	            throw se;
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
	        
	        return null;
	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.TaskDAO#updateTask(org.ndexbio.common.models.object.Task)
	 */
	
	public void updateTask(Task updatedTask, String userId) throws IllegalArgumentException,
			ObjectNotFoundException, SecurityException, NdexException {
		Preconditions.checkArgument(null != updatedTask, 
	    		   "A task is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");
	    	
	/*        ORID taskRid = IdConverter.toRid(updatedTask.getId());
	        String userName ="";
	        try
	        {
	        	
	            setupDatabase();
	            userName = this.findIuserById(userId).getUsername();
	            final ITask taskToUpdate = _orientDbGraph.getVertex(taskRid, ITask.class);
	            if (taskToUpdate == null)
	                throw new ObjectNotFoundException("Task", updatedTask.getId());
	            else if (!taskToUpdate.getOwner().getUsername().equals(userName))
	                throw new SecurityException("Access denied.");
	            taskToUpdate.setDescription(updatedTask.getDescription());
	   //         taskToUpdate.setPriority(updatedTask.getPriority());
	            taskToUpdate.setProgress(updatedTask.getProgress());
	            taskToUpdate.setStatus(updatedTask.getStatus());
	            taskToUpdate.setType(updatedTask.getType());

	        }
	        catch (SecurityException | ObjectNotFoundException onfe)
	        {
	            throw onfe;
	        }
	        catch (Exception e)
	        {
	            if (e.getMessage().indexOf("cluster: null") > -1){
	                throw new ObjectNotFoundException("Task", updatedTask.getId());
	            }
	            
	            _logger.error("Failed to update task: " + updatedTask.getId() + ".", e);
	           
	            throw new NdexException("Failed to update task: " + updatedTask.getId() + ".");
	        } */
//	        finally
	        {
	            teardownDatabase();
	        }

	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.TaskDAO#setTaskStatus(java.lang.String, java.lang.String)
	 */
	public Task setTaskStatus(String taskId, String status, String userId)
			throws NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId),
				"A task ID is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(status), "A status is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");

		
		 String userName = null;
		try {
			 
			setupDatabase();
	/*		userName = this.findIuserById(userId).getUsername();
			ORID taskRid = IdConverter.toRid(taskId);
			final ITask taskToUpdate = _orientDbGraph.getVertex(taskRid,
					ITask.class);
			if (taskToUpdate == null)
				throw new ObjectNotFoundException("Task", taskId);
			else if (!taskToUpdate.getOwner().getUsername()
					.equals(userName))
				throw new SecurityException("Access denied.");

			if (!isValidTaskStatus(status))
				throw new IllegalArgumentException(status
						+ " is not a known TaskStatus");

			Status s = Status.valueOf(status);

			taskToUpdate.setStatus(s);
			Task updatedTask = new Task(); //taskToUpdate);
			return updatedTask; */
			return null;
		} catch (Exception e) {
			_logger.error("Error changing task status for: "
					+ userName + ".", e);
			throw new NdexException("Error changing task status.");
		} finally {
			teardownDatabase();
		}
	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.TaskDAO#createXBELExportNetworkTask(java.lang.String)
	 */
	public Task createXBELExportNetworkTask(String networkId, String userId)
			throws IllegalArgumentException, SecurityException, NdexException {
		Preconditions
		.checkArgument(!Strings.isNullOrEmpty(networkId), "A network ID is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");

		
		
		String username = "unknown";

	try {
		
		setupDatabase();
/*		final IUser taskOwner = this.findIuserById(userId);
		username = taskOwner.getUsername();
		final INetwork network = _orientDbGraph.getVertex(
				IdConverter.toRid(networkId), INetwork.class);
		if (network == null)
			throw new ObjectNotFoundException("Network", networkId);
		
		
		ITask processNetworkTask = _orientDbGraph.addVertex(
				"class:task", ITask.class);
		processNetworkTask.setDescription(network.getName() + ".xbel");
		processNetworkTask.setType(TaskType.EXPORT_NETWORK_TO_FILE);
		processNetworkTask.setOwner(taskOwner);
	//	processNetworkTask.setPriority(Priority.LOW);
		processNetworkTask.setProgress(0);
		processNetworkTask.setResource(networkId);
		processNetworkTask.setStartTime(new Date());
		processNetworkTask.setStatus(Status.QUEUED);
		// retain commit statement for planned return to transaction-based operation
		_orientDbGraph.getBaseGraph().commit();
		Task newTask = new Task(); //processNetworkTask);
		return newTask;*/
		return null;
	} 
	catch (Exception e)
    {
        _logger.error("Error creating task for: " + username + ".", e);
        throw new NdexException("Error creating a task.");
    } 
	finally {
		teardownDatabase();
	}

	}
	
	private boolean isValidTaskStatus(String status) {
		for (Status value : Status.values()) {
			if (value.name().equals(status)) {
				return true;
			}
		}
		return false;
	}

}
