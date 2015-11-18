package org.ndexbio.common.models.dao.orientdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.solr.SingleNetworkSolrIdxManager;
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

	protected ODatabaseDocumentTx localConnection;
	private OIndex<?> btermIdIdx;
    private OIndex<?> nsIdIdx;
    private OIndex<?> citationIdIdx;
    private OIndex<?> supportIdIdx;
    private OIndex<?> funcIdIdx;
    private OIndex<?> reifiedEdgeIdIdx;
    private OIndex<?> nodeIdIdx;
	private OIndex<?> edgeIdIdx;
	
	public BasicNetworkDAO() throws NdexException {
		this (NdexDatabase.getInstance().getAConnection());	
	}

	public BasicNetworkDAO(ODatabaseDocumentTx dbconnection) throws NdexException {
		localConnection  = dbconnection;	
		btermIdIdx = localConnection.getMetadata().getIndexManager().getIndex(NdexClasses.Index_bterm_id);
    	nsIdIdx = localConnection.getMetadata().getIndexManager().getIndex(NdexClasses.Index_ns_id);
    	citationIdIdx = localConnection.getMetadata().getIndexManager().getIndex(NdexClasses.Index_citation_id);
    	supportIdIdx = localConnection.getMetadata().getIndexManager().getIndex(NdexClasses.Index_support_id);
        funcIdIdx  = localConnection.getMetadata().getIndexManager().getIndex(NdexClasses.Index_function_id);
        reifiedEdgeIdIdx = localConnection.getMetadata().getIndexManager().getIndex(NdexClasses.Index_reifiededge_id);
        nodeIdIdx = localConnection.getMetadata().getIndexManager().getIndex(NdexClasses.Index_node_id);
        edgeIdIdx = localConnection.getMetadata().getIndexManager().getIndex(NdexClasses.Index_edge_id);
	}
	
	
	@Override
	public void close() throws Exception {
		localConnection.commit();
		localConnection.close();
	}
	
	public ODatabaseDocumentTx getDbConnection() { return localConnection; }
	
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
			
			Idx = this.localConnection.getMetadata().getIndexManager().getIndex(NdexClasses.Index_UUID);
			OIdentifiable temp = (OIdentifiable) Idx.get(id);
			if((temp != null) )
				record = temp;
			else	
				throw new ObjectNotFoundException("Network with ID: " + id + " doesn't exist.");
			
			return (ODocument) record.getRecord();
	}
	
	
	private void addNodeToIndex(SingleNetworkSolrIdxManager c, ODocument doc) throws SolrServerException, IOException, ObjectNotFoundException {
		String name =  doc.field(NdexClasses.Node_P_name);
		Long id = doc.field(NdexClasses.Element_ID);
	
		// get the alias term list
		List<String> aliasList = null;
		Collection<Long> aliases = doc.field(NdexClasses.Node_P_alias);
		if ( aliases != null) {
			aliasList = new ArrayList<>(aliases.size()*2+1);
			for ( Long aliasId : aliases) {
				addTermsToList(aliasId, aliasList);
			}
		}
		
		// get the relatedTo term list
		List<String> relatedTermList = null;
		Collection<Long> relatedTo = doc.field(NdexClasses.Node_P_relatedTo);
		if ( relatedTo !=null) {
			relatedTermList = new ArrayList<>(relatedTo.size()*2+1);
			for ( Long relatedToId : relatedTo) {
				addTermsToList(relatedToId, relatedTermList);
			}
		}
		// get the represent term list.
		List<String> representList =  null;
		Long represents = doc.field(NdexClasses.Node_P_represents);
		if ( represents !=null ) {
			String representTermType = doc.field(NdexClasses.Node_P_representTermType);
			representList = new ArrayList<>(2);
			if ( representTermType.equals(NdexClasses.BaseTerm)) {
				addTermsToList(represents, representList);
			} else if (representTermType.equals(NdexClasses.FunctionTerm)) {
				ODocument functionTermDoc = this.getFunctionDocById(represents);
				addFunctionTermsToList(functionTermDoc, representList);
			}  
			// ignore reified edge nodes	
		}
		c.addNodeIndex(id, name, representList, aliasList, relatedTermList);
	}
	
	private void addTermsToList(Long baseTermId, List<String> termList) throws ObjectNotFoundException {
		ODocument doc = getBasetermDocById(baseTermId);
		addTermsToList(doc, termList);
	}
	
	private void addTermsToList(ODocument doc, List<String> termList) throws ObjectNotFoundException {
		String name = doc.field(NdexClasses.BTerm_P_name);
		String prefix = doc.field(NdexClasses.BTerm_P_prefix);

		termList.add(name);
	   
		if (prefix !=null) {
			termList.add ( prefix + name);
		}    
		
	    Long nsId = doc.field(NdexClasses.BTerm_NS_ID); 
	    	
	    if ( nsId != null && nsId.longValue() > 0) {
	    	ODocument nsdoc = getNamespaceDocById(nsId);
	        prefix = nsdoc.field(NdexClasses.ns_P_prefix)	;
	    	termList.add ( prefix + ":"+ name);
	    }	
	}
	
	private void addFunctionTermsToList ( ODocument funcDoc, List<String> termList) throws ObjectNotFoundException {
		Long btId = funcDoc.field(NdexClasses.BaseTerm);
		addTermsToList(btId, termList); 
	
 	    Object f = funcDoc.field("out_"+ NdexClasses.FunctionTerm_E_paramter);

 	    if ( f == null)   {   // function without parameters.
 	    	return;
 	    }

 	    Iterable<ODocument> iterable =  ( f instanceof ODocument) ?
    		 (new OrientDBIterableSingleLink((ODocument)f) ) :  (Iterable<ODocument>)f;
	    
    	for (ODocument para : iterable) {
	    	if (para.getClassName().equals(NdexClasses.BaseTerm)) {
	    		addTermsToList(para, termList);
	    	} else {  // add nested functionTerm
	    		addFunctionTermsToList(para, termList);
	    	}
	    }
	}
	
	protected void createSolrIndex(ODocument networkDocument) throws SolrServerException, IOException, NdexException {
		SingleNetworkSolrIdxManager c = new SingleNetworkSolrIdxManager((String)networkDocument.field(NdexClasses.ExternalObj_ID));
		
		c.createIndex();
		
 	    Object f = networkDocument.field("out_"+ NdexClasses.Network_E_Nodes);
    	
    	if ( f == null) return;
    	
    	if ( f instanceof ODocument) {
    		addNodeToIndex(c,(ODocument)f);
    		c.commit();
    		return;
    	}
    	
    	for ( ODocument doc :  (Iterable<ODocument>)f ) {
    	    addNodeToIndex(c,doc);
    	}
    	c.commit();
	}
	
	
    
}
