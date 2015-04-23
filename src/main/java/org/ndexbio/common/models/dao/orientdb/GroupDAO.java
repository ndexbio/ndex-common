package org.ndexbio.common.models.dao.orientdb;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.exceptions.*;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.Group;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.MembershipType;
import org.ndexbio.model.object.Permissions;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class GroupDAO extends GroupDocDAO {
	
	private OrientGraph graph;
	private static final Logger logger = Logger.getLogger(GroupDAO.class.getName());

	/**************************************************************************
	    * GroupDAO
	    * 
	    * @param db
	    *            Database instance from the Connection pool, should be opened
	    * @param graph
	    * 			OrientBaseGraph layer on top of db instance. 
	    **************************************************************************/
	
	public GroupDAO(ODatabaseDocumentTx dbConnection) {
		super(dbConnection);
		this.graph = new OrientGraph(dbConnection, false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);

	}
	
	/**************************************************************************
	    * Create a new group
	    * 
	    * @param newGroup
	    *            A Group object, from the NDEx Object Model
	    * @param adminId
	    * 			UUID for logged in user
	    * @throws NdexException
	    *            Attempting to save an ODocument to the database
	    * @throws IllegalArgumentException
	    * 			 The newUser does not contain proper fields
	    * @throws DuplicateObjectException
	    * 			 The account name and/or email already exist
	    * @returns Group object, from the NDEx Object Model
	    **************************************************************************/
	public Group createNewGroup(Group newGroup, UUID adminId)
			throws NdexException, IllegalArgumentException, DuplicateObjectException {

			Preconditions.checkArgument(null != newGroup, 
					"A group is required");
			Preconditions.checkArgument(!Strings.isNullOrEmpty(newGroup.getOrganizationName()),
					"An organizationName is required");
			Preconditions.checkArgument(!Strings.isNullOrEmpty( newGroup.getAccountName()),
					"An accountName is required" );
			Preconditions.checkArgument(!Strings.isNullOrEmpty(adminId.toString()),
					"An admin id is required" );
			
			this.checkForExistingGroup(newGroup);
			final ODocument admin = this.getRecordByUUID(adminId, NdexClasses.User);
				
			try {
				Group result = new Group();
				    
				result.setExternalId(NdexUUIDFactory.INSTANCE.getNDExUUID());
				result.setAccountName(newGroup.getAccountName());
				result.setOrganizationName(newGroup.getOrganizationName());
				result.setWebsite(newGroup.getWebsite());
				result.setDescription(newGroup.getDescription());
				result.setImage(newGroup.getImage());
				
				ODocument group = new ODocument(NdexClasses.Group).
						fields("description", newGroup.getDescription(),
							"websiteURL", newGroup.getWebsite(),
							"imageURL", newGroup.getImage(),
							"organizationName", newGroup.getOrganizationName(),
			    			NdexClasses.account_P_accountName, newGroup.getAccountName(),
			    			NdexClasses.ExternalObj_ID, result.getExternalId(),
			    			NdexClasses.ExternalObj_cTime, result.getCreationTime(),
			    			NdexClasses.ExternalObj_mTime, result.getModificationTime(),
			    			NdexClasses.ExternalObj_isDeleted, false);
			
				group = group.save();
				
				OrientVertex vGroup = graph.getVertex(group);

				OrientVertex vAdmin = graph.getVertex(admin);
		   		for	(int retry = 0;	retry <	maxRetries;	++retry)	{
		   			try	{
						graph.addEdge(null, vAdmin, vGroup, Permissions.GROUPADMIN.toString().toLowerCase());
		  				break;
		   			} catch(ONeedRetryException	e)	{
		   				logger.warning("Retry creating task add edge.");
		   				vAdmin.reload();
		   			}
		   		}

		   		logger.info("A new group with accountName "
							+ newGroup.getAccountName() 
							+" and owner "+ (String)admin.field(NdexClasses.account_P_accountName) 
							+" has been created");
				
				return result;
			} 
			catch(Exception e) {
				logger.severe("Could not save new group to the database:" + e.getMessage());
				e.printStackTrace();
				throw new NdexException("Unable to create new group with accountName " + newGroup.getAccountName());
			}
		}
	
	
	/**************************************************************************
	    * Delete a group
	    * 
	    * @param groupId
	    *            UUID for Group
	    * @param adminId
	    * 			UUID for admin of group
	    * @throws NdexException
	    *            Attempting to access and delete an ODocument from the database
	    * @throws ObjectNotFoundException
	    * 			Specified group does not exist
	    **************************************************************************/
	public void deleteGroupById(UUID groupId, UUID adminId) 
		throws NdexException, ObjectNotFoundException{
			
			//TODO cannot orphan networks, has not been tested
		
		Preconditions.checkArgument(null != groupId, 
				"group UUID required");
		Preconditions.checkArgument(null != adminId, 
				"admin UUID required");
		
		// get records and validate admin permissions
		final ODocument group = this.getRecordByUUID(groupId, NdexClasses.Group);
		final ODocument admin = this.getRecordByUUID(adminId, NdexClasses.User);	
		if (!isGroupAdmin(group, admin) )
			throw new NdexException ("User " + adminId + " doesn't have permission to delete group "
		            + groupId);
		
		try {
			OrientVertex vGroup = graph.getVertex(group);
			boolean safe = true;
			
			for(Edge e : vGroup.getEdges( Direction.OUT, Permissions.ADMIN.toString().toLowerCase() ) ) {
				// get the resource the group edge points to
				OrientVertex vResource = (OrientVertex) e.getVertex(Direction.IN);
				
				//not a network, continue 
				if(!vResource.getRecord().getSchemaClass().getName().equals( NdexClasses.Network ) || 
					(vResource.getRecord().field(NdexClasses.ExternalObj_isDeleted) != null &&
					 ((Boolean)vResource.getRecord().field(NdexClasses.ExternalObj_isDeleted)).booleanValue()) )
					continue;
				// is a network, unsafe to delete if only admin
				safe = false;	
				
				//iterate across all edges of type ADMIN, to find groups or users
				for(Edge ee : vResource.getEdges( Direction.IN, Permissions.ADMIN.toString().toLowerCase() ) ) {
					
					if( !( (OrientVertex) ee.getVertex(Direction.OUT) ).equals(vGroup) ) {
						safe = true;
						break;
					}
				}
					
			}

			if(!safe)
				throw new NdexException("Cannot orphan networks");
			String acctName = group.field(NdexClasses.account_P_accountName);

			//TODO: check if there are pending requests.
			
				
            			
	   		for	(int retry = 0;	retry <	maxRetries;	++retry)	{
	   			try	{
	   				group.fields(NdexClasses.ExternalObj_isDeleted, true,
	   						NdexClasses.ExternalObj_mTime, new Date(),
	   						NdexClasses.account_P_accountName, null,
	   						NdexClasses.account_P_oldAcctName, acctName).save();
	  				break;
	   			} catch(ONeedRetryException	e)	{
	   				logger.warning("Retry update " + e.getMessage());
	   				group.reload();
	   			}
	   		}
			
		}
		catch (NdexException e) {
			logger.severe("Could not delete group from the database " + e.getMessage());
			throw e;
		}
		catch (Exception e) {
			logger.severe("Could not delete group from the database " + e.getMessage());
			throw new NdexException("Unable to delete group");
		}
		
	}
	
	
	/**************************************************************************
	    * updateMember
	    * 
	    * @param membership
	    * 			Membership object, should specify memberUUID, membershipType and permissions
	    * @param groupId
	    *            UUID for associated group
	    * @param adminId
	    * 			UUID for valid admin of group
	    * @throws NdexException
	    *            Invalid parameters or an error occurred while accessing the database
	    * @throws ObjectNotFoundException
	    * 			Invalid groupId, memberId, or adminId
	    **************************************************************************/
	public void updateMember(Membership membership, UUID groupId, UUID adminId)
			throws ObjectNotFoundException, NdexException {
		Preconditions.checkArgument((membership.getMembershipType() != null)  
				&& membership.getMembershipType().equals( MembershipType.GROUP ),
				"Incorrect membership type");
		Preconditions.checkArgument(membership.getMemberUUID() != null,
				"member UUID required");
		Preconditions.checkArgument(groupId != null,
				"group UUID required");
		Preconditions.checkArgument(adminId != null,
				"admin UUID required");
		Preconditions.checkArgument( (membership.getPermissions() != null) 
				&& (membership.getPermissions().equals( Permissions.GROUPADMIN) 
				|| membership.getPermissions().equals( Permissions.MEMBER) ) ,
				"Valid permissions required");
		
		final ODocument group = this.getRecordByUUID(groupId, NdexClasses.Group);
		final ODocument admin = this.getRecordByUUID(adminId, NdexClasses.User);
		final ODocument member = this.getRecordByUUID(membership.getMemberUUID(), NdexClasses.User);
		
		try {
			OrientVertex vGroup = graph.getVertex(group);
			OrientVertex vAdmin = graph.getVertex(admin);
			
			boolean isAdmin = false;
			boolean isOnlyAdmin = true;
			
			for(Edge e : vGroup.getEdges(Direction.IN, Permissions.GROUPADMIN.toString().toLowerCase())) {
				if( ( (OrientVertex) e.getVertex(Direction.OUT) ).getIdentity().equals( vAdmin.getIdentity() ) ) 
					isAdmin = true;
				else 
					isOnlyAdmin = false;
			}
			
			OrientVertex vMember = graph.getVertex(member);
			if(isOnlyAdmin && ( vAdmin.getIdentity().equals( vMember.getIdentity() ) ) ) {
				logger.severe("Action will orphan group");
				throw new NdexException("Cannot orphan group to have no admin");
			}
			
			if(isAdmin) {
				OrientEdge edge = null; 
				for(Edge e : vGroup.getEdges(Direction.IN)) {
					if( ( (OrientVertex) e.getVertex(Direction.OUT) ).getIdentity().equals( vMember.getIdentity() ) ) { 
						edge = (OrientEdge)e;
						break;
					}
				}

				if ( edge != null) {
			   		for	(int retry = 0;	retry <	maxRetries;	++retry)	{
			   			try	{
							graph.removeEdge(edge);
			  				break;
			   			} catch(ONeedRetryException	e)	{
			   				edge.reload();
			   			}
			   		}
				}
				
		   		for	(int retry = 0;	retry <	maxRetries;	++retry)	{
		   			try	{
						graph.addEdge(null, vMember, vGroup, membership.getPermissions().toString().toLowerCase());
		  				break;
		   			} catch(ONeedRetryException	e)	{
		   				logger.warning("Retry update " + e.getMessage());
						vGroup.getRecord().reload();
						vMember.getRecord().reload();
		   			}
		   		}

				logger.info("Added membership edge between group "
				+ (String) group.field("accountName")
				 + " and member " 
				+ (String) group.field("accountName"));
				
			} else {
				logger.severe("Invalid admin for group");
				throw new NdexException("Specified user is not an admin for the group");
			}
			
		} catch (Exception e) {
			logger.severe("Unable to update membership permissions for "
					+ "group with UUID "+ groupId
					+ " and admin with UUID " + adminId
					+ " and member with UUID " + membership.getMemberUUID());
			logger.severe(e.getMessage());
			throw new NdexException("Unable to update privileges for user");
		}
		
	}
	
	/**************************************************************************
	    * removeMember
	    * 
	    * @param memberId
	    * 			UUID for valid member
	    * @param groupId
	    *            UUID for associated group
	    * @param adminId
	    * 			UUID for valid admin of group or member to be removed, e.g. logged in user
	    * @throws NdexException
	    *            Invalid parameters or an error occurred while accessing the database
	    * @throws ObjectNotFoundException
	    * 			Invalid groupId, memberId, or adminId
	    * @throws IllegalArgumentException
	    * 			UUID identifiers required
	    **************************************************************************/
	
	public void removeMember(UUID memberId, UUID groupId, UUID adminId) 
			throws ObjectNotFoundException, IllegalArgumentException, NdexException {
		Preconditions.checkArgument( memberId != null ,
				"member UUID required");
		Preconditions.checkArgument( groupId != null ,
				"group UUID required");
		Preconditions.checkArgument(adminId !=null ,
				"admin UUID required");
		
		
		final ODocument group = this.getRecordByUUID(groupId, NdexClasses.Group);
		final ODocument admin = this.getRecordByUUID(adminId, NdexClasses.User);
		final ODocument member = this.getRecordByUUID(memberId, NdexClasses.User);
		
		try {
			OrientVertex vGroup = graph.getVertex(group);
			OrientVertex vAdmin = graph.getVertex(admin);
			OrientVertex vMember = graph.getVertex(member);
			
			boolean isAdmin = false;
			boolean isOnlyAdmin = true;
			for(Edge e : vGroup.getEdges(Direction.BOTH, Permissions.GROUPADMIN.toString().toLowerCase())) {
				if( ( (OrientVertex) e.getVertex(Direction.OUT) ).getIdentity().equals( vAdmin.getIdentity() ) ) 
					isAdmin = true;
				else 
					isOnlyAdmin = false;
			}
			
			if(isOnlyAdmin && ( vAdmin.getIdentity().equals( vMember.getIdentity() ) ) && isAdmin ) {
				throw new NdexException("Cannot orphan group to have no admin");
			}
			
			if( isAdmin || ( vAdmin.getIdentity().equals( vMember.getIdentity() ) ) ) {
				for(Edge e : vGroup.getEdges(Direction.BOTH)) {
					if( ( (OrientVertex) e.getVertex(Direction.OUT) ).getIdentity().equals( vMember.getIdentity() ) ) 
						graph.removeEdge(e);
				}
				logger.info("removed member if it was a member");
				
			} else {
				logger.severe("Invalid admin for group");
				throw new NdexException("Specified user is not an admin for the group");
			}
			
		} catch (NdexException e) {
			logger.severe(e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.severe("Unable to remove member for "
					+ "group with UUID "+ groupId
					+ " and admin with UUID " + adminId
					+ " and member with UUID " + memberId);
			logger.severe(e.getMessage());
			throw new NdexException("Cannot remove member");
		}
	}
	
	

	@Override
	public void commit() {
		this.graph.commit();
	}
	
	@Override
	public void close() {
		this.graph.shutdown();
	}
	
	
	private void checkForExistingGroup(final Group group) 
			throws IllegalArgumentException, NdexException {
		
		Preconditions.checkArgument(null != group, 
				"UUID required");

		try {
			getRecordByAccountName(group.getAccountName(), null);
			String msg = "Group with name " + group.getAccountName() + " already exists.";
			logger.info(msg);
			throw new DuplicateObjectException(msg);
		} catch ( ObjectNotFoundException e) {
			// when account doesn't exists return as normal.
		}
		
	}
	
	/**************************************************************************
	    * Update a group
	    * 
	    * @param updatedGroup
	    * 			group object with update fields
	    * @param groupId
	    *            UUID for Group
	    * @param memberId
	    * 			UUID for valid member to edit group
	    * @throws NdexException
	    *            Attempting to access and delete an ODocument from the database
	    * @throws ObjectNotFoundException
	    * 			Specified group does not exist
	    * @throws IllegalArgumentException
	    * 			Group object cannot be null
	    **************************************************************************/
	public Group updateGroup(Group updatedGroup, UUID groupId, UUID memberId) 
		throws IllegalArgumentException, NdexException, ObjectNotFoundException {
			
			Preconditions.checkArgument(groupId != null, 
					"A group id is required");
			Preconditions.checkArgument(groupId != null, 
					"A member id is required");
			Preconditions.checkArgument(updatedGroup != null, 
					"An updated group is required");
		
		ODocument group =  this.getRecordByUUID(groupId, NdexClasses.Group);
		ODocument member = this.getRecordByUUID(memberId, NdexClasses.User);
		if (!isGroupAdmin(group, member) )
			throw new NdexException ("User " + memberId + " doesn't have permission to update group "
		            + groupId);
		
		try {
			//updatedGroup.getDescription().isEmpty();
			if(!Strings.isNullOrEmpty(updatedGroup.getDescription())) group.field("description", updatedGroup.getDescription());
			if(!Strings.isNullOrEmpty(updatedGroup.getWebsite())) group.field("websiteURL", updatedGroup.getWebsite());
			if(!Strings.isNullOrEmpty(updatedGroup.getImage())) group.field("imageURL", updatedGroup.getImage());
			if(!Strings.isNullOrEmpty(updatedGroup.getOrganizationName())) group.field("organizationName", updatedGroup.getOrganizationName()); 
			group.field(NdexClasses.ExternalObj_mTime, new Date());

			group = group.save();
			logger.info("Updated group profile with UUID " + groupId);
			
			return GroupDAO.getGroupFromDocument(group);
			
		} catch (Exception e) {
			
			logger.severe("An error occured while updating group profile with UUID " + groupId + e.getMessage());
			throw new NdexException("Unable to update group");
			
		} 
	}
	
	/**
	 * Check if the member has given permission in a group.
	 * @param group the group to check this member in
	 * @param member the member to be checked.
	 * @return
	 * @throws ObjectNotFoundException
	 * @throws NdexException
	 */
	private boolean isGroupAdmin(ODocument group, ODocument member) 
			throws ObjectNotFoundException, NdexException {

		boolean isMember = false;
		try {
			OrientVertex vGroup = graph.getVertex(group);
			OrientVertex vmember = graph.getVertex(member);
			
			for(Edge e : vGroup.getEdges(Direction.IN, Permissions.GROUPADMIN.toString().toLowerCase())) {
				if( ( (OrientVertex) e.getVertex(Direction.OUT) ).getIdentity().equals( vmember.getIdentity() ) ) 
					isMember = true;
			}
			
		} catch(Exception e) {
			String message = "Unexpected error while validating group member for group: " + e.getMessage(); 
			logger.severe(message);
			throw new NdexException(message);
		}
		
		
		return isMember;
		
	}

	
}
