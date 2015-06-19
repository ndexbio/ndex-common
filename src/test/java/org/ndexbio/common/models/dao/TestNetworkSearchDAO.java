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
package org.ndexbio.common.models.dao;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkSearchDAO;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.SimpleNetworkQuery;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.NewUser;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class TestNetworkSearchDAO   {
	//relys on test database
	
	private static NetworkSearchDAO dao;
	private static NdexDatabase database;
	private static ODatabaseDocumentTx  localConnection;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// For acquiring connections from the pool
		database =NdexDatabase.createNdexDatabase("http://localhost", "plocal:/opt/ndex/orientdb/databases/cjtest", "admin", "admin", 10);
		
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
    public void findNetworks_noUser_noAccountSpecifier() {
    	
    	try {

	    	final SimpleNetworkQuery simpleQuery = new SimpleNetworkQuery();
	    	simpleQuery.setSearchString("ca");
	    	simpleQuery.setAccountName("");
	    	
	    	assertTrue(!dao.findNetworks(simpleQuery, 0, 1, null).isEmpty());
	    	//assertTrue(dao.findNetworks(simpleQuery, 0, 1).size() == 1);
    	
		} catch (Exception e) {
			
			fail(e.getMessage());
			e.printStackTrace();
			
		} 
    	
    }
	
	@Test
    public void findNetworks_noUser() {
    	
    	try {

	    	final SimpleNetworkQuery simpleQuery = new SimpleNetworkQuery();
	    	simpleQuery.setSearchString("ca");
	    	simpleQuery.setAccountName("Support");
	    	
	    	assertTrue(!dao.findNetworks(simpleQuery, 0, 1, null).isEmpty());
	    	//assertTrue(dao.findNetworks(simpleQuery, 0, 1).size() == 1);
    	
		} catch (Exception e) {
			
			fail(e.getMessage());
			e.printStackTrace();
			
		} 
    	
    }
	
	
	@Test
    public void findNetwork_noAccountSpecifier() {
    	
    	try {

    		User loggedInUser = new User();
    		loggedInUser.setAccountName("Support");
    		
	    	final SimpleNetworkQuery simpleQuery = new SimpleNetworkQuery();
	    	simpleQuery.setSearchString("ca");
	    	simpleQuery.setAccountName("");
	    	
	    	assertTrue(!dao.findNetworks(simpleQuery, 0, 1, loggedInUser).isEmpty());
	    	//assertTrue(dao.findNetworks(simpleQuery, 0, 1).size() == 1);
    	
		} catch (Exception e) {
			
			fail(e.getMessage());
			e.printStackTrace();
			
		} 
    	
    }
	
}
