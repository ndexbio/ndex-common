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
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.User;
import org.ndexbio.task.Configuration;
import org.ndexbio.task.utility.XGMMLNetworkExporter;
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
    	NdexAOrientDBConnectionPool.createOrientDBConnectionPool(
    			configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd(),1);
    	

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	@Test 
	public void testConcurrency() throws NdexException {
		ODatabaseDocumentTx conn1 = NdexAOrientDBConnectionPool.getInstance().acquire();
		UserDAO dao1 = new UserDAO(conn1);
		
		User u1 = dao1.getUserByAccountName("cjtest");
		ODatabaseDocumentTx conn2 = NdexAOrientDBConnectionPool.getInstance().acquire();
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
