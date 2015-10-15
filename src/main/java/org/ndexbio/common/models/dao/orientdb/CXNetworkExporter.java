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
import org.cxio.aspects.datamodels.AbstractAttributesAspectElement;
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
import org.ndexbio.model.cx.FunctionTermElement;
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


	private void init() {
		nodeIdCounter = 0 ;
		edgeIdCounter = 0;
		citationIdCounter = 0;
		supportIdCounter = 0;
	}
	
	public void writeNetworkInCX(OutputStream out, final boolean use_default_pretty_printer) throws IOException, NdexException {
       
		init();
	
		CxWriter cxwtr = getNdexCXWriter(out, use_default_pretty_printer);     
        
		MetaDataCollection md = networkDoc.field(NdexClasses.Network_P_metadata);
		if ( md == null) {
			md = this.createCXMataData();
		}
		
        cxwtr.addPreMetaData(md);

        cxwtr.start();
        
        //write NdexStatus
        writeNdexStatus(cxwtr);
        
        // write name, desc and other properties;
        writeNetworkAttributes(cxwtr,-1);
        
        writeNamespacesInCX(cxwtr, -1);
        
        Map<Long,String> citationIdMap = new TreeMap<> ();
        Map<Long,String> supportIdMap = new TreeMap<> ();
 //       Set<Long> repIdSet = new TreeSet<> ();

        
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
        	writeNodeInCX(doc, cxwtr,/* repIdSet,*/ citationIdMap, supportIdMap); 
        }        
        
        for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Edges)) {
        	writeEdgeInCX(doc,cxwtr, citationIdMap, supportIdMap);
        }
        
        
        writeOpaqueAspects(cxwtr);
        
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
        if ( postmd.size() > 0 ) 
        	cxwtr.addPostMetaData(postmd);        
        cxwtr.end();

	}


	private int writeNetworkAttributes(CxWriter cxwtr, int limit) throws IOException {
		
		int counter = 0;
		
		if ( limit <=0 || counter < limit) {
			String title = networkDoc.field(NdexClasses.Network_P_name);
			if ( title != null ) {
				writeNdexAspectElementAsAspectFragment(cxwtr,
            		new NetworkAttributesElement(null,NdexClasses.Network_P_name, title));
			}
			counter ++;
		}
        
		if (limit <=0 || counter < limit) {
			String desc = networkDoc.field(NdexClasses.Network_P_desc);
			if ( desc != null) {
				writeNdexAspectElementAsAspectFragment(cxwtr,
            		new NetworkAttributesElement(null,NdexClasses.Network_P_desc, desc));
			}
			counter ++;
		}
		
		if ( limit <=0 || counter < limit) {
			String version = networkDoc.field(NdexClasses.Network_P_version);
			if ( version !=null) {
				writeNdexAspectElementAsAspectFragment(cxwtr,
            		new NetworkAttributesElement(null,NdexClasses.Network_P_version, version));
			}
			counter ++;
		}
        
		if ( limit <=0 || counter < limit) {
			String srcFmtStr = networkDoc.field(NdexClasses.Network_P_source_format);
			if ( srcFmtStr !=null) {
				writeNdexAspectElementAsAspectFragment(cxwtr,
            		new NetworkAttributesElement(null,CXsrcFormatAttrName, srcFmtStr));
			}
			counter ++;
		}	
		
        List<NdexPropertyValuePair> props = networkDoc.field(NdexClasses.ndexProperties);
        if ( props !=null) {
        	for ( NdexPropertyValuePair p : props) {
        		if ( limit <=0 || counter < limit ) 
        		   counter ++;
        		else 
        			break;
        		ATTRIBUTE_DATA_TYPE t = ATTRIBUTE_DATA_TYPE.STRING;
        		try {
        			t = NetworkAttributesElement.toDataType(p.getDataType().toLowerCase());
        		} catch (IllegalArgumentException e) {
        			System.out.println("Property type " + p.getDataType() + " unsupported. Converting it to String in CX output.");
        		}	
        		if ( !AbstractAttributesAspectElement.isListType(t)) {
        			writeNdexAspectElementAsAspectFragment(cxwtr,
        					NetworkAttributesElement.createInstanceWithSingleValue(p.getSubNetworkId(),p.getPredicateString(), p.getValue(),
        							t ));
        		} else 
        			writeNdexAspectElementAsAspectFragment(cxwtr,
        					new NetworkAttributesElement(p.getSubNetworkId(),p.getPredicateString(), p.getValue(),
        							t ));
        	}	
        }
        
        return counter;
	}

	private void writeOpaqueAspects(CxWriter cxwtr) throws IOException {
		Map<String,String> tab = networkDoc.field(NdexClasses.Network_P_opaquEdgeTable);
		if (tab != null) {
			for ( Map.Entry<String, String> e: tab.entrySet()) {
				String edgeName = e.getValue();
				cxwtr.startAspectFragment(e.getKey());
				for( ODocument doc : getNetworkElements(edgeName)) {
					String value = doc.field(edgeName);
					cxwtr.writeOpaqueAspectElement(value);
				}
				cxwtr.endAspectFragment();
			}
		}
		
	}

	private void writeOpaqueAspect(CxWriter cxwtr, String opaqueAspectName, int limit) throws IOException {
		Map<String,String> tab = networkDoc.field(NdexClasses.Network_P_opaquEdgeTable);
		int counter = 0 ; 
		if (tab != null) {
			String edgeName = tab.get(opaqueAspectName);
			cxwtr.startAspectFragment(opaqueAspectName);
			for( ODocument doc : getNetworkElements(edgeName)) {
					if ( limit <=0 || counter < limit)
						counter ++;
					else 
						break;
					String value = doc.field(edgeName);
					cxwtr.writeOpaqueAspectElement(value);
			}
			cxwtr.endAspectFragment();	
		}
	}

	private int writeNdexStatus(CxWriter cxwtr) throws NdexException, IOException {
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
        return 1;
	}
	
	private MetaDataCollection createCXMataData() {
		MetaDataCollection md= new MetaDataCollection();

		MetaDataElement node_meta = new MetaDataElement();

        Timestamp lastUpdate = new Timestamp( ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime());
        int edgecount = networkDoc.field(NdexClasses.Network_P_edgeCount);
        int nodecount = networkDoc.field(NdexClasses.Network_P_nodeCount);
        
        node_meta.setName(NodesElement.NAME);
        node_meta.setVersion(nodeMDVersion);
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
        
        addMetadata(md, NdexNetworkStatus.NAME, ndexStatusMDVersion,lastUpdate, 0l);
        addMetadata(md, NetworkAttributesElement.NAME, "1.0",lastUpdate, 2l);
        addMetadata(md, NodeAttributesElement.NAME, "1.0",lastUpdate, 1l);
        addMetadata(md, EdgeAttributesElement.NAME, "1.0",lastUpdate, 1l);
        addMetadata(md, EdgeCitationLinksElement.NAME, EdgeCitationLinksMDVersion,lastUpdate, 1l);
        addMetadata(md, EdgeSupportLinksElement.NAME, EdgeSupportLinksMDVersion,lastUpdate, 1l);
        addMetadata(md, NodeCitationLinksElement.NAME, "1.0",lastUpdate, 1l);
        addMetadata(md, NodeSupportLinksElement.NAME, "1.0",lastUpdate, 1l);
        addMetadata(md, NamespacesElement.NAME, "1.0",lastUpdate, 3l);

        
        
        //citations
        addMetadata(md,NdexClasses.Network_E_Citations, CitationElement.NAME,citationMDVersion,lastUpdate);
      
        //supports
        addMetadata(md,NdexClasses.Network_E_Supports,SupportElement.NAME,supportMDVersion,lastUpdate);

        //functionTerms
        addMetadata(md,NdexClasses.Network_E_FunctionTerms,FunctionTermElement.NAME,functionMDVersion,lastUpdate);
        
        //reifiedEdgeTerms
        addMetadata(md,NdexClasses.Network_E_ReifiedEdgeTerms,ReifiedEdgeElement.NAME,reifiedEdgeMDVersion,lastUpdate);

        
        return md;
	}
	
	private void addMetadata(MetaDataCollection md, String aspectName, String version,Timestamp lastUpdate, long groupId) {
		 MetaDataElement networkAttr = new MetaDataElement();
		 networkAttr.setName(aspectName);
		 networkAttr.setVersion(version);
		 networkAttr.setLastUpdate(lastUpdate.getTime());
		 networkAttr.setConsistencyGroup(groupId);
	        md.add(networkAttr);   
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
	
		String SID = getSIDFromDoc ( doc);
		if ( SID.equals("1110"))
			System.out.println("err");
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
			ATTRIBUTE_DATA_TYPE t = AbstractAttributesAspectElement.toDataType(p.getDataType().toLowerCase());
			EdgeAttributesElement ep = AbstractAttributesAspectElement.isListType(t) ? 
					EdgeAttributesElement.createInstanceWithMultipleValues ( p.getSubNetworkId(), 
							SID, p.getPredicateString(), p.getValue(), t) : 
					new EdgeAttributesElement ( p.getSubNetworkId(), SID, p.getPredicateString(), p.getValue(),t);
			cxwtr.writeAspectElement(ep);
		}
		cxwtr.endAspectFragment();
	}
	
	//write citations
	writeCitationAndSupportLinks(SID,  doc, cxwtr, citationIdMap,
		   supportIdMap , true, true,true );
   
}

	
	private void writeEdgeAspectsInCX(ODocument doc, CxWriter cxwtr, Map<Long,String> citationIdMap,
		    Map<Long,String> supportIdMap , 
		    boolean writeEdges, boolean writeEdgeAttr,
		    boolean writeEdgeCitationLinks, boolean writeEdgeSupportLinks) throws ObjectNotFoundException, IOException {
	
		String SID = getSIDFromDoc ( doc);
	
		if ( writeEdges) {
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
		}
  
		if ( writeEdgeAttr) {
			// write other properties
			List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);
			if ( props !=null) {
					cxwtr.startAspectFragment(EdgeAttributesElement.NAME);
					for ( NdexPropertyValuePair p : props ) {
						ATTRIBUTE_DATA_TYPE t = AbstractAttributesAspectElement.toDataType(p.getDataType().toLowerCase());
						EdgeAttributesElement ep = AbstractAttributesAspectElement.isListType(t) ? 
								EdgeAttributesElement.createInstanceWithMultipleValues ( p.getSubNetworkId(), 
										SID, p.getPredicateString(), p.getValue(), t) : 
								new EdgeAttributesElement ( p.getSubNetworkId(), SID, p.getPredicateString(), p.getValue(),t);
								cxwtr.writeAspectElement(ep);
					}
					cxwtr.endAspectFragment();
			}
		}
		
		//write citations
		writeCitationAndSupportLinks(SID,  doc, cxwtr, citationIdMap,
		   supportIdMap , true, writeEdgeCitationLinks,writeEdgeSupportLinks );
   
}
	

	
	private String writeCitationInCX(ODocument doc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		
	    CitationElement result = new CitationElement();
		
  	    String SID = getSIDFromDoc(doc);
		
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
		String eid = getSIDFromDoc(e);
			
		writeNdexAspectElementAsAspectFragment(cxwtr, new ReifiedEdgeElement(nodeId, eid));
			
	}
	
	
	private void writeNodeInCX(ODocument doc, CxWriter cxwtr, /*Set<Long> repIdSet,*/
			 Map<Long,String> citationIdMap,  Map<Long,String> supportIdMap) 
			throws IOException, NdexException {
		
		String SID = getSIDFromDoc(doc);
		
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
   	   	
   	   writeNodeAttributesInCX(doc, cxwtr, /*repIdSet,*/ SID, true, true, true);

   	   //writeCitations and supports
   	   writeCitationAndSupportLinks(SID,  doc, cxwtr, citationIdMap,
			   supportIdMap , false, true,true );
	}
	
	private void writeNodeAspectsInCX(ODocument doc, CxWriter cxwtr, /*Set<Long> repIdSet, */
			 Map<Long,String> citationIdMap,  Map<Long,String> supportIdMap,
			boolean writeNodes,boolean	writeNodeAttr, boolean writeFunctionTerm, boolean writeReifiedEdgeTerm,
			boolean writeNodeCitationLinks, boolean writeNodeSupportLinks) 
			throws IOException, NdexException {
		
		String SID = getSIDFromDoc(doc);
		
		if (writeNodes) {	
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
		}
  	
		//write rep 
		writeNodeAttributesInCX(doc, cxwtr, /*repIdSet, */SID, writeNodeAttr,
				writeReifiedEdgeTerm,
  			   	writeFunctionTerm);
  	
		//writeCitations and supports
		writeCitationAndSupportLinks(SID,  doc, cxwtr, citationIdMap,
			   supportIdMap , false, writeNodeCitationLinks, writeNodeSupportLinks );
	}


	private void writeNodeAttributesInCX(ODocument doc, CxWriter cxwtr, /*Set<Long> repIdSet,*/ String SID, boolean writeAttribute, boolean writeReifiedTerms, boolean writeFunctionTerms)
			throws ObjectNotFoundException, IOException, NdexException {
		Long repId = doc.field(NdexClasses.Node_P_represents);
		
		if ( repId != null && repId.longValue() > 0) {
			String termType = doc.field(NdexClasses.Node_P_representTermType);
			
			if ( termType.equals(NdexClasses.BaseTerm) ) {
				if (writeAttribute) {
				   String repStr = this.getBaseTermStringById(repId);
				   writeNdexAspectElementAsAspectFragment(cxwtr, new NodeAttributesElement(null, SID, NdexClasses.Node_P_represents, repStr));
				}   
			} else if (termType.equals(NdexClasses.ReifiedEdgeTerm) ) {
					if ( writeReifiedTerms) {
							ODocument reifiedEdgeDoc = this.getReifiedEdgeDocById(repId);
							writeReifiedEdgeTermInCX(SID, reifiedEdgeDoc, cxwtr);
					}
			} else if (termType.equals(NdexClasses.FunctionTerm) ) {
					if ( writeFunctionTerms) {
						ODocument funcDoc = this.getFunctionDocById(repId);
					    writeNdexAspectElementAsAspectFragment(cxwtr, getFunctionTermsElementFromDoc(SID, funcDoc));
					}
			} else 
				throw new NdexException ("Unsupported term type '" + termType + 
								"' found for term Id:" + repId);
		}

		if ( writeAttribute) {
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
				writeNdexAspectElementAsAspectFragment(cxwtr, new NodeAttributesElement(null,SID,NdexClasses.Node_P_alias,terms, ATTRIBUTE_DATA_TYPE.LIST_OF_STRING));
			}
		    	
			Set<Long> relatedTerms = doc.field(NdexClasses.Node_P_relatedTo);
			if ( relatedTerms !=null) {
					List<String> terms = new ArrayList<> (relatedTerms.size());
					for ( Long id : relatedTerms) {
						terms.add(getBaseTermStringById(id));
					}
					writeNdexAspectElementAsAspectFragment(cxwtr, new NodeAttributesElement(null,SID,NdexClasses.Node_P_relatedTo,terms,ATTRIBUTE_DATA_TYPE.LIST_OF_STRING));
			}
			
			// write properties
		 	List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);
		 	if ( props !=null) {
		 		cxwtr.startAspectFragment(NodeAttributesElement.NAME);
		 		for ( NdexPropertyValuePair p : props ) {
		 			ATTRIBUTE_DATA_TYPE t = AbstractAttributesAspectElement.toDataType(p.getDataType().toLowerCase());
		 			NodeAttributesElement ep = AbstractAttributesAspectElement.isListType(t) ?
						NodeAttributesElement.createInstanceWithMultipleValues(p.getSubNetworkId(),
			   					SID, p.getPredicateString(), p.getValue(), t) : 
						new NodeAttributesElement (p.getSubNetworkId(),SID, p.getPredicateString(), p.getValue(), t);
					cxwtr.writeAspectElement(ep);
		 		}
		 		cxwtr.endAspectFragment();
		 	}
		}
	}


	
	
	private void writeCitationAndSupportLinks(String SID, ODocument doc,  CxWriter cxwtr, Map<Long,String> citationIdMap,
		    Map<Long,String> supportIdMap ,boolean isEdge, boolean writeCitationLinks, boolean writeSupportLinks ) throws ObjectNotFoundException, IOException {
   	
	//write citations
	if ( writeCitationLinks ) {
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
	}
	
	//writeSupports
	if (writeSupportLinks) {
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
}

	
	private String writeSupportInCX(ODocument doc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		
		SupportElement result = new SupportElement();
		
 	    String SID = getSIDFromDoc(doc);
 	    
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
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(FunctionTermElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(NamespacesElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(ReifiedEdgeElement.NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(NdexNetworkStatus.NAME));
        
        return cxwtr;
	}
	

	private FunctionTermElement getFunctionTermsElementFromDoc(String nodeId, ODocument funcDoc) throws ObjectNotFoundException {
		Long btId = funcDoc.field(NdexClasses.BaseTerm);
		String bt = this.getBaseTermStringById(btId);
	
 	   	List<Object> args = new ArrayList<>();

 	    Object f = funcDoc.field("out_"+ NdexClasses.FunctionTerm_E_paramter);

 	    if ( f == null)   {   // function without parameters.
 	    	return new FunctionTermElement(nodeId,bt, args);
 	    }

 	    Iterable<ODocument> iterable =  ( f instanceof ODocument) ?
    		 (new OrientDBIterableSingleLink((ODocument)f) ) :  (Iterable<ODocument>)f;
	    
    	for (ODocument para : iterable) {
	    	if (para.getClassName().equals(NdexClasses.BaseTerm)) {
	    		args.add(getBaseTermStringFromDoc(para));
	    	} else {  // add nested functionTerm
	    		FunctionTermElement func = getFunctionTermsElementFromDoc ( null, para);
	    		args.add(func);
	    	}
	    }
	    return new FunctionTermElement(nodeId, bt, args);
	}

	
	public void writeOneAspectInCX(OutputStream out, String aspectName, int elementLimit, boolean use_default_pretty_printer) throws IOException, NdexException {
		init();
		//prepare metadata
		MetaDataCollection preMetaData = new MetaDataCollection();
		
		MetaDataCollection metadata = networkDoc.field(NdexClasses.Network_P_metadata);
		if ( metadata != null) {
				MetaDataElement e = metadata.getMetaDataElement(aspectName);
				if ( e == null)
					throw new NdexException ("Aspect " + aspectName + " not found in network " + uuid);
					e.getData().remove(MetaDataElement.ELEMENT_COUNT);
				preMetaData.add(e);
		} else {
			// construct metadata
		    Timestamp lastUpdate = new Timestamp( ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime());
	//	    int edgecount = networkDoc.field(NdexClasses.Network_P_edgeCount);
	//	    int nodecount = networkDoc.field(NdexClasses.Network_P_nodeCount);
				if ( aspectName.equals(NodesElement.NAME)) {
			        addMetadata(preMetaData, NodesElement.NAME, nodeMDVersion,lastUpdate, 1l);
		//	        if ( elementLimit <=0 )
		//	            preMetaData.setElementCount(NodesElement.NAME, Long.valueOf(nodecount));
				} else if ( aspectName.equals(EdgesElement.NAME) ) {
			        addMetadata(preMetaData, EdgesElement.NAME, edgeMDVersion,lastUpdate, 1l);
		//	        if ( elementLimit <=0 )
		//	        	preMetaData.setElementCount(EdgesElement.NAME, Long.valueOf(edgecount));
				} else if ( aspectName.equals(NdexNetworkStatus.NAME) ) {
				  addMetadata(preMetaData, NdexNetworkStatus.NAME, ndexStatusMDVersion,lastUpdate, 0l);
				} else if ( aspectName.equals(NetworkAttributesElement.NAME) ) {
				  addMetadata(preMetaData, NetworkAttributesElement.NAME, "1.0",lastUpdate, 2l);
				}  else if ( aspectName.equals(NodeAttributesElement.NAME) ) {
			       addMetadata(preMetaData, NodeAttributesElement.NAME, "1.0",lastUpdate, 1l);
				} else if ( aspectName.equals(EdgeAttributesElement.NAME) ) {
				  addMetadata(preMetaData, EdgeAttributesElement.NAME, "1.0",lastUpdate, 1l);
				} else if ( aspectName.equals(EdgeCitationLinksElement.NAME) ) {
			      addMetadata(preMetaData, EdgeCitationLinksElement.NAME, EdgeCitationLinksMDVersion,lastUpdate, 1l);
				} else if ( aspectName.equals(EdgeSupportLinksElement.NAME) ) {
			      addMetadata(preMetaData, EdgeSupportLinksElement.NAME, EdgeSupportLinksMDVersion,lastUpdate, 1l);
				} else if ( aspectName.equals(NodeCitationLinksElement.NAME) ) {
			      addMetadata(preMetaData, NodeCitationLinksElement.NAME, "1.0",lastUpdate, 1l);
				} else if ( aspectName.equals(NodeSupportLinksElement.NAME) ) {
			      addMetadata(preMetaData, NodeSupportLinksElement.NAME, "1.0",lastUpdate, 1l);
				} else if ( aspectName.equals(CitationElement.NAME) ) {
  			       addMetadata(preMetaData,/*NdexClasses.Network_E_Citations, */ CitationElement.NAME,citationMDVersion,lastUpdate, 1L);
				} else if ( aspectName.equals(SupportElement.NAME) ) {
			      //supports
			      addMetadata(preMetaData, /*NdexClasses.Network_E_Supports, */ SupportElement.NAME,supportMDVersion,lastUpdate, 1L);
				} else if ( aspectName.equals(FunctionTermElement.NAME) ) {
			      //functionTerms
			      addMetadata(preMetaData,/* NdexClasses.Network_E_FunctionTerms,*/ FunctionTermElement.NAME,functionMDVersion,lastUpdate,1L);
				} else if ( aspectName.equals(ReifiedEdgeElement.NAME) ) {
			      //reifiedEdgeTerms
			      addMetadata(preMetaData,/*NdexClasses.Network_E_ReifiedEdgeTerms,*/ ReifiedEdgeElement.NAME,reifiedEdgeMDVersion,lastUpdate,1L);
				} else if ( aspectName.equals(NamespacesElement.NAME)) {
			        addMetadata(preMetaData, NamespacesElement.NAME, "1.0",lastUpdate, 3l);
 
				} else 
					throw new NdexException ("Aspect " + aspectName + " not found in network " + uuid);			
		}
		
		CxWriter cxwtr = getNdexCXWriter(out, use_default_pretty_printer);     
        cxwtr.addPreMetaData(preMetaData);
        cxwtr.start();

		// start writing aspects out.
        //write NdexStatus
        long counter =0 ;
        if (aspectName.equals(NdexNetworkStatus.NAME)) {
           counter = writeNdexStatus(cxwtr);
        } else if ( aspectName.equals(NetworkAttributesElement.NAME)) {
        // write name, desc and other properties;
           counter = writeNetworkAttributes(cxwtr, elementLimit);
        } else if (   aspectName.equals(NamespacesElement.NAME)) {
        	//write namespaces 
        	counter = writeNamespacesInCX(cxwtr, elementLimit);
        } else {
        	
        	// tracking ids to SID mapping and baseterms that has been outputed.
        	Map<Long,String> citationIdMap = new TreeMap<> ();
        	Map<Long,String> supportIdMap = new TreeMap<> ();
        
        	if ( aspectName.equals(CitationElement.NAME)) {
        		for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Citations)) {
        			if ( elementLimit <=0 || counter < elementLimit)
        				counter ++;
        			else 
        				break;
        			Long citationId = doc.field(NdexClasses.Element_ID);
        			String SID = writeCitationInCX(doc, cxwtr);
        			citationIdMap.put(citationId, SID);
        		}
        	} else if (aspectName.equals(SupportElement.NAME) ) {
        		for ( ODocument doc: getNetworkElements (NdexClasses.Network_E_Supports)) {
        			if ( elementLimit <=0 || counter < elementLimit)
        				counter ++;
        			else 
        				break;
        			Long supportId = doc.field(NdexClasses.Element_ID);
        			String SID = writeSupportInCX(doc, cxwtr);
        			supportIdMap.put(supportId, SID);
        		}
        	} else {
                boolean writeNodes             = aspectName.equals(NodesElement.NAME);
                boolean writeNodeAttr          = aspectName.equals(NodeAttributesElement.NAME);
                boolean writeFunctionTerm      = aspectName.equals(FunctionTermElement.NAME);
                boolean writeReifiedEdgeTerm   = aspectName.equals(ReifiedEdgeElement.NAME);
                boolean writeNodeCitationLinks = aspectName.equals(NodeCitationLinksElement.NAME);
                boolean writeNodeSupportLinks  = aspectName.equals(NodeSupportLinksElement.NAME);
                if ( writeNodes|| writeNodeAttr ||writeFunctionTerm || writeReifiedEdgeTerm || 
                		writeNodeCitationLinks || writeNodeSupportLinks) {
                	for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Nodes)) {
            			if ( elementLimit <=0 || counter < elementLimit)
            				counter ++;
            			else 
            				break;
                		writeNodeAspectsInCX(doc, cxwtr,/* repIdSet, */citationIdMap, supportIdMap,
                				writeNodes,	writeNodeAttr, writeFunctionTerm, writeReifiedEdgeTerm,
                				writeNodeCitationLinks,writeNodeSupportLinks);
                	}  
                } else {
                    boolean writeEdges = aspectName.equals(EdgesElement.NAME);
                    boolean writeEdgeAttr = aspectName.equals(EdgeAttributesElement.NAME);
                    boolean writeEdgeCitationLinks = aspectName.equals(EdgeCitationLinksElement.NAME);
                    boolean writeEdgeSupportLinks = aspectName.equals(EdgeSupportLinksElement.NAME);
                    if ( writeEdges || writeEdgeAttr || writeEdgeCitationLinks || writeEdgeSupportLinks ) {
                    	for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Edges)) {
                    		if ( elementLimit <=0 || counter < elementLimit)
                				counter ++;
                			else 
                				break;
                    		writeEdgeAspectsInCX(doc,cxwtr, citationIdMap, supportIdMap,
                    			writeEdges, writeEdgeAttr, writeEdgeCitationLinks, writeEdgeSupportLinks);
                    	}
                    } else {
                       	writeOpaqueAspect(cxwtr, aspectName, elementLimit);    
                    }	
                }
        		
        	}

        }
        
        //Add post metadata
        MetaDataCollection postmd = new MetaDataCollection ();
        
        MetaDataElement e = new MetaDataElement();
        e.setName(aspectName);
        e.setElementCount(counter);
        postmd.add(e);
        	
        cxwtr.addPostMetaData(postmd);        

        cxwtr.end();

	}
	
	/**
	 * 
	 * @param out
	 * @param aspects should be not null and not empty.
	 * @throws NdexException 
	 * @throws IOException 
	 */
	public void writeAspectsInCX(OutputStream out, Set<String> aspects, boolean use_default_pretty_printer) throws NdexException, IOException {
		
		init();
		
		//prepare metadata
		MetaDataCollection preMetaData = new MetaDataCollection();
		
		boolean dbHasMetadata = false;
		MetaDataCollection metadata = networkDoc.field(NdexClasses.Network_P_metadata);
		if ( metadata != null) {
			dbHasMetadata = true;
			for (String aspectName : aspects) {
				MetaDataElement e = metadata.getMetaDataElement(aspectName);
				if ( e == null)
					throw new NdexException ("Aspect " + aspectName + " not found in network " + uuid);
				preMetaData.add(e);
			}
		} else {
			// construct metadata
		    Timestamp lastUpdate = new Timestamp( ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime());
		    int edgecount = networkDoc.field(NdexClasses.Network_P_edgeCount);
		    int nodecount = networkDoc.field(NdexClasses.Network_P_nodeCount);
			for (String aspectName : aspects) {
				if ( aspectName.equals(NodesElement.NAME)) {
			        addMetadata(preMetaData, NodesElement.NAME, nodeMDVersion,lastUpdate, 1l);
			        preMetaData.setElementCount(NodesElement.NAME, Long.valueOf(nodecount));
				} else if ( aspectName.equals(EdgesElement.NAME) ) {
			        addMetadata(preMetaData, EdgesElement.NAME, edgeMDVersion,lastUpdate, 1l);
			        preMetaData.setElementCount(EdgesElement.NAME, Long.valueOf(edgecount));
				} else if ( aspectName.equals(NdexNetworkStatus.NAME) ) {
				  addMetadata(preMetaData, NdexNetworkStatus.NAME, ndexStatusMDVersion,lastUpdate, 0l);
				} else if ( aspectName.equals(NetworkAttributesElement.NAME) ) {
				  addMetadata(preMetaData, NetworkAttributesElement.NAME, "1.0",lastUpdate, 2l);
				}  else if ( aspectName.equals(NodeAttributesElement.NAME) ) {
			       addMetadata(preMetaData, NodeAttributesElement.NAME, "1.0",lastUpdate, 1l);
				} else if ( aspectName.equals(EdgeAttributesElement.NAME) ) {
				  addMetadata(preMetaData, EdgeAttributesElement.NAME, "1.0",lastUpdate, 1l);
				} else if ( aspectName.equals(EdgeCitationLinksElement.NAME) ) {
			      addMetadata(preMetaData, EdgeCitationLinksElement.NAME, EdgeCitationLinksMDVersion,lastUpdate, 1l);
				} else if ( aspectName.equals(EdgeSupportLinksElement.NAME) ) {
			      addMetadata(preMetaData, EdgeSupportLinksElement.NAME, EdgeSupportLinksMDVersion,lastUpdate, 1l);
				} else if ( aspectName.equals(NodeCitationLinksElement.NAME) ) {
			      addMetadata(preMetaData, NodeCitationLinksElement.NAME, "1.0",lastUpdate, 1l);
				} else if ( aspectName.equals(NodeSupportLinksElement.NAME) ) {
			      addMetadata(preMetaData, NodeSupportLinksElement.NAME, "1.0",lastUpdate, 1l);
				} else if ( aspectName.equals(CitationElement.NAME) ) {
  			       addMetadata(preMetaData,NdexClasses.Network_E_Citations, CitationElement.NAME,citationMDVersion,lastUpdate);
				} else if ( aspectName.equals(SupportElement.NAME) ) {
			      //supports
			      addMetadata(preMetaData,NdexClasses.Network_E_Supports,SupportElement.NAME,supportMDVersion,lastUpdate);
				} else if ( aspectName.equals(FunctionTermElement.NAME) ) {
			      //functionTerms
			      addMetadata(preMetaData,NdexClasses.Network_E_FunctionTerms,FunctionTermElement.NAME,functionMDVersion,lastUpdate);
				} else if ( aspectName.equals(ReifiedEdgeElement.NAME) ) {
			      //reifiedEdgeTerms
			      addMetadata(preMetaData,NdexClasses.Network_E_ReifiedEdgeTerms,ReifiedEdgeElement.NAME,reifiedEdgeMDVersion,lastUpdate);
				} else if ( aspectName.equals(NamespacesElement.NAME)) {
			        addMetadata(preMetaData, NamespacesElement.NAME, "1.0",lastUpdate, 3l);
 
				} else 
					throw new NdexException ("Aspect " + aspectName + " not found in network " + uuid);
				
			}
			
		}
		
		CxWriter cxwtr = getNdexCXWriter(out, use_default_pretty_printer);     
        cxwtr.addPreMetaData(preMetaData);
        cxwtr.start();

		// start writing aspects out.
        
        //write NdexStatus
        if (aspects.contains(NdexNetworkStatus.NAME)) {
           writeNdexStatus(cxwtr);
           aspects.remove(NdexNetworkStatus.NAME);
        }
        
        // write name, desc and other properties;
        if ( aspects.contains(NetworkAttributesElement.NAME)) {
           writeNetworkAttributes(cxwtr, -1);
           aspects.remove(NetworkAttributesElement.NAME);
        }
        
        //write namespaces 
        if (  aspects.contains(NamespacesElement.NAME)) {
        	writeNamespacesInCX(cxwtr, -1);
        	aspects.remove(NetworkAttributesElement.NAME);
        }
        
        // tracking ids to SID mapping and baseterms that has been outputed.
        Map<Long,String> citationIdMap = new TreeMap<> ();
        Map<Long,String> supportIdMap = new TreeMap<> ();
     //   Set<Long> repIdSet = new TreeSet<> ();

        
        if ( aspects.contains(CitationElement.NAME)) {
        	for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Citations)) {
        		Long citationId = doc.field(NdexClasses.Element_ID);
        		String SID = writeCitationInCX(doc, cxwtr);
        		citationIdMap.put(citationId, SID);
        	}
        	aspects.remove(CitationElement.NAME);
        }
        
        if (aspects.contains(SupportElement.NAME) ) {
        	for ( ODocument doc: getNetworkElements (NdexClasses.Network_E_Supports)) {
        		Long supportId = doc.field(NdexClasses.Element_ID);
        		String SID = writeSupportInCX(doc, cxwtr);
        		supportIdMap.put(supportId, SID);
        	}
        	aspects.remove(SupportElement.NAME);
        }   
        
        boolean writeNodes             = aspects.contains(NodesElement.NAME);
        boolean writeNodeAttr          = aspects.contains(NodeAttributesElement.NAME);
        boolean writeFunctionTerm      = aspects.contains(FunctionTermElement.NAME);
        boolean writeReifiedEdgeTerm   = aspects.contains(ReifiedEdgeElement.NAME);
        boolean writeNodeCitationLinks = aspects.contains(NodeCitationLinksElement.NAME);
        boolean writeNodeSupportLinks  = aspects.contains(NodeSupportLinksElement.NAME);
        if ( writeNodes|| writeNodeAttr ||writeFunctionTerm || writeReifiedEdgeTerm || 
        		writeNodeCitationLinks || writeNodeSupportLinks) {
        	for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Nodes)) {
        		writeNodeAspectsInCX(doc, cxwtr,/* repIdSet, */citationIdMap, supportIdMap,
        				writeNodes,	writeNodeAttr, writeFunctionTerm, writeReifiedEdgeTerm,
        				writeNodeCitationLinks,writeNodeSupportLinks);
        	}  
        	
        	aspects.remove(NodesElement.NAME);
        	aspects.remove(NodeAttributesElement.NAME);
        	aspects.remove(FunctionTermElement.NAME);
        	aspects.remove(ReifiedEdgeElement.NAME);
        	aspects.remove(NodeCitationLinksElement.NAME);
        	aspects.remove(NodeSupportLinksElement.NAME);
        }
        
        
        boolean writeEdges = aspects.contains(EdgesElement.NAME);
        boolean writeEdgeAttr = aspects.contains(EdgeAttributesElement.NAME);
        boolean writeEdgeCitationLinks = aspects.contains(EdgeCitationLinksElement.NAME);
        boolean writeEdgeSupportLinks = aspects.contains(EdgeSupportLinksElement.NAME);
        if ( writeEdges || writeEdgeAttr || writeEdgeCitationLinks || writeEdgeSupportLinks ) {
        	for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Edges)) {
        		writeEdgeAspectsInCX(doc,cxwtr, citationIdMap, supportIdMap,
        			writeEdges, writeEdgeAttr, writeEdgeCitationLinks, writeEdgeSupportLinks);
        	}
        	aspects.remove(EdgesElement.NAME);
        	aspects.remove(EdgeAttributesElement.NAME);
        	aspects.remove(EdgeCitationLinksElement.NAME);
        	aspects.remove(EdgeSupportLinksElement.NAME);
        }
        
        for ( String opaqueAspectName : aspects) {
        	writeOpaqueAspect(cxwtr, opaqueAspectName, -1);
        }
        //Add post metadata
        
        MetaDataCollection postmd = new MetaDataCollection ();
        
        if ( !dbHasMetadata) {
        	if ( preMetaData.getMetaDataElement(NodesElement.NAME)!=null && nodeIdCounter > 0 ) {
        		postmd.setIdCounter(NodesElement.NAME, nodeIdCounter);
        	}
        
        	if ( preMetaData.getMetaDataElement(EdgesElement.NAME) != null && edgeIdCounter > 0 ) {
        		postmd.setIdCounter(EdgesElement.NAME, edgeIdCounter);
        	}
        	if ( preMetaData.getMetaDataElement(CitationElement.NAME)!= null && citationIdCounter >0)
        		postmd.setIdCounter(CitationElement.NAME, citationIdCounter);
        
        	if ( preMetaData.getMetaDataElement(SupportElement.NAME) != null && supportIdCounter > 0 )
        		postmd.setIdCounter(SupportElement.NAME, supportIdCounter);
        }
        
        if ( postmd.size()>0)
          cxwtr.addPostMetaData(postmd);        
        cxwtr.end();

	}


	private int writeNamespacesInCX(CxWriter cxwtr, int limit) throws IOException {
		NamespacesElement prefixtab = new NamespacesElement();
        int counter = 0;
        for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Namespace))  {
           if ( limit <= 0 || counter < limit )
        	   counter ++;
           else 
        	   break;
           String prefix = doc.field(NdexClasses.ns_P_prefix);
           if ( prefix !=null ) {
        	   String uri = doc.field(NdexClasses.ns_P_uri);
        	   prefixtab.put(prefix, uri);
           }
        }
         
        if ( prefixtab .size() >0) {
        	writeNdexAspectElementAsAspectFragment(cxwtr, prefixtab);
        }
        
        return counter;
	}


}
