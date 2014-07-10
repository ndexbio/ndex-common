package org.ndexbio.common.models.dao;

import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.object.Request;

@Deprecated
public interface RequestDAO {

	/**************************************************************************
	 * Creates a request. 
	 * 
	 * @param newRequest
	 *            The request to create.
	 * @throws IllegalArgumentException
	 *            Bad input.
	 * @throws DuplicateObjectException
	 *            The request is a duplicate.
	 * @throws NdexException
	 *            Duplicate requests or failed to create the request in the
	 *            database.
	 * @return The newly created request.
	 **************************************************************************/
	public abstract Request createRequest(Request newRequest)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException;

	/**************************************************************************
	 * Deletes a request.
	 * 
	 * @param requestId
	 *            The request ID.
	 * @throws IllegalArgumentException
	 *            Bad input.
	 * @throws ObjectNotFoundException
	 *            The request doesn't exist.
	 * @throws NdexException
	 *            Failed to delete the request from the database.
	 **************************************************************************/
	public abstract void deleteRequest(String requestId)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException;

	/**************************************************************************
	 * Gets a request by ID.
	 * 
	 * @param requestId
	 *           The request ID.
	 * @throws IllegalArgumentException
	 *           Bad input.
	 * @throws NdexException
	 *           Failed to query the database.
	 * @return The request.
	 **************************************************************************/
	public abstract Request getRequest(String requestId)
			throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Updates a request.
	 * 
	 * @param updatedRequest
	 *            The updated request information.
	 * @throws IllegalArgumentException
	 *            Bad input.
	 * @throws NdexException
	 *            Failed to update the request in the database.
	 **************************************************************************/
	public abstract void updateRequest(Request updatedRequest)
			throws IllegalArgumentException, NdexException;

}