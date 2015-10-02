package org.ndexbio.common.models.dao.orientdb;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.cxio.aspects.datamodels.AbstractAttributesAspectElement.ATTRIBUTE_TYPE;
import org.cxio.aspects.datamodels.EdgeAttributesElement;
import org.cxio.aspects.datamodels.EdgesElement;
import org.cxio.aspects.datamodels.NetworkAttributesElement;
import org.cxio.aspects.datamodels.NodeAttributesElement;
import org.cxio.aspects.datamodels.NodesElement;
import org.cxio.core.CxWriter;
import org.cxio.core.interfaces.AspectElement;
import org.cxio.core.interfaces.AspectFragmentWriter;
import org.cxio.metadata.MetaData;
import org.cxio.metadata.MetaDataElement;
import org.cxio.util.Util;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.cx.aspect.GeneralAspectFragmentWriter;
import org.ndexbio.model.cx.SupportElement;
import org.ndexbio.model.cx.FunctionTermsElement;
import org.ndexbio.model.cx.NamespacesElement;
import org.ndexbio.model.cx.NdexNetworkStatus;
import org.ndexbio.model.cx.NodeCitationLinksElement;
import org.ndexbio.model.cx.NodeSupportLinksElement;
import org.ndexbio.model.cx.CXSimpleAttribute;
import org.ndexbio.model.cx.CitationElement;
import org.ndexbio.model.cx.CitationLinksElement;
import org.ndexbio.model.cx.EdgeCitationLinksElement;
import org.ndexbio.model.cx.EdgeSupportLinksElement;
import org.ndexbio.model.cx.ReifiedEdgeElement;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.PropertiedObject;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.FileFormat;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.task.Configuration;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class SingleNetworkDAO extends BasicNetworkDAO {
		
	public static final String CXsrcFormatAttrName="ndex:sourceFormat";
	private ODocument networkDoc;
    
	public SingleNetworkDAO ( String UUID) throws NdexException {
		super();
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
		
	public Iterable<CitationElement>  getCXCitations () {
		return new CXCitationCollection(getNetworkElements(NdexClasses.Network_E_Citations),db);
	}
	
	
	private CxWriter getNdexCXWriter(OutputStream out, boolean use_default_pretty_printer) throws IOException {
        CxWriter cxwtr = CxWriter.createInstance(out, use_default_pretty_printer);
        
        GeneralAspectFragmentWriter cfw = new GeneralAspectFragmentWriter(CitationElement.NAME);
        
        for (AspectFragmentWriter afw : Util.getAllAvailableAspectFragmentWriters() ) {
        	cxwtr.addAspectFragmentWriter(afw);
        }
        
        cxwtr.addAspectFragmentWriter(cfw);
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(SupportElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(NodeCitationLinksElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(EdgeCitationLinksElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(EdgeSupportLinksElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(NodeSupportLinksElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(FunctionTermsElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(NamespacesElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(ReifiedEdgeElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(NdexNetworkStatus.NAME));
        
        return cxwtr;
	}
	
	private void writeCXPreMetadata(CxWriter cxwtr) throws IOException {
		MetaData md = networkDoc.field(NdexClasses.Network_P_metadata);
		if ( md == null) {
			md= new MetaData();

			MetaDataElement node_meta = new MetaDataElement();

	        Timestamp lastUpdate = new Timestamp( ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime());
	        int edgecount = networkDoc.field(NdexClasses.Network_P_edgeCount);
	        int nodecount = networkDoc.field(NdexClasses.Network_P_nodeCount);
	        
	        node_meta.setName(NodesElement.NAME);
	        node_meta.setVersion("1.0");
	      //  node_meta.setIdCounter();
	        node_meta.setLastUpdate(lastUpdate.getTime());
	        node_meta.setElementCount(new Long(nodecount));
	        node_meta.setConsistencyGroup(1l);

	        md.addMetaDataElement(node_meta);

/*	        MetaDataElement citation_meta = new MetaDataElement();

	        citation_meta.setName(CitationElement.NAME);
	        citation_meta.setVersion("1.0");
	        citation_meta.setLastUpdate(lastUpdate.getTime());
	        citation_meta.setConsistencyGroup(1l);

	        md.addMetaDataElement(citation_meta); */
	        
	        MetaDataElement edge_meta = new MetaDataElement();

	        edge_meta.setName(EdgesElement.NAME);
	        edge_meta.setVersion("1.0");
	        edge_meta.setLastUpdate(lastUpdate.getTime());
	        edge_meta.setConsistencyGroup(1l);
	        edge_meta.setElementCount(new Long(edgecount));

	        md.addMetaDataElement(edge_meta);
	        

		}
		
        cxwtr.addPreMetaData(md);

	}
	
	public void writeNetworkInCX(OutputStream out, final boolean use_default_pretty_printer) throws IOException, NdexException {
        CxWriter cxwtr = getNdexCXWriter(out, use_default_pretty_printer);
        
        cxwtr.start();
        
		MetaData md = networkDoc.field(NdexClasses.Network_P_metadata);
		if ( md == null) {
			md= new MetaData();

			MetaDataElement node_meta = new MetaDataElement();

	        Timestamp lastUpdate = new Timestamp( ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime());
	        int edgecount = networkDoc.field(NdexClasses.Network_P_edgeCount);
	        int nodecount = networkDoc.field(NdexClasses.Network_P_nodeCount);
	        
	        node_meta.setName(NodesElement.NAME);
	        node_meta.setVersion("1.0");
	      //  node_meta.setIdCounter();
	        node_meta.setLastUpdate(lastUpdate.getTime());
	        node_meta.setElementCount(new Long(nodecount));
	        node_meta.setConsistencyGroup(1l);

	        md.addMetaDataElement(node_meta);

/*	        MetaDataElement citation_meta = new MetaDataElement();

	        citation_meta.setName(CitationElement.NAME);
	        citation_meta.setVersion("1.0");
	        citation_meta.setLastUpdate(lastUpdate.getTime());
	        citation_meta.setConsistencyGroup(1l);

	        md.addMetaDataElement(citation_meta); */
	        
	        MetaDataElement edge_meta = new MetaDataElement();

	        edge_meta.setName(EdgesElement.NAME);
	        edge_meta.setVersion("1.0");
	        edge_meta.setLastUpdate(lastUpdate.getTime());
	        edge_meta.setConsistencyGroup(1l);
	        edge_meta.setElementCount(new Long(edgecount));

	        md.addMetaDataElement(edge_meta);
	        

		}
		
        cxwtr.addPreMetaData(md);

//        writeCXPreMetadata(cxwtr);
        
        //write NdexStatus
        
        NdexNetworkStatus nstatus = new NdexNetworkStatus();
        
        int edgecount = networkDoc.field(NdexClasses.Network_P_edgeCount);
        int nodecount = networkDoc.field(NdexClasses.Network_P_nodeCount);
        Timestamp lastUpdate = new Timestamp( ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime());

        nstatus.setCreationTime(new Timestamp(((Date)networkDoc.field(NdexClasses.ExternalObj_cTime)).getTime()));
        nstatus.setEdgeCount(edgecount);
        nstatus.setNodeCount(nodecount);
        nstatus.setExternalId((String)networkDoc.field(NdexClasses.ExternalObj_ID));
        nstatus.setModificationTime(lastUpdate);
        nstatus.setNdexServerURI(Configuration.getInstance().getHostURI());
        nstatus.setOwner((String)networkDoc.field(NdexClasses.Network_P_owner));
        //nstatus.setPublished(isPublished);
        
        Long commitId = networkDoc.field(NdexClasses.Network_P_readOnlyCommitId);
        Long cacheId = networkDoc.field(NdexClasses.Network_P_cacheId);
        nstatus.setReadOnly(
        		commitId !=null && cacheId !=null && commitId.equals(cacheId));
     
        nstatus.setVisibility(
        		VisibilityType.valueOf((String)networkDoc.field(NdexClasses.Network_P_visibility)));
        
        writeNdexAspectElementAsAspectFragment(cxwtr, nstatus);
        
        
        // write name, desc and other properties;
        String title = networkDoc.field(NdexClasses.Network_P_name);
        if ( title != null ) {
            writeNdexAspectElementAsAspectFragment(cxwtr,
            		new NetworkAttributesElement(null,NdexClasses.Network_P_name, title));
        }
        
        String desc = networkDoc.field(NdexClasses.Network_P_desc);
        if ( desc != null) {
            writeNdexAspectElementAsAspectFragment(cxwtr,
            		new NetworkAttributesElement(null,NdexClasses.Network_P_desc, desc));
        }

        String version = networkDoc.field(NdexClasses.Network_P_version);
        if ( version !=null) {
        	writeNdexAspectElementAsAspectFragment(cxwtr,
            		new NetworkAttributesElement(null,NdexClasses.Network_P_version, version));
        }
        
        String srcFmtStr = networkDoc.field(NdexClasses.Network_P_source_format);
        if ( srcFmtStr !=null) {
        	writeNdexAspectElementAsAspectFragment(cxwtr,
            		new NetworkAttributesElement(null,CXsrcFormatAttrName, srcFmtStr));
        }
        List<NdexPropertyValuePair> props = networkDoc.field(NdexClasses.ndexProperties);
        if ( props !=null) {
        	for ( NdexPropertyValuePair p : props) {
            ATTRIBUTE_TYPE t = ATTRIBUTE_TYPE.STRING;
            try {
        	    t = NetworkAttributesElement.toDataType(p.getDataType().toLowerCase());
            } catch (IllegalArgumentException e) {}	
        	 writeNdexAspectElementAsAspectFragment(cxwtr,
             		new NetworkAttributesElement(p.getSubNetworkId(),p.getPredicateString(), p.getValue(),
             				t ));
        	}
        }
        
        //write namespaces
        NamespacesElement prefixtab = new NamespacesElement();
        
        for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Namespace))  {
           String prefix = doc.field(NdexClasses.ns_P_prefix);
           if ( prefix !=null ) {
        	   String uri = doc.field(NdexClasses.ns_P_uri);
        	   prefixtab.put(prefix, uri);
           }
        }
         
        if ( prefixtab .size() >0) {
        	writeNdexAspectElementAsAspectFragment(cxwtr, prefixtab);
        }
        
        Map<Long,String> citationIdMap = new TreeMap<> ();
        Map<Long,String> supportIdMap = new TreeMap<> ();
        Set<Long> repIdSet = new TreeSet<> ();

        
        
        for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Citations)) {
        	Long citationId = doc.field(NdexClasses.Element_ID);
        	String SID = writeCitationInCX(doc, cxwtr);
        	citationIdMap.put(citationId, SID);
        }
        
        for ( ODocument doc: getNetworkElements (NdexClasses.Network_E_Supports)) {
        	Long supportId = doc.field(NdexClasses.Element_ID);
        	String SID = writeSupportInCX(doc, cxwtr);
        	supportIdMap.put(supportId, SID);
        }
           
        
        for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Nodes)) {
        	writeNodeInCX(doc, cxwtr, repIdSet, citationIdMap, supportIdMap);
        }        
        
        for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Edges)) {
        	writeEdgeInCX(doc,cxwtr, citationIdMap, supportIdMap);
        }
        
        cxwtr.end();

	}
	
	private void writeEdgeInCX(ODocument doc, CxWriter cxwtr, Map<Long,String> citationIdMap,
			    Map<Long,String> supportIdMap ) throws ObjectNotFoundException, IOException {
		String SID = doc.field(NdexClasses.Element_SID);
		
		if ( SID ==null) {
			 SID = ((Long)doc.field(NdexClasses.Element_ID)).toString();
		}
		
		ODocument srcDoc = doc.field("in_"+ NdexClasses.Edge_E_subject);
		ODocument tgtDoc = doc.field("out_"+NdexClasses.Edge_E_object);
		
		String srcId = srcDoc.field(NdexClasses.Element_SID);
		if ( srcId == null )
			srcId = ( (Long)srcDoc.field(NdexClasses.Element_ID)).toString();
		
		String tgtId = tgtDoc.field(NdexClasses.Element_SID);
		if ( tgtId == null)
			tgtId = ((Long)tgtDoc.field(NdexClasses.Element_ID)).toString();
		
		String relation = null;
		Long predicate= doc.field(NdexClasses.Edge_P_predicateId);
		
		if ( predicate !=null) {
			relation = this.getBaseTermStringById(predicate);
		}
		
		EdgesElement e = new EdgesElement(SID, srcId, tgtId,relation);
		
		writeNdexAspectElementAsAspectFragment(cxwtr,e);
	  
    	// write other properties
	   	List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);
    	if ( props !=null) {
    		cxwtr.startAspectFragment(EdgeAttributesElement.NAME);
    		for ( NdexPropertyValuePair p : props ) {
    			EdgeAttributesElement ep = new EdgeAttributesElement ( p.getSubNetworkId(), 
    					SID, p.getPredicateString(), p.getValue(), 
    					EdgeAttributesElement.toDataType(p.getDataType().toLowerCase()));
    			cxwtr.writeAspectElement(ep);
    		}
    		cxwtr.endAspectFragment();
    	}
  //  	writeDocPropertiesAsCX(doc, cxwtr);
    	
    	//write citations
    	writeCitationsAndSupports(SID,  doc, cxwtr, citationIdMap,
			   supportIdMap , true );
       
	}
	
	private void writeCitationsAndSupports(String SID, ODocument doc,  CxWriter cxwtr, Map<Long,String> citationIdMap,
			    Map<Long,String> supportIdMap ,boolean isEdge ) throws ObjectNotFoundException, IOException {
	   	
		//write citations
    	Collection<Long> citations = doc.field(NdexClasses.Citation);
    	
    	if ( citations !=null) {
    		List<String> cids = new ArrayList<String> (citations.size());
    		
    		for ( Long citationId : citations) {
    			String csid = citationIdMap.get(citationId);
    			if ( csid == null) {
    				csid = writeCitationInCX(getCitationDocById(citationId), cxwtr);
    				citationIdMap.put(citationId, csid);
    			}
    			
    			cids.add(csid);
    		}
    		if (isEdge)
    		  writeNdexAspectElementAsAspectFragment(cxwtr, new EdgeCitationLinksElement(SID, cids));
    		else
    		  writeNdexAspectElementAsAspectFragment(cxwtr, new NodeCitationLinksElement(SID, cids));	
    	}
    	
    	//writeSupports
    	
    	Collection<Long> supports = doc.field(NdexClasses.Support);
    	
    	if ( supports !=null) {
    		List<String> supIds = new ArrayList<String> (supports.size());
    		
    		for ( Long supId : supports) {
    			String ssid = supportIdMap.get(supId);
    			if ( ssid == null) {
    				ssid = writeSupportInCX(getSupportDocById(supId), cxwtr);
    				supportIdMap.put(supId, ssid);
    			}
    			
    			supIds.add(ssid);
    		}
    		if ( isEdge)
    		  writeNdexAspectElementAsAspectFragment(cxwtr, new EdgeSupportLinksElement(SID, supIds));
    		else 
      		  writeNdexAspectElementAsAspectFragment(cxwtr, new NodeSupportLinksElement(SID, supIds));

    	}
	}
	
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
	private void writeNodeInCX(ODocument doc, CxWriter cxwtr, Set<Long> repIdSet,
			 Map<Long,String> citationIdMap,  Map<Long,String> supportIdMap) 
			throws IOException, NdexException {
		
		String SID = doc.field(NdexClasses.Element_SID);
		
		if ( SID ==null)  {
			Long id = doc.field(NdexClasses.Element_ID);
			SID = id.toString();
		}
		
		writeNdexAspectElementAsAspectFragment(cxwtr, new NodesElement(SID));
    	
    	//write rep 
    	
    	Long repId = doc.field(NdexClasses.Node_P_represents);
    	
    	if ( repId != null && repId.longValue() > 0) {
    		String termType = doc.field(NdexClasses.Node_P_representTermType);
    		
  			if ( termType.equals(NdexClasses.BaseTerm)) {
    			String repStr = this.getBaseTermStringById(repId);
    			writeNdexAspectElementAsAspectFragment(cxwtr, new NodeAttributesElement(null, SID, NdexClasses.Node_P_represents, repStr));

			} else if ( !repIdSet.contains(repId) ) {
				if (termType.equals(NdexClasses.ReifiedEdgeTerm)) {
					ODocument reifiedEdgeDoc = this.getReifiedEdgeDocById(repId);
    				writeReifiedEdgeTermInCX(SID, reifiedEdgeDoc, cxwtr);
				} else if (termType.equals(NdexClasses.FunctionTerm)) {
					ODocument funcDoc = this.getFunctionDocById(repId);
    				writeNdexAspectElementAsAspectFragment(cxwtr, getFunctionTermsElementFromDoc(SID, funcDoc));
				} else 
					throw new NdexException ("Unsupported term type '" + termType + 
							"' found for term Id:" + repId);
				repIdSet.add(repId);
			}
    	}
    	 

		String name = doc.field(NdexClasses.Node_P_name);
    	    	
    	if (name !=null) {
    		writeNdexAspectElementAsAspectFragment(cxwtr, new NodeAttributesElement (null, SID,NdexClasses.Node_P_name,name ));
    	}
    	
    	Set<Long> aliases = doc.field(NdexClasses.Node_P_alias);
    	
    	if ( aliases !=null) {
        	List<String> terms = new ArrayList<> (aliases.size());
        	for ( Long id : aliases) {
        		terms.add(getBaseTermStringById(id));
        	}
        	writeNdexAspectElementAsAspectFragment(cxwtr, new NodeAttributesElement(null,SID,NdexClasses.Node_P_alias,terms, ATTRIBUTE_TYPE.STRING));
    	}
    	    	
    	Set<Long> relatedTerms = doc.field(NdexClasses.Node_P_relatedTo);
      	if ( relatedTerms !=null) {
        	List<String> terms = new ArrayList<> (relatedTerms.size());
        	for ( Long id : relatedTerms) {
        		terms.add(getBaseTermStringById(id));
        	}
        	writeNdexAspectElementAsAspectFragment(cxwtr, new NodeAttributesElement(null,SID,NdexClasses.Node_P_relatedTo,terms,ATTRIBUTE_TYPE.STRING));
    	}
      	
    	// write properties
       	List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);
    	if ( props !=null) {
    		cxwtr.startAspectFragment(NodeAttributesElement.NAME);
    		for ( NdexPropertyValuePair p : props ) {
    			NodeAttributesElement ep = new NodeAttributesElement ( 
    					p.getSubNetworkId(),
    					SID, p.getPredicateString(), p.getValue(), 
    					NodeAttributesElement.toDataType(p.getDataType().toLowerCase()));
    			cxwtr.writeAspectElement(ep);
    		}
    		cxwtr.endAspectFragment();
    	}

    	//writeCitations and supports
    	writeCitationsAndSupports(SID,  doc, cxwtr, citationIdMap,
 			   supportIdMap , false );
 	}
	
	
	private FunctionTermsElement getFunctionTermsElementFromDoc(String nodeId, ODocument funcDoc) throws ObjectNotFoundException {
		Long btId = funcDoc.field(NdexClasses.BaseTerm);
		String bt = this.getBaseTermStringById(btId);
	
 	   	List<Object> args = new ArrayList<>();

 	    Object f = funcDoc.field("out_"+ NdexClasses.FunctionTerm_E_paramter);

 	    if ( f == null)   {   // function without parameters.
 	    	return new FunctionTermsElement(nodeId,bt, args);
 	    }

 	    Iterable<ODocument> iterable =  ( f instanceof ODocument) ?
    		 (new OrientDBIterableSingleLink((ODocument)f) ) :  (Iterable<ODocument>)f;
	    
    	for (ODocument para : iterable) {
	    	if (para.getClassName().equals(NdexClasses.BaseTerm)) {
	    		args.add(getBaseTermStringFromDoc(para));
	    	} else {  // add nested functionTerm
	    		FunctionTermsElement func = getFunctionTermsElementFromDoc ( null, para);
	    		args.add(func);
	    	}
	    }
	    return new FunctionTermsElement(nodeId, bt, args);
	}
	
	private void writeReifiedEdgeTermInCX(String nodeId, ODocument reifiedEdgeDoc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		ODocument e = reifiedEdgeDoc.field("out_" + NdexClasses.ReifiedEdge_E_edge);
		String eid = e.field(NdexClasses.Element_SID);
		if ( eid == null) {
			eid = ((Long)e.field(NdexClasses.Element_ID)).toString();
		}
			
		writeNdexAspectElementAsAspectFragment(cxwtr, new ReifiedEdgeElement(nodeId, eid));
			
	}
		
	private String writeCitationInCX(ODocument doc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
	
	    CitationElement result = new CitationElement();
		
		Long citationID = doc.field(NdexClasses.Element_ID);

  	    String SID = doc.field(NdexClasses.Element_SID); 
		
		if ( SID ==null) {
			 SID = citationID.toString();
		} 
		result.setId(SID);
		result.setTitle((String)doc.field(NdexClasses.Citation_P_title));
		result.setCitationType((String)doc.field(NdexClasses.Citation_p_idType));
		result.setIdentifier((String)doc.field(NdexClasses.Citation_P_identifier));
		
		List<String> o = doc.field(NdexClasses.Citation_P_contributors);
		
		if ( o!=null && !o.isEmpty())
			result.setContributor(o);
		
	   	List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);
	   	
	   	if (props !=null && props.size()> 0) {
	   	  List<CXSimpleAttribute> attrs = new ArrayList<>(props.size());
	   	  for ( NdexPropertyValuePair p : props ) {
	   		 attrs.add(new CXSimpleAttribute (p)) ;
	   	  }
	   	  result.setProps(attrs);
	   	}
		writeNdexAspectElementAsAspectFragment(cxwtr,result);
	  	
    	return SID;
	}

	
	private String writeSupportInCX(ODocument doc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		
		SupportElement result = new SupportElement();
		
		Long supportID = doc.field(NdexClasses.Element_ID);

 	    String SID = doc.field(NdexClasses.Element_SID); 
		
		if ( SID ==null) {
			 SID = supportID.toString();
		} 
		
		result.setId(SID);
		result.setText((String)doc.field(NdexClasses.Support_P_text));
		
		Long citationId = doc.field(NdexClasses.Citation);
		
		if ( citationId !=null) {
			ODocument cDoc = this.getCitationDocById(citationId);
			String cId = cDoc.field(NdexClasses.Element_SID);
			if ( cId == null)
				cId = ((Long)cDoc.field(NdexClasses.Element_ID)).toString();
			result.setCitationId(cId);
		}

	   	List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);

	   	if (props !=null && props.size()> 0) {
		   	  List<CXSimpleAttribute> attrs = new ArrayList<>(props.size());
		   	  for ( NdexPropertyValuePair p : props ) {
		   		 attrs.add(new CXSimpleAttribute (p)) ;
		   	  }
		   	  result.setProps(attrs);
		}
	   	
		writeNdexAspectElementAsAspectFragment(cxwtr,result);
		
       	return SID;
	}
     
    private String getBaseTermStringById(long id) throws ObjectNotFoundException {
    	ODocument doc = getBasetermDocById(id);
    	return  getBaseTermStringFromDoc(doc);
    	
    }
    
    private String getBaseTermStringFromDoc(ODocument doc) throws ObjectNotFoundException {
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
    

    private void writeNdexAspectElementAsAspectFragment (CxWriter cxwtr, AspectElement element ) throws IOException {
    	cxwtr.startAspectFragment(element.getAspectName());
		cxwtr.writeAspectElement(element);
		cxwtr.endAspectFragment();
    }
}
