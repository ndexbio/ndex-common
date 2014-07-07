package org.ndexbio.common.persistence.orientdb;

import java.util.UUID;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.network.Network;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class NetworkPersistenceService {
	
	private NdexDatabase database;
	
	private ODatabaseDocumentTx ndexDatabase;
	
	public NetworkPersistenceService (NdexDatabase db) {
		database = db;
	    ndexDatabase = database.getAConnection();
	}
	
	
	public void commit() {
		ndexDatabase.commit();
	}
	
	
	public void close () {
		ndexDatabase.close();
	}

/*	public Network getOrCreateNetwork(UUID networkId) {
        NetworkDAO dao = new NetworkDAO(database);
        Network n = dao.getNetworkByID(networkId); 
		if ( n != null)
			return n;

		n = new Network();
		n.setExternalId(networkId);
			
		return n;
		
	}
	*/
}
