package org.ndexbio.common.access;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class NdexDatabaseTest {

	static NdexDatabase db;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = new NdexDatabase();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.close();
	}

	@Test
	public void test() {
		//Logger l = Logger.getLogger("ndexDatabaseTest");
		//l.info("start");
	/*	for (int i = 0; i < 100000; i++ )
			db.getNextId();
//	   	  System.out.println(db.getNextId());
		l.info("end");
	*/	
		long n1 = db.getNextId();
		long n2 = db.getNextId();
		assertEquals (n2, n1+1);
		//System.out.println(db.getNextId());
		//System.out.println(db.getNextId());
	}

}
