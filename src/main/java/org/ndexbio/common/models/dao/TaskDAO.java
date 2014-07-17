package org.ndexbio.common.models.dao;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.Task;

@Deprecated
public interface TaskDAO {

	/**************************************************************************
	 * Creates a task. 
	 * 
	 * @param newTask
	 *            The task to create.
	 * @param userId
	 *            The id of the user creating the Task
	 * @throws IllegalArgumentException
	 *            Bad input.
	 * @throws NdexException
	 *            Failed to create the task in the database.
	 * @return The newly created task.
	 **************************************************************************/
	public abstract Task createTask(Task newTask, String userId)
			throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Deletes a task. 
	 * 
	 * @param taskId
	 *            The task ID.
	 * @param userId
	 *            The user id of the requester.
	 * @throws IllegalArgumentException
	 *            Bad input.
	 * @throws ObjectNotFoundException
	 *            The task doesn't exist.
	 * @throws SecurityException
	 *            The user doesn't own the task.
	 * @throws NdexException
	 *            Failed to delete the task from the database.
	 **************************************************************************/
	public abstract void deleteTask(String taskId, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException;

	/**************************************************************************
	 * Gets a task by ID.
	 * 
	 * @param taskId
	 *            The task ID.
	 * @param userId
	 *            The user ID of the requester.
	 * @throws IllegalArgumentException
	 *            Bad input.
	 * @throws SecurityException
	 *            The user doesn't own the task.
	 * @throws NdexException
	 *            Failed to query the database.
	 **************************************************************************/
	public abstract Task getTask(String taskId, String userId)
			throws IllegalArgumentException, SecurityException, NdexException;

	/**************************************************************************
	 * Updates a task.
	 * 
	 * @param updatedTask
	 *            The updated request.
	 * @param userId
	 *            The user id of the requester.
	 * @throws IllegalArgumentException
	 *            Bad input.
	 * @throws ObjectNotFoundException
	 *            The task doesn't exist.
	 * @throws SecurityException
	 *            The user doesn't own the task.
	 * @throws NdexException
	 *            Failed to update the task in the database.
	 **************************************************************************/
	public abstract void updateTask(Task updatedTask, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException;

	public abstract Task setTaskStatus(String taskId, String status, String userId)
			throws NdexException;

	/**************************************************************************
	 * Exports a network to an xbel-formatted file. Creates a network upload task
	 * 
	 * @param networkId
	 *            The id of the network to export
	  * @param userId
	 *            The id of the user creating the task
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to create a network export task
	 **************************************************************************/
	public abstract Task createXBELExportNetworkTask(String networkId, String userId)
			throws IllegalArgumentException, SecurityException, NdexException;

}