package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.SimpleNetworkQuery;
import org.ndexbio.model.object.User;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NetworkSearchDAO extends OrientdbDAO{
	
	private static final Logger logger = Logger.getLogger(NetworkSearchDAO.class.getName());
	
	/**************************************************************************
	    * NetworkSearchDAO
	    * 
	    * @param db
	    *            Database instance from the Connection pool, should be opened
	    **************************************************************************/
	public NetworkSearchDAO (ODatabaseDocumentTx db) {
		super( db);
	}
	
	public List<NetworkSummary> findNetworks(SimpleNetworkQuery simpleNetworkQuery, int skip, int top, User loggedInUser) 
			throws NdexException, IllegalArgumentException {
		
		Preconditions.checkArgument(null != simpleNetworkQuery, 
				"A query is required");
		
		String userRID;
		String traversePermission;
		Integer traverseDepth;
		OSQLSynchQuery<ODocument> query;
		Iterable<ODocument> networks;
		final List<NetworkSummary> foundNetworks = new ArrayList<NetworkSummary>();
		final int startIndex = skip * top;
		
		// treat "*" and "" the same way
		if (simpleNetworkQuery.getSearchString().equals("*") )
			simpleNetworkQuery.setSearchString("");

		if( simpleNetworkQuery.getPermission() == null ) 
			traversePermission = "out_admin, out_write, out_read";
		else 
			traversePermission = "out_"+simpleNetworkQuery.getPermission().name().toLowerCase();
		
		if( simpleNetworkQuery.getIncludeGroups())
			traverseDepth = 2;
		else
			traverseDepth = 1;
		
		try {
			
			// get RID for logged in user if any
			if( loggedInUser != null ) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
				OIdentifiable user = (OIdentifiable) accountNameIdx.get( loggedInUser.getAccountName() ); // account to traverse by
				userRID = user.getIdentity().toString();
			} else {
				userRID = "#0:0"; // should fix, done to avoid parsing errors. 
			}
			// search across a traversal of an accounts networks if accountName is specified
			if(!Strings.isNullOrEmpty(simpleNetworkQuery.getAccountName())) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
				OIdentifiable nAccount = (OIdentifiable) accountNameIdx.get(simpleNetworkQuery.getAccountName()); // account to traverse by
				
				if(nAccount == null) 
					throw new NdexException("Invalid accountName to filter by");
				
				String traverseRID = nAccount.getIdentity().toString();
				query = new OSQLSynchQuery<ODocument>(
			  			"SELECT  FROM"
			  			+ " (TRAVERSE out_groupadmin, out_member, "+traversePermission+" FROM"
			  				+ " " + traverseRID
			  				+ "  WHILE $depth <= "+traverseDepth.toString()+" )"
			  			+ " WHERE name.toLowerCase() LIKE '%" + simpleNetworkQuery.getSearchString().toLowerCase() +"%'"
			  			+ " AND @class = '"+ NdexClasses.Network +"'"
			  			+ " AND isComplete"
			 			+ " AND ( visibility <> 'PRIVATE'"
						+ " OR in() contains "+userRID
						+ " OR in().in() contains "+userRID+" )"
			 			+ " ORDER BY "+ NdexClasses.ExternalObj_cTime +" DESC " + " SKIP " + startIndex
			 			+ " LIMIT " + top);
				
				networks = this.db.command(query).execute();
				
				//no results returned, eliminate search filter and return
				if( !networks.iterator().hasNext() ) {
					query = new OSQLSynchQuery<ODocument>(
				  			"SELECT FROM"
				  			+ " (TRAVERSE out_groupadmin, out_member, "+traversePermission+" FROM"
				  				+ " " + traverseRID
				  				+ "  WHILE $depth <= "+traverseDepth.toString()+" )"
				  			+ " WHERE @class = '"+ NdexClasses.Network +"'"
						  	+ " AND isComplete"
				 			+ " AND ( visibility <> 'PRIVATE'"
							+ " OR in() contains "+userRID
							+ " OR in().in() contains "+userRID+" )"
				 			+ " ORDER BY "+ NdexClasses.ExternalObj_cTime +" DESC " + " SKIP " + startIndex
				 			+ " LIMIT " + top);
					
					networks = this.db.command(query).execute();
				}
				
				for (final ODocument network : networks) {
					foundNetworks.add(NetworkDAO.getNetworkSummary(network));
				}
					
				return foundNetworks;
			    
			} 
			
			query = new OSQLSynchQuery<ODocument>(
			  			"SELECT FROM " + NdexClasses.Network
			  			+ " WHERE name.toLowerCase() LIKE '%"+ simpleNetworkQuery.getSearchString().toLowerCase() +"%'"
			  			+ " AND isComplete"
			 			+ " AND ( visibility <> 'PRIVATE'"
						+ " OR in() contains "+userRID
						+ " OR in().in() contains "+userRID+" )"
			 			+ " ORDER BY "+ NdexClasses.ExternalObj_cTime +" DESC " + " SKIP " + startIndex
			 			+ " LIMIT " + top);
				
			networks = this.db.command(query).execute();
			    
				// no results returned, return arbitrary selection. may need to be converted to select for performance
			if( !networks.iterator().hasNext() && simpleNetworkQuery.getSearchString().equals("")) 
					networks = db.browseClass(NdexClasses.Network).setLimit(top);
				
			for (final ODocument network : networks) {
					foundNetworks.add(NetworkDAO.getNetworkSummary(network));
			}
					
			return foundNetworks;
			
			
		} catch (Exception e) {
			
			logger.severe("An error occured while attempting to query the database for networks");
			throw new NdexException("Failed to search for networks.\n" + e.getMessage());
			
		} 
	}

/*	
	private List<NetworkSummary> getNetworkSummaryByOwnerAccount(String accountName) throws NdexException {
		try {
			ODocument accountDoc = this.getRecordByAccountName(accountName, NdexClasses.Account);
			
		} catch (ObjectNotFoundException e) {
			return new ArrayList<NetworkSummary>(0);
		}
		
		
	} */ 
}
