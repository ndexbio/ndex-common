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
package org.ndexbio.common.query;


import java.util.List;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.common.query.filter.orientdb.EdgeByEdgePropertyFilterODB;
import org.ndexbio.common.query.filter.orientdb.EdgeByNodePropertyFilterODB;
import org.ndexbio.common.query.filter.orientdb.EdgeCollectionQueryODB;
import org.ndexbio.common.query.filter.orientdb.PropertyFilterODB;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.NdexPropertyValuePair;
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
	
   // TODO: need to make this case insensitive.
	private static boolean elementHasPropertySatisfyFilter(ODocument elementDoc, PropertyFilterODB filter) {

		List<NdexPropertyValuePair> props = elementDoc.field(NdexClasses.ndexProperties);
		if ( props !=null )
		for (NdexPropertyValuePair property : props) {
			if ( filter.containsPropertyName(property.getPredicateString()) ) {
					return true;
			}
		}
		return false;
	}
	
	
	private boolean EdgeRecordSatisfyFilter(ODocument edgeDoc) {
	    
		return EdgeRecordSatisfyEdgePropertyFilter(edgeDoc, query.getEdgeFilter()) &&
				EdgeRecordSatisfyNodeFilters  (edgeDoc, query.getNodeFilter())		;
	}

	private static boolean EdgeRecordSatisfyEdgePropertyFilter (ODocument edgeDoc, EdgeByEdgePropertyFilterODB edgeFilter) {
		
		if ( edgeFilter == null) return true;
		
		// check the predicates
		  Long predicateId = edgeDoc.field(NdexClasses.Edge_P_predicateId);
		  if ( edgeFilter.containsPredicateId(predicateId))
			  return true;
		
		//check other edgeProperties.
		return elementHasPropertySatisfyFilter(edgeDoc, edgeFilter);
		
	}
	
	private static boolean EdgeRecordSatisfyNodeFilters (ODocument edgeDoc, EdgeByNodePropertyFilterODB nodeFilter) {
		
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
		Long termId = nodeDoc.field(NdexClasses.Node_P_represents);
		if (termId != null && nodeFilter.containsRepresentTermId(termId))
				return true;
		
		return elementHasPropertySatisfyFilter(nodeDoc,nodeFilter);
	}
}
