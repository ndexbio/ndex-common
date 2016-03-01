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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.cxio.aspects.datamodels.AbstractAttributesAspectElement;
import org.cxio.aspects.datamodels.ATTRIBUTE_DATA_TYPE;
import org.cxio.misc.OpaqueElement;
import org.junit.Test;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.UserDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.Network;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.index.OIndexManager;

public class NdexCXLoaderTest {

	private String testUser = "cj2";
	private String passwd  = testUser;
	
	private String testFile = 
			"/Users/chenjing/working/cx/ligand.cx";
		//	"/Users/chenjing/working/cx/tiny_corpus.cx";

	@Test
	public void test() throws FileNotFoundException, Exception {
		
		OpaqueElement o = new OpaqueElement("foo", "null") ;
		System.out.println(o.toJsonString());
		
	//	String foo = AbstractAttributesAspectElement.ATTRIBUTE_TYPE.BOOLEAN.toString();
		
	//	System.out.println(AbstractAttributesAspectElement.ATTRIBUTE_TYPE.valueOf(foo));
		NdexDatabase db = NdexDatabase.createNdexDatabase("http://localhost", "plocal:/opt/ndex/orientdb/databases/ndex", "admin", "admin", 10);
		
		try (UserDocDAO dao = new UserDocDAO(db.getAConnection())) {
	    	
			Helper.createUserIfnotExist(dao, testUser,
				"foobartest123@something.net", 
				passwd);
		}		
		
		try (CXNetworkLoader loader = new CXNetworkLoader(new FileInputStream(testFile), testUser)) {
			loader.persistCXNetwork();
		}
		
		db.close();
		
	}
	
	
}
