package org.ndexbio.common.models.dao;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkSearchDAO;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.model.object.SimpleNetworkQuery;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.NewUser;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class TestNetworkSearchDAO  extends TestDAO {
	//relys on test database
	
	private static NetworkSearchDAO dao;
	private static NdexDatabase database;
	private static ODatabaseDocumentTx  localConnection;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// For acquiring connections from the pool
		database = new NdexDatabase();
		
		// For use with Orient Document API
		localConnection = database.getAConnection();
		
		dao = new NetworkSearchDAO(localConnection);
		
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	
		localConnection.close();
		database.close();
	
	}

	@Test
    public void findNetworks() {
    	
    	try {

	    	final SimpleNetworkQuery simpleQuery = new SimpleNetworkQuery();
	    	simpleQuery.setSearchString("ca");
	    	simpleQuery.setAccountName("support");
	    	
	    	assertTrue(!dao.findNetworks(simpleQuery, 0, 1).isEmpty());
	    	assertTrue(dao.findNetworks(simpleQuery, 0, 1).size() == 1);
    	
		} catch (Exception e) {
			
			fail(e.getMessage());
			e.printStackTrace();
			
		} 
    	
    }
	
	@Test
    public void findAllNetworks() {
    	
    	try {

	    	final SimpleNetworkQuery simpleQuery = new SimpleNetworkQuery();
	    	simpleQuery.setSearchString("*");
	    	simpleQuery.setAccountName("support");
	    	
	    	assertTrue(!dao.findNetworks(simpleQuery, 0, 1).isEmpty());
	    	assertTrue(dao.findNetworks(simpleQuery, 0, 1).size() == 1);
    	
		} catch (Exception e) {
			
			fail(e.getMessage());
			e.printStackTrace();
			
		} 
    	
    }
	
}
