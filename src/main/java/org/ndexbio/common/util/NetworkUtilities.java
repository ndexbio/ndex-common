/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
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
package org.ndexbio.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Support;

public class NetworkUtilities {

	
	public static Map<Long, List<Collection<Long>> > groupEdgesByCitationsFromNetwork (Network network) {
		
		Set<Long> processedNodes = new TreeSet<>();
		
		Map<Long, List<Collection<Long>>> citationEdgeNodeGroups = new TreeMap<> ();
		
		for ( Edge e : network.getEdges().values()) {
			if (e.getCitationIds() != null && !e.getCitationIds().isEmpty()) {
				for ( Long citationId : e.getCitationIds()) {
					List<Collection<Long>> edgeNodeGroup = citationEdgeNodeGroups.get(citationId);
					if ( edgeNodeGroup == null) {
						edgeNodeGroup = new ArrayList<>(2);
						
						edgeNodeGroup.add(new LinkedList<Long>());
						edgeNodeGroup.add(new LinkedList<Long>());
						citationEdgeNodeGroups.put(citationId, edgeNodeGroup);
					} 
					edgeNodeGroup.get(0).add(e.getId());
					processedNodes.add(e.getSubjectId());
					processedNodes.add(e.getObjectId());
				}
			}
		}
		
		// go through the nodes to pick up orphan nodes.
		for ( Node n : network.getNodes().values()) {
			if (n.getCitationIds() != null && !n.getCitationIds().isEmpty()
					&& !processedNodes.contains(n.getId())) {
				for ( Long citationId : n.getCitationIds()) {
					List<Collection<Long>> edgeNodeGroup = citationEdgeNodeGroups.get(citationId);
					if ( edgeNodeGroup == null) {
						edgeNodeGroup = new ArrayList<>(2);
						
						edgeNodeGroup.add(new LinkedList<Long>());
						edgeNodeGroup.add(new LinkedList<Long>());
						citationEdgeNodeGroups.put(citationId, edgeNodeGroup);
					} 
					edgeNodeGroup.get(1).add(n.getId());
				}
			}
		}
		return citationEdgeNodeGroups;
	}

	
	/**
	 * Create subn
	 * @param network
	 * @param edgeIds
	 * @param nodeIds
	 * @return
	 */
	public static Network getSubNetworkByEdgeNodeIds(Network network, Collection<Long> edgeIds, Collection<Long> nodeIds) {
		Network result = new Network();
		for ( Long edgeId : edgeIds) {
			addEdgeToNetwork(result, edgeId, network);
		}
		
		for (Long nodeId : nodeIds) {
			addNodeToNetwork(result,nodeId, network);
		}
		
    	result.setEdgeCount(result.getEdges().size());
    	result.setNodeCount(result.getNodes().size());

		return result;
	}

	private static void addEdgeToNetwork(Network result, Long edgeId, Network network) {
		if ( !result.getEdges().containsKey(edgeId)) {
			Edge edge = network.getEdges().get(edgeId);
			result.getEdges().put(edgeId, edge);
			for ( Long citationId : edge.getCitationIds()) {
				addCitation(result, citationId, network);
			}
			
			addNodeToNetwork(result, edge.getSubjectId(), network); 
			addNodeToNetwork(result, edge.getObjectId(), network);
			
			addBaseTermToNetwork(result, edge.getPredicateId(), network);
			for ( Long supportId : edge.getSupportIds()) {
				addSupportToNetwork( result,supportId, network);
			}
		}
	}
	
	private static void addCitation(Network result, Long citationId, Network network) {
		if ( !result.getCitations().containsKey(citationId) ) {
			Citation citation = network.getCitations().get(citationId);
			result.getCitations().put(citationId, citation);
		}
	}
	
	private static void addNodeToNetwork(Network result, Long nodeId, Network network) {
		if ( !result.getNodes().containsKey(nodeId)) {
			Node node = network.getNodes().get(nodeId);
			result.getNodes().put(nodeId, node);
			
			for ( Long aliasId: node.getAliases()) {
				addBaseTermToNetwork(result,aliasId,network);
			}
			for ( Long citationId : node.getCitationIds() ) {
				addCitation(result,citationId, network);
			}
			for (Long relateToId : node.getRelatedTerms()) {
				addBaseTermToNetwork(result,relateToId, network);
			}
			for( Long supportId : node.getSupportIds()) {
				addSupportToNetwork(result,supportId,network);
			}
			
			Long representsId = node.getRepresents();
			if ( representsId !=null ) {
				String termType = node.getRepresentsTermType();
				if ( termType.equals(NdexClasses.BaseTerm)) {
					addBaseTermToNetwork(result, representsId,network);
				} else if ( termType.equals(NdexClasses.ReifiedEdgeTerm)) {
					addReifiedEdgeTermToNetwork(result, representsId, network);
				} else {
					addFunctionTermToNetwork(result, representsId, network);
				}
			}
		}
	}
	
