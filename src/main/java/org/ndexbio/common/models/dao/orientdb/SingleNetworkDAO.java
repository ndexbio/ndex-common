package org.ndexbio.common.models.dao.orientdb;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.cxio.aspects.datamodels.EdgesElement;
import org.cxio.aspects.datamodels.NodesElement;
import org.cxio.core.CxWriter;
import org.cxio.core.interfaces.AspectElement;
import org.cxio.core.interfaces.AspectFragmentWriter;
import org.cxio.util.Util;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.PropertiedObject;
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
	
	public Iterable<NodesElement>  getCXNodes () {
		return new CXNodeCollection(getNetworkElements(NdexClasses.Network_E_Nodes));
	}
	
	
	public Iterable<EdgesElement>  getCXEdges () {
		return new CXEdgeCollection(getNetworkElements(NdexClasses.Network_E_Edges));
	}
	
	
	public void writeNetworkInCX(OutputStream out, final boolean use_default_pretty_printer) throws IOException {
        CxWriter cxwtr = CxWriter.createInstance(out, use_default_pretty_printer);
        
        for (AspectFragmentWriter afw : Util.getAllAvailableAspectFragmentWriters() ) {
        	cxwtr.addAspectFragmentWriter(afw);
        }
        
        cxwtr.start();
        
        List<AspectElement> aspect_elements = new ArrayList<AspectElement>(1);
        
        for ( EdgesElement edge : getCXEdges()) {
        	aspect_elements.add(edge);
        	cxwtr.writeAspectElements(aspect_elements);
        	aspect_elements.remove(0);
        }

        for ( NodesElement edge : getCXNodes()) {
        	aspect_elements.add(edge);
        	cxwtr.writeAspectElements(aspect_elements);
        	aspect_elements.remove(0);
        }
        
        cxwtr.end();

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
