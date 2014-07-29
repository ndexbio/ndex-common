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
		
		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
			"SELECT FROM"
			+ " (TRAVERSE "+ NdexClasses.User +".out_admin FROM"
				+ " (SELECT FROM "+ NdexClasses.User
					+ " WHERE accountName.toLowerCase() LIKE '%"+ simpleNetworkQuery.getAccountName().toLowerCase() +"%')"
				+ "  WHILE $depth <=1)"
			+ " WHERE name.toLowerCase() LIKE '%"+ simpleNetworkQuery.getSearchString().toLowerCase() +"%'"
			+ " AND visibility <> 'PRIVATE'"
			+ " AND @class = '"+ NdexClasses.Network +"'"
			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
			+ " LIMIT " + top);
		
		try {
			
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
