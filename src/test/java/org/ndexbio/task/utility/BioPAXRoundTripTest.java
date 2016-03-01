/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
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

import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exporter.BioPAXNetworkExporter;
import org.ndexbio.task.Configuration;
import org.ndexbio.task.parsingengines.BioPAXParser;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class BioPAXRoundTripTest {

	static Configuration configuration ;
	static String propertyFilePath = "/opt/ndex/conf/ndex.properties";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		setEnv();

    	// read configuration
    	Configuration configuration = Configuration.getInstance();
    	
    	//and initialize the db connections
		NdexDatabase.createNdexDatabase(configuration.getHostURI(),
				configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd(), 10);
		
		// Parse and import the original file
		String user = "cjtest";
		String originalFileName = "exported_biopax";
		BioPAXParser parser = new BioPAXParser(
				"/home/chenjing/Dropbox/Network_test_files/" + originalFileName + ".owl", 
				user, 
				NdexDatabase.getInstance(), originalFileName, "");
		
		parser.parseFile();
		String networkUUIDString = parser.getNetworkUUID();
		
		// Export the NDEx network
		ODatabaseDocumentTx connection = NdexDatabase.getInstance().getAConnection();
		BioPAXNetworkExporter exporter = new BioPAXNetworkExporter(connection);
		String exportedFilePath = "/home/chenjing/Dropbox/Network_test_files/" + originalFileName + "_export" + ".owl";
		FileOutputStream out = new FileOutputStream (exportedFilePath);
		
		exporter.exportNetwork(UUID.fromString(networkUUIDString), out);
		
		// Import again
		BioPAXParser parser2 = new BioPAXParser(
				exportedFilePath, 
				user, 
				NdexDatabase.getInstance(), originalFileName, "");
		
		parser2.parseFile();
		
		// Compare parser metrics
/*		System.out.println("entities: " + parser.getEntityCount() + " -> " + parser2.getEntityCount());
		System.out.println("pubXrefs: " + parser.getPubXrefCount() + " -> " + parser2.getPubXrefCount());
		System.out.println("uXrefs: " + parser.getuXrefCount() + " -> " + parser2.getuXrefCount());
		System.out.println("rXrefs: " + parser.getrXrefCount() + " -> " + parser2.getrXrefCount());
		System.out.println("literalProps: " + parser.getLiteralPropertyCount() + " -> " + parser2.getLiteralPropertyCount());
		System.out.println("referenceProps: " + parser.getReferencePropertyCount() + " -> " + parser2.getReferencePropertyCount());
		*/
		
        connection.close();
        out.close();
		NdexDatabase.close();
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		//fail("Not yet implemented");
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
