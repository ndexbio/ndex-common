package org.ndexbio.common.models.dao.orientdb;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.cxio.aspects.datamodels.EdgeAttributesElement;
import org.cxio.aspects.datamodels.EdgesElement;
import org.cxio.aspects.datamodels.NetworkAttributesElement;
import org.cxio.aspects.datamodels.NodeAttributesElement;
import org.cxio.aspects.datamodels.NodesElement;
import org.cxio.aspects.datamodels.AbstractAttributesAspectElement.ATTRIBUTE_DATA_TYPE;
import org.cxio.core.CxWriter;
import org.cxio.core.interfaces.AspectElement;
import org.cxio.core.interfaces.AspectFragmentWriter;
import org.cxio.metadata.MetaDataCollection;
import org.cxio.metadata.MetaDataElement;
import org.cxio.util.Util;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.cx.aspect.GeneralAspectFragmentWriter;
import org.ndexbio.model.cx.CXSimpleAttribute;
import org.ndexbio.model.cx.CitationElement;
import org.ndexbio.model.cx.EdgeCitationLinksElement;
import org.ndexbio.model.cx.EdgeSupportLinksElement;
import org.ndexbio.model.cx.FunctionTermsElement;
import org.ndexbio.model.cx.NamespacesElement;
import org.ndexbio.model.cx.NdexNetworkStatus;
import org.ndexbio.model.cx.NodeCitationLinksElement;
import org.ndexbio.model.cx.NodeSupportLinksElement;
import org.ndexbio.model.cx.ReifiedEdgeElement;
import org.ndexbio.model.cx.SupportElement;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.task.Configuration;

import com.orientechnologies.orient.core.record.impl.ODocument;

public class CXNetworkExporter extends SingleNetworkDAO {
	
	//Metadata version of Node aspect
	static private final String nodeMDVersion = "1.0";  
	
	static private final String edgeMDVersion = "1.0";  

	static private final String citationMDVersion = "1.0";  
	
	static private final String supportMDVersion = "1.0";  

	static private final String functionMDVersion = "1.0";  

	static private final String reifiedEdgeMDVersion = "1.0";  

	static private final String ndexStatusMDVersion = "1.0";
	static private final String EdgeCitationLinksMDVersion = "1.0";
	static private final String EdgeSupportLinksMDVersion = "1.0";
	
	private long nodeIdCounter;
	private long edgeIdCounter;
	private long citationIdCounter;
	private long supportIdCounter;
	

	public CXNetworkExporter(String UUID) throws NdexException {
		super(UUID);
	}

	
	public void writeNetworkInCX(OutputStream out, final boolean use_default_pretty_printer) throws IOException, NdexException {
       
		nodeIdCounter = 0 ;
		edgeIdCounter = 0;
		citationIdCounter = 0;
		supportIdCounter = 0;
	
		CxWriter cxwtr = getNdexCXWriter(out, use_default_pretty_printer);     
        
		MetaDataCollection md = networkDoc.field(NdexClasses.Network_P_metadata);
		if ( md == null) {
			md = this.createCXMataData();
		}
		
        cxwtr.addPreMetaData(md);

        cxwtr.start();
        
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
        		commitId !=null && cacheId !=null && cacheId > 0 && commitId.equals(cacheId));
     
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
        		ATTRIBUTE_DATA_TYPE t = ATTRIBUTE_DATA_TYPE.STRING;
        		try {
        			t = NetworkAttributesElement.toDataType(p.getDataType().toLowerCase());
        		} catch (IllegalArgumentException e) {
        			System.out.println("Property type " + p.getDataType() + " unsupported. Converting it to String in CX output.");
        		}	
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
        
        //Add post metadata
        
        MetaDataCollection postmd = new MetaDataCollection ();
        
