package org.ndexbio.common.models.dao.orientdb;

import java.util.Iterator;
import java.util.List;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.PropertiedObject;
import org.ndexbio.model.object.network.CXEdge;
import org.ndexbio.model.object.network.Namespace;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class SingleNetworkDAO implements AutoCloseable {
	
	private ODatabaseDocumentTx db;
	private ODocument networkDoc;

	public SingleNetworkDAO ( String UUID) throws NdexException {
		db  = NdexDatabase.getInstance().getAConnection();	
		networkDoc = getRecordByUUIDStr(UUID);
	}

	private ODocument getRecordByUUIDStr(String id) 
			throws ObjectNotFoundException, NdexException {
		
			OIndex<?> Idx;
			OIdentifiable record = null;
			
			Idx = this.db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_UUID);
			OIdentifiable temp = (OIdentifiable) Idx.get(id);
			if((temp != null) )
				record = temp;
			else	
				throw new ObjectNotFoundException("Network with ID: " + id + " doesn't exist.");
			
			return (ODocument) record.getRecord();
	}
	
    private Iterable<ODocument> getNetworkElements(String elementEdgeString) {	
    	
    	Object f = networkDoc.field("out_"+ elementEdgeString);
    	
    	if ( f == null) return Helper.emptyDocs;
    	
    	if ( f instanceof ODocument)
    		 return new OrientDBIterableSingleLink((ODocument)f);
    	
    	Iterable<ODocument> iterable = (Iterable<ODocument>)f;
		return iterable;
    	     
    }
	
	public Iterator<Namespace> getNamespaces() {
		return new NamespaceIterator(getNetworkElements(NdexClasses.Network_E_Namespace));
	}
	
	public Iterator<String>  getNodeSIDs () {
		return new NodeSIDIterator(getNetworkElements(NdexClasses.Network_E_Nodes));
	}
	
	
	public Iterator<CXEdge>  geCXEdges () {
		return new CXEdgeIterator(getNetworkElements(NdexClasses.Network_E_Edges));
	}
	
	@Override
	public void close() throws Exception {
		db.commit();
		db.close();
	}
	
    
    protected static void getPropertiesFromDoc(ODocument doc, PropertiedObject obj) {
    	List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);
    	if (props != null && props.size()> 0) 
    		obj.setProperties(props);
    }

}
