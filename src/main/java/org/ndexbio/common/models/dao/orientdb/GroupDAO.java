package org.ndexbio.common.models.dao.orientdb;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.common.models.dao.CommonDAOValues;
import org.ndexbio.model.object.SimpleUserQuery;
import org.ndexbio.common.util.Email;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.Group;
import org.ndexbio.model.object.User;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class GroupDAO {
	
	private ODatabaseDocumentTx db;
	private static final Logger logger = Logger.getLogger(GroupDAO.class.getName());

	/**************************************************************************
	    * GroupDAO
	    * 
	    * @param db
	    *            Database instance from the Connection pool, should be opened
	    **************************************************************************/
	public GroupDAO(ODatabaseDocumentTx db) {
		this.db = db;
	}
	
	/**************************************************************************
	    * Create a new group
	    * 
	    * @param newGroup
	    *            A Group object, from the NDEx Object Model
	    * @throws NdexException
	    *            Attempting to save an ODocument to the database
	    * @throws IllegalArgumentException
	    * 			 The newUser does not contain proper fields
	    * @throws DuplicateObjectException
	    * 			 The account name and/or email already exist
	    * @returns Group object, from the NDEx Object Model
	    **************************************************************************/
	public Group createNewGroup(Group newGroup)
			throws NdexException, IllegalArgumentException, DuplicateObjectException {

			Preconditions.checkArgument(null != newGroup, 
					"A group is required");
			Preconditions.checkArgument(!Strings.isNullOrEmpty( newGroup.getAccountName()),
					"A accountName is required" );
			
			_checkForExistingGroup(newGroup);
				
			try {
				Group result = new Group();
				    
				result.setExternalId(NdexUUIDFactory.INSTANCE.getNDExUUID());
				result.setAccountName(newGroup.getAccountName());
				result.setOrganizationName(newGroup.getOrganizationName());
				result.setWebsite(newGroup.getWebsite());
				result.setDescription(newGroup.getDescription());
				result.setImage(newGroup.getImage());
				
				ODocument group = new ODocument(NdexClasses.Group);
				group.field("description", newGroup.getDescription());
				group.field("websiteURL", newGroup.getWebsite());
				group.field("imageURL", newGroup.getImage());
				group.field("organizationName", newGroup.getOrganizationName());
			    group.field("accountName", newGroup.getAccountName());
			    group.field("UUID", result.getExternalId());
			    group.field("creationDate", result.getCreationDate());
			    group.field("modificationDate", result.getModificationDate());
			   
				group.save();
				
				logger.info("A new group with accountName " + newGroup.getAccountName() + " has been created");
				
				return result;
			} 
			catch(Exception e) {
				logger.severe("Could not save new group to the database");
				throw new NdexException(e.getMessage());
			}
		}
	
	/**************************************************************************
	    * Get a Group
	    * 
	    * @param id
	    *            UUID for Group
	    * @throws NdexException
	    *            Attempting to access database
	    * @throws IllegalArgumentexception
	    * 			The id is invalid
	    * @throws ObjectNotFoundException
	    * 			The group specified by id does not exist
	    **************************************************************************/
	public Group getGroupById(UUID id)
			throws IllegalArgumentException, ObjectNotFoundException, NdexException{
		Preconditions.checkArgument(null != id, 
				"UUID required");
		
		final ODocument group = _getGroupById(id);
	    return _getGroupFromDocument(group);
	}
	
	/**************************************************************************
	    * Delete a group
	    * 
	    * @param id
	    *            UUID for Group
	    * @throws NdexException
	    *            Attempting to access and delete an ODocument from the database
	    * @throws ObjectNotFoundException
	    * 			Specified group does not exist
	    **************************************************************************/
	public void deleteGroupById(UUID id) 
		throws NdexException, ObjectNotFoundException{
			
			//TODO cannot orphan networks
		
		Preconditions.checkArgument(null != id, 
				"UUID required");
		
			ODocument group = _getGroupById(id);
			try {
				group.delete();
			}
			catch (Exception e) {
				logger.severe("Could not delete group from the database");
				throw new NdexException(e.getMessage());
			}
		
	}
	
	/**************************************************************************
	    * Update a group
	    * 
	    * @param updatedGroup
	    * 			group object with update fields
	    * @param id
	    *            UUID for Group
	    * @throws NdexException
	    *            Attempting to access and delete an ODocument from the database
	    * @throws ObjectNotFoundException
	    * 			Specified group does not exist
	    * @throws IllegalArgumentException
	    * 			Group object cannot be null
	    **************************************************************************/
	public Group updateGroup(Group updatedGroup, UUID id) 
		throws IllegalArgumentException, NdexException, ObjectNotFoundException {
			
			Preconditions.checkArgument(id != null, 
					"A user id is required");
			Preconditions.checkArgument(updatedGroup != null, 
					"An updated user is required");
		
		ODocument group =  _getGroupById(id);
		
		try {
			//updatedGroup.getDescription().isEmpty();
			if(!Strings.isNullOrEmpty(updatedGroup.getDescription())) group.field("description", updatedGroup.getDescription());
			if(!Strings.isNullOrEmpty(updatedGroup.getWebsite())) group.field("websiteURL", updatedGroup.getWebsite());
			if(!Strings.isNullOrEmpty(updatedGroup.getImage())) group.field("imageURL", updatedGroup.getImage());
			if(!Strings.isNullOrEmpty(updatedGroup.getOrganizationName())) group.field("organizationName", updatedGroup.getOrganizationName()); 
			group.field("modificationDate", updatedGroup.getModificationDate());

			group.save();
			logger.info("Updated group profile with UUID " + id);
			
			return _getGroupFromDocument(group);
			
		} catch (Exception e) {
			
			logger.severe("An error occured while updating group profile with UUID " + id);
			throw new NdexException(e.getMessage());
			
		} 
	}
	
	/**************************************************************************
	    * Find groups
	    * 
	    * @param query
	    * 			group object with update fields
	    * @param skip
	    *            amount of blocks to skip
	    * @param top
	    * 			the size of a block
	    * @throws NdexException
	    *            Attempting to access and delete an ODocument from the database
	    * @throws IllegalArgumentException
	    * 			Group object cannot be null
	    **************************************************************************/
	public List<Group> findGroups(SimpleUserQuery simpleQuery, int skip, int top) 
			throws NdexException, IllegalArgumentException {
		
		Preconditions.checkArgument(null != simpleQuery, "Search parameters are required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(simpleQuery.getSearchString()), 
				"A search string is required");
		
		simpleQuery.setSearchString(simpleQuery.getSearchString()
					.toLowerCase().trim());

		final List<Group> foundgroups = new ArrayList<Group>();

		final int startIndex = skip
				* top;

		String query = "SELECT FROM " + NdexClasses.Group + " "
					+ "WHERE accountName.toLowerCase() LIKE '%"
					+ simpleQuery.getSearchString() + "%'"
					+ "  OR organizationName.toLowerCase() LIKE '%"
					+ simpleQuery.getSearchString() + "%'"
					+ "  ORDER BY creation_date DESC " + " SKIP " + startIndex
					+ " LIMIT " + top;
		
		try {
			
			final List<ODocument> groups = this.db.query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument group : groups) {
				foundgroups.add(_getGroupFromDocument(group));
				
			}
			return foundgroups;
			
		} catch (Exception e) {
			logger.severe("Unable to query the database");
			throw new NdexException("Failed to search for groups.\n" + e.getMessage());
			
		} 
	}
	/*
	 * Convert the database results into our object model
	 * TODO should this be moved to util? being used by other classes, not really a data object but a helper class
	 */
	public static Group _getGroupFromDocument(ODocument n) {
		
		Group result = new Group();
		
		result.setExternalId(UUID.fromString((String)n.field("UUID")));
		result.setAccountName((String)n.field("accountName"));
		result.setOrganizationName((String)n.field("organizationName"));
		result.setWebsite((String)n.field("websiteURL"));
		result.setDescription((String)n.field("description"));
		result.setImage((String)n.field("imageURL"));
		result.setCreationDate((Date)n.field("creationDate"));
		result.setModificationDate((Date)n.field("modificationDate"));
		
		return result;
	}
	
	private void _checkForExistingGroup(final Group group) 
			throws DuplicateObjectException, IllegalArgumentException {
		
		Preconditions.checkArgument(null != group, 
				"UUID required");
		
		List<ODocument> existingGroups = db.query(
				new OSQLSynchQuery<Object>(
						"SELECT FROM " + NdexClasses.Group
						+ " WHERE accountName = '" + group.getAccountName() + "'"));
		
		if (!existingGroups.isEmpty()) {
			logger.info("Group with accountName " + group.getAccountName() + " already exists");
			throw new DuplicateObjectException(
					CommonDAOValues.DUPLICATED_ACCOUNT_FLAG);
		}
	}
	
	private ODocument _getGroupById(UUID id) 
			throws NdexException, ObjectNotFoundException {
		
		final List<ODocument> groups;
		
		String query = "select from " + NdexClasses.Group 
		 		+ " where UUID = ?";
 
		try {
		     groups = db.command( new OCommandSQL(query))
					   .execute(id.toString()); 
		} 
		catch (Exception e) {
			logger.severe("An error occured while attempting to query the database");
			throw new NdexException(e.getMessage());
		}
		
		if (groups.isEmpty()) {
			logger.info("Group with UUID " + id + " does not exist");
			throw new ObjectNotFoundException("Group", id.toString());
		}
		
		return groups.get(0);
	}
	
	private ODocument _getGroupByAccountName(String accountName) 
			throws NdexException, ObjectNotFoundException {

		final List<ODocument> groups;

		String query = "select from " + NdexClasses.Group 
		 		+ " where accountName = ?";
 
		try {
		     groups = db.command( new OCommandSQL(query))
					   .execute(accountName);
		} 
		catch (Exception e) {
			logger.severe("An error occured while attempting to query the database");
			throw new NdexException(e.getMessage());
		}

		if (groups.isEmpty()) {
			logger.info("User with accountName " + accountName + " does not exist");
			throw new ObjectNotFoundException("Group", accountName);
		}

		return groups.get(0);
	}
	
	
}
