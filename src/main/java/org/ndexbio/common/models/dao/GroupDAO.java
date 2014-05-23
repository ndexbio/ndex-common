package org.ndexbio.common.models.dao;

import java.util.List;

import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.object.Group;
import org.ndexbio.common.models.object.Membership;
import org.ndexbio.common.models.object.SearchParameters;


public interface GroupDAO {

	/**************************************************************************
	 * Creates a group.
	 * 
	 * @param newGroup
	 *            The group to create.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws DuplicateObjectException
	 *             A group with that name already exists.
	 * @throws NdexException
	 *             Failed to create the user in the database.
	 * @return The newly created group.
	 **************************************************************************/
	public abstract Group createGroup(Group newGroup, String userId)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException;

	/**************************************************************************
	 * Deletes a group.
	 * 
	 * @param groupId
	 *            The ID of the group to delete.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws ObjectNotFoundException
	 *             The group doesn't exist.
	 * @throws SecurityException
	 *             The user doesn't have permissions to delete the group.
	 * @throws NdexException
	 *             Failed to delete the user from the database.
	 **************************************************************************/

	public abstract void deleteGroup(String groupId, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException;

	/**************************************************************************
	 * Find Groups based on search parameters - string matching for now
	 * 
	 * @params searchParameters The search parameters.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return Groups that match the search criteria.
	 **************************************************************************/
	public abstract List<Group> findGroups(SearchParameters searchParameters,
			String searchOperator) throws IllegalArgumentException,
			NdexException;

	/**************************************************************************
	 * Gets a group by ID or name.
	 * 
	 * @param groupId
	 *            The ID or name of the group.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return The group.
	 **************************************************************************/
	public abstract Group getGroup(String groupId)
			throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Removes a member from a group.
	 * 
	 * @param groupId
	 *            The group ID.
	 * @param userId
	 *            The ID of the member to remove.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws ObjectNotFoundException
	 *             The network or member doesn't exist.
	 * @throws SecurityException
	 *             The user doesn't have access to change members.
	 * @throws NdexException
	 *             Failed to query the database.
	 **************************************************************************/
	public abstract void removeMember(String groupId, String memberId, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException;

	/**************************************************************************
	 * Updates a group.
	 * 
	 * @param updatedGroup
	 *            The updated group information.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws ObjectNotFoundException
	 *             The group doesn't exist.
	 * @throws SecurityException
	 *             The user doesn't have access to update the group.
	 * @throws NdexException
	 *             Failed to update the user in the database.
	 **************************************************************************/
	public abstract void updateGroup(Group updatedGroup, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException;

	/**************************************************************************
	 * Changes a member's permissions to a group.
	 * 
	 * @param groupId
	 *            The group ID.
	 * @param groupMember
	 *            The member being updated.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws ObjectNotFoundException
	 *             The network or member doesn't exist.
	 * @throws SecurityException
	 *             The user doesn't have access to change members.
	 * @throws NdexException
	 *             Failed to query the database.
	 **************************************************************************/
	public abstract void updateMember(String groupId, Membership groupMember, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException;

}