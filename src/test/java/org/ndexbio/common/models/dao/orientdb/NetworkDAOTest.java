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

	@Test
    public void test() throws NdexException, JsonProcessingException {
		NetworkDAO dao = new NetworkDAO(db);
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
		System.out.println ( s);
	}
	
}
