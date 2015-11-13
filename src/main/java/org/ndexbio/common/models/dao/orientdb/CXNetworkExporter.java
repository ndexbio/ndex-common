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

import org.cxio.aspects.datamodels.EdgeAttributesElement;
import org.cxio.aspects.datamodels.EdgesElement;
import org.cxio.aspects.datamodels.NetworkAttributesElement;
import org.cxio.aspects.datamodels.NodeAttributesElement;
import org.cxio.aspects.datamodels.NodesElement;
import org.cxio.aspects.datamodels.ATTRIBUTE_DATA_TYPE;
import org.cxio.aspects.datamodels.AttributesAspectUtils;
import org.cxio.core.CxWriter;
import org.cxio.core.interfaces.AspectElement;
import org.cxio.core.interfaces.AspectFragmentWriter;
import org.cxio.metadata.MetaDataCollection;
import org.cxio.metadata.MetaDataElement;
import org.cxio.util.CxioUtil;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.cx.aspect.CXMetaDataManager;
import org.ndexbio.common.cx.aspect.GeneralAspectFragmentWriter;
import org.ndexbio.model.cx.BELNamespaceElement;
import org.ndexbio.model.cx.CXSimpleAttribute;
import org.ndexbio.model.cx.CitationElement;
import org.ndexbio.model.cx.EdgeCitationLinksElement;
import org.ndexbio.model.cx.EdgeSupportLinksElement;
import org.ndexbio.model.cx.FunctionTermElement;
import org.ndexbio.model.cx.NamespacesElement;
import org.ndexbio.model.cx.NdexNetworkStatus;
import org.ndexbio.model.cx.NodeCitationLinksElement;
import org.ndexbio.model.cx.NodeSupportLinksElement;
import org.ndexbio.model.cx.Provenance;
import org.ndexbio.model.cx.ReifiedEdgeElement;
import org.ndexbio.model.cx.SupportElement;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.task.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Direction;

