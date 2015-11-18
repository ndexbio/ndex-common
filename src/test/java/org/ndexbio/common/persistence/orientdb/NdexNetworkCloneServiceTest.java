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
package org.ndexbio.common.persistence.orientdb;

import static org.junit.Assert.*;

import java.util.UUID;
import java.util.concurrent.ExecutionException;









import org.junit.Test;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.Network;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.index.OSimpleKeyIndexDefinition;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class NdexNetworkCloneServiceTest {

	@Test
	public void test() throws Exception {
		NdexDatabase db = NdexDatabase.createNdexDatabase("http://localhost", "plocal:/opt/ndex/orientdb/databases/cjtest", "admin", "admin", 10);
		ODatabaseDocumentTx connection = db.getAConnection();
		
		// tryout the manual index.
		OIndexManager idxManager = connection.getMetadata().getIndexManager();
	/*	OIndex myIdx = idxManager.createIndex("mytestIdx", OClass.INDEX_TYPE.UNIQUE.toString(), new OSimpleKeyIndexDefinition(OType.STRING),
				null,null, null); 
		
		ODocument d1 = new ODocument("support").fields("foo", "1", "bar",11).save();
		ODocument d2 = new ODocument("support").fields("foo", "2", "bar", 22).save();
		ODocument d3 = new ODocument("support").fields("foo", "3","bar",33).save();
		
		myIdx.put("1", d1);
		myIdx.put("2", d2);
		myIdx.put("3", d3); 
		
		connection.commit();
		connection.close();
		
		connection = db.getAConnection();
		// tryout read the manual index.
		idxManager = connection.getMetadata().getIndexManager();
		myIdx = idxManager.getIndex("mytestIdx");
		ORID r4 = (ORID)myIdx.get("2");
		ODocument d4 = new ODocument(r4);
		d4.reload();
		System.out.print(d4);
		
		d4 = new ODocument((ORID)myIdx.get("1"));
		System.out.print(d4);
		d4 = new ODocument((ORID)myIdx.get("3"));
		System.out.print(d4);
		d4 = new ODocument((ORID)myIdx.get("4"));
		System.out.print(d4);
				*/
		
		NetworkDAO dao = new NetworkDAO ( connection);
	//	Network network =  dao.getNetworkById(UUID.fromString("4842a831-1e5c-11e4-9f34-90b11c72aefa"));
		Network network =  dao.getNetworkById(UUID.fromString(
				"503cfcd7-20ae-11e4-b3cf-001f3bca188f"));
		//		"3b42c607-1f3f-11e4-907f-90b11c72aefa"));
		//		"1db5f2c1-1e5e-11e4-9f34-90b11c72aefa"));
		network.setName("---" + network.getName() + "---");
		NdexNetworkCloneService service = new NdexNetworkCloneService(db, network, "Support");
		service.cloneNetwork();
		connection.close();
		service.close();
		db.close();
		
	}

}
