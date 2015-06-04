package org.ndexbio.common.persistence.orientdb;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.PropertyGraphNetwork;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class PropertyGraphLoaderTest {

//	private static NdexPersistenceService service;
	private static NdexDatabase db;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = NdexDatabase.createNdexDatabase("http://localhost", "plocal:/opt/ndex/orientdb/databases/cjtest", "admin", "admin", 10);
//		service = new NdexPersistenceService(db);
	}
	
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() throws Exception {
		ODatabaseDocumentTx conn = db.getAConnection();
		NetworkDAO dao = new NetworkDAO (conn);
		PropertyGraphNetwork pn = 
		    dao.getProperytGraphNetworkById(UUID.fromString("d9ed6aa1-1364-11e4-8b0d-90b11c72aefa"), 0, 12);
		conn.close();

		int i = 0;
		for ( NdexPropertyValuePair p : pn.getProperties()) {
			if ( p.getPredicateString().equals(PropertyGraphNetwork.uuid) ) {
				break;
			}
			i++;
		}
		pn.getProperties().remove(i);
		
		NdexPropertyValuePair pname = new NdexPropertyValuePair();
		pname.setPredicateString(PropertyGraphNetwork.name);
		pname.setValue("my test network1");
		pn.getProperties().add(pname);
		
		PropertyGraphLoader loader = new PropertyGraphLoader ( db);
		
//		loader.insertNetwork(pn, "Support", null);
		
	}

}