public class CXNetworkExporter extends SingleNetworkDAO {
	

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
      try {
        cxwtr.start();
        
        //write NdexStatus & provenance
        writeNdexStatus(cxwtr);
        
        writeProvenance( cxwtr);
        
        //always export the context first.
        writeNamespacesInCX(cxwtr, -1);
        
        // write name, desc and other properties;
        writeNetworkAttributes(cxwtr,-1);
        
        
        Map<Long,Long> citationIdMap = new TreeMap<> ();
        Map<Long,Long> supportIdMap = new TreeMap<> ();
        
        for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Citations)) {
        	Long citationId = doc.field(NdexClasses.Element_ID);
        	Long SID = writeCitationInCX(doc, cxwtr);
        	citationIdMap.put(citationId, SID);
        }
        
        for ( ODocument doc: getNetworkElements (NdexClasses.Network_E_Supports)) {
        	Long supportId = doc.field(NdexClasses.Element_ID);
        	Long SID = writeSupportInCX(doc, cxwtr);
        	supportIdMap.put(supportId, SID);
        }
           
        
        for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Nodes)) {
        	writeNodeInCX(doc, cxwtr,/* repIdSet,*/ citationIdMap, supportIdMap); 
        }        
        
        for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Edges)) {
        	writeEdgeInCX(doc,cxwtr, citationIdMap, supportIdMap);
        }
        
        for ( ODocument doc : getNetworkElements(BELNamespaceElement.ASPECT_NAME)) {
        	writeNamespaceFileInCX(doc,cxwtr);
        }

        writeOpaqueAspects(cxwtr);
        
        //Add post metadata
        
        MetaDataCollection postmd = new MetaDataCollection ();
        
        if ( nodeIdCounter > 0 )
        	postmd.setIdCounter(NodesElement.ASPECT_NAME, nodeIdCounter);
        if ( edgeIdCounter > 0 )
        	postmd.setIdCounter(EdgesElement.ASPECT_NAME, edgeIdCounter);
        if ( citationIdCounter >0)
        	postmd.setIdCounter(CitationElement.ASPECT_NAME, citationIdCounter);
        if ( supportIdCounter > 0 )
        	postmd.setIdCounter(SupportElement.ASPECT_NAME, supportIdCounter);
        if ( postmd.size() > 0 ) 
        	cxwtr.addPostMetaData(postmd);        
        cxwtr.end(true,"");
      } catch (Exception e ) {
    	  cxwtr.end(false, "Error: " + e.getMessage() );
    	  throw e;
      }

	}

	private void writeNamespaceFileInCX(ODocument doc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		String prefix = doc.field(NdexClasses.BELPrefix);
		String content = doc.field(NdexClasses.BELNamespaceFileContent);
		
		writeNdexAspectElementAsAspectFragment(cxwtr, new BELNamespaceElement(prefix, content));
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
        			t = AttributesAspectUtils.toDataType(p.getDataType().toLowerCase());
        		} catch (IllegalArgumentException e) {
        			System.out.println("Property type " + p.getDataType() + " unsupported. Converting it to String in CX output.");
        		}	
        		if ( !AttributesAspectUtils.isListType(t)) {
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

	private int writeProvenance(CxWriter cxwtr) throws NdexException, IOException {
		Provenance provenance = new Provenance();
        
        ObjectMapper mapper = new ObjectMapper();
        // get the provenance string
        String provenanceString = this.networkDoc.field(NdexClasses.Network_P_provenance);
        // deserialize it to create a ProvenanceEntity object
        if (provenanceString != null && provenanceString.length() > 0){
            ProvenanceEntity e = mapper.readValue(provenanceString, ProvenanceEntity.class);
            provenance.setEntity(e);
            writeNdexAspectElementAsAspectFragment(cxwtr, provenance);
            return 1;
        }
        return 0;
	}

	
	private MetaDataCollection createCXMataData() {
		MetaDataCollection md = CXMetaDataManager.getInstance().createCXMataDataTemplate();

        Timestamp lastUpdate = new Timestamp( ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime());
        int edgecount = networkDoc.field(NdexClasses.Network_P_edgeCount);
        int nodecount = networkDoc.field(NdexClasses.Network_P_nodeCount);
        
        md.setElementCount(NodesElement.ASPECT_NAME, new Long(nodecount));
        md.setElementCount(EdgesElement.ASPECT_NAME, new Long(edgecount));
  
        for ( MetaDataElement e : md ) {
            e.setLastUpdate(lastUpdate.getTime());
        }
        
        return md;
	}
	

	private void writeEdgeInCX(ODocument doc, CxWriter cxwtr, Map<Long,Long> citationIdMap,
		    Map<Long,Long> supportIdMap ) throws ObjectNotFoundException, IOException {
	
		Long SID = getSIDFromDoc ( doc);

		// track the counter
	if ( edgeIdCounter >=0 ) {
		long l = SID.longValue();
		if (l>edgeIdCounter)
			edgeIdCounter = l;
	}
	
	ODocument srcDoc = doc.field("in_"+ NdexClasses.Edge_E_subject);
	ODocument tgtDoc = doc.field("out_"+NdexClasses.Edge_E_object);
	
	Long srcId = srcDoc.field(NdexClasses.Element_SID);
	if ( srcId == null )
		srcId = srcDoc.field(NdexClasses.Element_ID);
	
	Long tgtId = tgtDoc.field(NdexClasses.Element_SID);
	if ( tgtId == null)
		tgtId = tgtDoc.field(NdexClasses.Element_ID);
	
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
		cxwtr.startAspectFragment(EdgeAttributesElement.ASPECT_NAME);
		for ( NdexPropertyValuePair p : props ) {
			ATTRIBUTE_DATA_TYPE t = AttributesAspectUtils.toDataType(p.getDataType().toLowerCase());
			EdgeAttributesElement ep = AttributesAspectUtils.isListType(t) ? 
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

	
	private void writeEdgeAspectsInCX(ODocument doc, CxWriter cxwtr, Map<Long,Long> citationIdMap,
		    Map<Long,Long> supportIdMap , 
		    boolean writeEdges, boolean writeEdgeAttr,
		    boolean writeEdgeCitationLinks, boolean writeEdgeSupportLinks) throws ObjectNotFoundException, IOException {
	
		Long SID = getSIDFromDoc ( doc);
	
		if ( writeEdges) {
			// track the counter
			if ( edgeIdCounter >=0 ) {
				try { 
	   
					long l = SID.longValue();
					if (l>edgeIdCounter)
						edgeIdCounter = l;
				} catch ( NumberFormatException e) {
					System.out.println("Non-numeric SID found in edge aspect. Will ignore tracking id counters for this aspect.");
					edgeIdCounter = -1;
				}
			}
	
			ODocument srcDoc = doc.field("in_"+ NdexClasses.Edge_E_subject);
			ODocument tgtDoc = doc.field("out_"+NdexClasses.Edge_E_object);
	
			Long srcId = srcDoc.field(NdexClasses.Element_SID);
			if ( srcId == null )
				srcId = srcDoc.field(NdexClasses.Element_ID);
	
			Long tgtId = tgtDoc.field(NdexClasses.Element_SID);
			if ( tgtId == null)
				tgtId = tgtDoc.field(NdexClasses.Element_ID);
	
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
					cxwtr.startAspectFragment(EdgeAttributesElement.ASPECT_NAME);
					for ( NdexPropertyValuePair p : props ) {
						ATTRIBUTE_DATA_TYPE t = AttributesAspectUtils.toDataType(p.getDataType().toLowerCase());
						EdgeAttributesElement ep = AttributesAspectUtils.isListType(t) ? 
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
	

	
	private Long writeCitationInCX(ODocument doc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		
	    CitationElement result = new CitationElement();
		
  	    Long SID = getSIDFromDoc(doc);
		
		// track the counter
		if ( citationIdCounter >=0 ) {
			if (SID.longValue()>citationIdCounter)
				citationIdCounter = SID.longValue();

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

	private void writeReifiedEdgeTermInCX(Long nodeSID, ODocument reifiedEdgeDoc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		ODocument e = reifiedEdgeDoc.field("out_" + NdexClasses.ReifiedEdge_E_edge);
		Long eid = getSIDFromDoc(e);
			
		writeNdexAspectElementAsAspectFragment(cxwtr, new ReifiedEdgeElement(nodeSID, eid));
			
	}
	
	
	private void writeNodeInCX(ODocument doc, CxWriter cxwtr, 
			 Map<Long,Long> citationIdMap,  Map<Long,Long> supportIdMap) 
			throws IOException, NdexException {		
		writeNodeAspectsInCX(doc, cxwtr, citationIdMap, supportIdMap,
				true,true, true, true,true, true);
	}
	
	private void writeNodeAspectsInCX(ODocument doc, CxWriter cxwtr, /*Set<Long> repIdSet, */
			 Map<Long,Long> citationIdMap,  Map<Long,Long> supportIdMap,
			boolean writeNodes,boolean	writeNodeAttr, boolean writeFunctionTerm, boolean writeReifiedEdgeTerm,
			boolean writeNodeCitationLinks, boolean writeNodeSupportLinks) 
			throws IOException, NdexException {
		
		Long SID = getSIDFromDoc(doc);
		
		Long repId = doc.field(NdexClasses.Node_P_represents);
		String represents = null;
		if ( repId != null && repId.longValue() > 0) {
			String termType = doc.field(NdexClasses.Node_P_representTermType);
			
			if ( termType.equals(NdexClasses.BaseTerm) ) {
				represents = this.getBaseTermStringById(repId);
			} else if (termType.equals(NdexClasses.ReifiedEdgeTerm) ) {
				if ( writeReifiedEdgeTerm) {
					ODocument reifiedEdgeDoc = this.getReifiedEdgeDocById(repId);
					writeReifiedEdgeTermInCX(SID, reifiedEdgeDoc, cxwtr);
				}
			} else if (termType.equals(NdexClasses.FunctionTerm) ) {
				if ( writeFunctionTerm) {
					ODocument funcDoc = this.getFunctionDocById(repId);
					writeNdexAspectElementAsAspectFragment(cxwtr, getFunctionTermsElementFromDoc(SID, funcDoc));
				}
			} else 
				throw new NdexException ("Unsupported term type '" + termType + 
								"' found for term Id:" + repId);
		}		
		
		if (writeNodes) {
			  // track the counter
			  if ( nodeIdCounter >=0 ) {
				long l = SID.longValue();
				if (l>nodeIdCounter)
					nodeIdCounter = l;
			  }
			  writeNdexAspectElementAsAspectFragment(cxwtr,
					  new NodesElement(SID , (String) doc.field(NdexClasses.Node_P_name),represents));
		}
		
		if ( writeNodeAttr)
			writeNodeAttributesInCX(doc, cxwtr, /*repIdSet, */SID);
  	
		//writeCitations and supports
		writeCitationAndSupportLinks(SID,  doc, cxwtr, citationIdMap,
			   supportIdMap , false, writeNodeCitationLinks, writeNodeSupportLinks );
	}

	private void writeNodeAttributesInCX(ODocument doc, CxWriter cxwtr, Long SID)
			throws ObjectNotFoundException, IOException, NdexException {

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
		 		cxwtr.startAspectFragment(NodeAttributesElement.ASPECT_NAME);
		 		for ( NdexPropertyValuePair p : props ) {
		 			ATTRIBUTE_DATA_TYPE t = AttributesAspectUtils.toDataType(p.getDataType().toLowerCase());
		 			NodeAttributesElement ep = AttributesAspectUtils.isListType(t) ?
						NodeAttributesElement.createInstanceWithMultipleValues(p.getSubNetworkId(),
			   					SID, p.getPredicateString(), p.getValue(), t) : 
						new NodeAttributesElement (p.getSubNetworkId(),SID, p.getPredicateString(), p.getValue(), t);
					cxwtr.writeAspectElement(ep);
		 		}
		 		cxwtr.endAspectFragment();
		 	}
	}
	
	private void writeCitationAndSupportLinks(Long SID, ODocument doc,  CxWriter cxwtr, Map<Long,Long> citationIdMap,
		    Map<Long,Long> supportIdMap ,boolean isEdge, boolean writeCitationLinks, boolean writeSupportLinks ) throws ObjectNotFoundException, IOException {
   	
	//write citations
	if ( writeCitationLinks ) {
	  Collection<Long> citations = doc.field(NdexClasses.Citation);
	
	  if ( citations !=null) {
		List<Long> cids = new ArrayList<> (citations.size());
		
		for ( Long citationId : citations) {
			Long csid = citationIdMap.get(citationId);
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
			List<Long> supIds = new ArrayList<> (supports.size());
		
			for ( Long supId : supports) {
				Long ssid = supportIdMap.get(supId);
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

	
	private Long writeSupportInCX(ODocument doc, CxWriter cxwtr) throws ObjectNotFoundException, IOException {
		
		SupportElement result = new SupportElement();
		
 	    Long SID = getSIDFromDoc(doc);
 	    
		// track the counter
		if ( supportIdCounter >=0 ) {
			long l = SID.longValue();
			if (l>supportIdCounter)
				supportIdCounter = l;
		}
		result.setId(SID);
		result.setText((String)doc.field(NdexClasses.Support_P_text));
		
		Long citationId = doc.field(NdexClasses.Citation);
		
		if ( citationId !=null) {
			ODocument cDoc = this.getCitationDocById(citationId);
			Long cId = cDoc.field(NdexClasses.Element_SID);
			if ( cId == null)
				cId = cDoc.field(NdexClasses.Element_ID);
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
        
        GeneralAspectFragmentWriter cfw = new GeneralAspectFragmentWriter(CitationElement.ASPECT_NAME);
        
        for (AspectFragmentWriter afw : CxioUtil.getAllAvailableAspectFragmentWriters() ) {
        	cxwtr.addAspectFragmentWriter(afw);
        }
        
        cxwtr.addAspectFragmentWriter(cfw);
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(SupportElement.ASPECT_NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(NodeCitationLinksElement.ASPECT_NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(EdgeCitationLinksElement.ASPECT_NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(EdgeSupportLinksElement.ASPECT_NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(NodeSupportLinksElement.ASPECT_NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(FunctionTermElement.ASPECT_NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(NamespacesElement.ASPECT_NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(ReifiedEdgeElement.ASPECT_NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(NdexNetworkStatus.ASPECT_NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(Provenance.ASPECT_NAME));
        cxwtr.addAspectFragmentWriter(new GeneralAspectFragmentWriter(BELNamespaceElement.ASPECT_NAME));
        
        return cxwtr;
	}
	

	private FunctionTermElement getFunctionTermsElementFromDoc(Long nodeSID, ODocument funcDoc) throws ObjectNotFoundException {
		Long btId = funcDoc.field(NdexClasses.BaseTerm);
		String bt = this.getBaseTermStringById(btId);
	
 	   	List<Object> args = new ArrayList<>();

 	    Object f = funcDoc.field("out_"+ NdexClasses.FunctionTerm_E_paramter);

 	    if ( f == null)   {   // function without parameters.
 	    	return new FunctionTermElement(nodeSID,bt, args);
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
	    return new FunctionTermElement(nodeSID, bt, args);
	}

	
	public void writeOneAspectInCX(OutputStream out, String aspectName, int elementLimit, boolean use_default_pretty_printer) throws IOException, NdexException {
		init();
		//prepare metadata
		MetaDataCollection preMetaData ;
		
		MetaDataCollection metadata = networkDoc.field(NdexClasses.Network_P_metadata);
		if ( metadata != null) {
				MetaDataElement e = metadata.getMetaDataElement(aspectName);
				if ( e == null)
					throw new NdexException ("Aspect " + aspectName + " not found in network " + uuid);
				e.getData().remove(MetaDataElement.ELEMENT_COUNT);
				preMetaData = new MetaDataCollection();
				preMetaData.add(e);
		} else {
			// construct metadata
			List<String> l = new ArrayList<>(1);
			l.add(aspectName);
			preMetaData = CXMetaDataManager.getInstance().createCXMataDataTemplateForAspects(l);
		    Timestamp lastUpdate = new Timestamp( ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime());
		    preMetaData.setLastUpdate(aspectName, lastUpdate.getTime());
		}
		
		CxWriter cxwtr = getNdexCXWriter(out, use_default_pretty_printer);     
        cxwtr.addPreMetaData(preMetaData);
        
        try {
        	cxwtr.start();

        	// start writing aspects out.
        	//write NdexStatus
        	long counter =0 ;
        	if (aspectName.equals(NdexNetworkStatus.ASPECT_NAME)) {
        		counter = writeNdexStatus(cxwtr);
        	} else if ( aspectName.equals(NetworkAttributesElement.ASPECT_NAME)) {
        		// write name, desc and other properties;
        		counter = writeNetworkAttributes(cxwtr, elementLimit);
        	} else if (   aspectName.equals(NamespacesElement.ASPECT_NAME)) {
        		//write namespaces 
        		counter = writeNamespacesInCX(cxwtr, elementLimit);
        	} else if (  aspectName.equals(Provenance.ASPECT_NAME) ) { 
        		writeProvenance(cxwtr);
        		counter = 1;
        	} else {
        	
        		// tracking ids to SID mapping and baseterms that has been outputed.
        		Map<Long,Long> citationIdMap = new TreeMap<> ();
        		Map<Long,Long> supportIdMap = new TreeMap<> ();
        
        		if ( aspectName.equals(CitationElement.ASPECT_NAME)) {
        			for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Citations)) {
        				if ( elementLimit <=0 || counter < elementLimit)
        					counter ++;
        				else 
        					break;
        				Long citationId = doc.field(NdexClasses.Element_ID);
        				Long SID = writeCitationInCX(doc, cxwtr);
        				citationIdMap.put(citationId, SID);
        			}
        		} else if (aspectName.equals(SupportElement.ASPECT_NAME) ) {
        			for ( ODocument doc: getNetworkElements (NdexClasses.Network_E_Supports)) {
        			if ( elementLimit <=0 || counter < elementLimit)
        				counter ++;
        			else 
        				break;
        			Long supportId = doc.field(NdexClasses.Element_ID);
        			Long SID = writeSupportInCX(doc, cxwtr);
        			supportIdMap.put(supportId, SID);
        			}
        		} else {
        			boolean writeNodes             = aspectName.equals(NodesElement.ASPECT_NAME);
        			boolean writeNodeAttr          = aspectName.equals(NodeAttributesElement.ASPECT_NAME);
        			boolean writeFunctionTerm      = aspectName.equals(FunctionTermElement.ASPECT_NAME);
        			boolean writeReifiedEdgeTerm   = aspectName.equals(ReifiedEdgeElement.ASPECT_NAME);
        			boolean writeNodeCitationLinks = aspectName.equals(NodeCitationLinksElement.ASPECT_NAME);
        			boolean writeNodeSupportLinks  = aspectName.equals(NodeSupportLinksElement.ASPECT_NAME);
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
        				boolean writeEdges = aspectName.equals(EdgesElement.ASPECT_NAME);
        				boolean writeEdgeAttr = aspectName.equals(EdgeAttributesElement.ASPECT_NAME);
        				boolean writeEdgeCitationLinks = aspectName.equals(EdgeCitationLinksElement.ASPECT_NAME);
        				boolean writeEdgeSupportLinks = aspectName.equals(EdgeSupportLinksElement.ASPECT_NAME);
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

        	cxwtr.end(true, "");
        } catch (Exception e) {
        	cxwtr.end(false, "Error on the server:" + e.getMessage());
        	throw e;
        }

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
		MetaDataCollection preMetaData ;
		
		boolean dbHasMetadata = false;
		MetaDataCollection metadata = networkDoc.field(NdexClasses.Network_P_metadata);
		if ( metadata != null) {
			dbHasMetadata = true;
			preMetaData = new MetaDataCollection();
			for (String aspectName : aspects) {
				MetaDataElement e = metadata.getMetaDataElement(aspectName);
				if ( e == null)
					throw new NdexException ("Aspect " + aspectName + " not found in network " + uuid);
				preMetaData.add(e);
			}
		} else {
			// construct metadata
			preMetaData = CXMetaDataManager.getInstance().createCXMataDataTemplateForAspects(aspects);
		    long lastUpdate =  ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime();
		    int edgecount = networkDoc.field(NdexClasses.Network_P_edgeCount);
		    int nodecount = networkDoc.field(NdexClasses.Network_P_nodeCount);
		    
		    if ( aspects.contains(NodesElement.ASPECT_NAME))
		    	preMetaData.setElementCount(NodesElement.ASPECT_NAME, new Long(nodecount));
		    if ( aspects.contains(EdgesElement.ASPECT_NAME))
		    	preMetaData.setElementCount(EdgesElement.ASPECT_NAME, new Long(edgecount));
		    
		    for ( MetaDataElement e : preMetaData) {
		    	e.setLastUpdate(lastUpdate);
		    }

		}
		
		CxWriter cxwtr = getNdexCXWriter(out, use_default_pretty_printer);     
        cxwtr.addPreMetaData(preMetaData);
        
     try {   
        cxwtr.start();

		// start writing aspects out.
        
        //write NdexStatus
        if (aspects.contains(NdexNetworkStatus.ASPECT_NAME)) {
           writeNdexStatus(cxwtr);
           aspects.remove(NdexNetworkStatus.ASPECT_NAME);
        }
        
        // write provenance
        if ( aspects.contains(Provenance.ASPECT_NAME)) {
        	writeProvenance(cxwtr);
        	aspects.remove(Provenance.ASPECT_NAME);
        }
        
        // write name, desc and other properties;
        if ( aspects.contains(NetworkAttributesElement.ASPECT_NAME)) {
           writeNetworkAttributes(cxwtr, -1);
           aspects.remove(NetworkAttributesElement.ASPECT_NAME);
        }
        
        //write namespaces 
        if (  aspects.contains(NamespacesElement.ASPECT_NAME)) {
        	writeNamespacesInCX(cxwtr, -1);
        	aspects.remove(NetworkAttributesElement.ASPECT_NAME);
        }
        
        // tracking ids to SID mapping and baseterms that has been outputed.
        Map<Long,Long> citationIdMap = new TreeMap<> ();
        Map<Long,Long> supportIdMap = new TreeMap<> ();
     //   Set<Long> repIdSet = new TreeSet<> ();

        
        if ( aspects.contains(CitationElement.ASPECT_NAME)) {
        	for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Citations)) {
        		Long citationId = doc.field(NdexClasses.Element_ID);
        		Long SID = writeCitationInCX(doc, cxwtr);
        		citationIdMap.put(citationId, SID);
        	}
        	aspects.remove(CitationElement.ASPECT_NAME);
        }
        
        if (aspects.contains(SupportElement.ASPECT_NAME) ) {
        	for ( ODocument doc: getNetworkElements (NdexClasses.Network_E_Supports)) {
        		Long supportId = doc.field(NdexClasses.Element_ID);
        		Long SID = writeSupportInCX(doc, cxwtr);
        		supportIdMap.put(supportId, SID);
        	}
        	aspects.remove(SupportElement.ASPECT_NAME);
        }   
        
        boolean writeNodes             = aspects.contains(NodesElement.ASPECT_NAME);
        boolean writeNodeAttr          = aspects.contains(NodeAttributesElement.ASPECT_NAME);
        boolean writeFunctionTerm      = aspects.contains(FunctionTermElement.ASPECT_NAME);
        boolean writeReifiedEdgeTerm   = aspects.contains(ReifiedEdgeElement.ASPECT_NAME);
        boolean writeNodeCitationLinks = aspects.contains(NodeCitationLinksElement.ASPECT_NAME);
        boolean writeNodeSupportLinks  = aspects.contains(NodeSupportLinksElement.ASPECT_NAME);
        if ( writeNodes|| writeNodeAttr ||writeFunctionTerm || writeReifiedEdgeTerm || 
        		writeNodeCitationLinks || writeNodeSupportLinks) {
        	for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Nodes)) {
        		writeNodeAspectsInCX(doc, cxwtr,/* repIdSet, */citationIdMap, supportIdMap,
        				writeNodes,	writeNodeAttr, writeFunctionTerm, writeReifiedEdgeTerm,
        				writeNodeCitationLinks,writeNodeSupportLinks);
        	}  
        	
        	aspects.remove(NodesElement.ASPECT_NAME);
        	aspects.remove(NodeAttributesElement.ASPECT_NAME);
        	aspects.remove(FunctionTermElement.ASPECT_NAME);
        	aspects.remove(ReifiedEdgeElement.ASPECT_NAME);
        	aspects.remove(NodeCitationLinksElement.ASPECT_NAME);
        	aspects.remove(NodeSupportLinksElement.ASPECT_NAME);
        }
        
        
        boolean writeEdges = aspects.contains(EdgesElement.ASPECT_NAME);
        boolean writeEdgeAttr = aspects.contains(EdgeAttributesElement.ASPECT_NAME);
        boolean writeEdgeCitationLinks = aspects.contains(EdgeCitationLinksElement.ASPECT_NAME);
        boolean writeEdgeSupportLinks = aspects.contains(EdgeSupportLinksElement.ASPECT_NAME);
        if ( writeEdges || writeEdgeAttr || writeEdgeCitationLinks || writeEdgeSupportLinks ) {
        	for ( ODocument doc : getNetworkElements(NdexClasses.Network_E_Edges)) {
        		writeEdgeAspectsInCX(doc,cxwtr, citationIdMap, supportIdMap,
        			writeEdges, writeEdgeAttr, writeEdgeCitationLinks, writeEdgeSupportLinks);
        	}
        	aspects.remove(EdgesElement.ASPECT_NAME);
        	aspects.remove(EdgeAttributesElement.ASPECT_NAME);
        	aspects.remove(EdgeCitationLinksElement.ASPECT_NAME);
        	aspects.remove(EdgeSupportLinksElement.ASPECT_NAME);
        }
        
        for ( String opaqueAspectName : aspects) {
        	writeOpaqueAspect(cxwtr, opaqueAspectName, -1);
        }
        //Add post metadata
        
        MetaDataCollection postmd = new MetaDataCollection ();
        
        if ( !dbHasMetadata) {
        	if ( preMetaData.getMetaDataElement(NodesElement.ASPECT_NAME)!=null && nodeIdCounter > 0 ) {
        		postmd.setIdCounter(NodesElement.ASPECT_NAME, nodeIdCounter);
        	}
        
        	if ( preMetaData.getMetaDataElement(EdgesElement.ASPECT_NAME) != null && edgeIdCounter > 0 ) {
        		postmd.setIdCounter(EdgesElement.ASPECT_NAME, edgeIdCounter);
        	}
        	if ( preMetaData.getMetaDataElement(CitationElement.ASPECT_NAME)!= null && citationIdCounter >0)
        		postmd.setIdCounter(CitationElement.ASPECT_NAME, citationIdCounter);
        
        	if ( preMetaData.getMetaDataElement(SupportElement.ASPECT_NAME) != null && supportIdCounter > 0 )
        		postmd.setIdCounter(SupportElement.ASPECT_NAME, supportIdCounter);
        }
        
        if ( postmd.size()>0)
          cxwtr.addPostMetaData(postmd);        
        cxwtr.end(true, "");
     } catch (Exception e )  {
    	 cxwtr.end(false, "Error: " + e.getMessage());
    	 throw e;
     }
        
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

	
	public MetaDataCollection getMetaDataCollection () {
		MetaDataCollection md = networkDoc.field(NdexClasses.Network_P_metadata);
		if ( md != null) {
			return md;
		}
			
		md = CXMetaDataManager.getInstance().createCXMataDataTemplate();

        Timestamp lastUpdate = new Timestamp( ((Date)networkDoc.field(NdexClasses.ExternalObj_mTime)).getTime());
        int edgecount = networkDoc.field(NdexClasses.Network_P_edgeCount);
        int nodecount = networkDoc.field(NdexClasses.Network_P_nodeCount);
        
        md.setElementCount(NodesElement.ASPECT_NAME, new Long(nodecount));
        md.setElementCount(EdgesElement.ASPECT_NAME, new Long(edgecount));
  
        //check citation
        populateMDElementCnt(md, NdexClasses.Network_E_Citations, CitationElement.ASPECT_NAME);
        populateMDElementCnt(md, NdexClasses.Network_E_FunctionTerms, FunctionTermElement.ASPECT_NAME);
        populateMDElementCnt(md, NdexClasses.Network_E_Namespace, NamespacesElement.ASPECT_NAME);
        populateMDElementCnt(md, NdexClasses.Network_E_ReifiedEdgeTerms, ReifiedEdgeElement.ASPECT_NAME);
        populateMDElementCnt(md, NdexClasses.Network_E_Supports, SupportElement.ASPECT_NAME);
        populateMDElementCnt(md, BELNamespaceElement.ASPECT_NAME, BELNamespaceElement.ASPECT_NAME);
        
        if ( md.getMetaDataElement(CitationElement.ASPECT_NAME)==null) {
        	md.remove(NodeCitationLinksElement.ASPECT_NAME);
        	md.remove(EdgeCitationLinksElement.ASPECT_NAME);
        }
        if ( md.getMetaDataElement(SupportElement.ASPECT_NAME) == null) {
        	md.remove(NodeSupportLinksElement.ASPECT_NAME);
        	md.remove(EdgeSupportLinksElement.ASPECT_NAME);
        }
        for ( MetaDataElement e : md ) {
            e.setLastUpdate(lastUpdate.getTime());
        }
        
        return md;
	}


	private void populateMDElementCnt(MetaDataCollection md,String edgeName, String aspectName) {
		long count = networkVertex.countEdges(Direction.OUT, edgeName);     
        if ( count == 0 ) {
        	md.remove(aspectName);
        } else 
        	md.setElementCount(aspectName, count);
	}

}
