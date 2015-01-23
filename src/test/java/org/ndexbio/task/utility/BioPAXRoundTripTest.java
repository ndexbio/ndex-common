package org.ndexbio.task.utility;

import static org.junit.Assert.*;

import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
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
    	NdexAOrientDBConnectionPool.createOrientDBConnectionPool(
    			configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd(),1);
    	
    	
		NdexDatabase db = new NdexDatabase(configuration.getHostURI());
		
		// Parse and import the original file
		String user = "cjtest";
		String originalFileName = "testnfkb";
		BioPAXParser parser = new BioPAXParser(
				"/opt/biopax/L3/" + originalFileName + ".owl", 
				user, 
				db);
		
		parser.parseFile();
		String networkUUIDString = parser.getNetworkUUID();
		
		// Export the NDEx network
		ODatabaseDocumentTx connection = db.getAConnection();
		BioPAXNetworkExporter exporter = new BioPAXNetworkExporter(connection);
		String exportedFilePath = "/opt/biopax/L3/" + originalFileName + "_export" + ".owl";
		FileOutputStream out = new FileOutputStream (exportedFilePath);
		
		exporter.exportNetwork(UUID.fromString(networkUUIDString), out);
		
		// Import again
		BioPAXParser parser2 = new BioPAXParser(
				exportedFilePath, 
				user, 
				db);
		
		parser2.parseFile();
		
		// Compare parser metrics
		System.out.println("entities: " + parser.getEntityCount() + " -> " + parser2.getEntityCount());
		System.out.println("pubXrefs: " + parser.getPubXrefCount() + " -> " + parser2.getPubXrefCount());
		System.out.println("uXrefs: " + parser.getuXrefCount() + " -> " + parser2.getuXrefCount());
		System.out.println("rXrefs: " + parser.getrXrefCount() + " -> " + parser2.getrXrefCount());
		System.out.println("literalProps: " + parser.getLiteralPropertyCount() + " -> " + parser2.getLiteralPropertyCount());
		System.out.println("referenceProps: " + parser.getReferencePropertyCount() + " -> " + parser2.getReferencePropertyCount());
		
		

		db.close();
		NdexAOrientDBConnectionPool.close();
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
