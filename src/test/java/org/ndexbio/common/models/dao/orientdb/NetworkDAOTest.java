package org.ndexbio.common.models.dao.orientdb;

import static org.junit.Assert.*;

import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.persistence.orientdb.PropertyGraphLoader;
import org.ndexbio.model.object.NdexProperty;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.PropertyGraphEdge;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.model.object.network.PropertyGraphNode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
/*
	@Test
	public void testPropertyGraph() throws JsonProcessingException, NdexException {
		NetworkDAO dao = new NetworkDAO(db);
		
		PropertyGraphNetwork n = dao.getProperytGraphNetworkById(
				UUID.fromString("503cfcd7-20ae-11e4-b3cf-001f3bca188f"),
				0, 1000);
		
		System.out.println( n.toString() + "...");
	}

	
	@Test
	public void testdelete() {
		NetworkDAO dao = new NetworkDAO(db);
		
		int r = dao.deleteNetwork("4842a831-1e5c-11e4-9f34-90b11c72aefa");
		
		System.out.println( r + "Vertex deleted from graph.");
	}
*/	
	@Test
    public void test() throws NdexException, JsonProcessingException {
		NetworkDAO dao = new NetworkDAO(db);
		
//		System.out.println(dao.checkPrivilege("Support", "e026e93c-1997-11e4-8f64-90b11c72aefa", Permissions.READ));

		Network network = dao.getNetwork(UUID.fromString("fdea795b-28ae-11e4-9366-90b11c72aefa"),
				0, 2500);
		
		System.out.println(network);

/*		
		network = dao.getNetwork(UUID.fromString("b4e09a69-180c-11e4-b734-00219b422d69"),
				0, 15);
		
		System.out.println(network);
		
		network = dao.getNetworkById(UUID.fromString("cbe27f2d-180c-11e4-bd2c-00219b422d69"));
		
		System.out.println(network);

		
		PropertyGraphNetwork n = dao.getProperytGraphNetworkById( 
				UUID.fromString("e4e82535-1422-11e4-a931-90b11c72aefa"),
				
				//	UUID.fromString("bb952930-137a-11e4-b507-90b11c72aefa"),
				0,12);
		ObjectMapper mapper = new ObjectMapper(); // create once, reuse
		String s = mapper.writeValueAsString( n);
		System.out.println ( s);

		n = dao.getProperytGraphNetworkById( 
				UUID.fromString("c16614aa-094a-11e4-b7e2-001f3bca188f"),
				1,12);
		s = mapper.writeValueAsString( n);
		System.out.println ( s);
        		
		
	}

	@Test
	public void test2 () throws Exception {
		NetworkDAO dao = new NetworkDAO(db);
		PropertyGraphNetwork pn = dao.getProperytGraphNetworkById(UUID.fromString("e4e82535-1422-11e4-a931-90b11c72aefa"), 0,12);
		
		for (Long l : pn.getNodes().keySet()) 
			System.out.println(l);
		
		for (PropertyGraphEdge e: pn.getEdges()) {
			System.out.println(
					e.getSubjectId()+ "\t" +e.getObjectId()
					//		pn.getNodes().get(e.getSubjectId()).getName() + "\t" +
			//		pn.getNodes().get(e.getObjectId()).getName()
					);
			
		}

		NdexProperty p1 = new NdexProperty("foo", "bestBar");	
		
		int i = 0;
		for ( NdexProperty p : pn.getProperties()) {
			if ( p.getPredicateString().equals(PropertyGraphNetwork.uuid) ) {
				break;
			}
			i++;
		}
		pn.getProperties().remove(i);
		
		
		pn.getProperties().add(p1);
		i = 0 ;
		for ( PropertyGraphNode n :  pn.getNodes().values()) {
			NdexProperty p2 = new NdexProperty ("fooN", "bestBarInNode-"+ i);
			n.getProperties().add(p2);
		}
		
		NdexDatabase db = new NdexDatabase();
		
		PropertyGraphLoader loader = new PropertyGraphLoader(db);
		pn.setName("my new name");
		NetworkSummary nks = loader.insertNetwork(pn, "Support");
		
		
		PropertyGraphNetwork n = dao.getProperytGraphNetworkById(nks.getExternalId(),	0,12);
		
		ObjectMapper mapper = new ObjectMapper(); // create once, reuse
		String s = mapper.writeValueAsString( n);
		System.out.println ( s); */
	}
	
}
