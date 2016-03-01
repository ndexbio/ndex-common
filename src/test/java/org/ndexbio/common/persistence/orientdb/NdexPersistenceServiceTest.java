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
package org.ndexbio.common.persistence.orientdb;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Node;


public class NdexPersistenceServiceTest {

	private static NdexPersistenceService service;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		service = new NdexPersistenceService(NdexDatabase.createNdexDatabase("http://localhost", "plocal:/opt/ndex/orientdb/databases/cjtest", "admin", "admin", 10));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		//service.commit();
	}

	@Test
	public void test() throws Exception {
		service.createNewNetwork("Support", "cjtest network", "0.1");
		
		RawNamespace rns1 = new RawNamespace("ns1", "http://foo.bar.com/"); 
		Namespace ns1 = service.getNamespace(rns1);
		
		RawNamespace rns2 = new RawNamespace("ns2", "http://something.com/");
		Namespace ns2 = service.getNamespace(rns2);

		RawNamespace rns3 = new RawNamespace("ns1", "http://foo.bar.com/"); 
		Namespace ns3 = service.getNamespace(rns3);

		
	/*	String s3 = "http://foo.newdomain.com/P001";
		BaseTerm t3 = service.getBaseTermId(s3);

		String s1 = "Y0002";
		BaseTerm t1 = service.getBaseTerm(s1);
		
		String s2 = "ns1:y003";
		BaseTerm t2 = service.getBaseTerm(s2);
		
		
		String s4 = "http://something.com/XYC002";
		BaseTerm t4 = service.getBaseTerm(s4); */
		
		String n1String = "term1";
		Long n1 = service.getNodeIdByBaseTerm( n1String);

		String n2String = "Y00002";
		Long n2 = service.getNodeIdByBaseTerm(n2String);
		
	//	Edge e = service.createEdge(n1, n2, t4, null, null, null);
		
	    service.persistNetwork();
	}

/*	@Test
	public void testBaseTermInsert() throws NdexException {
		
		
	} */
}
