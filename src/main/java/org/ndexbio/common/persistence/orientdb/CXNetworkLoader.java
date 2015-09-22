package org.ndexbio.common.persistence.orientdb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.cxio.aspects.datamodels.EdgesElement;
import org.cxio.aspects.datamodels.NodesElement;
import org.cxio.core.CxElementReader;
import org.cxio.core.CxReader;
import org.cxio.core.interfaces.AspectElement;
import org.cxio.core.interfaces.AspectFragmentReader;
import org.cxio.metadata.MetaData;
import org.cxio.metadata.MetaDataElement;
import org.cxio.util.Util;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.cx.aspect.GeneralAspectFragmentReader;
import org.ndexbio.common.cx.aspect.GeneralAspectFragmentWriter;
import org.ndexbio.model.cx.NdexNetworkStatus;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;

import com.google.common.cache.LoadingCache;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class CXNetworkLoader implements AutoCloseable {

	private InputStream inputStream;
	private NdexDatabase db;
	private ODatabaseDocumentTx connection;
	private String ownerAcctName;
	
	protected LoadingCache<String, ODocument>  nodeIdCache;
	protected LoadingCache<String, ODocument>  edgeIdCache;
	protected LoadingCache<String, ODocument>  citationIdCache;
	protected LoadingCache<String, ODocument>  supportIdCache;

	
	private Map<String, MetaDataElement> metaData;
	
	public CXNetworkLoader(InputStream iStream,String ownerAccountName)  throws NdexException {
		this.inputStream = iStream;
		
		db = NdexDatabase.getInstance();
		
		connection = db.getAConnection();
		ownerAcctName = ownerAccountName;
		metaData = new TreeMap<>();
		
		
	}

	
	//TODO: will modify this function to return a CX version of NetworkSummary object.
	public void persistCXNetwork() throws IOException {
		
		NdexNetworkStatus netStatus = null;
		
		Set<AspectFragmentReader> readers = Util.getAllAvailableAspectFragmentReaders();
		readers.add(new GeneralAspectFragmentReader (NdexNetworkStatus.NAME,
				NdexNetworkStatus.class));
		
		CxElementReader cxreader = CxElementReader.createInstance(inputStream, true,
				   readers);
		
		for (MetaData md : cxreader.getMetaData()) {
			for ( MetaDataElement e : md.asListOfMetaDataElements() ) {
				String name = e.getName();
				System.out.println(name);
			}
			
		}
		
		
		
		
		for ( AspectElement elmt : cxreader ) {
			String aspectName = elmt.getAspectName();
			if ( aspectName.equals(NodesElement.NAME)) {
			    // Do something with a NodesElement...
			    NodesElement ne = (NodesElement) elmt;
			    String nid = ne.getId();
			    createCXNodeById(nid);
			       
			} else if (aspectName.equals(NdexNetworkStatus.NAME)) {
				netStatus = (NdexNetworkStatus) elmt;
			} else if ( aspectName.equals(EdgesElement.NAME)) {
				EdgesElement ee = (EdgesElement) elmt;
				
			}

		}
		
	}
	
	private void createCXNodeById(String nid) {
		
	}


	@Override
	public void close() throws Exception {
		connection.close();
	}
	
}
