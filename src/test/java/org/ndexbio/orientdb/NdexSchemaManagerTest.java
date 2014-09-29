package org.ndexbio.orientdb;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.NdexException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NdexSchemaManagerTest {

	 private static ODatabaseDocumentTx db;
	 //private static String DB_URL = "memory:ndex";
	 
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
//		db = new ODatabaseDocumentTx(DB_URL);
//		db.create();
		db = NdexAOrientDBConnectionPool.getInstance().acquire();
		long s = db.getDictionary().size();
		System.out.println(s);
		/*for ( Object o : db.getDictionary().keys())
			System.out.println(o.toString());
		*/
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.close();
	}

	@Test
	public void test() throws NdexException {
		NdexSchemaManager.INSTANCE.init(db);
		
		OSchema schema = db.getMetadata().getSchema();
		
		for ( OClass c :schema.getClasses()) 
		{
			System.out.println(c.getName());
		}
		
		assertEquals (schema.countClasses(), 19+10);  // 10 internal classes.
		
	}
	
	@Test
	public void testInsert() {
		db.begin();
		ODocument ns = new ODocument(NdexClasses.Namespace);
		ns.field("prefix", "ns1");
		ns.field("uri", "http://foo.bar.org/");
		ns.field("id", (long)2);
		ODocument network = new ODocument(NdexClasses.Network);
		network.field("uuid", "2453");
		network.field("name", "foo");
		
		ns.field("in_ns",network,OType.LINK);
		ns.save();
	//	String id1 = ns.getIdentity().toString();
		Set<ODocument> s = new HashSet<> ();
		s.add(ns);
		network.field("out_ns", s, OType.LINKSET);
		network.save();
/*		String id2 = network.getIdentity().toString();
		String query = "create edge ns from "+ id2 + " to " + id1;
		Integer execute = db.command(
			      new OCommandSQL(query)).execute();
		int recordsUpdated = execute.intValue();
        System.out.println(recordsUpdated);     
	*/	db.commit();
		
		db.begin ();
		ns = new ODocument(NdexClasses.Namespace);
		ns.field("prefix", "ns2");
		ns.field("uri", "http://example2.org/");
		ns.field("id", (long)3);
		
		
		List<ODocument> networks = db.query(
					      new OSQLSynchQuery<ODocument>("select from network where uuid = '2453'"));

		network=networks.get(0);
        ns.field("in_ns",network,OType.LINK);
		ns.save();
		Collection<ODocument> s1 = network.field("out_ns");
		s1.add(ns);
		network.field("out_ns", s1, OType.LINKSET);
		network.save();
		
		db.commit();

		
		db.begin ();
		ns = new ODocument(NdexClasses.Namespace);
		ns.field("prefix", "ns3");
		ns.field("uri", "http://cjtest3.org/");
		ns.field("id", (long)4);
		
		
		networks = db.query(
					      new OSQLSynchQuery<ODocument>("select from network where uuid = '2453'"));

		network=networks.get(0);

		ns.field("in_ns",network,OType.LINK);
		ns.save();
		
		network.field("out_ns", ns, OType.LINK);
		network.save();
		
		db.commit();

	}

}
