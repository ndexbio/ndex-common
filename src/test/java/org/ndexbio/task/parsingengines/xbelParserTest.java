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
package org.ndexbio.task.parsingengines;

import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.persistence.orientdb.PropertyGraphLoader;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.task.Configuration;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class xbelParserTest {

	static Configuration configuration ;
	static String propertyFilePath = "/opt/ndex/conf/ndex.properties";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		setEnv();
    	// read configuration
    	configuration = Configuration.getInstance();
    	
    	//and initialize the db connections
    	NdexDatabase.createNdexDatabase(configuration.getHostURI(),
				configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd(), 1);
		
	}

	@AfterClass
	public static void tearDownAfterClass() {
		NdexDatabase.close();
		System.out.println("Connection pool closed.");
	}

	
//	@Test
	public void test0() throws Exception {
		
		
		ODatabaseDocumentTx conn = NdexDatabase.getInstance().getAConnection();
		NetworkDAO dao = new NetworkDAO(conn);
		
		PropertyGraphNetwork pg = dao.getProperytGraphNetworkById(UUID.fromString("40127714-6b8d-11e4-87d2-90b11c72aefa"));
		//Network n = dao.getNetworkById(UUID.fromString("14ee1740-5644-11e4-963e-90b11c72aefa"));
		
		System.out.println(pg.getName());
		pg.setName(pg.getName() + " - new name");
		
		
		PropertyGraphLoader pgl = new PropertyGraphLoader(NdexDatabase.getInstance());
//        NetworkSummary s = pgl.insertNetwork( pg, "cjtest", null);
		//NetworkSummary s = pgl.updateNetwork(pg);

//        System.out.println(s);

    //    NetworkSummary s = service.updateNetwork();

	} 
	
/*
	@Test
	public void test1() throws NdexException, JAXBException, URISyntaxException {
    	
    	
		NdexDatabase db = new NdexDatabase(configuration.getHostURI());

		ODatabaseDocumentTx conn = db.getAConnection();
		OrientGraph graph = new OrientGraph(conn,false);
		
		ODocument doc = new ODocument (NdexClasses.Citation);
		doc = doc.field(NdexClasses.Element_ID, -2).save();
		OrientVertex v1 = graph.getVertex(doc);
		
		ODocument doc2 = new ODocument (NdexClasses.Edge);
		doc2 = doc2.field(NdexClasses.Element_ID, -3).save();
		OrientVertex v2 = graph.getVertex(doc2);
		v2.addEdge(NdexClasses.Edge_E_citations, v1);
		v2.addEdge(NdexClasses.Edge_E_citations, v1);
		
		conn.commit();
		conn.close();  
		System.out.println("closing db.");
		db.close();
	}
*/	
	
	@Test
	public void test2() throws NdexException, JAXBException, URISyntaxException {
    	
    	
		NdexDatabase db = NdexDatabase.getInstance();
		
    	ODatabaseDocumentTx conn = db.getAConnection();

//    	UserDAO dao = new UserDAO(conn);
    	
 //   	DatabaseInitializer.createUserIfnotExist(dao, configuration.getSystmUserName(), "support@ndexbio.org", 
 //   				configuration.getSystemUserPassword());

		String user = "cjtest"; //configuration.getSystmUserName();
		  XbelParser parser = new XbelParser(
				  "/home/chenjing/Dropbox/Network_test_files/tiny_corpus_2.xbel"
				//  "/home/chenjing/Downloads/wiki-pain.xbel"
				//  "/opt/ndex/exported-networks/157f410b-6539-11e4-9955-90b11c72aefa.xbel"
				 //  "/home/chenjing/git/ndex-task/src/test/resources/small_corpus.xbel"
				    , user, db, "");
		  parser.parseFile();
		System.out.println("closing db.");
		
		conn.commit();
		conn.close();
		NdexDatabase.close();
	}

	
	private static void setEnv()
	{
	  try
	    {
	        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
	        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
	        theEnvironmentField.setAccessible(true);
	        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
	        env.put("ndexConfigurationPath", propertyFilePath);
	        //env.putAll(newenv);
	        Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
	        theCaseInsensitiveEnvironmentField.setAccessible(true);
	        Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
	        //cienv.putAll(newenv);
	        env.put("ndexConfigurationPath", propertyFilePath);
	    }
	    catch (NoSuchFieldException e)
	    {
	      try {
	        Class[] classes = Collections.class.getDeclaredClasses();
	        Map<String, String> env = System.getenv();
	        for(Class cl : classes) {
	            if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
	                Field field = cl.getDeclaredField("m");
	                field.setAccessible(true);
	                Object obj = field.get(env);
	                Map<String, String> map = (Map<String, String>) obj;
	                //map.clear();
	                //map.putAll(newenv);
	                map.put("ndexConfigurationPath", propertyFilePath);
	            }
	        }
	      } catch (Exception e2) {
	        e2.printStackTrace();
	      }
	    } catch (Exception e1) {
	        e1.printStackTrace();
	    } 
	}
	
}