	private static void addBaseTermToNetwork(Network result, Long baseTermId, Network network) {
		if ( !result.getBaseTerms().containsKey(baseTermId)) {
			BaseTerm bt = network.getBaseTerms().get(baseTermId);
			result.getBaseTerms().put(baseTermId, bt);
			if (bt.getNamespaceId()>0) {
				addNamespaceToNetwork(result,bt.getNamespaceId(),network);
			}
		}
	}
	
	private static void addNamespaceToNetwork(Network result,Long namespaceId, Network network) {
		if ( !result.getNamespaces().containsKey(namespaceId)) {
			Namespace ns = network.getNamespaces().get(namespaceId);
			result.getNamespaces().put(namespaceId, ns);
		}
	}

	private static void addSupportToNetwork(Network result,Long supportId, Network network) {
		if ( !result.getSupports().containsKey(supportId)) {
			Support support = network.getSupports().get(supportId);
			result.getSupports().put(supportId, support);
			if ( support.getCitationId() >= 0 ) 
				addCitation(result, support.getCitationId(),network);
		}
	}
	
	private static void addReifiedEdgeTermToNetwork(Network result, Long reifiedEdgeTermId, Network network) {
		if ( ! result.getReifiedEdgeTerms().containsKey(reifiedEdgeTermId)) {
			ReifiedEdgeTerm rt = network.getReifiedEdgeTerms().get(reifiedEdgeTermId);
			result.getReifiedEdgeTerms().put(reifiedEdgeTermId, rt);
			addEdgeToNetwork(result, rt.getEdgeId(), network);
		}
	}

	private static void addFunctionTermToNetwork(Network result, Long functionTermId, Network network) {
		if ( ! result.getFunctionTerms().containsKey(functionTermId) ) {
			FunctionTerm ft = network.getFunctionTerms().get(functionTermId);
			result.getFunctionTerms().put(functionTermId, ft);
			addBaseTermToNetwork(result,ft.getFunctionTermId(), network);
			
			for ( Long parameterId : ft.getParameterIds() ) {
				if ( network.getBaseTerms().containsKey(parameterId)) {
					addBaseTermToNetwork(result,parameterId,network);
				} else if ( network.getFunctionTerms().containsKey(parameterId)) {
					addFunctionTermToNetwork(result,parameterId,network);
				} else 
					addReifiedEdgeTermToNetwork(result,parameterId,network);
			}
			
		}
	}

    // returns a subnetwork that contains all the orphan supports (supports that have no citation links) 
	// and other network elements that are related to these supports 

	public static Network getOrphanSupportsSubNetwork(Network network, Set<Long> excludingEdgeIds, Set<Long> excludingNodeIds) {
		Network result = new Network();
		for (Support support: network.getSupports().values()) {
			if ( support.getCitationId() < 0 ) {
				result.getSupports().put(support.getId(), support);
			}
		}
		
		for (Node node: network.getNodes().values() ) {
			if (  !node.getSupportIds().isEmpty() && !excludingNodeIds.contains(node.getId()) ) {
				boolean nodeOnlyHasOrphenSupports = true;
				for ( Long supportId :  node.getSupportIds()) {
					if ( !result.getSupports().containsKey(supportId)) {
						nodeOnlyHasOrphenSupports = false;
						break;
					}	
				}
				if ( nodeOnlyHasOrphenSupports ) 
					addNodeToNetwork(result,node.getId(), network);
			}
		}
		
		for (Edge edge: network.getEdges().values() ) {
			if ( !edge.getSupportIds().isEmpty() && !excludingEdgeIds.contains(edge.getId())) {
				boolean nodeOnlyHasOrphenSupports = true;
				for ( Long supportId :  edge.getSupportIds()) {
					if ( !result.getSupports().containsKey(supportId)) {
						nodeOnlyHasOrphenSupports = false;
						break;
					}	
				}
				if ( nodeOnlyHasOrphenSupports ) 
					addEdgeToNetwork(result,edge.getId(), network);
			}
		}

    	result.setEdgeCount(result.getEdges().size());
    	result.setNodeCount(result.getNodes().size());
		return result;
	}
	
	
	/**
	 * Get all the node and edges that has neither citations nor supports as a subnetwork from a given network. This is a 
	 * utitlity function for xbel export.
	 * @return
	 */
    public static Network getOrphanStatementsSubnetwork(Network network,Set<Long> excludingEdgeIds, Set<Long> excludingNodeIds) {
		Network result = new Network();
		
		for (Node node: network.getNodes().values() ) {
			if ( node.getSupportIds().isEmpty() && node.getCitationIds().isEmpty() 
					&& !excludingNodeIds.contains(node.getId())) {
					addNodeToNetwork(result,node.getId(), network);
			}
		}
		
		for (Edge edge: network.getEdges().values() ) {
			if ( edge.getSupportIds().isEmpty() && edge.getCitationIds().isEmpty() 
					&& !excludingEdgeIds.contains(edge.getId()) ) {
					addEdgeToNetwork(result,edge.getId(), network);
			}
		}

    	result.setEdgeCount(result.getEdges().size());
    	result.setNodeCount(result.getNodes().size());
		return result;
    	
    }


}
