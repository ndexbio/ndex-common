package org.ndexbio.orientdb;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;

public class NdexSchemaManagerTest {

	static ODatabaseDocumentTx db;
	 private static String DB_URL = "memory:ndex";
	 
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		db = new ODatabaseDocumentTx(DB_URL);
		db.create();
	//	db = NdexAOrientDBConnectionPool.getInstance().acquire();
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.close();
	}

	@Test
	public void test() {
		NdexSchemaManager.INSTANCE.init(db);
		
		OSchema schema = db.getMetadata().getSchema();
		
		for ( OClass c :schema.getClasses()) 
		{
			System.out.println(c.getName());
		}
		
		assertEquals (schema.countClasses(), 19+10);
		
	}

}
