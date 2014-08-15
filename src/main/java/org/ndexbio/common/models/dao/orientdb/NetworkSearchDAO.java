package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.NdexException;
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

public class NetworkSearchDAO {
	
	private ODatabaseDocumentTx db;
	private static final Logger logger = Logger.getLogger(NetworkSearchDAO.class.getName());
	
	/**************************************************************************
	    * NetworkSearchDAO
	    * 
	    * @param db
	    *            Database instance from the Connection pool, should be opened
	    **************************************************************************/
	public NetworkSearchDAO (ODatabaseDocumentTx db) {
		this.db = db;
	}
	
	public List<NetworkSummary> findNetworks(SimpleNetworkQuery simpleNetworkQuery, int skip, int top, User loggedInUser) 
			throws NdexException, IllegalArgumentException {
		
		Preconditions.checkArgument(null != simpleNetworkQuery, 
				"A query is required");
		
		String userRID;
		OSQLSynchQuery<ODocument> query;
		Iterable<ODocument> networks;
		final List<NetworkSummary> foundNetworks = new ArrayList<NetworkSummary>();
		final int startIndex = skip * top;
		
		String userAccountName = "";
		String traversePermission = ""; //used to handle cases where permission was not specified. 
		
		if( simpleNetworkQuery.getPermission() == null ) {
			traversePermission = "out_admin, out_write, out_read";
		} else {
			traversePermission = "out_"+simpleNetworkQuery.getPermission().name().toLowerCase();
		}
		
		if(loggedInUser != null)
			userAccountName = loggedInUser.getAccountName();
		
		try {
			
			// get RID for logged in user if any
			if(!Strings.isNullOrEmpty(userAccountName)) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
				OIdentifiable user = (OIdentifiable) accountNameIdx.get(userAccountName); // account to traverse by
				userRID = user.getIdentity().toString();
			} else {
				userRID = "#0:0"; // should fix, to avoid parsing errors. 
			}
			
			// set permission to traverse by if any
			if( simpleNetworkQuery.getPermission() != null ) ;
				//traversePermission = "out_" + simpleNetworkQuery.getPermission().toString().toLowerCase();
			
			// search across a traversal of an accounts networks if accountName is specfiied
			if(!Strings.isNullOrEmpty(simpleNetworkQuery.getAccountName())) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
				OIdentifiable nAccount = (OIdentifiable) accountNameIdx.get(simpleNetworkQuery.getAccountName()); // account to traverse by
				
				if(nAccount == null) 
					throw new NdexException("Invalid accountName to filter by");
				
				String traverseRID = nAccount.getIdentity().toString();
				query = new OSQLSynchQuery<ODocument>(
			  			"SELECT FROM"
			  			+ " (TRAVERSE out_groupadmin, out_member, "+traversePermission+" FROM"
			  				+ " " + traverseRID
			  				+ "  WHILE $depth <= 2 )"
			  			+ " WHERE name.toLowerCase() LIKE '%" + simpleNetworkQuery.getSearchString().toLowerCase() +"%'"
			  			+ " AND @class = '"+ NdexClasses.Network +"'"
			 			+ " AND ( visibility <> 'PRIVATE'"
						+ " OR in() contains "+userRID
						+ " OR in().in() contains "+userRID+" )"
			 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
			 			+ " LIMIT " + top);
				
				networks = this.db.command(query).execute();
				
				//no results returned, eliminate search filter and return
				if( !networks.iterator().hasNext() ) {
					query = new OSQLSynchQuery<ODocument>(
				  			"SELECT FROM"
				  			+ " (TRAVERSE out_groupadmin, out_member, "+traversePermission+" FROM"
				  				+ " " + traverseRID
				  				+ "  WHILE $depth <= 2 )"
				  			+ " WHERE @class = '"+ NdexClasses.Network +"'"
				 			+ " AND ( visibility <> 'PRIVATE'"
							+ " OR in() contains "+userRID
							+ " OR in().in() contains "+userRID+" )"
				 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
				 			+ " LIMIT " + top);
					
					networks = this.db.command(query).execute();
				}
				
				for (final ODocument network : networks) {
					foundNetworks.add(NetworkDAO.getNetworkSummary(network));
				}
					
				return foundNetworks;
			    
			} else {
				query = new OSQLSynchQuery<ODocument>(
			  			"SELECT FROM " + NdexClasses.Network
			  			+ " WHERE name.toLowerCase() LIKE '%"+ simpleNetworkQuery.getSearchString().toLowerCase() +"%'"
			 			+ " AND ( visibility <> 'PRIVATE'"
						+ " OR in() contains "+userRID
						+ " OR in().in() contains "+userRID+" )"
			 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
			 			+ " LIMIT " + top);
				
				networks = this.db.command(query).execute();
			    
				// no results returned, return arbitrary selection.
				if( !networks.iterator().hasNext() ) 
					networks = db.browseClass(NdexClasses.Network).setLimit(top);
				
				for (final ODocument network : networks) {
					foundNetworks.add(NetworkDAO.getNetworkSummary(network));
				}
					
				return foundNetworks;
			}
			
		} catch (Exception e) {
			
			logger.severe("An error occured while attempting to query the database for networks");
			throw new NdexException("Failed to search for networks.\n" + e.getMessage());
			
		} 
	}
}
