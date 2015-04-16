package org.ndexbio.common.query;

import org.ndexbio.model.network.query.EdgeCollectionQuery;
import org.ndexbio.model.object.network.Network;

public class NetworkQuery {

	private EdgeCollectionQuery query;
	private String networkId;
	
	public NetworkQuery(String networkIdStr, EdgeCollectionQuery edgeQuery) {
		this.query = edgeQuery;
		this.networkId = networkIdStr;
	}
	
	public Network evaluate() {
		Network result = new Network () ;

	//	NetworkDAO dao = new NetworkDAO()
		
		
		
		
		if( query.getQueryName() != null) 
			result.setName(query.getQueryName());
		else
			result.setName("");
		
		return result;
	}
	
	
//	private Collection<Long> getFilteredNodeIds ()

}
