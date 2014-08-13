package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.dao.CommonDAOValues;
import org.ndexbio.model.object.SimpleUserQuery;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.Group;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.MembershipType;
import org.ndexbio.model.object.Permissions;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class GroupDAO extends OrientdbDAO {
	
	private ODatabaseDocumentTx db;
	private OrientBaseGraph graph;
	private static final Logger logger = Logger.getLogger(GroupDAO.class.getName());

	/**************************************************************************
	    * GroupDAO
	    * 
	    * @param db
	    *            Database instance from the Connection pool, should be opened
	    **************************************************************************/
	public GroupDAO(ODatabaseDocumentTx db, OrientBaseGraph graph) {
		super(db);
		this.db = db;
		this.graph = graph;
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
			final ODocument admin = this.getRecordById(adminId, NdexClasses.User);
				
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
				
				Vertex vGroup = graph.getVertex(group);
				Vertex vAdmin = graph.getVertex(admin);
				
				graph.addEdge(null, vAdmin, vGroup, Permissions.ADMIN.toString().toLowerCase());
				
				logger.info("A new group with accountName "
							+ newGroup.getAccountName() 
							+" and owner "+ (String)admin.field("accountName") 
							+" has been created");
				
				return result;
			} 
			catch(Exception e) {
				logger.severe("Could not save new group to the database:" + e.getMessage());
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
		
		final ODocument group = this.getRecordById(id, NdexClasses.Group);
	    return GroupDAO.getGroupFromDocument(group);
	}
	
	/**************************************************************************
	    * Get a Group
	    * 
	    * @param accountName
	    *            Group's accountName
	    * @throws NdexException
	    *            Attempting to access database
	    * @throws IllegalArgumentexception
	    * 			The id is invalid
	    * @throws ObjectNotFoundException
	    * 			The group specified by id does not exist
	    **************************************************************************/
	public Group getGroupByAccountName(String accountName)
			throws IllegalArgumentException, ObjectNotFoundException, NdexException{
		Preconditions.checkArgument(!Strings.isNullOrEmpty(accountName), 
				"UUID required");
		
		final ODocument group = this.getRecordByAccountName(accountName, NdexClasses.Group);
	    return GroupDAO.getGroupFromDocument(group);
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
	public void deleteGroupById(UUID groupId, UUID adminId) 
		throws NdexException, ObjectNotFoundException{
			
			//TODO cannot orphan networks, has not been tested
		
		Preconditions.checkArgument(null != groupId, 
				"group UUID required");
		Preconditions.checkArgument(null != adminId, 
				"admin UUID required");
		
		final ODocument group = this.getRecordById(groupId, NdexClasses.Group);
			
		this.validatePermission(groupId, adminId, Permissions.ADMIN);
		
		try {
			OrientVertex vGroup = graph.getVertex(group);
			boolean safe = true;
			
			for(Edge e : vGroup.getEdges( Direction.BOTH, Permissions.ADMIN.toString().toLowerCase() ) ) {
				OrientVertex vResource = (OrientVertex) e.getVertex(Direction.IN);
				
				if(!vResource.getRecord().getSchemaClass().getName().equals( NdexClasses.Network ))
					continue;
				
				safe = false;	
				
				for(Edge ee : vResource.getEdges( Direction.BOTH, Permissions.ADMIN.toString().toLowerCase() ) ) {
					if( !( (OrientVertex) ee.getVertex(Direction.OUT) ).equals(vGroup) ) {
						safe = true;
					}
				}
					
			}
				
			if(!safe)
				throw new NdexException("Cannot orphan networks");
			
			
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
	public Group updateGroup(Group updatedGroup, UUID groupId, UUID memberId) 
		throws IllegalArgumentException, NdexException, ObjectNotFoundException {
			
			Preconditions.checkArgument(groupId != null, 
					"A group id is required");
			Preconditions.checkArgument(groupId != null, 
					"A member id is required");
			Preconditions.checkArgument(updatedGroup != null, 
					"An updated group is required");
		
		ODocument group =  this.getRecordById(groupId, NdexClasses.Group);
		
		try {
			this.validatePermission(groupId, memberId, Permissions.ADMIN);
		} catch (Exception e) {
			this.validatePermission(groupId, memberId, Permissions.WRITE);
		}
		
		try {
			//updatedGroup.getDescription().isEmpty();
			if(!Strings.isNullOrEmpty(updatedGroup.getDescription())) group.field("description", updatedGroup.getDescription());
			if(!Strings.isNullOrEmpty(updatedGroup.getWebsite())) group.field("websiteURL", updatedGroup.getWebsite());
			if(!Strings.isNullOrEmpty(updatedGroup.getImage())) group.field("imageURL", updatedGroup.getImage());
			if(!Strings.isNullOrEmpty(updatedGroup.getOrganizationName())) group.field("organizationName", updatedGroup.getOrganizationName()); 
			group.field("modificationDate", updatedGroup.getModificationDate());

			group.save();
			logger.info("Updated group profile with UUID " + groupId);
			
			return GroupDAO.getGroupFromDocument(group);
			
		} catch (Exception e) {
			
			logger.severe("An error occured while updating group profile with UUID " + groupId);
			throw new NdexException(e.getMessage());
			
		} 
	}
	
	/**************************************************************************
	    * Find groups
	    * 
	    * @param query
	    * 			SimpleUserQuery object. The search string filters by 
	    * 			group account name and organization name. The accountName
	    * 			filters to groups owned by the account specified.
	    * @param skipBlocks
	    *            amount of blocks to skip
	    * @param blockSize
	    * 			the size of a block
	    * @throws NdexException
	    *            Attempting to access and delete an ODocument from the database
	    * @throws IllegalArgumentException
	    * 			Group object cannot be null
	    **************************************************************************/
	public List<Group> findGroups(SimpleUserQuery simpleQuery, int skipBlocks, int blockSize) 
			throws NdexException, IllegalArgumentException {
		
		Preconditions.checkArgument(null != simpleQuery, "Search parameters are required");

		OSQLSynchQuery<ODocument> query;
		Iterable<ODocument> groups;
		final List<Group> foundgroups = new ArrayList<Group>();
		
		simpleQuery.setSearchString(simpleQuery.getSearchString()
					.toLowerCase().trim());
		final int startIndex = skipBlocks * blockSize;
		
		try {
			if(!Strings.isNullOrEmpty(simpleQuery.getAccountName())) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
				OIdentifiable nUser = (OIdentifiable) accountNameIdx.get(simpleQuery.getAccountName()); // account to traverse by
				
				if(nUser == null) 
					throw new NdexException("Invalid accountName to filter by");
				
				String traverseRID = nUser.getIdentity().toString();
				query = new OSQLSynchQuery<ODocument>("SELECT FROM"
						+ " (TRAVERSE * FROM"
			  				+ " " + traverseRID
			  				+ " WHILE $depth <=1)"
			  			+ " WHERE @class = '"+ NdexClasses.Group +"'"
			  			+ " AND accountName.toLowerCase() LIKE '%"+ simpleQuery.getSearchString() +"%'"
						+ " OR organizationName.toLowerCase() LIKE '%"+ simpleQuery.getSearchString() +"%'"
						+ " ORDER BY creation_date DESC " 
						+ " SKIP " + startIndex
						+ " LIMIT " + blockSize );
				
				groups = this.db.command(query).execute();
				
				if( !groups.iterator().hasNext() ) {
					query = new OSQLSynchQuery<ODocument>("SELECT FROM"
						+ " (TRAVERSE * FROM"
			  				+ " " + traverseRID
			  				+ " WHILE $depth <=1)"
			  			+ " WHERE @class = '"+ NdexClasses.Group +"'"
						+ " ORDER BY creation_date DESC " 
						+ " SKIP " + startIndex
						+ " LIMIT " + blockSize );
					
					groups = this.db.command(query).execute();
				}
				
				for (final ODocument group : groups) {
					foundgroups.add(GroupDAO.getGroupFromDocument(group));
				}
				return foundgroups;
			
			} else {
				query = new OSQLSynchQuery<ODocument>("SELECT FROM"
						+ " " + NdexClasses.Group
						+ " WHERE accountName.toLowerCase() LIKE '%"+ simpleQuery.getSearchString() +"%'"
						+ " OR organizationName.toLowerCase() LIKE '%"+ simpleQuery.getSearchString() +"%'"
						+ " ORDER BY creation_date DESC " 
						+ " SKIP " + startIndex
						+ " LIMIT " + blockSize );
				
				groups = this.db.command(query).execute();
				
				if( !groups.iterator().hasNext() ) 
					groups = this.db.browseClass(NdexClasses.Group).setLimit(blockSize);
				
				for (final ODocument group : groups) {
					foundgroups.add(GroupDAO.getGroupFromDocument(group));
				}
				return foundgroups;
			}
			
		} catch (Exception e) {
			logger.severe("Unable to query the database");
			throw new NdexException("Failed to search for groups.\n" + e.getMessage());
			
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
		Preconditions.checkArgument(membership.getMembershipType() == MembershipType.GROUP,
				"Incorrect membership type");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(membership.getMemberUUID().toString()),
				"member UUID required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(groupId.toString()),
				"group UUID required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(adminId.toString()),
				"admin UUID required");
		Preconditions.checkArgument( (membership.getPermissions() == Permissions.ADMIN)
				|| (membership.getPermissions() == Permissions.READ)
				|| (membership.getPermissions() == Permissions.WRITE),
				"Valid permissions required");
		
		final ODocument group = this.getRecordById(groupId, NdexClasses.Group);
		final ODocument admin = this.getRecordById(adminId, NdexClasses.User);
		final ODocument member = this.getRecordById(membership.getMemberUUID(), NdexClasses.User);
		
		try {
			OrientVertex vGroup = graph.getVertex(group);
			OrientVertex vAdmin = graph.getVertex(admin);
			OrientVertex vMember = graph.getVertex(member);
			
			boolean isAdmin = false;
			boolean isOnlyAdmin = true;
			for(Edge e : vGroup.getEdges(Direction.BOTH, Permissions.ADMIN.toString().toLowerCase())) {
				if( ( (OrientVertex) e.getVertex(Direction.OUT) ).getIdentity().equals( vAdmin.getIdentity() ) ) 
					isAdmin = true;
				else 
					isOnlyAdmin = false;
			}
			
			if(isOnlyAdmin && ( vAdmin.getIdentity().equals( vMember.getIdentity() ) ) ) {
				logger.severe("Action will orphan group");
				throw new NdexException("Cannot orphan group to have no admin");
			}
			
			if(isAdmin) {
				
				for(Edge e : vGroup.getEdges(Direction.BOTH)) {
					if( ( (OrientVertex) e.getVertex(Direction.OUT) ).getIdentity().equals( vMember.getIdentity() ) ) 
						graph.removeEdge(e);
				}
				
				graph.addEdge(null, vMember, vGroup, membership.getPermissions().toString().toLowerCase());
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
			throw new NdexException(e.getMessage());
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
	    * 			UUID for valid admin of group
	    * @throws NdexException
	    *            Invalid parameters or an error occurred while accessing the database
	    * @throws ObjectNotFoundException
	    * 			Invalid groupId, memberId, or adminId
	    * @throws IllegalArgumentException
	    * 			UUID identifiers required
	    **************************************************************************/
	
	public void removeMember(UUID memberId, UUID groupId, UUID adminId) 
			throws ObjectNotFoundException, IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(memberId.toString()),
				"member UUID required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(groupId.toString()),
				"group UUID required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(adminId.toString()));
		
		final ODocument group = this.getRecordById(groupId, NdexClasses.Group);
		final ODocument admin = this.getRecordById(adminId, NdexClasses.User);
		final ODocument member = this.getRecordById(memberId, NdexClasses.User);
		
		try {
			OrientVertex vGroup = graph.getVertex(group);
			OrientVertex vAdmin = graph.getVertex(admin);
			OrientVertex vMember = graph.getVertex(member);
			
			boolean isAdmin = false;
			boolean isOnlyAdmin = true;
			for(Edge e : vGroup.getEdges(Direction.BOTH, Permissions.ADMIN.toString().toLowerCase())) {
				if( ( (OrientVertex) e.getVertex(Direction.OUT) ).getIdentity().equals( vAdmin.getIdentity() ) ) 
					isAdmin = true;
				else 
					isOnlyAdmin = false;
			}
			
			if(isOnlyAdmin && ( vAdmin.getIdentity().equals( vMember.getIdentity() ) ) ) {
				throw new NdexException("Cannot orphan group to have no admin");
			}
			
			if(isAdmin) {
				for(Edge e : vGroup.getEdges(Direction.BOTH)) {
					if( ( (OrientVertex) e.getVertex(Direction.OUT) ).getIdentity().equals( vMember.getIdentity() ) ) 
						graph.removeEdge(e);
				}
				logger.info("removed member if existed");
				
			} else {
				logger.severe("Invalid admin for group");
				throw new NdexException("Specified user is not an admin for the group");
			}
			
		} catch (Exception e) {
			logger.severe("Unable to remove member for "
					+ "group with UUID "+ groupId
					+ " and admin with UUID " + adminId
					+ " and member with UUID " + memberId);
			throw new NdexException(e.getMessage());
		}
	}
	
	/**************************************************************************
	    * getGroupNetworkMemberships
	    *
	    * @param groupId
	    *            UUID for associated group
	    * @param permission
	    * 			Type of memberships to retrieve, ADMIN, WRITE, or READ
	    * @param skipBlocks
	    * 			amount of blocks to skip
	    * @param blockSize
	    * 			The size of blocks to be skipped and retrieved
	    * @throws NdexException
	    *            Invalid parameters or an error occurred while accessing the database
	    * @throws ObjectNotFoundException
	    * 			Invalid groupId
	    **************************************************************************/
	
	public List<Membership> getGroupNetworkMemberships(UUID groupId, Permissions permission, int skipBlocks, int blockSize) 
			throws ObjectNotFoundException, NdexException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(groupId.toString()),
				"A group UUID is required");
		Preconditions.checkArgument( (permission == Permissions.ADMIN)
				|| (permission == Permissions.READ)
				|| (permission == Permissions.WRITE),
				"Valid permissions required");
		
		ODocument group = this.getRecordById(groupId, NdexClasses.Group);
		
		final int startIndex = skipBlocks
				* blockSize;
		
		try {
			List<Membership> memberships = new ArrayList<Membership>();
			String groupRID = group.getIdentity().toString();
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
		  			"SELECT FROM"
		  			+ " (TRAVERSE "+ NdexClasses.Group +".out_"+ permission.name().toString().toLowerCase() +" FROM"
		  				+ " " + groupRID
		  				+ "  WHILE $depth <=1)"
		  			+ " WHERE @class = '" + NdexClasses.Network + "'"
		 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
		 			+ " LIMIT " + blockSize);
			
			List<ODocument> records = this.db.command(query).execute(); 
			for(ODocument member: records) {
				
				//if( !member.getSchemaClass().getName().equals( NdexClasses.Network ) )
					//continue;
				
				Membership membership = new Membership();
				membership.setMembershipType( MembershipType.NETWORK );
				membership.setMemberAccountName( (String) group.field("accountName") ); 
				membership.setMemberUUID( groupId );
				membership.setPermissions( permission );
				membership.setResourceName( (String) member.field("name") );
				membership.setResourceUUID( UUID.fromString( (String) member.field("UUID") ) );
				
				memberships.add(membership);
			}
			
			/*OrientVertex vGroup = graph.getVertex(group);
			
			for(Edge e : vGroup.getEdges(Direction.BOTH)) {
				OrientVertex vMember = (OrientVertex) e.getVertex(Direction.IN);
				ODocument member = vMember.getRecord();
				
				if( !member.getSchemaClass().getName().equals( NdexClasses.Network ) )
					continue;
				
				Membership membership = new Membership();
				membership.setMembershipType( MembershipType.NETWORK );
				membership.setMemberAccountName( (String) group.field("accountName") ); 
				membership.setMemberUUID( groupId );
				membership.setPermissions( Permissions.valueOf( e.getLabel() ) );
				membership.setResourceName( (String) member.field("name") );
				membership.setResourceUUID( UUID.fromString( (String) member.field("UUID") ) );
				
				memberships.add(membership);
				
			}*/
			
			logger.info("Successfuly retrieved group-network memberships");
			return memberships;
			
		} catch(Exception e) {
			logger.severe("An unexpected error occured while retrieving group-network memberships");
			throw new NdexException(e.getMessage());
		}
	}
	
	/**************************************************************************
	    * getGroupUserMemberships
	    *
	    * @param groupId
	    *            UUID for associated group
	    * @param permission
	    * 			Type of memberships to retrieve, ADMIN, WRITE, or READ
	    * @param skipBlocks
	    * 			amount of blocks to skip
	    * @param blockSize
	    * 			The size of blocks to be skipped and retrieved
	    * @throws NdexException
	    *            Invalid parameters or an error occurred while accessing the database
	    * @throws ObjectNotFoundException
	    * 			Invalid groupId
	    **************************************************************************/
	
	public List<Membership> getGroupUserMemberships(UUID groupId, Permissions permission, int skipBlocks, int blockSize) 
			throws ObjectNotFoundException, NdexException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(groupId.toString()),
				"A group UUID is required");
		Preconditions.checkArgument( (permission == Permissions.ADMIN)
				|| (permission == Permissions.READ)
				|| (permission == Permissions.WRITE),
				"Valid permissions required");
		
		ODocument group = this.getRecordById(groupId, NdexClasses.Group);
		
		final int startIndex = skipBlocks
				* blockSize;
		
		try {
			List<Membership> memberships = new ArrayList<Membership>();
			
			String groupRID = group.getIdentity().toString();
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
		  			"SELECT FROM"
		  			+ " (TRAVERSE "+ NdexClasses.Group +".in_"+ permission.name().toString().toLowerCase() +" FROM"
		  				+ " " + groupRID
		  				+ "  WHILE $depth <=1)"
		  			+ " WHERE @class = '" + NdexClasses.User + "'"
		 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
		 			+ " LIMIT " + blockSize);
			
			List<ODocument> records = this.db.command(query).execute(); 
			for(ODocument member: records) {
				
				//if( !member.getSchemaClass().getName().equals( NdexClasses.User ) )
					//continue;
				
				Membership membership = new Membership();
				membership.setMembershipType( MembershipType.GROUP );
				membership.setMemberAccountName( (String) member.field("accountName") ); 
				membership.setMemberUUID( UUID.fromString( (String) member.field("UUID") ) );
				membership.setPermissions( permission );
				membership.setResourceName( (String) group.field("organizationName") );
				membership.setResourceUUID( groupId );
				
				memberships.add(membership);
			}
			
			
			/*OrientVertex vGroup = graph.getVertex(group);
			
			for(Edge e : vGroup.getEdges(Direction.BOTH)) {
				OrientVertex vMember = (OrientVertex) e.getVertex(Direction.OUT);
				ODocument member = vMember.getRecord();
				
				if(!member.getSchemaClass().getName().equals(NdexClasses.Network))
					continue;
				
				Membership membership = new Membership();
				membership.setMembershipType( MembershipType.GROUP );
				membership.setMemberAccountName( (String) member.field("accountName") ); 
				membership.setMemberUUID( UUID.fromString( (String) member.field("UUID") ) );
				membership.setPermissions( Permissions.valueOf( e.getLabel() ) );
				membership.setResourceName( (String) group.field("organizationName"));
				membership.setResourceUUID( groupId );
				
				memberships.add(membership);
				
			}*/
			
			logger.info("Successfuly retrieved group-user memberships");
			return memberships;
			
		} catch(Exception e) {
			logger.severe("An unexpected error occured while retrieving group-user memberships");
			throw new NdexException(e.getMessage());
		}
	}
	
	/*
	 * Convert the database results into our object model
	 * TODO should this be moved to util? being used by other classes, not really a data access object
	 */
	public static Group getGroupFromDocument(ODocument n) {
		
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
	
	private void checkForExistingGroup(final Group group) 
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
	
	private void validatePermission(UUID groupId, UUID memberId, Permissions permission) 
			throws ObjectNotFoundException, NdexException {
		
		final ODocument group = this.getRecordById(groupId, NdexClasses.Group);
		final ODocument member = this.getRecordById(memberId, NdexClasses.User);

		boolean isMember = false;
		try {
			OrientVertex vGroup = graph.getVertex(group);
			OrientVertex vmember = graph.getVertex(member);
			
			for(Edge e : vGroup.getEdges(Direction.BOTH, permission.toString().toLowerCase())) {
				if( ( (OrientVertex) e.getVertex(Direction.OUT) ).getIdentity().equals( vmember.getIdentity() ) ) 
					isMember = true;
			}
			
		} catch(Exception e) {
			logger.severe("Unexpected error while validating group member for group: "
					+ "\n   "+groupId.toString()+" and user: "
					+ "\n   "+memberId.toString());
			throw new NdexException("Unexpected error while validating group member");
		}
		
		if(!isMember) 
			throw new NdexException("Invalid permission to group for member UUID: " + memberId.toString());
		
	}
	
}
