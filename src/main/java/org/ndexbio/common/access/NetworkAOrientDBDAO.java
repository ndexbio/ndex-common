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

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.common.solr.SingleNetworkSolrIdxManager;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSourceFormat;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.SimplePathQuery;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class NetworkAOrientDBDAO extends NdexAOrientDBDAO  {

	private static NetworkAOrientDBDAO INSTANCE = null;
	
	public static final String resultOverLimitMsg = "Result set is too large for this query.";
	
	private static final Logger _logger = Logger.getLogger(NetworkAOrientDBDAO.class.getName());
	
	protected NetworkAOrientDBDAO() {
		super();
	}
	
	public static synchronized NetworkAOrientDBDAO getInstance() {
	      if(INSTANCE == null) {
	         INSTANCE = new NetworkAOrientDBDAO();
	      }
	      return INSTANCE;
	   }


	public PropertyGraphNetwork queryForSubPropertyGraphNetworkV2(final String networkId,
			final SimplePathQuery parameters
			//,final int skipBlocks,	final int blockSize
			) throws IllegalArgumentException, NdexException {

		Network n = queryForSubnetworkV2(networkId, parameters);
		return new PropertyGraphNetwork ( n);
	}

	
	public Network queryForSubnetworkV2(final String networkId,
			final SimplePathQuery parameters
			) throws IllegalArgumentException, NdexException {

		try (NetworkDocDAO dao = new NetworkDocDAO()){
			ODocument networkDoc = dao.getNetworkDocByUUIDString(networkId);
						
		//	final long startTime = System.currentTimeMillis();
			System.out.println("Starting subnetworkQuery ");
      
			Network result = new Network();
			Set<ORID> nodeRIDs = getNodeRidsFromSearchString(networkId, parameters.getSearchString(), parameters.getEdgeLimit()*2, 
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
			
	//		final long getSubnetworkTime = System.currentTimeMillis();
			System.out.println("  Network Nodes : " + result.getNodeCount());
			System.out.println("  Network Edges : " + result.getEdgeCount());
		//	System.out.println("Getting Network : " + (getSubnetworkTime - startTime) + "ms");
			
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
	 * @throws IOException 
	 * @throws SolrServerException 
	 */
	private static Set<ORID> getNodeRidsFromSearchString(String networkUUID,String searchString, int nodeLimit, boolean includeAliases, 
			NetworkDocDAO networkdao, Network resultNetwork) throws NdexException, SolrServerException, IOException {
		
		Set<ORID> result = new TreeSet<>();

		SingleNetworkSolrIdxManager mgr = new SingleNetworkSolrIdxManager(networkUUID);
		for ( SolrDocument d : mgr.getNodeIdsByQuery(searchString,nodeLimit)) {
			Object id = d.get(SingleNetworkSolrIdxManager.ID);
			ODocument nodeDoc = networkdao.getDocumentByElementId(NdexClasses.Node, Long.parseLong((String)id));
			result.add(nodeDoc.getIdentity());
		    Node n = networkdao.getNode(nodeDoc, resultNetwork);
            resultNetwork.getNodes().put(n.getId(), n);
		}
		
		return result;
	}


	private void  traverseNeighborHood(Set<ORID> nodeRIDs, int searchDepth, NetworkDocDAO dao, Network result, int edgeLimit ,Set<ORID> traversedEdges) throws NdexException {
		if ( searchDepth <= 0 ) return ;
		
		Set<ORID> newNodes1 = getNeighborHood(nodeRIDs, dao,result, edgeLimit, true,traversedEdges);  // upstream
		Set<ORID> newNodes2 = getNeighborHood(nodeRIDs, dao,result, edgeLimit, false,traversedEdges);  // downstream;
		
        newNodes1.addAll(newNodes2);
		traverseNeighborHood(newNodes1, searchDepth-1, dao,result, edgeLimit, traversedEdges);
	}
	
	
	private static Set<ORID> getNeighborHood(Set<ORID> nodeRIDs,NetworkDocDAO dao, Network resultNetwork, int edgeLimit ,boolean upstream, Set<ORID> traversedEdges) throws NdexException {
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
							         edgeDoc.field("out_"+ NdexClasses.Edge_E_object) );
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

	
}
