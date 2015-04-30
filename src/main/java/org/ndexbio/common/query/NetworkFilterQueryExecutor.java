package org.ndexbio.common.query;


import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.common.query.filter.orientdb.EdgeByEdgePropertyFilterODB;
import org.ndexbio.common.query.filter.orientdb.EdgeByNodePropertyFilterODB;
import org.ndexbio.common.query.filter.orientdb.EdgeCollectionQueryODB;
import org.ndexbio.common.query.filter.orientdb.PropertyFilterODB;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Network;

import com.orientechnologies.orient.core.record.impl.ODocument;

public class NetworkFilterQueryExecutor {

	private EdgeCollectionQueryODB query;
	private String networkId;
	
	public NetworkFilterQueryExecutor(String networkIdStr, EdgeCollectionQueryODB edgeQuery) {
		this.query = edgeQuery;
		this.networkId = networkIdStr;
	}
	
	public Network evaluate() throws NdexException {

		int limit = query.getEdgeLimit();
		try (NetworkDocDAO dao = new NetworkDocDAO() ) {
			Network result = new Network () ;
 		    ODocument networkDoc = dao.getRecordByUUIDStr(networkId, NdexClasses.Network);
		
		    Iterable<ODocument> edgeDocs = Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_Edges );
	        if ( edgeDocs != null)
		    for ( ODocument edgeDoc : edgeDocs) {

	        	// check against filter
	        	if ( EdgeRecordSatisfyFilter(edgeDoc)) {
	        			Edge e = dao.getEdgeFromDocument(edgeDoc,result);
	        			result.getEdges().put(e.getId(), e);
	        			if ( limit > 0 && result.getEdges().size() >= limit)
	        				break;
	        	}	
	    }
		
		if( query.getQueryName() != null) 
				result.setName(query.getQueryName());
		else
				result.setName("");
		
		result.setNodeCount(result.getNodes().size());
		result.setEdgeCount(result.getEdges().size());
		
		return result;
		}
		
	}
	

	private static boolean elementHasPropertySatisfyFilter(ODocument elementDoc, PropertyFilterODB filter) {

		for (ODocument propDoc : Helper.getDocumentLinks(elementDoc, "out_", NdexClasses.E_ndexProperties )) {
			if ( filter.containsPropertyId(propDoc.getIdentity().toString()) ) {
					return true;
			}
		}
		return false;
	}
	
	
	private boolean EdgeRecordSatisfyFilter(ODocument edgeDoc) {
	    
		return EdgeRecordSatisfyEdgePropertyFilter(edgeDoc, query.getEdgeFilter()) &&
				EdgeRecordSatisfyNodeFilters  (edgeDoc, query.getNodeFilter())		;
	}

	private boolean EdgeRecordSatisfyEdgePropertyFilter (ODocument edgeDoc, EdgeByEdgePropertyFilterODB edgeFilter) {
		
		if ( edgeFilter == null) return true;
		
		// check the predicates
		  ODocument predicateRec = edgeDoc.field("out_"+NdexClasses.Edge_E_predicate);
		  if ( edgeFilter.containsPredicateId(predicateRec.getIdentity().toString()))
			  return true;
		
		//check other edgeProperties.
		return elementHasPropertySatisfyFilter(edgeDoc, edgeFilter);
		
	}
	
	private boolean EdgeRecordSatisfyNodeFilters (ODocument edgeDoc, EdgeByNodePropertyFilterODB nodeFilter) {
		
		if ( nodeFilter == null) return true;
		
		switch (nodeFilter.getMode()) {
		case Both: {
			ODocument subject= edgeDoc.field("in_"+ NdexClasses.Edge_E_subject);
			ODocument object = edgeDoc.field("out_"+NdexClasses.Edge_E_object);
			return nodeSatisfyNodeFilter(subject,nodeFilter) && nodeSatisfyNodeFilter(object, nodeFilter);
		}
		case Either: {
			ODocument subject= edgeDoc.field("in_"+ NdexClasses.Edge_E_subject);
			ODocument object = edgeDoc.field("out_"+NdexClasses.Edge_E_object);
			return nodeSatisfyNodeFilter(subject,nodeFilter) || nodeSatisfyNodeFilter(object, nodeFilter);
		}
		case Source:
			ODocument subject= edgeDoc.field("in_"+ NdexClasses.Edge_E_subject);
			return nodeSatisfyNodeFilter(subject,nodeFilter) ;
			
		case Target:
			ODocument object = edgeDoc.field("out_"+NdexClasses.Edge_E_object);
			return  nodeSatisfyNodeFilter(object, nodeFilter);

		default:
			return false;
		
		}
	}
	
	
	private static boolean nodeSatisfyNodeFilter(ODocument nodeDoc, EdgeByNodePropertyFilterODB nodeFilter) {
		
		//check on node name
		String nName = nodeDoc.field(NdexClasses.Node_P_name);
		if (  nName !=null && nodeFilter.conatinsNodeName(nName) )
				return true;
		
		// check on baseTerm
		ODocument term = nodeDoc.field("out_"+NdexClasses.Node_E_represents);
		if (term != null && nodeFilter.containsRepresentTermId(term.getIdentity().toString()))
				return true;
		
		return elementHasPropertySatisfyFilter(nodeDoc,nodeFilter);
	}
}
