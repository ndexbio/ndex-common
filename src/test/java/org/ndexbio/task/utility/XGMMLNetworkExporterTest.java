/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.task.utility;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exporter.XGMMLNetworkExporter;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.User;
import org.ndexbio.task.Configuration;
import org.xml.sax.SAXException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class XGMMLNetworkExporterTest {

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
    			configuration.getDBPasswd(), 10);


	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	@Test 
	public void testConcurrency() throws NdexException {
		ODatabaseDocumentTx conn1 = NdexDatabase.getInstance().getAConnection();
		UserDAO dao1 = new UserDAO(conn1);
		
		User u1 = dao1.getUserByAccountName("cjtest");
		ODatabaseDocumentTx conn2 = NdexDatabase.getInstance().getAConnection();
		UserDAO dao2 = new UserDAO(conn2);
		
		User u2 = dao2.getUserByAccountName("cjtest");

		System.out.println("U1 password:" + dao1.setNewPassword("cjtest"));
		System.out.println("U2 password:" + dao2.setNewPassword("cjtest"));
		dao1.commit();
		dao2.commit();
		System.out.print(u1.toString() + u2.toString());
		conn1.close();
		conn2.close();
	}

	@Test
	public void test() throws NdexException, ParserConfigurationException, TransformerException, ClassCastException, SAXException, IOException {
	/*	ODatabaseDocumentTx db = NdexAOrientDBConnectionPool.getInstance().acquire();
		
		XGMMLNetworkExporter exporter = new XGMMLNetworkExporter(db);
		
		exporter.exportNetwork(UUID.fromString("ba902d91-7ffa-11e4-b6e2-90b11c72aefa"), System.out);
		
		db.close(); */
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
