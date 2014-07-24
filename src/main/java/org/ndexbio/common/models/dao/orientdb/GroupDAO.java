package org.ndexbio.common.models.dao.orientdb;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
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
//import org.ndexbio.model.object.SimpleUserQuery;
import org.ndexbio.common.util.Email;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.common.util.Security;
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
	public Group createNeGroup(Group newGroup)
			throws NdexException, IllegalArgumentException, DuplicateObjectException {

			Preconditions.checkArgument(null != newGroup, 
					"A group is required");
			Preconditions.checkArgument(!Strings.isNullOrEmpty( newGroup.getAccountName()),
					"A accountName is required" );
			Preconditions.checkArgument(!Strings.isNullOrEmpty( newGroup.getPassword()),
					"A group password is required" );
			
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
			    group.field("password", Security.hashText(newGroup.getPassword()));
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
	    * Delete a group
	    * 
	    * @param id
	    *            UUID for Group
	    * @throws NdexException
	    *            Attempting to access and delete an ODocument from the database
	    **************************************************************************/
	public void deleteGroupById(UUID id) 
		throws NdexException, ObjectNotFoundException{
		
			ODocument group = _getGroupById(id);
			try {
				group.delete();
			}
			catch (Exception e) {
				logger.severe("Could not delete user from the database");
				throw new NdexException(e.getMessage());
			}
		
	}
	
	
	
	
	private void _checkForExistingGroup(final Group group) 
			throws DuplicateObjectException {
		
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
			logger.info("User with UUID " + id + " does not exist");
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
			throw new ObjectNotFoundException("User", accountName);
		}

		return groups.get(0);
	}
	
	
}
