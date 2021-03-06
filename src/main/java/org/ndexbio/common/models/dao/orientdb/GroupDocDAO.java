/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.SimpleUserQuery;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;
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
			  			+ " AND ( " + NdexClasses.ExternalObj_isDeleted + " = false)" 
			  			+ " AND (accountName.toLowerCase() LIKE '%"+ Helper.escapeOrientDBSQL(simpleQuery.getSearchString()) +"%'"
						+ " OR " + NdexClasses.GRP_P_NAME + ".toLowerCase() LIKE '%"+ Helper.escapeOrientDBSQL(simpleQuery.getSearchString()) +"%')"
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
			  			+ " ( " + NdexClasses.ExternalObj_isDeleted + " = false )" 
						+ " AND (accountName.toLowerCase() LIKE '%"+ Helper.escapeOrientDBSQL(simpleQuery.getSearchString()) +"%'"
						+ " OR " + NdexClasses.GRP_P_NAME +".toLowerCase() LIKE '%"+ Helper.escapeOrientDBSQL(simpleQuery.getSearchString()) +"%')"
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
		  			+ " WHERE @class = '" + NdexClasses.Network + "' and ( " + NdexClasses.ExternalObj_isDeleted + " = false) "
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
	
	public List<NetworkSummary> getGroupNetworks (String groupId, String userAccountName) 
			throws ObjectNotFoundException, NdexException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(groupId.toString()),
				"A group UUID is required");
		
		ODocument group = this.getRecordByUUIDStr(groupId, NdexClasses.Group);
	
		List<NetworkSummary> results = new ArrayList<>(20);
		
		try {
			String groupRID = group.getIdentity().toString();
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
		  			"SELECT FROM"
		  			+ " (TRAVERSE "+ NdexClasses.Group +".out_"+ Permissions.READ.toString().toLowerCase()  + 
		  			     "," + NdexClasses.Group + ".out_"+ Permissions.WRITE.toString().toLowerCase() +" FROM"
		  				+ " " + groupRID
		  				+ "  WHILE $depth <=1)"
		  			+ " WHERE @class = '" + NdexClasses.Network + "' and ( " + NdexClasses.ExternalObj_isDeleted + " = false)"
		 			);
			
			List<ODocument> records = this.db.command(query).execute(); 
			for(ODocument netDoc: records) {
				NetworkSummary summary = NetworkDocDAO.getNetworkSummary(netDoc);
				
				if ( summary.getVisibility() == VisibilityType.PUBLIC) {
					results.add(summary );
				} else if ( userAccountName !=null ){
					 NetworkDocDAO dao = new NetworkDocDAO(this.getDBConnection());
					 if ( dao.networkSummaryIsReadable(userAccountName, summary.getExternalId().toString()) )
						results.add(summary);
				}
			}
			
			logger.info("Successfuly retrieved group-networks");
			return results;
			
		} catch(Exception e) {
			logger.severe("An unexpected error occured while retrieving group-networks " + e.getMessage());
			throw new NdexException("Unable to get networks for group with UUID "+groupId);
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
		  			+ " WHERE @class = '" + NdexClasses.User + "' AND ( " + NdexClasses.ExternalObj_isDeleted + " = false) "
		 			+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " + " SKIP " + startIndex
		 			+ " LIMIT " + blockSize);
			
			List<ODocument> records = this.db.command(query).execute(); 
			for(ODocument member: records) {
				
				Membership membership = new Membership();
				membership.setMembershipType( MembershipType.GROUP );
				membership.setMemberAccountName( (String) member.field(NdexClasses.account_P_accountName) ); 
				membership.setMemberUUID( UUID.fromString( (String) member.field(NdexClasses.ExternalObj_ID) ) );
				membership.setPermissions( permission );
				membership.setResourceName( (String) group.field(NdexClasses.GRP_P_NAME) );
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
	
	
	public List<String> getGroupUserAccount(String groupAccount) 
			throws ObjectNotFoundException, NdexException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(groupAccount.toString()),
				"A group UUID is required");
	
		ODocument group = this.getRecordByAccountName(groupAccount, NdexClasses.Group);
		
		List<String> result = new ArrayList<>();
		try {
			List<Membership> memberships = new ArrayList<>();
			
			String groupRID = group.getIdentity().toString();
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
		  			"SELECT FROM"
		  			+ " (TRAVERSE "+ NdexClasses.Group +".in_"+ Permissions.GROUPADMIN.name().toString().toLowerCase() +
		  					",in_" + NdexClasses.Group +".in_"+ Permissions.MEMBER.name().toString().toLowerCase() 
		  					+ " FROM " + groupRID + "  WHILE $depth <=1) WHERE @class = '" + NdexClasses.User + 
		  					"' AND ( " + NdexClasses.ExternalObj_isDeleted + " = false) ");
			
			List<ODocument> records = this.db.command(query).execute(); 
			for(ODocument member: records) {
				result.add((String)member.field(NdexClasses.account_P_accountName));		
			}
			
			return result;
			
		} catch(Exception e) {
			logger.severe("An unexpected error occured while retrieving group-user memberships "+e.getMessage());
			throw new NdexException("Unable to get user memberships for group "+groupAccount);
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

		result.setGroupName((String)n.field(NdexClasses.GRP_P_NAME));
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
