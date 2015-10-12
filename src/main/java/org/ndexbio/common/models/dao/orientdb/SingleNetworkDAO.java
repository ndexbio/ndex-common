package org.ndexbio.common.models.dao.orientdb;


import java.util.Iterator;


import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.task.Configuration;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class SingleNetworkDAO extends BasicNetworkDAO {
		
	public static final String CXsrcFormatAttrName="ndex:sourceFormat";
	protected ODocument networkDoc;
	
	private OrientVertex networkVertex;
	
    protected OrientGraph graph;
    protected String uuid;

    
	public SingleNetworkDAO ( String UUID) throws NdexException {
		super();
		uuid = UUID;
		networkDoc = getRecordByUUIDStr(UUID);
		
		graph =  new OrientGraph(db,false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);
		networkVertex = graph.getVertex(networkDoc);
		
		
	}

	
	protected long getVertexCount(String edgeName) {
		return networkVertex.countEdges(Direction.OUT,edgeName);
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
	
    protected Iterable<ODocument> getNetworkElements(String elementEdgeString) {	
    	
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
		
	/*
	public Iterable<CitationElement>  getCXCitations () {
		return new CXCitationCollection(getNetworkElements(NdexClasses.Network_E_Citations),db);
	}
	
	*/
	/*
	private void writeDocPropertiesAsCX(ODocument doc, CxWriter cxwtr) throws IOException {
	   	List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);
    	if ( props !=null) {
    		cxwtr.startAspectFragment(EdgeAttributesElement.NAME);
    		for ( NdexPropertyValuePair p : props ) {
    			EdgeAttributesElement ep = new EdgeAttributesElement ( null, p.getPredicateString(), p.getValue(), p.getDataType());
    			cxwtr.writeAspectElement(ep);
    		}
    		cxwtr.endAspectFragment();
    	}
	}
	*/


    protected String getBaseTermStringById(long id) throws ObjectNotFoundException {
    	ODocument doc = getBasetermDocById(id);
    	return  getBaseTermStringFromDoc(doc);
    	
    }
    
    protected String getBaseTermStringFromDoc(ODocument doc) throws ObjectNotFoundException {
	    String name = doc.field(NdexClasses.BTerm_P_name);
    	
	    String prefix = doc.field(NdexClasses.BTerm_P_prefix);
	    if (prefix !=null)
	    	name = prefix + name;
	    
    	Long nsId = doc.field(NdexClasses.BTerm_NS_ID); 
    	if ( nsId == null || nsId.longValue() <= 0) 
    		return name;
    	
    	ODocument nsdoc = getNamespaceDocById(nsId);
        prefix = nsdoc.field(NdexClasses.ns_P_prefix)	;
    	return prefix + ":"+ name;
    	
    //	return nsdoc.field(NdexClasses.ns_P_uri) + name;
    }
    


}
