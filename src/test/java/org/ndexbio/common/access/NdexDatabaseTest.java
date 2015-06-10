/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.common.access;

import static org.junit.Assert.*;

import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

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
		
        String ldapAdServer = 
        		"ldap://10.37.62.139:389";
      
        String ldapSearchBase="...";
        
        String user = "myadname";
        String ldapUser = "NA\\" + user;
        String ldapPassword = "myadpassword";
        
        
        Hashtable <String,Object> env = new Hashtable<>();
        
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, ldapUser);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapAdServer);
        env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
        
        env.put("java.naming.ldap.attributes.binary", "objectSID");
        env.put("com.sun.jndi.ldap.trace.ber", System.err);
        LdapContext ctx = new InitialLdapContext(env,null);
        
        
        String searchFilter = "(&(SAMAccountName="+ user +")(objectClass=user)(objectCategory=person))";
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        //searchControls.se
		NamingEnumeration<SearchResult> results = ctx.search(ldapSearchBase, searchFilter, searchControls);
		
		
		Pattern pattern = Pattern.compile("^CN=(.*?[^\\\\]),");
		SearchResult searchResult = null;
		if ( results.hasMoreElements()) {
			searchResult = results.nextElement();
			Attributes attrs = searchResult.getAttributes();
			Attribute uWWID = attrs.get("employeeID");
			Attribute grp = attrs.get("memberOf");
			NamingEnumeration enu = grp.getAll();
	        while ( enu.hasMore()) {
	        	String obj = (String)enu.next();
	        	System.out.println(obj);

	    		Matcher matcher = pattern.matcher(obj);

	    		if (matcher.find())
	    		{
	    		    System.out.println(matcher.group(1));
	    		}
	        }
			
			System.out.print(uWWID.toString() + grp.toString());
		}
		
		
		String[] foo = TermUtilities.getNdexQName("abc:3fg");
		System.out.println(foo);
		
		foo = TermUtilities.getNdexQName("abc:3 fg");
		System.out.println(foo);

		foo = TermUtilities.getNdexQName("ab(c:3fg)");
		System.out.println(foo);

		foo = TermUtilities.getNdexQName("something bad:3fg)");
		System.out.println(foo);

		
		NdexDatabase.createNdexDatabase("http://localhost/", "plocal:/opt/ndex/orientdb/databases/ndextest", "admin", "admin", 2);
		db = NdexDatabase.getInstance();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.close();
	}

	
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
