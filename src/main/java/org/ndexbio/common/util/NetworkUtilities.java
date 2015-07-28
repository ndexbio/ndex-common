package org.ndexbio.common.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.exceptions.NdexException;
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

	
	public static Map<Long, Collection<Long>> groupEdgesByCitationsFromNetwork (Network network) {
		Map<Long, Collection<Long>> citationEdgeGroups = new TreeMap<> ();
		
		for ( Edge e : network.getEdges().values()) {
			if (e.getCitationIds() != null && !e.getCitationIds().isEmpty()) {
				for ( Long citationId : e.getCitationIds()) {
					Collection<Long> edgeGroup = citationEdgeGroups.get(citationId);
					if ( edgeGroup == null) {
						edgeGroup = new LinkedList<>();
						citationEdgeGroups.put(citationId, edgeGroup);
					} 
					edgeGroup.add(e.getId());	
				}
			}
		}
		
		return citationEdgeGroups;
	}

	
	public static Network getSubNetworkByEdgeIds(Network network, Collection<Long> edgeIds) {
		Network result = new Network();
		for ( Long edgeId : edgeIds) {
			addEdgeToNetwork(result, edgeId, network);
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

	public static Network getOrphanSupportsSubNetwork(Network network) {
		Network result = new Network();
		for (Support support: network.getSupports().values()) {
			if ( support.getCitationId() < 0 ) {
				result.getSupports().put(support.getId(), support);
			}
		}
		
		for (Node node: network.getNodes().values() ) {
			if ( !node.getSupportIds().isEmpty()) {
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
			if ( !edge.getSupportIds().isEmpty()) {
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
    public static Network getOrphanStatementsSubnetwork(Network network) {
		Network result = new Network();
		
		for (Node node: network.getNodes().values() ) {
			if ( node.getSupportIds().isEmpty() && node.getCitationIds().isEmpty()) {
					addNodeToNetwork(result,node.getId(), network);
			}
		}
		
		for (Edge edge: network.getEdges().values() ) {
			if ( edge.getSupportIds().isEmpty() && edge.getCitationIds().isEmpty()) {
					addEdgeToNetwork(result,edge.getId(), network);
			}
		}

    	result.setEdgeCount(result.getEdges().size());
    	result.setNodeCount(result.getNodes().size());
		return result;
    	
    }

	
}
