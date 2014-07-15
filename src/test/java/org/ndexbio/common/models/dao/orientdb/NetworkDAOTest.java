package org.ndexbio.common.models.dao.orientdb;

import static org.junit.Assert.*;

import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.PropertyGraphNetwork;

import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NetworkDAOTest {

	static ODatabaseDocumentTx db;
	
	@BeforeClass
    public static void initializeTests() 
    {
    	NdexAOrientDBConnectionPool p = NdexAOrientDBConnectionPool.getInstance();
    	db = p.acquire();
    }
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.close();
	}

	@Test
    public void test() throws NdexException {
		NetworkDAO dao = new NetworkDAO(db);
		PropertyGraphNetwork n = dao.getProperytGraphNetworkById( UUID.fromString("5164cf01-0942-11e4-8380-90b11c72aefa"));
		System.out.println ( n);
		
	}

}