        if ( nodeIdCounter > 0 )
        	postmd.setIdCounter(NodesElement.NAME, nodeIdCounter);
        if ( edgeIdCounter > 0 )
        	postmd.setIdCounter(EdgesElement.NAME, edgeIdCounter);
        if ( citationIdCounter >0)
        	postmd.setIdCounter(CitationElement.NAME, citationIdCounter);
        if ( supportIdCounter > 0 )
        	postmd.setIdCounter(SupportElement.NAME, supportIdCounter);
        cxwtr.addPostMetaData(postmd);        
        cxwtr.end();

	}
	
	private MetaDataCollection createCXMataData() {
		MetaDataCollection md= new MetaDataCollection();

		MetaDataElement node_meta = new MetaDataElement();

        Timestamp lastUpdate = new Timestamp( ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime());
        int edgecount = networkDoc.field(NdexClasses.Network_P_edgeCount);
        int nodecount = networkDoc.field(NdexClasses.Network_P_nodeCount);
        
        node_meta.setName(NodesElement.NAME);
        node_meta.setVersion(nodeMDVersion);
      //  node_meta.setIdCounter();
        node_meta.setLastUpdate(lastUpdate.getTime());
        node_meta.setElementCount(new Long(nodecount));
        node_meta.setConsistencyGroup(1l);

        md.add(node_meta);

        
        MetaDataElement edge_meta = new MetaDataElement();
        edge_meta.setName(EdgesElement.NAME);
        edge_meta.setVersion(edgeMDVersion);
        edge_meta.setLastUpdate(lastUpdate.getTime());
        edge_meta.setConsistencyGroup(1l);
        edge_meta.setElementCount(new Long(edgecount));
        md.add(edge_meta);
        
        MetaDataElement ndexStatus = new MetaDataElement();
        edge_meta.setName(NdexNetworkStatus.NAME);
        edge_meta.setVersion(ndexStatusMDVersion);
        edge_meta.setLastUpdate(lastUpdate.getTime());
        edge_meta.setConsistencyGroup(0l);
        md.add(ndexStatus);
        
        MetaDataElement networkAttr = new MetaDataElement();
        edge_meta.setName(NetworkAttributesElement.NAME);
        edge_meta.setVersion("1.0");
        edge_meta.setLastUpdate(lastUpdate.getTime());
        edge_meta.setConsistencyGroup(2l);
        md.add(networkAttr);   
        
        
        //citations
        addMetadata(md,NdexClasses.Network_E_Citations, CitationElement.NAME,citationMDVersion,lastUpdate);
      
        //supports
        addMetadata(md,NdexClasses.Network_E_Supports,SupportElement.NAME,supportMDVersion,lastUpdate);

        //functionTerms
        addMetadata(md,NdexClasses.Network_E_FunctionTerms,FunctionTermsElement.NAME,functionMDVersion,lastUpdate);
        
        //reifiedEdgeTerms
        addMetadata(md,NdexClasses.Network_E_ReifiedEdgeTerms,ReifiedEdgeElement.NAME,reifiedEdgeMDVersion,lastUpdate);

        return md;
	}
	
	private void addMetadata(MetaDataCollection md, String networkEdgeName, String aspectName,String aspectVersion, Timestamp lastUpdate) {
	      long cnt = this.getVertexCount(networkEdgeName);
	        if (cnt > 0 ) {
	            
	        	MetaDataElement meta = new MetaDataElement();

	        	meta.setName(aspectName);
	        	meta.setVersion(aspectVersion);
	        	meta.setElementCount(cnt);
	        	meta.setLastUpdate(lastUpdate.getTime());
	        	meta.setConsistencyGroup(1l);

	        	md.add(meta); 
	        } 
	}
	
	private void writeEdgeInCX(ODocument doc, CxWriter cxwtr, Map<Long,String> citationIdMap,
		    Map<Long,String> supportIdMap ) throws ObjectNotFoundException, IOException {
	String SID = doc.field(NdexClasses.Element_SID);
	
	if ( SID ==null) {
		 SID = ((Long)doc.field(NdexClasses.Element_ID)).toString();
	}
	
	// track the counter
	if ( edgeIdCounter >=0 ) {
		try { 
	   
			long l = Long.parseLong(SID);
			if (l>edgeIdCounter)
				edgeIdCounter = l;
		} catch ( NumberFormatException e) {
			System.out.println("Non-numeric SID found in edge aspect. Will ignore tracking id counters for this aspect.");
			edgeIdCounter = -1;
		}
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
	writeCitationAndSupportLinks(SID,  doc, cxwtr, citationIdMap,
		   supportIdMap , true );
   
}

	
	

	
	private String writeCitationInCX(ODocument doc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		
	    CitationElement result = new CitationElement();
		
		Long citationID = doc.field(NdexClasses.Element_ID);

  	    String SID = doc.field(NdexClasses.Element_SID); 
		
		if ( SID ==null) {
			 SID = citationID.toString();
		} 
		
		// track the counter
		if ( citationIdCounter >=0 ) {
			try { 
		   
				long l = Long.parseLong(SID);
				if (l>citationIdCounter)
					citationIdCounter = l;
			} catch ( NumberFormatException e) {
				System.out.println("Non-numeric SID found in citation aspect. Will ignore tracking id counters for this aspect.");
				citationIdCounter = -1;
			}
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

	private void writeReifiedEdgeTermInCX(String nodeId, ODocument reifiedEdgeDoc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		ODocument e = reifiedEdgeDoc.field("out_" + NdexClasses.ReifiedEdge_E_edge);
		String eid = e.field(NdexClasses.Element_SID);
		if ( eid == null) {
			eid = ((Long)e.field(NdexClasses.Element_ID)).toString();
		}
			
		writeNdexAspectElementAsAspectFragment(cxwtr, new ReifiedEdgeElement(nodeId, eid));
			
	}
	
	
	private void writeNodeInCX(ODocument doc, CxWriter cxwtr, Set<Long> repIdSet,
			 Map<Long,String> citationIdMap,  Map<Long,String> supportIdMap) 
			throws IOException, NdexException {
		
		String SID = doc.field(NdexClasses.Element_SID);
		
		if ( SID ==null)  {
			Long id = doc.field(NdexClasses.Element_ID);
			SID = id.toString();
		}
		
		// track the counter
		if ( nodeIdCounter >=0 ) {
			try { 
		   
				long l = Long.parseLong(SID);
				if (l>nodeIdCounter)
					nodeIdCounter = l;
			} catch ( NumberFormatException e) {
				System.out.println("Non-numeric SID found in node aspect. Will ignore tracking id counters for this aspect.");
				nodeIdCounter = -1;
			}
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
       	writeNdexAspectElementAsAspectFragment(cxwtr, new NodeAttributesElement(null,SID,NdexClasses.Node_P_alias,terms, ATTRIBUTE_DATA_TYPE.STRING));
   	}
   	    	
   	Set<Long> relatedTerms = doc.field(NdexClasses.Node_P_relatedTo);
     	if ( relatedTerms !=null) {
       	List<String> terms = new ArrayList<> (relatedTerms.size());
       	for ( Long id : relatedTerms) {
       		terms.add(getBaseTermStringById(id));
       	}
       	writeNdexAspectElementAsAspectFragment(cxwtr, new NodeAttributesElement(null,SID,NdexClasses.Node_P_relatedTo,terms,ATTRIBUTE_DATA_TYPE.STRING));
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
   	writeCitationAndSupportLinks(SID,  doc, cxwtr, citationIdMap,
			   supportIdMap , false );
	}
	
	
	private void writeCitationAndSupportLinks(String SID, ODocument doc,  CxWriter cxwtr, Map<Long,String> citationIdMap,
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

	
	private String writeSupportInCX(ODocument doc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		
		SupportElement result = new SupportElement();
		
		Long supportID = doc.field(NdexClasses.Element_ID);

 	    String SID = doc.field(NdexClasses.Element_SID); 
		
		if ( SID ==null) {
			 SID = supportID.toString();
		} 
		
		// track the counter
		if ( supportIdCounter >=0 ) {
			try { 
		   
				long l = Long.parseLong(SID);
				if (l>supportIdCounter)
					supportIdCounter = l;
			} catch ( NumberFormatException e) {
				System.out.println("Non-numeric SID found in support aspect. Will ignore tracking id counters for this aspect.");
				supportIdCounter = -1;
			}
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
     
		
    private void writeNdexAspectElementAsAspectFragment (CxWriter cxwtr, AspectElement element ) throws IOException {
    	cxwtr.startAspectFragment(element.getAspectName());
		cxwtr.writeAspectElement(element);
		cxwtr.endAspectFragment();
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


}
