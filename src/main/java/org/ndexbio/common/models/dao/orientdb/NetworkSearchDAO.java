package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.List;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.SimpleNetworkQuery;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NetworkSearchDAO {
	
	private ODatabaseDocumentTx db;
	
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
			throws NdexException {
		
		final List<NetworkSummary> foundNetworks = new ArrayList<NetworkSummary>();

		final int startIndex = skip
				* top;

		String query = "SELECT FROM " + NdexClasses.Network + " "
					+ "WHERE name.toLowerCase() LIKE '%"
					+ simpleNetworkQuery.getSearchString() + "%'"
					+ "  ORDER BY creation_date DESC " + " SKIP " + startIndex
					+ " LIMIT " + top;
		
		//eventually include accountName
		
		try {
			
			final List<ODocument> networks = this.db.query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument network : networks) {
				foundNetworks.add(NetworkDAO.getNetworkSummary(network));
				
			}
			return foundNetworks;
			
		} catch (Exception e) {
			
			throw new NdexException("Failed to search for users.\n" + e.getMessage());
			
		} 
	}
}
