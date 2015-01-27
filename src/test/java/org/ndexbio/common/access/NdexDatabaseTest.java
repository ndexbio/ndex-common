package org.ndexbio.common.access;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.util.TermUtilities;
import org.ndexbio.model.exceptions.NdexException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class NdexDatabaseTest {

	static NdexDatabase db;
	
	static String error = null;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		String[] foo = TermUtilities.getNdexQName("abc:3fg");
		System.out.println(foo);
		
		foo = TermUtilities.getNdexQName("abc:3 fg");
		System.out.println(foo);

		foo = TermUtilities.getNdexQName("ab(c:3fg)");
		System.out.println(foo);

		NdexDatabase.createNdexDatabase("http://localhost/", "plocal:/opt/ndex/orientdb/databases/cjtest", "admin", "admin", 2);
		db = NdexDatabase.getInstance();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.close();
	}

/*	
	@Test
	public void test() {
		System.out.println("testing get id function.");
		long n1 = db.getNextId();
		long n2 = db.getNextId();
		assertEquals (n2, n1+1);
		//System.out.println(db.getNextId());
		//System.out.println(db.getNextId());
	}

	@Test
	public void test1() {

		long id0 = db.getNextId();
		for ( int i = 0 ; i < 1022000; i ++ ) {
			long id = db.getNextId();
			assertEquals (id0+i+1, id);
			if ( i % 100000 == 0) {
				   Logger.getGlobal().info("Number " + i + " I got is: " + id);
			}
		}

	}
*/	
	
	@Test
	public void testleaks1() throws NdexException {
		
		int size = 600;
		Thread[] pool = new Thread[size];
		try {
		for ( int i = 0 ; i < size ; i++ ) {
			System.out.println("Running thread " + i);
			pool[i] = new Thread(new MessageLoop());
			pool[i].start();
			Thread.sleep(50);
		}
			for ( int i = 0; i < size ; i++)
				pool[i].join();
			if (error != null)
				throw new NdexException (error);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new NdexException ("Interrupted.");
		}
		 	        
	}
	
	
	private static class MessageLoop implements Runnable {
	
		@Override
		public void run() {
			try {
				System.out.println("getting a connection.");
				ODatabaseDocumentTx conn1 = db.getAConnection();
				System.out.println("got a connection in db " + conn1.getName());
				Thread.sleep(30000);
				conn1.close();
				System.out.println("Connection released.");
			} catch (InterruptedException e) {
				System.out.println("I wasn't done!");
			} catch (NdexException e) {
				// TODO Auto-generated catch block
				error = e.getMessage(); 
				e.printStackTrace();
			}
		}
    }
}
