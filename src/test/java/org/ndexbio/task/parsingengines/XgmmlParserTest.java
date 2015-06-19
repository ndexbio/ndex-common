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
package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.network.Network;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;


public class XgmmlParserTest {

/*	static Configuration configuration ;
	static String propertyFilePath = "/opt/ndex/conf/ndex.properties";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		setEnv();

    	// read configuration
    	Configuration configuration = Configuration.getInstance();
    	
    	//and initialize the db connections
    	NdexAOrientDBConnectionPool.createOrientDBConnectionPool(
    			configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd(),1);
    	
    	
		NdexDatabase db = new NdexDatabase(configuration.getHostURI());
		
		String user = "cjtest";
		XgmmlParser parser = new XgmmlParser("/home/chenjing/Dropbox/Network_test_files/pdmap130712.xgmml", user, 
				db);
		parser.parseFile();
//		XbelParser 
//		parser = new XbelParser("/home/chenjing/working/ndex/networks/selventa_full.xbel", user);
//		parser.parseFile();

		db.close();
		NdexAOrientDBConnectionPool.close();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
*/
	@Test
	public void test() throws Exception {

		for ( TestMeasurement m : AllTests.testList) {
		  if ( m.srcFormat == NetworkSourceFormat.XGMML) {
			  // load to db	
			  XgmmlParser parser = new XgmmlParser(AllTests.testFileDirectory + m.fileName, AllTests.testUser, 
			  			AllTests.db,m.fileName, "");
			  	parser.parseFile();
			  	
			  	
			 // get the UUID of the new test network
			 UUID networkID = parser.getUUIDOfUploadedNetwork();
			
			 // verify the uploaded network
			 ODatabaseDocumentTx conn = AllTests.db.getAConnection();
			 NetworkDAO dao = new NetworkDAO(conn);
			 Network n = dao.getNetworkById(networkID);
			 assertEquals(n.getName(), m.networkName);
			 assertEquals(n.getNodeCount(), n.getNodes().size());
			 assertEquals(n.getNodeCount(), m.nodeCnt);
			 assertEquals(n.getEdgeCount(), m.edgeCnt);
			 assertEquals(n.getEdges().size(), m.edgeCnt);
			 if (m.basetermCnt >=0 )
				 assertEquals(n.getBaseTerms().size(), m.basetermCnt);
			 
			 conn.close();
			 
			 //export the uploaded network.
			 
		  }	  	
		}
	}

/*	private static void setEnv()
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
	*/
}
