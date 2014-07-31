package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.SimpleNetworkQuery;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
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
	
	//TODO does not take into consideration membership edges of logged in user
	public List<NetworkSummary> findNetworks(SimpleNetworkQuery simpleNetworkQuery, int skip, int top) 
			throws NdexException, IllegalArgumentException {
		
		Preconditions.checkArgument(null != simpleNetworkQuery, 
				"A query is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(simpleNetworkQuery.getAccountName()) || !Strings.isNullOrEmpty(simpleNetworkQuery.getSearchString()), 
				"An account name or search string is required");
		
		final List<NetworkSummary> foundNetworks = new ArrayList<NetworkSummary>();
		
		final int startIndex = skip
				* top;
		
		if (simpleNetworkQuery.getSearchString().equals("*")) {
			simpleNetworkQuery.setSearchString("");
		}
		
		// probably not important for now but this may not be the best implementation. 
		
		String RID = "";
		
		OSQLSynchQuery<ODocument> userQuery = new OSQLSynchQuery<ODocument>("SELECT FROM "+ NdexClasses.User +" "
				+ "WHERE accountName = '"+simpleNetworkQuery.getAccountName() +"'");
		
		try {
			final List<ODocument> users = this.db.command(userQuery).execute();
			if(!users.isEmpty())
				RID = users.get(0).getIdentity().toString();
			
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
					"SELECT FROM "+ NdexClasses.Network
					+ " WHERE name.toLowerCase() LIKE '%"+ simpleNetworkQuery.getSearchString().toLowerCase() +"%'"
					+ " AND visibility <> 'PRIVATE'"
					+ " OR in_admin LIKE '%"+ RID +"%'"
					+ " OR in_write LIKE '%"+ RID +"%'"
					+ " OR in_read LIKE '%"+ RID +"%'"
					+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
					+ " LIMIT " + top);
			
			final List<ODocument> networks = this.db.command(query).execute();
			for (final ODocument network : networks) {
				foundNetworks.add(NetworkDAO.getNetworkSummary(network));
			}
			logger.info("initiated network search query with account: " + simpleNetworkQuery.getAccountName()
					+ " and searchStrings: " + simpleNetworkQuery.getSearchString());
			
			return foundNetworks;
			
		} catch (Exception e) {
			
			logger.severe("An error occured while attempting to query the database for networks");
			throw new NdexException("Failed to search for users.\n" + e.getMessage());
			
		} 
	}
}
