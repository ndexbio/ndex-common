package org.ndexbio.common.persistence.orientdb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.cxio.aspects.datamodels.NodesElement;
import org.cxio.core.CxElementReader;
import org.cxio.core.CxReader;
import org.cxio.core.interfaces.AspectElement;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class CXNetworkLoader {

	private InputStream inputStream;
	private NdexDatabase db;
	private ODatabaseDocumentTx connection;
	private String ownerAcctName;
	
	public CXNetworkLoader(InputStream iStream,String ownerAccountName) throws NdexException {
		this.inputStream = iStream;
		
		db = NdexDatabase.getInstance();
		
		connection = db.getAConnection();
		ownerAcctName = ownerAccountName;
		
	}

	
	//TODO: will modify this function to return a CX version of NetworkSummary object.
	public void persistCXNetwork() throws IOException {
		
		
		CxElementReader cxreader = CxElementReader.createInstance(inputStream, true, null);
		
		for ( AspectElement elmt : cxreader ) {
			if (elmt.getAspectName() == NodesElement.NAME) {
			    // Do something with a NodesElement...
			    NodesElement ne = (NodesElement) elmt;
			    String nid = ne.getId();
			    createCXNodeById(nid);
			       
			}

		}
		
	}
	
	private void createCXNodeById(String nid) {
		
	}
	
}
