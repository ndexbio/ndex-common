package org.ndexbio.common.query;

import java.util.ArrayList;
import java.util.Collection;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.network.query.EdgeCollectionQuery;
import org.ndexbio.model.network.query.PropertySpecification;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Network;

import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;

public class NetworkQuery {

	private EdgeCollectionQuery query;
	private String networkId;
	
	public NetworkQuery(String networkIdStr, EdgeCollectionQuery edgeQuery) {
		this.query = edgeQuery;
		this.networkId = networkIdStr;
	}
	
	public Network evaluate() throws NdexException {
		Network result = new Network () ;

		try (NetworkDAO dao = new NetworkDAO()) {
			ODocument networkDoc = dao.getRecordByUUIDStr(networkId, NdexClasses.Network);
		
		
	        for (OIdentifiable aDoc : new OTraverse()
            	.field("out_"+ NdexClasses.Network_E_Edges )
            	.target(networkDoc)
            	.predicate( new OSQLPredicate("$depth <= 1"))) {

	        	ODocument edgeDoc = (ODocument) aDoc;

	        	if ( edgeDoc.getClassName().equals(NdexClasses.Edge) ) {
	        		// check against filter
	        		
	        		Edge e = dao.getEdgeFromDocument(edgeDoc,result);
	        		result.getEdges().put(e.getId(), e);
	 
	        	}
	        }

		
			if( query.getQueryName() != null) 
				result.setName(query.getQueryName());
			else
				result.setName("");
		
			return result;
		}
	}
	
	
	private boolean statisfyFilter (ODocument edgeDoc) {
		
		ArrayList<NdexPropertyValuePair> props = new ArrayList<> ();
		
		for ( PropertySpecification spec: query.getEdgeFilter().getPropertySpecList() ) {
			
			
		}
		
		
		return true;
	}

}
