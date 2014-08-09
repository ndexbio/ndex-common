package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.Permissions;
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
	
	//TODO make the search compatible to groups
	
	public List<NetworkSummary> findNetworks(SimpleNetworkQuery simpleNetworkQuery, int skip, int top, User loggedInUser) 
			throws NdexException, IllegalArgumentException {
		
		Preconditions.checkArgument(null != simpleNetworkQuery, 
				"A query is required");
		
		String userRID;
		OSQLSynchQuery<ODocument> query;
		Iterable<ODocument> networks;
		
		//permission labels
		String admin = Permissions.ADMIN.toString().toLowerCase();
		String write = Permissions.WRITE.toString().toLowerCase();
		String read = Permissions.READ.toString().toLowerCase();
		
		String userAccountName = "";
		if(loggedInUser != null)
			userAccountName = loggedInUser.getAccountName();
		
		final List<NetworkSummary> foundNetworks = new ArrayList<NetworkSummary>();
		
		final int startIndex = skip
				* top;
		try {
			
			if(!Strings.isNullOrEmpty(userAccountName)) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
				OIdentifiable user = (OIdentifiable) accountNameIdx.get(userAccountName); // account to traverse by
				userRID = user.getIdentity().toString();
			} else {
				userRID = "";
			}
			
			if(!Strings.isNullOrEmpty(simpleNetworkQuery.getAccountName())) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
				OIdentifiable nAccount = (OIdentifiable) accountNameIdx.get(simpleNetworkQuery.getAccountName()); // account to traverse by
				
				if(nAccount == null) 
					throw new NdexException("Invalid accountName to filter by");
				
				String traverseRID = nAccount.getIdentity().toString();
				query = new OSQLSynchQuery<ODocument>(
			  			"SELECT FROM"
			  			+ " (TRAVERSE "+ NdexClasses.User +".out_"+ admin +" FROM"
			  				+ " " + traverseRID
			  				+ "  WHILE $depth <=1)"
			  			+ " WHERE name.toLowerCase() LIKE '%" + simpleNetworkQuery.getSearchString().toLowerCase() +"%'"
			  			+ " AND visibility <> 'PRIVATE'"
			 			+ " AND @class = '"+ NdexClasses.Network +"'"
						+ " OR in_"+admin+" = '"+ userRID +"'"
						+ " OR in_"+write+" = '"+ userRID +"'"
						+ " OR in_"+read+" = '"+ userRID +"'"
			 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
			 			+ " LIMIT " + top);
				
				networks = this.db.command(query).execute();
				
				if( !networks.iterator().hasNext() ) {
					
					query = new OSQLSynchQuery<ODocument>(
				  			"SELECT FROM"
				  			+ " (TRAVERSE "+ NdexClasses.User +".out_"+ admin +" FROM"
				  				+ " " + traverseRID
				  				+ "  WHILE $depth <=1)"
				  			+ " WHERE visibility <> 'PRIVATE'"
				 			+ " AND @class = '"+ NdexClasses.Network +"'"
							+ " OR in_"+admin+" = '"+ userRID +"'"
							+ " OR in_"+write+" = '"+ userRID +"'"
							+ " OR in_"+read+" = '"+ userRID +"'"
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
			  			+ " AND visibility <> 'PRIVATE'"
						+ " OR in_"+admin+" = '"+ userRID +"'"
						+ " OR in_"+write+" = '"+ userRID +"'"
						+ " OR in_"+read+" = '"+ userRID +"'"
			 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
			 			+ " LIMIT " + top);
				
				networks = this.db.command(query).execute();
			    
				if( !networks.iterator().hasNext() ) {
					networks = db.browseClass(NdexClasses.Network).setLimit(top);
				}
				
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
