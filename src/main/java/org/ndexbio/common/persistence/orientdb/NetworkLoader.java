package org.ndexbio.common.persistence.orientdb;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.PropertyGraphEdge;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.model.object.network.PropertyGraphNode;
import org.ndexbio.model.object.network.Support;

import com.fasterxml.jackson.databind.ObjectMapper;


public class NetworkLoader {

	private NdexNetworkCloneService persistenceService;
	
	public NetworkLoader (NdexDatabase db, Network network, String accountName) throws ObjectNotFoundException, NdexException  {
		this.persistenceService = new NdexNetworkCloneService(db,network, accountName);
	}
	

	public NetworkSummary insertNetwork() throws Exception {
		return persistenceService.cloneNetwork();
	}
	
	
}
