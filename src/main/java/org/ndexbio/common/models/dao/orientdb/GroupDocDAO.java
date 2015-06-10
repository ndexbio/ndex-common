/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.exceptions.DuplicateObjectException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.SimpleUserQuery;
import org.ndexbio.model.object.Group;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.MembershipType;
import org.ndexbio.model.object.Permissions;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class GroupDocDAO extends OrientdbDAO {
	
	private static final Logger logger = Logger.getLogger(GroupDAO.class.getName());

	/**************************************************************************
	    * GroupDAO
	    * 
	    * @param db
	    *            Database instance from the Connection pool, should be opened
	    * @param graph
	    * 			OrientBaseGraph layer on top of db instance. 
	    **************************************************************************/
	
	public GroupDocDAO(ODatabaseDocumentTx dbConnection) {
		super(dbConnection);
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
		
		final ODocument group = this.getRecordByUUID(id, NdexClasses.Group);
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

		String traversePermission;
		OSQLSynchQuery<ODocument> query;
		Iterable<ODocument> groups;
		final List<Group> foundgroups = new ArrayList<>();
		final int startIndex = skipBlocks * blockSize;
		
		if (simpleQuery.getSearchString().equals("*") )
			simpleQuery.setSearchString("");
		
		if( simpleQuery.getPermission() == null ) 
			traversePermission = "out_groupadmin, out_member";
		else 
			traversePermission = "out_"+simpleQuery.getPermission().name().toLowerCase();
		
		simpleQuery.setSearchString(simpleQuery.getSearchString().toLowerCase().trim());
		
		try {
			if(!Strings.isNullOrEmpty(simpleQuery.getAccountName())) {
				ODocument nUser = this.getRecordByAccountName(simpleQuery.getAccountName(), NdexClasses.User);
				
				if(nUser == null) 
					throw new NdexException("Invalid accountName to filter by");
				
				String traverseRID = nUser.getIdentity().toString();
				query = new OSQLSynchQuery<>("SELECT FROM"
						+ " (TRAVERSE "+traversePermission+" FROM"
			  				+ " " + traverseRID
			  				+ " WHILE $depth <=1)"
			  			+ " WHERE @class = '"+ NdexClasses.Group +"'"
			  			+ " AND (not " + NdexClasses.ExternalObj_isDeleted + ")" 
			  			+ " AND (accountName.toLowerCase() LIKE '%"+ Helper.escapeOrientDBSQL(simpleQuery.getSearchString()) +"%'"
						+ " OR organizationName.toLowerCase() LIKE '%"+ Helper.escapeOrientDBSQL(simpleQuery.getSearchString()) +"%')"
						+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " 
						+ " SKIP " + startIndex
						+ " LIMIT " + blockSize );
				
				groups = this.db.command(query).execute();
				
				for ( ODocument group : groups) {
						foundgroups.add(GroupDAO.getGroupFromDocument(group));
				}
				return foundgroups;
			
			} 
			
			query = new OSQLSynchQuery<>("SELECT FROM"
						+ " " + NdexClasses.Group
						+ " WHERE "
			  			+ " (not " + NdexClasses.ExternalObj_isDeleted + ")" 
						+ " AND (accountName.toLowerCase() LIKE '%"+ Helper.escapeOrientDBSQL(simpleQuery.getSearchString()) +"%'"
						+ " OR organizationName.toLowerCase() LIKE '%"+ Helper.escapeOrientDBSQL(simpleQuery.getSearchString()) +"%')"
						+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " 
						+ " SKIP " + startIndex
						+ " LIMIT " + blockSize );
				
			groups = this.db.command(query).execute();
				
//			if( !groups.iterator().hasNext() && simpleQuery.getSearchString().equals("") ) 
//					groups = this.db.browseClass(NdexClasses.Group).setLimit(blockSize);
				
				for (final ODocument group : groups) {
					foundgroups.add(GroupDAO.getGroupFromDocument(group));
			}
			return foundgroups;
			
			
		} catch (Exception e) {
			logger.severe("Unable to query the database");
			throw new NdexException("Failed to search for groups.\n" + e.getMessage());
			
		} 
	}
	
	
	
	/**************************************************************************
	    * getGroupNetworkMemberships
	    *
	    * @param groupId
	    *            UUID for associated group
	    * @param permission
	    * 			Type of memberships to retrieve, GROUPADMIN or MEMBER
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
		Preconditions.checkArgument( (permission.equals( Permissions.ADMIN ))
				|| (permission.equals( Permissions.READ ))
				|| (permission.equals( Permissions.WRITE )),
				"Valid permissions required");
		
		ODocument group = this.getRecordByUUID(groupId, NdexClasses.Group);
		
		final int startIndex = skipBlocks
				* blockSize;
		
		try {
			List<Membership> memberships = new ArrayList<>();
			String groupRID = group.getIdentity().toString();
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
		  			"SELECT FROM"
		  			+ " (TRAVERSE "+ NdexClasses.Group +".out_"+ permission.name().toString().toLowerCase() +" FROM"
		  				+ " " + groupRID
		  				+ "  WHILE $depth <=1)"
		  			+ " WHERE @class = '" + NdexClasses.Network + "' and (not " + NdexClasses.ExternalObj_isDeleted + ") "
		 			+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " + " SKIP " + startIndex
		 			+ " LIMIT " + blockSize);
			
			List<ODocument> records = this.db.command(query).execute(); 
			for(ODocument member: records) {
				
				Membership membership = new Membership();
				membership.setMembershipType( MembershipType.NETWORK );
				membership.setMemberAccountName( (String) group.field(NdexClasses.account_P_accountName) ); 
				membership.setMemberUUID( groupId );
				membership.setPermissions( permission );
				membership.setResourceName( (String) member.field("name") );
				membership.setResourceUUID( UUID.fromString( (String) member.field(NdexClasses.ExternalObj_ID) ) );
				
				memberships.add(membership);
			}
			
			logger.info("Successfuly retrieved group-network memberships");
			return memberships;
			
		} catch(Exception e) {
			logger.severe("An unexpected error occured while retrieving group-network memberships " + e.getMessage());
			throw new NdexException("Unable to get network memberships for group with UUID "+groupId);
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
		Preconditions.checkArgument( (permission.equals( Permissions.GROUPADMIN) )
				|| (permission.equals( Permissions.MEMBER )),
				"Valid permissions required");
		
		ODocument group = this.getRecordByUUID(groupId, NdexClasses.Group);
		
		final int startIndex = skipBlocks
				* blockSize;
		
		try {
			List<Membership> memberships = new ArrayList<>();
			
			String groupRID = group.getIdentity().toString();
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
		  			"SELECT FROM"
		  			+ " (TRAVERSE "+ NdexClasses.Group +".in_"+ permission.name().toString().toLowerCase() +" FROM"
		  				+ " " + groupRID
		  				+ "  WHILE $depth <=1)"
		  			+ " WHERE @class = '" + NdexClasses.User + "' AND (not " + NdexClasses.ExternalObj_isDeleted + ") "
		 			+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " + " SKIP " + startIndex
		 			+ " LIMIT " + blockSize);
			
			List<ODocument> records = this.db.command(query).execute(); 
			for(ODocument member: records) {
				
				Membership membership = new Membership();
				membership.setMembershipType( MembershipType.GROUP );
				membership.setMemberAccountName( (String) member.field(NdexClasses.account_P_accountName) ); 
				membership.setMemberUUID( UUID.fromString( (String) member.field(NdexClasses.ExternalObj_ID) ) );
				membership.setPermissions( permission );
				membership.setResourceName( (String) group.field("organizationName") );
				membership.setResourceUUID( groupId );
				
				memberships.add(membership);
			}
			
			logger.info("Successfuly retrieved group-user memberships");
			return memberships;
			
		} catch(Exception e) {
			logger.severe("An unexpected error occured while retrieving group-user memberships "+e.getMessage());
			throw new NdexException("Unable to get user memberships for group with UUID "+groupId);
		}
	}
	
	public Membership getMembershipToNetwork(UUID groupId, UUID networkId) 
		throws IllegalArgumentException, ObjectNotFoundException, NdexException {
		
		Preconditions.checkArgument(groupId != null, "UUID for group required");
		Preconditions.checkArgument(networkId != null, "UUID for network required");
		
		Permissions permission = null;
		Membership membership = new Membership();
		
		ODocument OGroup = this.getRecordByUUID(groupId, NdexClasses.Group);
		ODocument ONetwork = this.getRecordByUUID(networkId, NdexClasses.Network);
		
		// order allows us to return most permissive permission
		if (checkPermission(OGroup.getIdentity(), 
								ONetwork.getIdentity(), 
								Direction.OUT, 
								1, 
								Permissions.READ))
			permission = Permissions.READ;
		
		if (checkPermission(OGroup.getIdentity(),
								ONetwork.getIdentity(), 
								Direction.OUT, 
								1,
								Permissions.WRITE))
			permission = Permissions.WRITE;
		
		if (checkPermission(OGroup.getIdentity(),
								ONetwork.getIdentity(), 
								Direction.OUT, 
								1,
								Permissions.ADMIN))
			permission = Permissions.ADMIN;

		membership.setMemberAccountName((String) OGroup.field("accountName"));
		membership.setMemberUUID(groupId);
		membership.setResourceName((String) ONetwork.field("name"));
		membership.setResourceUUID(networkId);
		membership.setPermissions(permission);
		membership.setMembershipType(MembershipType.NETWORK);
		
		return membership;
	}
	
	/*
	 * Convert the database results into our object model
	 * TODO should this be moved to util? being used by other classes, not really a data access object
	 */
	public static Group getGroupFromDocument(ODocument n) {
		
		Group result = new Group();

		Helper.populateExternalObjectFromDoc (result, n);

		result.setOrganizationName((String)n.field("organizationName"));
		result.setWebsite((String)n.field("websiteURL"));
		result.setDescription((String)n.field("description"));
		result.setImage((String)n.field("imageURL"));
		if ( result.getIsDeleted()) {
			result.setAccountName((String)n.field(NdexClasses.account_P_oldAcctName));	
		} else {
		  result.setAccountName((String)n.field(NdexClasses.account_P_accountName));
		}
		return result;
	}
	


}
