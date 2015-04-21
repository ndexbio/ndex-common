package org.ndexbio.common.persistence.orientdb;

import static org.junit.Assert.*;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.Network;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class NdexNetworkCloneServiceTest {

	@Test
	public void test() throws NdexException, ExecutionException {
		NdexDatabase db = NdexDatabase.createNdexDatabase("http://localhost", "plocal:/opt/ndex/orientdb/databases/cjtest", "admin", "admin", 10);
		ODatabaseDocumentTx connection = db.getAConnection();
		NetworkDAO dao = new NetworkDAO ( connection);
	//	Network network =  dao.getNetworkById(UUID.fromString("4842a831-1e5c-11e4-9f34-90b11c72aefa"));
		Network network =  dao.getNetworkById(UUID.fromString(
				"503cfcd7-20ae-11e4-b3cf-001f3bca188f"));
		//		"3b42c607-1f3f-11e4-907f-90b11c72aefa"));
		//		"1db5f2c1-1e5e-11e4-9f34-90b11c72aefa"));
		network.setName("---" + network.getName() + "---");
		NdexNetworkCloneService service = new NdexNetworkCloneService(db, network, "Support");
		service.cloneNetwork();
		connection.close();
		service.close();
		db.close();
		
	}

}
