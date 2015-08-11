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
package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.PropertyGraphNetwork;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.record.impl.ODocument;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class NetworkDAOTest {

	
	
	static ODatabaseDocumentTx db;
	
	@BeforeClass
    public static void initializeTests() throws NdexException 
    {
		db = NdexDatabase.createNdexDatabase("http://localhost", "plocal:/opt/ndex/orientdb/databases/ndex", "admin", "admin", 10)
				.getAConnection();
    	
    }
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.close();
	}

	@Test
	public void test0() throws ObjectNotFoundException, NdexException {
		NetworkDAO dao = new NetworkDAO(db);
	
		
		ODocument networkDoc = dao.getRecordByUUIDStr("6915a88d-ea14-11e4-8afc-92907a9fabf5", NdexClasses.Network);
		
		Object obj2 = networkDoc.field("in_admin");
		System.out.println(obj2);
		
/*		ORidBag obj = networkDoc.field("out_"+NdexClasses.Network_E_BaseTerms);
		for ( OIdentifiable o : obj ) {
		
		System.out.println(o);
		} */
		int i = 0;
		for ( OIdentifiable id : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_BaseTerms)) {
			ODocument d = (ODocument)id;
			System.out.println(i++ + "\t"+ d.toString());
		};
		
		Object f = networkDoc.field("out_"+NdexClasses.Network_E_Namespace);
		if ( f instanceof ORidBag ) {
			ORidBag e = (ORidBag)f;
			for ( OIdentifiable id : e) {
				System.out.println(id);
			}
		} else {
			System.out.println(f);
		}
		
//		System.out.println(e.toString());
		
		List<NdexPropertyValuePair> properties = new ArrayList<NdexPropertyValuePair>();
		
		properties.add(new NdexPropertyValuePair ("something", "good"));
		
	//	dao.setNetworkProperties(UUID.fromString(
	//			"bee7d26d-37a6-11e4-8cf5-90b11c72aefa"), properties);
		
		
		boolean s =Helper.canRemoveAdmin(db, "473d36ff-28d0-11e4-a48c-90b11c72aefa", "9e97e66f-28c9-11e4-a01a-90b11c72aefa");

		System.out.println(s);
		s =Helper.canRemoveAdmin(db, "473d36ff-28d0-11e4-a48c-90b11c72aefa", "9e9dd9e5-28c9-11e4-a01a-90b11c72aefa");

		
//		Permissions s  = Hdao.getNetworkPermissionByAccout("473d36ff-28d0-11e4-a48c-90b11c72aefa", "9e97e66f-28c9-11e4-a01a-90b11c72aefa");
		
		System.out.println(s);
		
	}
/*	
	@Test
	public void testPropertyGraph() throws JsonProcessingException, NdexException {
		NetworkDAO dao = new NetworkDAO(db);
		
		PropertyGraphNetwork n = dao.getProperytGraphNetworkById(
				UUID.fromString("503cfcd7-20ae-11e4-b3cf-001f3bca188f"),
				0, 1000);
		
		System.out.println( n.toString() + "...");
	}

	
	@Test
	public void testdelete() throws ObjectNotFoundException, NdexException {
		NetworkDAO dao = new NetworkDAO(db);
		
		int r = dao.deleteNetwork("4842a831-1e5c-11e4-9f34-90b11c72aefa");
		
		System.out.println( r + "Vertex deleted from graph.");
	}
	*/
	@Test
    public void test2() throws NdexException, JsonProcessingException {
/*		NetworkDAO dao = new NetworkDAO(db);
		
		System.out.println(dao.checkPrivilege("Support", "e026e93c-1997-11e4-8f64-90b11c72aefa", Permissions.READ));

		Network network = dao.getNetwork(UUID.fromString("931cce19-180c-11e4-9525-00219b422d69"),
				0, 15);
		
		System.out.println(network);
		
		network = dao.getNetwork(UUID.fromString("b4e09a69-180c-11e4-b734-00219b422d69"),
				0, 15);
		
		System.out.println(network);
		
		network = dao.getNetworkById(UUID.fromString("cbe27f2d-180c-11e4-bd2c-00219b422d69"));
		
		System.out.println(network);

		
		PropertyGraphNetwork n = dao.getProperytGraphNetworkById( 
				UUID.fromString("e4e82535-1422-11e4-a931-90b11c72aefa"),
				
				//	UUID.fromString("bb952930-137a-11e4-b507-90b11c72aefa"),
				0,12);
		ObjectMapper mapper = new ObjectMapper(); // create once, reuse
		String s = mapper.writeValueAsString( n);
		System.out.println ( s);

		n = dao.getProperytGraphNetworkById( 
				UUID.fromString("c16614aa-094a-11e4-b7e2-001f3bca188f"),
				1,12);
		s = mapper.writeValueAsString( n);
		System.out.println ( s);
        		
		
	}

	@Test
	public void test2 () throws Exception {
		NetworkDAO dao = new NetworkDAO(db);
		PropertyGraphNetwork pn = dao.getProperytGraphNetworkById(UUID.fromString("e4e82535-1422-11e4-a931-90b11c72aefa"), 0,12);
		
		for (Long l : pn.getNodes().keySet()) 
			System.out.println(l);
		
		for (PropertyGraphEdge e: pn.getEdges()) {
			System.out.println(
					e.getSubjectId()+ "\t" +e.getObjectId()
					//		pn.getNodes().get(e.getSubjectId()).getName() + "\t" +
			//		pn.getNodes().get(e.getObjectId()).getName()
					);
			
		}

		NdexProperty p1 = new NdexProperty("foo", "bestBar");	
		
		int i = 0;
		for ( NdexProperty p : pn.getProperties()) {
			if ( p.getPredicateString().equals(PropertyGraphNetwork.uuid) ) {
				break;
			}
			i++;
		}
		pn.getProperties().remove(i);
		
		
		pn.getProperties().add(p1);
		i = 0 ;
		for ( PropertyGraphNode n :  pn.getNodes().values()) {
			NdexProperty p2 = new NdexProperty ("fooN", "bestBarInNode-"+ i);
			n.getProperties().add(p2);
		}
		
		NdexDatabase db = new NdexDatabase();
		
		PropertyGraphLoader loader = new PropertyGraphLoader(db);
		pn.setName("my new name");
		NetworkSummary nks = loader.insertNetwork(pn, "Support");
		
		
		PropertyGraphNetwork n = dao.getProperytGraphNetworkById(nks.getExternalId(),	0,12);
		
		ObjectMapper mapper = new ObjectMapper(); // create once, reuse
		String s = mapper.writeValueAsString( n);
		System.out.println ( s); */
	}
	
}

