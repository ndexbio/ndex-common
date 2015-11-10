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

import java.util.Collection;

import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSourceFormat;
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

public class NetworkAOrientDBDAO extends NdexAOrientDBDAO  {

	private static NetworkAOrientDBDAO INSTANCE = null;
	
	private static final String resultOverLimitMsg = "Result set is too large for this query.";
	
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




	private static Set<ORID> getNodeORIDsFromNames(ORID networkRid, int nodeLimit,
			String searchString,NetworkDocDAO networkdao) throws NdexException {

		Set<ORID> result = new TreeSet<>();

		OIndex<?> basetermIdx =  networkdao.getDBConnection().getMetadata().getIndexManager().getIndex(NdexClasses.Index_BTerm_name);
		OIndex<?> nodeRepIdx =  networkdao.getDBConnection().getMetadata().getIndexManager().getIndex(NdexClasses.Index_node_rep_id);
		
		for (OIdentifiable termOID : (Collection<OIdentifiable>) basetermIdx.get( searchString)) {
			ODocument termDoc = (ODocument) termOID.getRecord();
			OIdentifiable tnId = termDoc.field("in_" + NdexClasses.Network_E_BaseTerms);
			if ( tnId != null && tnId.getIdentity().equals(networkRid)) {
				Long bTermId = termDoc.field(NdexClasses.Element_ID);
				for (OIdentifiable nodeOID : (Collection<OIdentifiable>) nodeRepIdx.get(bTermId )) {
					if (nodeLimit >0 && result.size() > nodeLimit)
						throw new NdexException(resultOverLimitMsg);
					result.add(nodeOID.getIdentity());
				}
			}
		}
		
		return result;
	}

	
	private static Collection<ORID> getBaseTermRidsFromNamesV2(ORID networkRid, int nodeLimit,
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
		
		Set<ORID> result = getNodeORIDsFromNames(networkRID,  nodeLimit, searchString,networkdao);
		
	  try {	
		OTraverse traverser = new OTraverse()
  			.fields("in_" + NdexClasses.FunctionTerm_E_paramter)
  			.target(getBaseTermRidsFromNamesV2(networkRID, nodeLimit, searchString, networkdao));
		
		//TODO: commented out for now. Will put it back when we implment the Solr search.
/*		if ( includeAliases)
			traverser.field("in_" + NdexClasses.Node_E_alias); */
		
		for (OIdentifiable nodeRec : traverser) {
            ODocument doc = null;
			if  ( nodeRec instanceof ORecordId) 
			    doc = new ODocument((ORecordId)nodeRec);
			else if ( ((ODocument)nodeRec).getClassName().equals(NdexClasses.Node)) 
				doc = (ODocument)nodeRec;
			if ( doc !=null) {
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

}
