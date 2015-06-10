/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
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

public class NdexNetworkCloneServiceTest {

	@Test
	public void test() throws NdexException, ExecutionException {
		NdexDatabase db = NdexDatabase.createNdexDatabase("http://localhost", "plocal:/opt/ndex/orientdb/databases/cjtest", "admin", "admin", 10);
		ODatabaseDocumentTx connection = db.getAConnection();
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
