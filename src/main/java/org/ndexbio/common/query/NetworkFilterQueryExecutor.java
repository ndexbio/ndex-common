package org.ndexbio.common.query;


import java.util.Collection;

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

import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;

public class NetworkFilterQueryExecutor {

	private EdgeCollectionQueryODB query;
	private String networkId;
//	private NetworkDocDAO dao;
	
	public NetworkFilterQueryExecutor(String networkIdStr, EdgeCollectionQueryODB edgeQuery) {
		this.query = edgeQuery;
		this.networkId = networkIdStr;
	}
	
	public Network evaluate() throws NdexException {

		try (NetworkDocDAO dao = new NetworkDocDAO() ) {
			Network result = new Network () ;
 		    ODocument networkDoc = dao.getRecordByUUIDStr(networkId, NdexClasses.Network);
		
		    Iterable<ODocument> edgeDocs = Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_Edges );
	        if ( edgeDocs != null)
		    for ( ODocument edgeDoc : edgeDocs) {

	        	if ( edgeDoc.getClassName().equals(NdexClasses.Edge) ) {
	        		// check against filter
	        		if ( EdgeRecordSatisfyFilter(edgeDoc)) {
	        			Edge e = dao.getEdgeFromDocument(edgeDoc,result);
	        			result.getEdges().put(e.getId(), e);
	        			if ( result.getEdges().size() >= query.getEdgeLimit())
	        				break;
	        		}	
	        	}
	    }
		
		if( query.getQueryName() != null) 
				result.setName(query.getQueryName());
		else
				result.setName("");
		
		return result;
		}
		
	}
	
	

	private static boolean elementHasPropertySatisfyFilter(ODocument elementDoc, PropertyFilterODB filter) {

		for (OIdentifiable ndexPropertyDoc : new OTraverse()
			.field("out_"+ NdexClasses.E_ndexProperties )
			.target(elementDoc)
			.predicate( new OSQLPredicate("$depth <= 1"))) {

			ODocument propDoc = (ODocument) ndexPropertyDoc;

			if ( propDoc.getClassName().equals(NdexClasses.NdexProperty)) {
				if ( propertySatisfyFilter(propDoc,filter)) {
					return true;
				}
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
		if ( edgeFilter.getPredicateIds() != null) {
		ODocument predicateRec = edgeDoc.field("out_"+NdexClasses.Edge_E_predicate);
		  for (String predicateId : edgeFilter.getPredicateIds()) {
		    if ( predicateId.equals(predicateRec.getIdentity().toString())) {
			  return true;
		    }
		  }
		}
		
		//check other edgeProperties.
		return elementHasPropertySatisfyFilter(edgeDoc, edgeFilter);
		
	}
	
	private boolean EdgeRecordSatisfyNodeFilters (ODocument edgeDoc, EdgeByNodePropertyFilterODB nodeFilter) {
		
		if ( nodeFilter == null) return true;
		
		switch (nodeFilter.getMode()) {
		case Both: {
			ODocument subject= edgeDoc.field(NdexClasses.Edge_E_subject);
			ODocument object = edgeDoc.field(NdexClasses.Edge_E_object);
			return nodeSatisfyNodeFilter(subject,nodeFilter) && nodeSatisfyNodeFilter(object, nodeFilter);
		}
		case Either: {
			ODocument subject= edgeDoc.field(NdexClasses.Edge_E_subject);
			ODocument object = edgeDoc.field(NdexClasses.Edge_E_object);
			return nodeSatisfyNodeFilter(subject,nodeFilter) || nodeSatisfyNodeFilter(object, nodeFilter);
		}
		case Source:
			ODocument subject= edgeDoc.field(NdexClasses.Edge_E_subject);
			return nodeSatisfyNodeFilter(subject,nodeFilter) ;
			
		case Target:
			ODocument object = edgeDoc.field(NdexClasses.Edge_E_object);
			return  nodeSatisfyNodeFilter(object, nodeFilter);

		default:
			return false;
		
		}
	}
	
	
	private static boolean nodeSatisfyNodeFilter(ODocument nodeDoc, EdgeByNodePropertyFilterODB nodeFilter) {
		
		//check on node name
		Collection<String> nodeNames = nodeFilter.getNodeNames();
		if( nodeNames !=null) {
			for ( String nodeName : nodeNames) {
				String nName = nodeDoc.field(NdexClasses.Node_P_name);
				if ( nName !=null && nName.equalsIgnoreCase(nodeName))
					return true;
			}
		}
		
		// check on baseTerm
		Collection<String> representIDs = nodeFilter.getRepresentTermIDs();
		if ( representIDs !=null) {
			for ( String termId: representIDs) {
				ORID id = nodeDoc.field(NdexClasses.Node_E_represents);
				if (id != null) {
					if ( id.toString().equals(termId))
						return true;
				}
			}
		}
		
		return elementHasPropertySatisfyFilter(nodeDoc,nodeFilter);
	}
	
	
	private static boolean propertySatisfyFilter (ODocument propDoc, PropertyFilterODB filter) {

		if ( filter.getPropertySpecList() == null) return true;
		
		String propDocId = propDoc.getIdentity().toString();
		for ( String propId: filter.getPropertySpecList() ) {
			if ( propDocId.equals(propId)) {
					return true;
			}
		}
		return false;
	}
	
}
