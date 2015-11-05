package org.ndexbio.common.models.dao.orientdb;


import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.cxio.aspects.datamodels.EdgeAttributesElement;
import org.cxio.aspects.datamodels.EdgesElement;
import org.cxio.aspects.datamodels.NetworkAttributesElement;
import org.cxio.aspects.datamodels.NodeAttributesElement;
import org.cxio.aspects.datamodels.NodesElement;
import org.cxio.metadata.MetaDataCollection;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.cx.CitationElement;
import org.ndexbio.model.cx.EdgeCitationLinksElement;
import org.ndexbio.model.cx.EdgeSupportLinksElement;
import org.ndexbio.model.cx.FunctionTermElement;
import org.ndexbio.model.cx.NamespacesElement;
import org.ndexbio.model.cx.NdexNetworkStatus;
import org.ndexbio.model.cx.NodeCitationLinksElement;
import org.ndexbio.model.cx.NodeSupportLinksElement;
import org.ndexbio.model.cx.ReifiedEdgeElement;
import org.ndexbio.model.cx.SupportElement;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.network.Namespace;

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
    
	/**
	 * This function check if the given network contains all the give aspects. 
	 * @param aspectNames
	 * @return the aspect list that are not found in this network. if all aspects are found in the given network,
	 * an empty set will be returned.
	 *
	 */
	public Set<String> findMissingAspect ( Collection<String> aspectNames) {
		MetaDataCollection md = networkDoc.field(NdexClasses.Network_P_metadata);
		TreeSet<String> result = new TreeSet<>();
		if ( md !=null) {
			for (String aspect: aspectNames) {
				if ( md.getMetaDataElement(aspect) == null) 
					result.add(aspect);
			}
			return result;
		}
		
		for ( String aspect : aspectNames) {
			if (Arrays.binarySearch(NdexDatabase.NdexSupportedAspects, aspect) ==-1)
				result.add(aspect);
		}
		return result;
	}
   
	/*private boolean isNdexSupportedAspect(String aspect) {
		switch (aspect ) {
		case NodesElement.ASPECT_NAME: 
		case EdgesElement.ASPECT_NAME:
		case EdgeAttributesElement.ASPECT_NAME:
		case NodeAttributesElement.ASPECT_NAME:
		case NetworkAttributesElement.ASPECT_NAME:
		case CitationElement.ASPECT_NAME:
		case EdgeCitationLinksElement.ASPECT_NAME:
		case EdgeSupportLinksElement.ASPECT_NAME:
		case FunctionTermElement.ASPECT_NAME:
		case NamespacesElement.ASPECT_NAME:
		case NdexNetworkStatus.ASPECT_NAME:
		case NodeCitationLinksElement.ASPECT_NAME:
		case NodeSupportLinksElement.ASPECT_NAME:
		case ReifiedEdgeElement.ASPECT_NAME:
		case SupportElement.ASPECT_NAME:
			return true;
		default: 
			return false;
		} 

	} */

}
