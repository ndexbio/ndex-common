package org.ndexbio.common.query;

import org.ndexbio.model.network.query.EdgeCollectionQuery;
import org.ndexbio.model.object.network.Network;

public class NetworkQuery {

	private EdgeCollectionQuery query;
	private String networkId;
	
	public NetworkQuery(String networkIdStr, EdgeCollectionQuery query) {
		this.query = query;
		this.networkId = networkIdStr;
	}
	
	public Network evaluate() {
		Network result = new Network () ;
		
		
		return result;
	}

}
