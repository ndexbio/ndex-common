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
		db = new NdexDatabase("http://localhost/");
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

	@Test
	public void test1() {

		
		for ( int i = 0 ; i < 202; i ++ ) {
			long id = db.getNextId();
			   System.out.println("Number " + i + " I got is: " + id);
		}

		
		long id0 = db.getNextId();
		System.out.println("First id got I got is:" + id0);
		for ( int i = 0 ; i < 1000; i ++ ) {
			 db.getNextId();
			
		}
		long id1 = db.getNextId();
		System.out.println("Last id got I got is:" + id1);
		assertEquals ( id0 + 1001 , id1);
	
	}
	
	
	@Test
	public void testleaks1() {

		System.out.println("First id got I got is:" + db.getNextId());
		for ( int i = 0 ; i < 1000000; i ++ ) {
			long id = db.getNextId();
			if ( i % 100000 == 0) {
			   Logger.getGlobal().info("Number " + i + " I got is: " + id);
			}
			
		}
		System.out.println("Last id got I got is:" + db.getNextId());
	
	}
	
	
}
