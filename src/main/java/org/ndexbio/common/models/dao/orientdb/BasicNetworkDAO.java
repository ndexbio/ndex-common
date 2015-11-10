package org.ndexbio.common.models.dao.orientdb;

import java.util.List;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.PropertiedObject;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class BasicNetworkDAO implements AutoCloseable {

	protected ODatabaseDocumentTx db;
	private OIndex<?> btermIdIdx;
    private OIndex<?> nsIdIdx;
    private OIndex<?> citationIdIdx;
    private OIndex<?> supportIdIdx;
    private OIndex<?> funcIdIdx;
    private OIndex<?> reifiedEdgeIdIdx;
    private OIndex<?> nodeIdIdx;
	private OIndex<?> edgeIdIdx;
	
	public BasicNetworkDAO() throws NdexException {
		db  = NdexDatabase.getInstance().getAConnection();	
		btermIdIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_bterm_id);
    	nsIdIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_ns_id);
    	citationIdIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_citation_id);
    	supportIdIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_support_id);
        funcIdIdx  = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_function_id);
        reifiedEdgeIdIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_reifiededge_id);
        nodeIdIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_node_id);
        edgeIdIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_edge_id);
	}

	@Override
	public void close() throws Exception {
		db.commit();
		db.close();
	}
	
	public ODatabaseDocumentTx getDbConnection() { return db; }
	
	protected static void getPropertiesFromDoc(ODocument doc, PropertiedObject obj) {
	    	List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);
	    	if (props != null && props.size()> 0) {
	    		for (NdexPropertyValuePair p : props)
	    			obj.getProperties().add(p);
	    	}
	    }

	protected ODocument getBasetermDocById (long id) throws ObjectNotFoundException {
	    	ORecordId rid =   (ORecordId)btermIdIdx.get( id ); 
	        
	    	if ( rid != null) {
	    		return rid.getRecord();
	    	}
	  
	    	throw new ObjectNotFoundException(NdexClasses.BaseTerm, id);
	}
	    

	protected ODocument getNodeDocById (long id) throws ObjectNotFoundException {
    	ORecordId rid =   (ORecordId)nodeIdIdx.get( id ); 
        
    	if ( rid != null) {
    		return rid.getRecord();
    	}
  
    	throw new ObjectNotFoundException(NdexClasses.Node, id);
	}
    
	protected ODocument getEdgeDocById (long id) throws ObjectNotFoundException {
    	ORecordId rid =   (ORecordId)edgeIdIdx.get( id ); 
        
    	if ( rid != null) {
    		return rid.getRecord();
    	}
  
    	throw new ObjectNotFoundException(NdexClasses.Edge, id);
	}
    


    protected ODocument getFunctionDocById (long id) throws ObjectNotFoundException {
    	ORecordId rid =   (ORecordId)funcIdIdx.get( id ); 
        
    	if ( rid != null) {
    		return rid.getRecord();
    	}
  
    	throw new ObjectNotFoundException(NdexClasses.FunctionTerm, id);
    }
    
    protected ODocument getReifiedEdgeDocById (long id) throws ObjectNotFoundException {
    	ORecordId rid =   (ORecordId)reifiedEdgeIdIdx.get( id ); 
        
    	if ( rid != null) {
    		return rid.getRecord();
    	}
  
    	throw new ObjectNotFoundException(NdexClasses.ReifiedEdgeTerm, id);
    }
    
    protected ODocument getCitationDocById (long id) throws ObjectNotFoundException {
    	ORecordId rid =   (ORecordId)citationIdIdx.get( id ); 
        
    	if ( rid != null) {
    		return rid.getRecord();
    	}
  
    	throw new ObjectNotFoundException(NdexClasses.Citation, id);
    }
    
    protected ODocument getSupportDocById (long id) throws ObjectNotFoundException {
    	ORecordId rid =   (ORecordId)supportIdIdx.get( id ); 
        
    	if ( rid != null) {
    		return rid.getRecord();
    	}
  
    	throw new ObjectNotFoundException(NdexClasses.Citation, id);
    }
    
    protected ODocument getNamespaceDocById(long id) throws ObjectNotFoundException {
    	ORecordId cIds =  (ORecordId) nsIdIdx.get( id ); 

    	if ( cIds !=null)
    		return cIds.getRecord();
    	
    	throw new ObjectNotFoundException(NdexClasses.Namespace, id);
    }
    
	protected static Long getSIDFromDoc(ODocument doc) {
		Long SID = doc.field(NdexClasses.Element_SID);
		
		if ( SID != null)  {
			return SID;
		}
		return doc.field(NdexClasses.Element_ID);
	}

	protected ODocument getRecordByUUIDStr(String id) 
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
	
    
}
