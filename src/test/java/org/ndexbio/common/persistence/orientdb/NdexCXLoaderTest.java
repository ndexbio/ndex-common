package org.ndexbio.common.persistence.orientdb;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
	
	private String testFile = "/Users/chenjing/working/cx/ligand.cx";
	
	@Test
	public void test() throws FileNotFoundException, Exception {
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
