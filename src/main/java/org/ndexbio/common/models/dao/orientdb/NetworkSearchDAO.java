package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.SimpleNetworkQuery;
import org.ndexbio.model.object.User;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
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
	
	
	public Collection<NetworkSummary> findNetworks(SimpleNetworkQuery simpleNetworkQuery, int skip, int top, User loggedInUser) 
			throws NdexException, IllegalArgumentException {
		
		Preconditions.checkArgument(null != simpleNetworkQuery, 
				"A query is required");

		// treat "*" and "" the same way
		if (simpleNetworkQuery.getSearchString().equals("*") )
			simpleNetworkQuery.setSearchString("");

		ORID userRID = null;
		if( loggedInUser != null ) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_accountName);
				OIdentifiable user = (OIdentifiable) accountNameIdx.get( loggedInUser.getAccountName() ); // account to traverse by
				userRID = user.getIdentity();
		}
		
		
		
		if ( simpleNetworkQuery.getSearchString().equals(""))
			return findNetworksV1 (simpleNetworkQuery,skip, top, userRID);
		
		return findNetworksV2 (simpleNetworkQuery,skip, top, userRID);
	}
	
	private List<NetworkSummary> findNetworksV1(SimpleNetworkQuery simpleNetworkQuery, int skip, int top, ORID userORID) 
			throws NdexException, IllegalArgumentException {
		
		// get RID for logged in user if any
		String userRID = userORID == null ? "#0:0" : userORID.toString();
		
		String traversePermission;
		int traverseDepth;
		OSQLSynchQuery<ODocument> query;
		Iterable<ODocument> networks;
		final List<NetworkSummary> foundNetworks = new ArrayList<>();
		final int startIndex = skip * top;
		

		if( simpleNetworkQuery.getPermission() == null ) 
			traversePermission = "out_admin, out_write, out_read";
		else 
			traversePermission = "out_"+simpleNetworkQuery.getPermission().name().toLowerCase();
		
		if( simpleNetworkQuery.getIncludeGroups())
			traverseDepth = 2;
		else
			traverseDepth = 1;
		
		String searchStr = Helper.escapeOrientDBSQL(simpleNetworkQuery.getSearchString().toLowerCase());
		
		try {
			
			// search across a traversal of an accounts networks if accountName is specified
			if(!Strings.isNullOrEmpty(simpleNetworkQuery.getAccountName())) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
				OIdentifiable nAccount = (OIdentifiable) accountNameIdx.get(simpleNetworkQuery.getAccountName()); // account to traverse by
				
				if(nAccount == null) 
					throw new NdexException("Invalid accountName to filter by");
				
				String traverseRID = nAccount.getIdentity().toString();
				query = new OSQLSynchQuery<>(
			  			"SELECT  FROM"
			  			+ " (TRAVERSE out_groupadmin, out_member, "+traversePermission+" FROM"
			  				+ " " + traverseRID
			  				+ "  WHILE $depth <= "+traverseDepth+" )"
			  			+ " WHERE name.toLowerCase() LIKE '%" + searchStr +"%'"
			  			+ " AND @class = '"+ NdexClasses.Network +"'"
			  			+ " AND isComplete = true"
			 			+ " AND ( visibility <> 'PRIVATE'"
						+ " OR in() contains "+userRID
						+ " OR in().in() contains "+userRID+" )"
			 			+ " ORDER BY "+ NdexClasses.ExternalObj_cTime +" DESC " + " SKIP " + startIndex
			 			+ " LIMIT " + top);
				
				networks = this.db.command(query).execute();
				
				for (final ODocument network : networks) {
					foundNetworks.add(NetworkDAO.getNetworkSummary(network));
				}
					
				return foundNetworks;
			    
			} 
			
			query = new OSQLSynchQuery<>(
			  			"SELECT FROM " + NdexClasses.Network
			  			+ " WHERE isComplete = true "
			  			
			  			+ (searchStr.equals("") ? " " : 
			  			  "AND name.toLowerCase() LIKE '%"+ searchStr +"%'")
			  			  
			 			+ " AND ( visibility <> 'PRIVATE'"
						+ " OR in() contains "+userRID
						+ " OR in().in() contains "+userRID+" )"
			 			+ " ORDER BY "+ NdexClasses.ExternalObj_cTime +" DESC " + " SKIP " + startIndex
			 			+ " LIMIT " + top);
				
			networks = this.db.command(query).execute();
			    
				
			for (final ODocument network : networks) {
					foundNetworks.add(NetworkDAO.getNetworkSummary(network));
			}
					
			return foundNetworks;
			
			
		} catch (Exception e) {
			
			logger.severe("An error occured while attempting to query the database for networks");
			throw new NdexException("Failed to search for networks.\n" + e.getMessage());
			
		} 
	}

	private Collection<NetworkSummary> findNetworksV2(SimpleNetworkQuery simpleNetworkQuery, int skip, int top, ORID userRID) 
			throws IllegalArgumentException {
		
		Collection<NetworkSummary> resultList =  new ArrayList<>(top);
		
		TreeSet<ORID> resultIDSet = new TreeSet<> ();
		
		int counter = 0;
		
		ORID adminUserRID = null;
		if( simpleNetworkQuery.getAccountName() != null ) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_accountName);
				OIdentifiable user = (OIdentifiable) accountNameIdx.get( simpleNetworkQuery.getAccountName()  ); // account to traverse by
				adminUserRID = user.getIdentity();
		}
		
		
		// search network first.
		OIndex<?> networkIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_network_name_desc);
		
		String searchStr = simpleNetworkQuery.getSearchString();

		Collection<OIdentifiable> networkIds =  (Collection<OIdentifiable>) networkIdx.get( searchStr); 

		for ( OIdentifiable dId : networkIds) {
			ODocument doc = dId.getRecord();
            if (isSearchable(doc, userRID, simpleNetworkQuery.getPermission(), adminUserRID)) {
					resultIDSet.add(dId.getIdentity());
					if ( counter >= skip) {
						NetworkSummary network =NetworkDAO.getNetworkSummary(doc); 
						if ( network.getIsComplete())
							resultList .add(network);
					}
					counter ++;
					if ( resultList.size()>= top)
						return resultList;
			}
		}
		
		// search baseterms
		OIndex<?> basetermIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_BTerm_name);
		
		Collection<OIdentifiable> bTermIds =  (Collection<OIdentifiable>) basetermIdx.get( searchStr); 

		
		  for (OIdentifiable networkRec : new OTraverse()
		  				.field(	"in_" + NdexClasses.Network_E_BaseTerms)
						.target(bTermIds)
						.predicate( new OSQLPredicate("$depth <= 1"))) {
			   
			  ORID id = networkRec.getIdentity();
			  if ( ! resultIDSet.contains(id)) {
				
				  ODocument doc = (ODocument) networkRec;
			    
				   if ( doc.getClassName().equals(NdexClasses.Network)) {
		            if (isSearchable(doc, userRID, simpleNetworkQuery.getPermission(), adminUserRID)) {
							resultIDSet.add(id);
							if ( counter >= skip) {
								NetworkSummary network =NetworkDAO.getNetworkSummary(doc); 
								if ( network.getIsComplete())
									resultList .add(network);
							}
							counter ++;
							if ( resultList.size()>= top)
								return resultList;
					}
				   }
			   }
		  }

		  // search node.name
 		  OIndex<?> nodeNameIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_node_name);
			
 		  Collection<OIdentifiable> nodeIds =  (Collection<OIdentifiable>) nodeNameIdx.get( searchStr); 

		  for (OIdentifiable networkRec : new OTraverse()
			  				.field(	"in_" + NdexClasses.Network_E_Nodes)
							.target(nodeIds)
							.predicate( new OSQLPredicate("$depth <= 1"))) {
				   
			  ORID id = networkRec.getIdentity();
			  if ( ! resultIDSet.contains(id)) {
					
			  ODocument doc = (ODocument) networkRec;
				    
				   if ( doc.getClassName().equals(NdexClasses.Network)) {
			            if (isSearchable(doc, userRID, simpleNetworkQuery.getPermission(), adminUserRID)) {
								resultIDSet.add(id);
								if ( counter >= skip) {
									NetworkSummary network =NetworkDAO.getNetworkSummary(doc); 
									if ( network.getIsComplete())
										resultList .add(network);
								}
								counter ++;
								if ( resultList.size()>= top)
									return resultList;
						}
				   }
			   }
			}
  
		
		return resultList;
	}
	
	
	private static boolean isSearchable(ODocument networkDoc, ORID userRID, Permissions permission, ORID adminUserRID) {
		
		VisibilityType visibility = VisibilityType.valueOf((String)networkDoc.field(NdexClasses.Network_P_visibility));
		if ( visibility != VisibilityType.PRIVATE || networkIsSearchableByAccount(networkDoc,userRID) ) {
			if ( adminUserRID == null || 
					networkHasPermissionOnAccount(networkDoc, permission,adminUserRID)) {
				return true;
			}
		}
		
		return false;

	 }
	
	  private static boolean networkIsSearchableByAccount(ODocument networkDoc, 
				ORID userORID) {
	
		  for (OIdentifiable reifiedTRec : new OTraverse()
					.fields(	"in_" + NdexClasses.account_E_canEdit,
							"in_" + NdexClasses.account_E_canRead,
							"in_" + NdexClasses.E_admin,
							"in_" + NdexClasses.GRP_E_admin, 
							"in_" + NdexClasses.GRP_E_member)
					.target(networkDoc)
					.predicate( new OSQLPredicate("$depth <= 2"))) {

			  if ( reifiedTRec.getIdentity().equals(userORID)) 
				  return true;
		  }
		  return false;
		  
	    }


	  private static boolean networkHasPermissionOnAccount(ODocument networkDoc, Permissions p, 
				ORID userORID) {
	
		  //if ( userORID == null) return true;
		  
		  for (OIdentifiable reifiedTRec : new OTraverse()
					.field(	"in_" + p.name().toLowerCase())
					.target(networkDoc)
					.predicate( new OSQLPredicate("$depth <= 1"))) {

			  if ( reifiedTRec.getIdentity().equals(userORID)) 
				  return true;
		  }
		  return false;
		  
	    }


}
