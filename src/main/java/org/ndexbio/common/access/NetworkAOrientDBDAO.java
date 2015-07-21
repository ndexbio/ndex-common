/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
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
package org.ndexbio.common.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.SimplePathQuery;

import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NetworkAOrientDBDAO extends NdexAOrientDBDAO  {

	private static NetworkAOrientDBDAO INSTANCE = null;
	
	private static final String resultOverLimitMsg = "Result set is too large for this query.";
	
	private static final Logger _logger = Logger.getLogger(NetworkAOrientDBDAO.class.getName());
	
//	private Class stringlist = new ArrayList<String>().getClass();

	protected NetworkAOrientDBDAO() {
		super();
	}
	
	public static synchronized NetworkAOrientDBDAO getInstance() {
	      if(INSTANCE == null) {
	         INSTANCE = new NetworkAOrientDBDAO();
	      }
	      return INSTANCE;
	   }



	/* (non-Javadoc)
	 * @see org.ndexbio.common.access.NetworkADAO#queryForSubnetwork(java.lang.String, org.ndexbio.common.models.object.NetworkQueryParameters, int, int)
	 */
/*	
	public Network queryForSubnetwork(final String networkId,
			final SimplePathQuery parameters
		//	,final int skipBlocks, final int blockSize
			) throws IllegalArgumentException, NdexException {

		try {
			setup();
			NetworkDAO dao = new NetworkDAO(this._ndexDatabase);
			ODocument networkDoc = dao.getNetworkDocByUUIDString(networkId);
			
			final ORID networkRid = networkDoc.getIdentity();
			
			final long startTime = System.currentTimeMillis();
			System.out.println("Starting subnetworkQuery ");
			Set<ORID> edgeRids = queryForEdgeRids(
					networkRid, 
					parameters 
					//,skipBlocks,blockSize
					
					);
			final long getEdgesTime = System.currentTimeMillis();
			Network network =  getSubnetworkByEdgeRids( edgeRids, networkDoc);
			final long getSubnetworkTime = System.currentTimeMillis();
			System.out.println("  Network Nodes : " + network.getNodes().size());
			System.out.println("  Network Edges : " + network.getEdges().values().size());
			System.out.println("  Getting Edges : " + (getEdgesTime - startTime));
			System.out.println("Getting Network : " + (getSubnetworkTime - getEdgesTime));
			return network;
		} catch (Exception e) {
			e.printStackTrace();
			throw new NdexException("Error in queryForSubnetwork: " + e.getLocalizedMessage());
		} finally {
			teardown();
		}
	}
*/
	public PropertyGraphNetwork queryForSubPropertyGraphNetwork(final String networkId,
			final SimplePathQuery parameters
			//,final int skipBlocks,	final int blockSize
			) throws IllegalArgumentException, NdexException {

		try {
			setup();
			NetworkDAO dao = new NetworkDAO(this._ndexDatabase);
			ODocument networkDoc = dao.getNetworkDocByUUIDString(networkId);
			
			final ORID networkRid = networkDoc.getIdentity();
			
			final long startTime = System.currentTimeMillis();
			System.out.println("Starting subnetworkQuery ");
			Set<ORID> edgeRids = queryForEdgeRids(
					networkRid, 
					parameters 
					//,skipBlocks,blockSize
					);
			final long getEdgesTime = System.currentTimeMillis();
			PropertyGraphNetwork network =  getSubPropertyGraphNetworkByEdgeRids( edgeRids);
			final long getSubnetworkTime = System.currentTimeMillis();
			System.out.println("     Network Nodes : " + network.getNodes().size());
			System.out.println("     Network Edges : " + network.getEdges().size());
			System.out.println("  Getting Edges MS : " + (getEdgesTime - startTime));
			System.out.println("Getting Network MS : " + (getSubnetworkTime - getEdgesTime));
			return network;
		} catch (Exception e) {
			e.printStackTrace();
			throw new NdexException("Error in queryForSubnetwork: " + e.getLocalizedMessage());
		} finally {
			teardown();
		}
	}

	public PropertyGraphNetwork queryForSubPropertyGraphNetworkV2(final String networkId,
			final SimplePathQuery parameters
			//,final int skipBlocks,	final int blockSize
			) throws IllegalArgumentException, NdexException {

		Network n = queryForSubnetworkV2(networkId, parameters);
		return new PropertyGraphNetwork ( n);
	}


	private Set<ORID> queryForEdgeRids( ORID networkRid, SimplePathQuery parameters
		//	,int skipBlocks,int blockSize
			) throws NdexException {
		
		// TODO validate parameters
			String searchString = parameters.getSearchString();

			Collection<ORID> sourceNodeRids = getNodeRidsFromTermNames(networkRid,
					searchString,
					parameters.getEdgeLimit(),
					true); // change to
																// nodes
			List<ORID> includedPredicateRids = new ArrayList<>();
			int maxDepth = parameters.getSearchDepth();
			
			System.out.println("-----------------------------------------\n");
			System.out.println(" Neighborhood Query ");
			System.out.println("search string: " + searchString);
			System.out.println("-----------------------------------------\n");

			return edgeIDsByTraversalFromSourceNode(networkRid, sourceNodeRids,
					includedPredicateRids, maxDepth, parameters.getEdgeLimit());
	}

	private Collection<ORID> getBaseTermRidsFromNames(ORID networkRid, int edgeLimit,
			String searchString) throws NdexException {

		Set<ORID> result = new TreeSet<>();

		OIndex<?> basetermIdx = _ndexDatabase.getMetadata().getIndexManager().getIndex(NdexClasses.Index_BTerm_name);
		
		
		for (OIdentifiable termOID : (Collection<OIdentifiable>) basetermIdx.get( searchString)) {
			ODocument termDoc = (ODocument) termOID.getRecord();
			OIdentifiable tnId = termDoc.field("in_" + NdexClasses.Network_E_BaseTerms);
			if ( tnId != null && tnId.getIdentity().equals(networkRid)) {
				result.add(termOID.getIdentity());
				if (edgeLimit >0 && result.size() > edgeLimit)
					throw new NdexException(resultOverLimitMsg);
			}
		}
		
		return result;
	}
	

	private Set<ORID> getNodeRidsFromTermNames(ORID networkRID,String searchString, int edgeLimit, boolean includeAliases ) throws NdexException {
		
		Set<ORID> result = new TreeSet<>();
		
	  try {	
		OTraverse traverser = new OTraverse()
  			.fields("in_" + NdexClasses.FunctionTerm_E_paramter, "in_" + NdexClasses.Node_E_represents)
  			.target(getBaseTermRidsFromNames(networkRID, edgeLimit, searchString));
		if ( includeAliases)
			traverser.field("in_" + NdexClasses.Node_E_alias);
		
		for (OIdentifiable nodeRec : traverser) {
 
			ODocument doc = (ODocument) nodeRec;
  
			if ( doc.getClassName().equals(NdexClasses.Node)) { 
				result.add(nodeRec.getIdentity());
				if ( edgeLimit >0 && result.size()> edgeLimit) { 
					throw new NdexException(resultOverLimitMsg);
				}

			}
		}
		
	    OIndex<?> nodeNameIdx = _ndexDatabase.getMetadata().getIndexManager().getIndex(NdexClasses.Index_node_name);
				
	 	for (OIdentifiable termOID : (Collection<OIdentifiable>) nodeNameIdx.get( searchString)) {
			ODocument termDoc = (ODocument) termOID.getRecord();
			OIdentifiable tnId = termDoc.field("in_" + NdexClasses.Network_E_Nodes);
			if ( tnId != null && tnId.getIdentity().equals(networkRID)) {
				result.add(termOID.getIdentity());
				if ( edgeLimit >0 && result.size() > edgeLimit) { 
					throw new NdexException(resultOverLimitMsg);
				}
			}
		}

		return result;
	  } catch (OIndexException e1) {
		  throw new NdexException ("Invalid search string. " + e1.getCause().getMessage());
	  }
	}

	
	
	private Collection<ORID> getBaseTermRidsFromNamesV2(ORID networkRid, int nodeLimit,
			String searchString,NetworkDocDAO networkdao) throws NdexException {

		Set<ORID> result = new TreeSet<>();

		OIndex<?> basetermIdx =  networkdao.getDBConnection().getMetadata().getIndexManager().getIndex(NdexClasses.Index_BTerm_name);
		
		for (OIdentifiable termOID : (Collection<OIdentifiable>) basetermIdx.get( searchString)) {
			ODocument termDoc = (ODocument) termOID.getRecord();
			OIdentifiable tnId = termDoc.field("in_" + NdexClasses.Network_E_BaseTerms);
			if ( tnId != null && tnId.getIdentity().equals(networkRid)) {
				result.add(termOID.getIdentity());
				if (nodeLimit >0 && result.size() > nodeLimit)
					throw new NdexException(resultOverLimitMsg);
			}
		}
		
		return result;
	}

	
	public Network queryForSubnetworkV2(final String networkId,
			final SimplePathQuery parameters
			) throws IllegalArgumentException, NdexException {

		try (NetworkDocDAO dao = new NetworkDocDAO()){
			ODocument networkDoc = dao.getNetworkDocByUUIDString(networkId);
			
			final ORID networkRid = networkDoc.getIdentity();
			
			final long startTime = System.currentTimeMillis();
			System.out.println("Starting subnetworkQuery ");
      
			Network result = new Network();
			Set<ORID> nodeRIDs = getNodeRidsFromSearchString(networkRid, parameters.getSearchString(), parameters.getEdgeLimit()*2, 
					    true, dao, result);
			
			Set<ORID> traversedEdges = new TreeSet<>();
			
			traverseNeighborHood(nodeRIDs, parameters.getSearchDepth(), dao, result, parameters.getEdgeLimit(),traversedEdges );

			
	        // copy the source format
	        NetworkSourceFormat fmt = Helper.getSourceFormatFromNetworkDoc(networkDoc);
	        if ( fmt!=null) {
	        	result.getProperties().add(new NdexPropertyValuePair(NdexClasses.Network_P_source_format, fmt.toString()));
	        }
	        	

			result.setEdgeCount(result.getEdges().size());
			result.setNodeCount(result.getNodes().size());
			
			final long getSubnetworkTime = System.currentTimeMillis();
			System.out.println("  Network Nodes : " + result.getNodeCount());
			System.out.println("  Network Edges : " + result.getEdgeCount());
			System.out.println("Getting Network : " + (getSubnetworkTime - startTime));
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new NdexException("Error in queryForSubnetwork: " + e.getLocalizedMessage());
		} 
	}
	
	
	/**
	 * Get a set of ORIDs of node from search term. This will be the start point of edge query.
	 * 
	 * @param networkRID
	 * @param searchString
	 * @param nodeLimit   if nodeLimit > 0 this query returns up to nodeLimit number of nodes.
	 * @param includeAliases
	 * @param networkdao
	 * @return
	 * @throws NdexException
	 */
	private Set<ORID> getNodeRidsFromSearchString(ORID networkRID,String searchString, int nodeLimit, boolean includeAliases, 
			NetworkDocDAO networkdao ,Network resultNetwork) throws NdexException {
		
		Set<ORID> result = new TreeSet<>();
		
	  try {	
		OTraverse traverser = new OTraverse()
  			.fields("in_" + NdexClasses.FunctionTerm_E_paramter, "in_" + NdexClasses.Node_E_represents)
  			.target(getBaseTermRidsFromNamesV2(networkRID, nodeLimit, searchString, networkdao));
		if ( includeAliases)
			traverser.field("in_" + NdexClasses.Node_E_alias);
		
		for (OIdentifiable nodeRec : traverser) {
 
			ODocument doc = (ODocument) nodeRec;
  
			if ( doc.getClassName().equals(NdexClasses.Node)) {
				ORID rid = nodeRec.getIdentity();
				if ( !result.contains(rid) ) {
					result.add(rid);
                    Node n = networkdao.getNode(doc, resultNetwork);
                    resultNetwork.getNodes().put(n.getId(), n);
				}
				if ( nodeLimit >0 && result.size()> nodeLimit) {
					break;
				}
			}
		}
		
	    OIndex<?> nodeNameIdx = networkdao.getDBConnection().getMetadata().getIndexManager().getIndex(NdexClasses.Index_node_name);
				
	 	for (OIdentifiable termOID : (Collection<OIdentifiable>) nodeNameIdx.get( searchString)) {
			ODocument nodeDoc = (ODocument) termOID.getRecord();
			OIdentifiable tnId = nodeDoc.field("in_" + NdexClasses.Network_E_Nodes);
			if ( tnId != null && tnId.getIdentity().equals(networkRID)) {
				
				if ( !result.contains(nodeDoc.getIdentity())) {
					result.add(nodeDoc.getIdentity());
					Node n = networkdao.getNode(nodeDoc, resultNetwork);
	                resultNetwork.getNodes().put(n.getId(), n);
				}
				if ( nodeLimit >0 && result.size() > nodeLimit) { 
					throw new NdexException(resultOverLimitMsg);
				}
			}
		}

		return result;
	  } catch (OIndexException e1) {
		  throw new NdexException ("Invalid search string. " + e1.getCause().getMessage());
	  }
	}


	private void  traverseNeighborHood(Set<ORID> nodeRIDs, int searchDepth, NetworkDocDAO dao, Network result, int edgeLimit ,Set<ORID> traversedEdges) throws NdexException {
		if ( searchDepth <= 0 ) return ;
		
		Set<ORID> newNodes1 = getNeighborHood(nodeRIDs, dao,result, edgeLimit, true,traversedEdges);  // upstream
		Set<ORID> newNodes2 = getNeighborHood(nodeRIDs, dao,result, edgeLimit, false,traversedEdges);  // downstream;
		
        newNodes1.addAll(newNodes2);
		traverseNeighborHood(newNodes1, searchDepth-1, dao,result, edgeLimit, traversedEdges);
	}
	
	
	private Set<ORID> getNeighborHood(Set<ORID> nodeRIDs,NetworkDocDAO dao, Network resultNetwork, int edgeLimit ,boolean upstream, Set<ORID> traversedEdges) throws NdexException {
		Set<ORID> newNodes = new TreeSet<>();
		for ( ORID nodeRID: nodeRIDs) {
			ODocument nodeDoc = new ODocument(nodeRID);
			for ( ODocument edgeDoc :
				       ( upstream ? 
				    		   Helper.getDocumentLinks( nodeDoc, "in_", NdexClasses.Edge_E_object) 
				    		 : Helper.getDocumentLinks( nodeDoc, "out_", NdexClasses.Edge_E_subject) )) {
				if( !traversedEdges.contains(edgeDoc.getIdentity())) { //new edge found
					traversedEdges.add(edgeDoc.getIdentity());
					Edge e = dao.getEdgeFromDocument(edgeDoc, resultNetwork);
					resultNetwork.getEdges().put(e.getId(), e);
					ODocument newNodeDoc = (ODocument)(upstream ? 
							         edgeDoc.field("in_" + NdexClasses.Edge_E_subject) : 
							         edgeDoc.field("out_", NdexClasses.Edge_E_object) );
					ORID newNodeRID = newNodeDoc.getIdentity();
					if ( !newNodes.contains(newNodeRID)) {
						newNodes.add(newNodeRID);
					}
					
					if(edgeLimit>0 && traversedEdges.size()> edgeLimit)
						throw new NdexException(resultOverLimitMsg);

				}
				
			}
			
			
		}
		return newNodes;
	}

	private Network getSubnetworkByEdgeRids( Set<ORID> edgeRids, ODocument networkDoc) throws Exception {
		
	    Network network = new Network(edgeRids.size());  //result holder
	    
	    NetworkDAO dao = new NetworkDAO (this._ndexDatabase);

	    // get namespaces from network
        for ( Namespace ns : dao.getNamespacesFromNetworkDoc(networkDoc, network)) {
        	network.getNamespaces().put(ns.getId(),ns);
        }


        for (ORID edgeId : edgeRids) {
            ODocument doc = new ODocument(edgeId);
            Edge e = dao.getEdgeFromDocument(doc,network);
            network.getEdges().put(e.getId(), e);
        }
        
        // copy the source format
        NetworkSourceFormat fmt = Helper.getSourceFormatFromNetworkDoc(networkDoc);
        if ( fmt!=null)
        	network.getProperties().add(new NdexPropertyValuePair(NdexClasses.Network_P_source_format, fmt.toString()));
        
        network.setNodeCount(network.getNodes().size());
        network.setEdgeCount(network.getEdges().size());
		 return network; 
	}

	private PropertyGraphNetwork getSubPropertyGraphNetworkByEdgeRids(Set<ORID> edgeRids) throws Exception {
		
	    PropertyGraphNetwork network = new PropertyGraphNetwork();  //result holder
    
	    TreeMap<ORID, String> termStringMap = new TreeMap<> ();
	    
	    NetworkDAO dao = new NetworkDAO (this._ndexDatabase);
        for (ORID edgeId : edgeRids) {
            ODocument doc = new ODocument(edgeId);
            dao.fetchPropertyGraphEdgeToNetwork(doc, network, termStringMap);
        }
        
		 return network; 
	}

	// This is the basis for a neighborhood query
	// post 1.0 
	private Set<ORID> edgeIDsByTraversalFromSourceNode(ORID networkRid,
			Collection<ORID> sourceNodeRids, List<ORID> includedPredicateRids,
			int maxDepth, int edgeLimit) throws NdexException {

		Set<ORID> foundEdgeRids = new HashSet<>();
		Set<ORID> foundNodeRids = new HashSet<>();
			
		searchFromNode(
					sourceNodeRids, 
					includedPredicateRids, 
					1,
					maxDepth,
					foundEdgeRids, 
					foundNodeRids, edgeLimit);
			
		return foundEdgeRids;
	}
	
	private void searchFromNode(
			Collection<ORID> sourceNodeRids,
			List<ORID> includedPredicateRids, 
			int depth,
			int maxDepth,
			Set<ORID> foundEdgeRids, 
			Set<ORID> foundNodeRids,
			int edgeLimit) throws NdexException {
		boolean belowMaxDepth = depth < maxDepth;
//		System.out.println("search from  : " + sourceNodeRids.size() + " nodes at depth " + depth + " starting with foundEdges " + foundEdgeRids.size());
		
		List<ORID[]> results = sourceSearchQuery(sourceNodeRids, includedPredicateRids, false);

		results.addAll(sourceSearchQuery(sourceNodeRids, includedPredicateRids, true));
		
		Set<ORID> nextSourceNodeRids = new HashSet<>();
		
		// for each edgeId + nodeId pair found:
		for (ORID[] result : results) {
			ORID nodeRid = result[1];
			ORID edgeRid = result[0];
			
			foundEdgeRids.add(edgeRid);
			if ( edgeLimit > 0 && foundEdgeRids.size() > edgeLimit)
				throw new NdexException(resultOverLimitMsg);
			
			if (!foundNodeRids.contains(nodeRid)) nextSourceNodeRids.add(nodeRid);
	
		}
		
		foundNodeRids.addAll(nextSourceNodeRids);
		if (belowMaxDepth){
			searchFromNode(nextSourceNodeRids, includedPredicateRids, depth + 1, maxDepth, foundEdgeRids, foundNodeRids, edgeLimit);
		}
	}

	private List<ORID[]> sourceSearchQuery(
			Collection<ORID> sourceNodeRids,
			List<ORID> includedPredicateRids,
			boolean upstream) throws NdexException {

		String query;
		String sourceNodeCsv = ridsToCsv(sourceNodeRids);
		
		if (upstream){
			query= "select $path from "
				+ "(traverse in_edgeObject, in_edgeSubject from [ "
				+ sourceNodeCsv
				+ " ] while $depth < 3) where $depth = 2";
		} else {
			query= "select $path from "
					+ "(traverse out_edgeSubject, out_edgeObject from [ "
					+ sourceNodeCsv 
					+ " ] while $depth < 3) where $depth = 2";
		}
		
		System.out.println("query = " + query);

		List<ODocument> paths = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));

		List<ORID[]> results = new ArrayList<>();

		for (ODocument doc : paths) {
			String path = (String) doc.fieldValues()[0];
			String[] components = path.split("\\.");
			ORID edgeRid = stringToRid(components[1]);
			ORID nodeRid = stringToRid(components[2]);
			ORID[] pair = { edgeRid, nodeRid };
			results.add(pair);
			//System.out.println("edgeId = " + pair[0] + " nodeRid = " + pair[1]);
		}
		if (upstream){
			System.out.println("sourceQuery upstream results : " + results.size());
		} else {
			System.out.println("sourceQuery downstream results : " + results.size());
		}

		return results;
		
	}
	
	private List<ORID[]> neighborhoodSearchQuery(
			Set<ORID> sourceNodeRids,
			List<ORID> includedPredicateRids,
			boolean upstream) throws NdexException {

		String query;
		String sourceNodeCsv = ridsToCsv(sourceNodeRids);
		
		if (upstream){
			query= "select $path from "
				+ "(traverse in_edgeObject, in_edgeSubject from [ "
				+ sourceNodeCsv
				+ " ] while $depth < 3) where $depth = 2";
		} else {
			query= "select $path from "
					+ "(traverse out_edgeSubject, out_edgeObject from [ "
					+ sourceNodeCsv 
					+ " ] while $depth < 3) where $depth = 2";
		}
		
		System.out.println("query = " + query);

		List<ODocument> paths = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));

		List<ORID[]> results = new ArrayList<>();

		for (ODocument doc : paths) {
			String path = (String) doc.fieldValues()[0];
			String[] components = path.split("\\.");
			ORID edgeRid = stringToRid(components[1]);
			ORID nodeRid = stringToRid(components[2]);
			ORID[] pair = { edgeRid, nodeRid };
			results.add(pair);
			//System.out.println("edgeId = " + pair[0] + " nodeRid = " + pair[1]);
		}
		if (upstream){
			System.out.println("sourceQuery upstream results : " + results.size());
		} else {
			System.out.println("sourceQuery downstream results : " + results.size());
		}

		return results;
		
	}	
	

	
	
	/*
	 * 
	 * Common Utilities
	 */
/*
	private void checkNetworkExists(ORID networkRid, String networkId)
			throws ObjectNotFoundException {
		if (null == _ndexDatabase.load(networkRid)) {
			throw new ObjectNotFoundException("Network", networkId);
		}
	} 

	private static ORID checkAndConvertNetworkId(String networkId) {
		if (networkId == null || networkId.isEmpty())
			throw new NdexException("No network ID was specified.");
		return new ORecordId(networkId);
	}

	private static void checkBlockSize(int blockSize) {
		if (blockSize < 1)
			throw new NdexException(
					"Number of results to return is less than 1.");
	}
*/
	private static ORID stringToRid(String ridString) throws NdexException {
		final Matcher m = Pattern.compile("^.*(#\\d+:\\d+).*$").matcher(
				ridString.trim());

		if (m.matches())
			return new ORecordId(m.group(1));
		
		throw new NdexException(ridString
				+ " is not a valid RID.");

	}

	private static String ridsToCsv(Collection<ORID> rids) {
		String resultString = "";
		if (null == rids || rids.size() < 1)
			return resultString;
		for (final ORID rid : rids) {
			resultString += rid + ", ";
		}
		resultString = resultString.substring(0, resultString.length() - 2);
		return resultString;
	}

}
