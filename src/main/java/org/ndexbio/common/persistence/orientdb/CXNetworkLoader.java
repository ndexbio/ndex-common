package org.ndexbio.common.persistence.orientdb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.cxio.aspects.datamodels.AbstractAttributesAspectElement;
import org.cxio.aspects.datamodels.AbstractAttributesAspectElement.ATTRIBUTE_DATA_TYPE;
import org.cxio.aspects.datamodels.EdgeAttributesElement;
import org.cxio.aspects.datamodels.EdgesElement;
import org.cxio.aspects.datamodels.NetworkAttributesElement;
import org.cxio.aspects.datamodels.NodeAttributesElement;
import org.cxio.aspects.datamodels.NodesElement;
import org.cxio.aspects.readers.EdgeAttributesFragmentReader;
import org.cxio.aspects.readers.EdgesFragmentReader;
import org.cxio.aspects.readers.NetworkAttributesFragmentReader;
import org.cxio.aspects.readers.NodeAttributesFragmentReader;
import org.cxio.aspects.readers.NodesFragmentReader;
import org.cxio.core.CxElementReader;
import org.cxio.core.OpaqueElement;
import org.cxio.core.interfaces.AspectElement;
import org.cxio.core.interfaces.AspectFragmentReader;
import org.cxio.metadata.MetaDataCollection;
import org.cxio.metadata.MetaDataElement;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.cx.aspect.GeneralAspectFragmentReader;
import org.ndexbio.common.models.dao.orientdb.BasicNetworkDAO;
import org.ndexbio.common.models.dao.orientdb.OrientdbDAO;
import org.ndexbio.common.models.dao.orientdb.SingleNetworkDAO;
import org.ndexbio.common.models.dao.orientdb.UserDocDAO;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.common.util.TermUtilities;
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
import org.ndexbio.model.exceptions.DuplicateObjectException;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;

import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.task.NdexServerQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class CXNetworkLoader extends BasicNetworkDAO {
	
//	private static final long CORE_CACHE_SIZE = 100000L;
//	private static final long NON_CORE_CACHE_SIZE = 100000L;
	
    protected static Logger logger = LoggerFactory.getLogger(CXNetworkLoader.class);;

	//private static final String nodeName = "name";
	
	private long counter;
	
	private InputStream inputStream;
	private NdexDatabase ndexdb;
//	private ODatabaseDocumentTx connection;
	private String ownerAcctName;
	private ODocument networkDoc;
	private OrientVertex networkVertex;
	
    protected OrientGraph graph;

	private Map<String, Long> nodeSIDMap;
	private Map<String, Long> edgeSIDMap;
	private Map<String, Long> citationSIDMap;
	private Map<String, Long> supportSIDMap;
	private Map<String, Long> namespaceMap;
	private Map<String, Long> baseTermMap;
	private Set<Long> undefinedNodeId;
	private Set<Long> undefinedEdgeId;
	private Set<Long> undefinedCitationId;
	private Set<Long> undefinedSupportId;
	
	private NamespacesElement ns;
	
	long opaqueCounter ;

	private UUID uuid;
	
	private Map<String, String> opaqueAspectEdgeTable;
	
//	private Map<String, MetaDataElement> metaData;
	
	public CXNetworkLoader(InputStream iStream,String ownerAccountName)  throws NdexException {
		super();
		this.inputStream = iStream;
		
		ndexdb = NdexDatabase.getInstance();
		
		ownerAcctName = ownerAccountName;
		
		graph =  new OrientGraph(db,false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);
				
	}

	private void init () {
		opaqueCounter = 0;
		counter =0; 
		
		nodeSIDMap = new TreeMap<>();
		edgeSIDMap = new TreeMap<> ();
		citationSIDMap = new TreeMap<> ();
		supportSIDMap = new TreeMap<> ();
		this.namespaceMap = new TreeMap<>();
		this.baseTermMap = new TreeMap<>();
		ns = null;
		
		undefinedNodeId = new TreeSet<>();
		undefinedEdgeId = new TreeSet<>();
		undefinedSupportId = new TreeSet<>();
		undefinedCitationId = new TreeSet<>();

		opaqueAspectEdgeTable = new HashMap<>();
	}
	
	private CxElementReader createCXReader () throws IOException {
		HashSet<AspectFragmentReader> readers = new HashSet<>(20);
		
		  readers.add(EdgesFragmentReader.createInstance());
		  readers.add(EdgeAttributesFragmentReader.createInstance());
		  readers.add(NetworkAttributesFragmentReader.createInstance());
		  readers.add(NodesFragmentReader.createInstance());
		  readers.add(NodeAttributesFragmentReader.createInstance());
		  
		  readers.add(new GeneralAspectFragmentReader (NdexNetworkStatus.NAME,
				NdexNetworkStatus.class));
		  readers.add(new GeneralAspectFragmentReader (NamespacesElement.NAME,NamespacesElement.class));
		  readers.add(new GeneralAspectFragmentReader (FunctionTermElement.NAME,FunctionTermElement.class));
		  readers.add(new GeneralAspectFragmentReader (CitationElement.NAME,CitationElement.class));
		  readers.add(new GeneralAspectFragmentReader (SupportElement.NAME,SupportElement.class));
		  readers.add(new GeneralAspectFragmentReader (ReifiedEdgeElement.NAME,ReifiedEdgeElement.class));
		  readers.add(new GeneralAspectFragmentReader (EdgeCitationLinksElement.NAME,EdgeCitationLinksElement.class));
		  readers.add(new GeneralAspectFragmentReader (EdgeSupportLinksElement.NAME,EdgeSupportLinksElement.class));
		  readers.add(new GeneralAspectFragmentReader (NodeCitationLinksElement.NAME,NodeCitationLinksElement.class));
		  readers.add(new GeneralAspectFragmentReader (NodeSupportLinksElement.NAME,NodeSupportLinksElement.class));
		  
		  return  CxElementReader.createInstance(inputStream, true,
				   readers);
	}
	
	//TODO: will modify this function to return a CX version of NetworkSummary object.
	public UUID persistCXNetwork() throws IOException, ObjectNotFoundException, NdexException {
		
        init();
        
		NdexNetworkStatus netStatus = null;
	    uuid = NdexUUIDFactory.INSTANCE.createNewNDExUUID();
	    
	    try {
		  networkDoc = this.createNetworkHeadNode(uuid);
		  networkVertex = graph.getVertex(networkDoc);
		

		  CxElementReader cxreader = createCXReader();
		  
		  MetaDataCollection metadata = cxreader.getPreMetaData();
		
		  for ( AspectElement elmt : cxreader ) {
			String aspectName = elmt.getAspectName();
			if ( aspectName.equals(NodesElement.NAME)) {       //Node
			    NodesElement ne = (NodesElement) elmt;
			    String nid = ne.getId();
			    Long nodeId = nodeSIDMap.get(nid);
			    if ( nodeId == null) 
			       createCXNodeBySID(nid);
			    else {
			       if ( !undefinedNodeId.remove(nodeId))  // it has been defined more than once
			    	 throw new DuplicateObjectException(NodesElement.NAME, nid);
			    }
			} else if (aspectName.equals(NdexNetworkStatus.NAME)) {    //ndexStatus
				netStatus = (NdexNetworkStatus) elmt;
				saveNetworkStatus(netStatus);
			} else if ( aspectName.equals(EdgesElement.NAME)) {        // Edge
				EdgesElement ee = (EdgesElement) elmt;
				createCXEdge(ee);
			} else if ( aspectName.equals(NamespacesElement.NAME)) {    // namespace
				this.ns = (NamespacesElement) elmt;
				createCXContext(ns);
			} else if ( aspectName.equals(NodeAttributesElement.NAME)) {  // node attributes
				NodeAttributesElement e = (NodeAttributesElement) elmt;
				addNodeAttribute(e );
			} else if ( aspectName.equals(FunctionTermElement.NAME)) {   // function term
				FunctionTermElement e = (FunctionTermElement) elmt;
				createFunctionTerm(e);
			} else if ( aspectName.equals(NetworkAttributesElement.NAME)) {  //network attributes
				NetworkAttributesElement e = ( NetworkAttributesElement) elmt;
				createNetworkAttribute(e);
			} else if ( aspectName.equals(EdgeAttributesElement.NAME)) {     // edge attibutes
				EdgeAttributesElement e = (EdgeAttributesElement) elmt;
				addEdgeAttribute(e );
			} else if ( aspectName.equals(ReifiedEdgeElement.NAME)) {   // reified edge
				createReifiedEdgeTerm((ReifiedEdgeElement) elmt);
			} else if ( aspectName.equals(CitationElement.NAME)) {      // citation
				createCitation((CitationElement) elmt);
			} else if ( aspectName.equals(SupportElement.NAME)) {
				createSupport((SupportElement) elmt);
			} else if ( aspectName.equals(EdgeCitationLinksElement.NAME)) {
				createEdgeCitation((EdgeCitationLinksElement) elmt);
			} else if ( aspectName.equals(EdgeSupportLinksElement.NAME)) {
				createEdgeSupport((EdgeSupportLinksElement) elmt);
			} else if ( aspectName.equals(NodeSupportLinksElement.NAME))  {
				createNodeSupport((NodeSupportLinksElement) elmt);
			} else if ( aspectName.equals(NodeCitationLinksElement.NAME)) {
				createNodeCitation((NodeCitationLinksElement) elmt);
			} else {    // opaque aspect
				addOpaqueAspectElement((OpaqueElement) elmt);
			}

		}
		
		  //save the metadata
		  MetaDataCollection postmetadata = cxreader.getPostMetaData();
		  if ( postmetadata !=null) {
			  if( metadata == null) {
				  metadata = postmetadata;
			  } else {
				  for (MetaDataElement e : postmetadata.toCollection()) {
					  Long cnt = e.getIdCounter();
					  if ( cnt !=null) {
						 metadata.setIdCounter(e.getName(),cnt);
					  }
					  cnt = e.getElementCount() ;
					  if ( cnt !=null) {
							 metadata.setElementCount(e.getName(),cnt);
					  }
				  }
			  }
		  }

		  if(metadata !=null)
		      networkDoc.field(NdexClasses.Network_P_metadata,metadata);
		  		  
		  // finalize the headnode
		  networkDoc.fields(NdexClasses.ExternalObj_mTime,new Timestamp(Calendar.getInstance().getTimeInMillis()),
				   NdexClasses.Network_P_isComplete,true,
				   NdexClasses.Network_P_opaquEdgeTable, this.opaqueAspectEdgeTable);
		  networkDoc.save(); 
		
		  // set the admin
		
		  UserDocDAO userdao = new UserDocDAO(db);
		  ODocument ownerDoc = userdao.getRecordByAccountName(ownerAcctName, null) ;
		  OrientVertex ownerV = graph.getVertex(ownerDoc);
		  
		  for	(int retry = 0;	retry <	OrientdbDAO.maxRetries;	++retry)	{
				try	{
					ownerV.reload();
					ownerV.addEdge(NdexClasses.E_admin, this.networkVertex);
					break;
				} catch(ONeedRetryException	e)	{
					logger.warn("Retry - " + e.getMessage());
					
				}
			}		
		graph.commit();
		return uuid;
		
		} catch (Exception e) {
			// delete network and close the database connection
			e.printStackTrace();
			this.abortTransaction();
			throw new NdexException("Error occurred when loading CX stream. " + e.getMessage());
		} 
       
	}
	
	private void addOpaqueAspectElement(OpaqueElement elmt) throws IOException {
		
		String aspectName = elmt.getAspectName();

		String edgeName = this.opaqueAspectEdgeTable.get(aspectName);
		if ( edgeName == null) {
			edgeName = NdexClasses.Network_E_opaque_asp_prefix + this.opaqueCounter;
			opaqueCounter ++;
			opaqueAspectEdgeTable.put(aspectName, edgeName);
		}
		ODocument doc = new ODocument (NdexClasses.OpaqueElement).
				field(edgeName, elmt.toJsonString() ).save();
		networkVertex.addEdge(edgeName, graph.getVertex(doc));
		tick();
	}


	private void createEdgeSupport(EdgeSupportLinksElement elmt) throws ObjectNotFoundException, DuplicateObjectException {
		for ( String sourceId : elmt.getSourceIds()) {
		   Long edgeId = edgeSIDMap.get(sourceId);
		   if ( edgeId == null) {
			  edgeId = createCXEdgeBySID(sourceId);
			  undefinedEdgeId.add(edgeId);
		   }
		
		   ODocument edgeDoc = getEdgeDocById(edgeId);
		   Set<Long> supportIds = edgeDoc.field(NdexClasses.Support);
		
		  if(supportIds == null)
			  supportIds = new HashSet<>(elmt.getSupportIds().size());
		
		  for ( String supportSID : elmt.getSupportIds()) {
			Long supportId = supportSIDMap.get(supportSID);
			if ( supportId == null) {
				supportId = createSupportBySID(supportSID);
				undefinedSupportId.add(supportId);
			}
			supportIds.add(supportId);
		  }
		
		  edgeDoc.field(NdexClasses.Support, supportIds).save();
		}
	}

	private void createNodeSupport(NodeSupportLinksElement elmt) throws ObjectNotFoundException, DuplicateObjectException {
	  for (String sourceId : elmt.getSourceIds())	 {
		Long nodeId = nodeSIDMap.get(sourceId);
		if ( nodeId == null) {
			nodeId = createCXNodeBySID(sourceId);
			undefinedNodeId.add(nodeId);
		}
		
		ODocument nodeDoc = getNodeDocById(nodeId);
		Set<Long> supportIds = nodeDoc.field(NdexClasses.Support);
		
		if(supportIds == null)
			supportIds = new HashSet<>(elmt.getSupportIds().size());
		
		for ( String supportSID : elmt.getSupportIds()) {
			Long supportId = supportSIDMap.get(supportSID);
			if ( supportId == null) {
				supportId = createSupportBySID(supportSID);
				undefinedSupportId.add(supportId);
			}
			supportIds.add(supportId);
		}
		
		nodeDoc.field(NdexClasses.Support, supportIds).save();
	  }	
	}
	
	private void createEdgeCitation(EdgeCitationLinksElement elmt) throws DuplicateObjectException, ObjectNotFoundException {
	  for ( String sourceId : elmt.getSourceIds())	 {
		Long edgeId = edgeSIDMap.get(sourceId);
		if ( edgeId == null) {
			edgeId = createCXEdgeBySID(sourceId);
			undefinedEdgeId.add(edgeId);
		}
		
		ODocument edgeDoc = getEdgeDocById(edgeId);
		Set<Long> citationIds = edgeDoc.field(NdexClasses.Citation);
		
		if(citationIds == null)
			citationIds = new HashSet<>(elmt.getCitationIds().size());
		
		for ( String citationSID : elmt.getCitationIds()) {
			Long citationId = citationSIDMap.get(citationSID);
			if ( citationId == null) {
				citationId = createCitationBySID(citationSID);
				undefinedCitationId.add(citationId);
			}
			citationIds.add(citationId);
		}
		
		edgeDoc.field(NdexClasses.Citation, citationIds).save();
	  }
	}

	
	private void createNodeCitation(NodeCitationLinksElement elmt) throws DuplicateObjectException, ObjectNotFoundException {
	  for ( String sourceId : elmt.getSourceIds())	{
		Long nodeId = nodeSIDMap.get(sourceId);
		if ( nodeId == null) {
			nodeId = createCXNodeBySID(sourceId);
			undefinedNodeId.add(nodeId);
		}
		
		ODocument nodeDoc = getNodeDocById(nodeId);
		Set<Long> citationIds = nodeDoc.field(NdexClasses.Citation);
		
		if(citationIds == null)
			citationIds = new HashSet<>(elmt.getCitationIds().size());
		
		for ( String citationSID : elmt.getCitationIds()) {
			Long citationId = citationSIDMap.get(citationSID);
			if ( citationId == null) {
				citationId = createCitationBySID(citationSID);
				undefinedCitationId.add(citationId);
			}
			citationIds.add(citationId);
		}
		
		nodeDoc.field(NdexClasses.Citation, citationIds).save();
	  }	
	}
	
	private Long createSupportBySID(String sid) {
		Long supportId =ndexdb.getNextId() ;

		ODocument supportDoc = new ODocument(NdexClasses.Support)
		   .fields(NdexClasses.Element_ID, supportId,
				   NdexClasses.Element_SID, sid).save()	;

		OrientVertex supportV = graph.getVertex(supportDoc);

		networkVertex.addEdge(NdexClasses.Network_E_Supports, supportV);

		this.supportSIDMap.put(sid, supportId);
		return supportId;
	}
	
	
	private Long createSupport(SupportElement elmt) {
		Long supportId =ndexdb.getNextId() ;

		ODocument supportDoc = new ODocument(NdexClasses.Support)
		   .fields(NdexClasses.Element_ID, supportId,
				   NdexClasses.Element_SID, elmt.getId(),
		           NdexClasses.Support_P_text, elmt.getText())	;

		if(elmt.getProps()!=null && elmt.getProps().size()>0) {
			Collection<NdexPropertyValuePair> properties = new ArrayList<>(elmt.getProps().size());
			for ( CXSimpleAttribute s : elmt.getProps())	{	
				properties.add(new NdexPropertyValuePair(s));
			}
			supportDoc.field(NdexClasses.ndexProperties, properties);
		}

		supportDoc.save();

		OrientVertex supportV = graph.getVertex(supportDoc);

		networkVertex.addEdge(NdexClasses.Network_E_Supports, supportV);

		this.supportSIDMap.put(elmt.getId(), supportId);
		return supportId;
	}
	
	
	private void createReifiedEdgeTerm(ReifiedEdgeElement e) throws DuplicateObjectException, ObjectNotFoundException {
		 String nodeSID = e.getNode();
		 Long nodeId = nodeSIDMap.get(nodeSID);
		 if(nodeId == null) {
			 nodeId = createCXNodeBySID(nodeSID);
			 undefinedNodeId.add(nodeId);
		 }
		
		 String edgeSID = e.getEdge();
		 Long edgeId = edgeSIDMap.get(edgeSID);
		 if (edgeId == null ) {
			 edgeId = createCXEdgeBySID(edgeSID);
			 undefinedEdgeId.add(edgeId);
		 }
		 
		 Long termId = ndexdb.getNextId();
		 ODocument reifiedEdgeTermDoc = new ODocument(NdexClasses.ReifiedEdgeTerm)
				 	.fields(NdexClasses.Element_ID, termId).save();
				 			
		 ODocument edgeDoc = this.getEdgeDocById(edgeId); 
		 OrientVertex retV = graph.getVertex(reifiedEdgeTermDoc);
		 retV.addEdge(NdexClasses.ReifiedEdge_E_edge, graph.getVertex(edgeDoc));
		 
		 
		 ODocument nodeDoc = this.getNodeDocById(nodeId);
		 nodeDoc.fields(NdexClasses.Node_P_represents, termId,
				    NdexClasses.Node_P_representTermType, NdexClasses.ReifiedEdgeTerm)
		   .save();
		 
		networkVertex.addEdge(NdexClasses.Network_E_ReifiedEdgeTerms, retV);

	}
	
	private void createNetworkAttribute(NetworkAttributesElement e) {
		if ( e.getName().equals(NdexClasses.Network_P_name)) {
			networkDoc.field(NdexClasses.Network_P_name,
					  e.getValue()).save();
		} else if ( e.getName().equals(NdexClasses.Network_P_desc)) {
			networkDoc.field(NdexClasses.Network_P_desc, e.getValue()).save();
		} else if ( e.getName().equals(NdexClasses.Network_P_version)) {
			networkDoc.field(NdexClasses.Network_P_version, e.getValue()).save();
		} else if ( e.getName().equals(SingleNetworkDAO.CXsrcFormatAttrName)) {
			networkDoc.field(NdexClasses.Network_P_source_format, e.getValue()).save();
		} else {
			
			NdexPropertyValuePair newProps= new NdexPropertyValuePair(e.getSubnetwork(),
					 e.getName(),
					 (e.isSingleValue() ? e.getValue(): e.getValuesAsString())
					 , e.getDataType().toString());
			List<NdexPropertyValuePair> props =networkDoc.field(NdexClasses.ndexProperties);
			if ( props == null) {
				props = new ArrayList<>(1);
			} 
			
			props.add(newProps);
			networkDoc.field(NdexClasses.ndexProperties,props).save();
		}
	}
	
	private static List<NdexPropertyValuePair> createNdexProperties(AbstractAttributesAspectElement e) {
		List <NdexPropertyValuePair> props = new ArrayList<> (e.getValues().size());
		for ( String value : e.getValues()) {
			props.add( new NdexPropertyValuePair(e.getSubnetwork(),
					 e.getName(),value, e.getDataType().toString()));
		}
		return props;
	}
	
	private void createCXContext(NamespacesElement context) throws DuplicateObjectException {
		for ( Map.Entry<String, String> e : context.entrySet()) {
			Long nsId = ndexdb.getNextId();

			ODocument nsDoc = new ODocument(NdexClasses.Namespace);
			nsDoc = nsDoc.fields(NdexClasses.Element_ID,nsId,
								 NdexClasses.ns_P_prefix, e.getKey(),
			                     NdexClasses.ns_P_uri, e.getValue())
			  .save();
			
	        
			OrientVertex nsV = graph.getVertex(nsDoc);
			networkVertex.addEdge(NdexClasses.Network_E_Namespace, nsV);
			Long oldv = this.namespaceMap.put(e.getKey(), nsId);
			if ( oldv !=null)
				throw new DuplicateObjectException(NamespacesElement.NAME, e.getKey());
			
			   tick();
		}
		
	}
	

	private void saveNetworkStatus(NdexNetworkStatus status ) {
		if ( status.getEdgeCount()>=0)
			networkDoc.field(NdexClasses.Network_P_edgeCount,status.getEdgeCount());
		if ( status.getNodeCount()>=0) {
			networkDoc.field(NdexClasses.Network_P_nodeCount, status.getNodeCount());
		}
		networkDoc.save();
	}
	
	private ODocument createNetworkHeadNode(UUID uuid ) {
	
		ODocument doc = new ODocument (NdexClasses.Network)
				  .fields(NdexClasses.Network_P_UUID,uuid,
						  NdexClasses.Network_P_name, "",
						  NdexClasses.Network_P_desc, "",
						  NdexClasses.Network_P_owner, ownerAcctName,
				          NdexClasses.ExternalObj_cTime, new Timestamp(Calendar.getInstance().getTimeInMillis()),
				          NdexClasses.ExternalObj_isDeleted, false,
				          NdexClasses.Network_P_isLocked, false,
				          NdexClasses.Network_P_isComplete, false,
				          NdexClasses.Network_P_cacheId, Long.valueOf(-1),
				          NdexClasses.Network_P_readOnlyCommitId, Long.valueOf(-1),
				          NdexClasses.Network_P_visibility,
				        		  VisibilityType.PRIVATE.toString() );
		return doc.save();
	}

	
	private Long createCXNodeBySID(String SID) throws DuplicateObjectException {
		Long nodeId = ndexdb.getNextId();
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node)
		   .fields(NdexClasses.Element_ID, nodeId,
				   NdexClasses.Element_SID, SID)
		   .save();
		Long oldId = nodeSIDMap.put(SID, nodeId);
		if ( oldId !=null)
			throw new DuplicateObjectException(NodesElement.NAME, SID);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,graph.getVertex(nodeDoc));
		   tick();
		return nodeId;
	}
		
	private Long createCXEdgeBySID(String SID) throws DuplicateObjectException {
		Long edgeId = ndexdb.getNextId();
		
		ODocument nodeDoc = new ODocument(NdexClasses.Edge)
		   .fields(NdexClasses.Element_ID, edgeId,
				   NdexClasses.Element_SID, SID)
		   .save();
		Long oldId = edgeSIDMap.put(SID, edgeId);
		if ( oldId !=null)
			throw new DuplicateObjectException(EdgesElement.NAME, SID);
		
		networkVertex.addEdge(NdexClasses.Network_E_Edges,graph.getVertex(nodeDoc));
		   tick();   
		return edgeId;
	}

	
	private Long createCXEdge(EdgesElement ee) throws NdexException {
		
		String relation = ee.getRelationship();
		Long btId = getBaseTermId(relation);

		Long edgeId = edgeSIDMap.get(ee.getId());
		ODocument edgeDoc;

		if ( edgeId == null ) { 
		  edgeId = ndexdb.getNextId();

	      edgeDoc = new ODocument(NdexClasses.Edge)
		   .fields(NdexClasses.Element_ID, edgeId,
				   NdexClasses.Element_SID, ee.getId(),
				   NdexClasses.Edge_P_predicateId, btId )
		   .save();
		   edgeSIDMap.put(ee.getId(), edgeId);
		} else {
			edgeDoc = this.getEdgeDocById(edgeId);
			edgeDoc.fields(NdexClasses.Edge_P_predicateId, btId).save();
		}
		
		OrientVertex edgeV = graph.getVertex(edgeDoc);
		
		Long newSubjectId = nodeSIDMap.get(ee.getSource());
        if ( newSubjectId == null) {
        	Long sId = createCXNodeBySID(ee.getSource());
        	undefinedNodeId.add(sId);
        }
	   
        ODocument subjectDoc = this.getNodeDocById(newSubjectId); 
        
	   graph.getVertex(subjectDoc).addEdge(NdexClasses.Edge_E_subject, edgeV);
		
	   
	   Long newObjectId = nodeSIDMap.get(ee.getTarget());
       if ( newObjectId == null) {
    	   Long tId = createCXNodeBySID(ee.getTarget());
    	   undefinedNodeId.add(tId);
       }
       
       
       ODocument objectDoc = getNodeDocById(newObjectId); 
       edgeV.addEdge(NdexClasses.Edge_E_object, graph.getVertex(objectDoc)); 
	   
	   networkVertex.addEdge(NdexClasses.Network_E_Edges,edgeV);
	   
	   tick();
	   undefinedNodeId.remove(edgeId);
		return edgeId;
	}
	
	private Long createCitationBySID(String sid) throws DuplicateObjectException {
		Long citationId = ndexdb.getNextId();
		
		ODocument citationDoc = new ODocument(NdexClasses.Citation)
		   .fields(NdexClasses.Element_ID, citationId,
				   NdexClasses.Element_SID, sid)
		   .save();
		
		Long oldId = citationSIDMap.put(sid, citationId);
		if ( oldId !=null)
			throw new DuplicateObjectException(CitationElement.NAME, sid);
		
		networkVertex.addEdge(NdexClasses.Network_E_Citations,graph.getVertex(citationDoc));
		   tick();   
		return citationId;
	}
	
	private Long createCitation(CitationElement c) {
		Long citationId = ndexdb.getNextId();

		//TODO: add description to citation.
		
		ODocument citationDoc = new ODocument(NdexClasses.Citation)
				  .fields(
						NdexClasses.Element_ID, citationId,
						NdexClasses.Element_SID, c.getId(),
				        NdexClasses.Citation_P_title, c.getTitle(),
				        NdexClasses.Citation_p_idType, c.getCitationType(),
				        NdexClasses.Citation_P_identifier, c.getIdentifier())
				        
				   .field( NdexClasses.Citation_P_contributors,c.getContributor(), OType.EMBEDDEDLIST);			   		
		
		if(c.getProps()!=null && c.getProps().size()>0) {
			Collection<NdexPropertyValuePair> properties = new ArrayList<>(c.getProps().size());
			for ( CXSimpleAttribute s : c.getProps())	{	
				properties.add(new NdexPropertyValuePair(s));
			}
			citationDoc.field(NdexClasses.ndexProperties, properties);
		}
		
		citationDoc.save();
        
		        
		OrientVertex citationV = graph.getVertex(citationDoc);
		networkVertex.addEdge(NdexClasses.Network_E_Citations, citationV);
		citationSIDMap.put(c.getId(), citationId);
		undefinedCitationId.remove(citationId);
		
		return citationId;
	}
	
	private Long createFunctionTerm(FunctionTermElement func) throws NdexException  {
		Long funcId = ndexdb.getNextId();
		
		Long baseTermId = getBaseTermId(func.getFunctionName());
		
		ODocument funcDoc = new ODocument(NdexClasses.FunctionTerm)
				.fields(NdexClasses.Element_ID, funcId,
						NdexClasses.BaseTerm, baseTermId).save();
		
		OrientVertex functionTermV = graph.getVertex(funcDoc);
					 
		for ( Object arg : func.getArgs()) {
			ODocument argumentDoc ;
			
			if ( arg instanceof String) {
				Long bId = getBaseTermId ((String)arg);
				argumentDoc = this.getBasetermDocById(bId);
			} else if ( arg instanceof FunctionTermElement ) {
				Long fId = createFunctionTerm((FunctionTermElement) arg);
				argumentDoc = this.getFunctionDocById(fId);
			} else
				throw new NdexException("Invalid function term argument type " + arg.getClass().getName() + " found." );
		    functionTermV.addEdge(NdexClasses.FunctionTerm_E_paramter, graph.getVertex(argumentDoc));
		}
			
		String nodeSID = func.getNodeID() ;
		if ( nodeSID != null) {
			Long nodeId = nodeSIDMap.get(nodeSID);
			if ( nodeId == null) {
				nodeId = createCXNodeBySID(nodeSID);
				undefinedNodeId.add(nodeId);
			}
			ODocument nodeDoc = this.getNodeDocById(nodeId);
			nodeDoc.fields(NdexClasses.Node_P_represents, funcId,
					NdexClasses.Node_P_representTermType,NdexClasses.FunctionTerm).save();
		}
		
		networkVertex.addEdge(NdexClasses.Network_E_FunctionTerms, functionTermV);
		
		return funcId;
	}
	
	
	
	private Long createBaseTerm(String termString) throws NdexException {
		
		// case 1 : termString is a URI
		// example: http://identifiers.org/uniprot/P19838
		// treat the last element in the URI as the identifier and the rest as
		// prefix string. Just to help the future indexing.
		//
		String prefix = null;
		String identifier = null;
		if ( termString.length() > 8 && termString.substring(0, 7).equalsIgnoreCase("http://") &&
				(!termString.endsWith("/"))) {
  		  try {
			URI termStringURI = new URI(termString);
				identifier = termStringURI.getFragment();
			
			    if ( identifier == null ) {
				    String path = termStringURI.getPath();
				    if (path != null && path.indexOf("/") != -1) {
				       int pos = termString.lastIndexOf('/');
					   identifier = termString.substring(pos + 1);
					   prefix = termString.substring(0, pos + 1);
				    } else
				       throw new NdexException ("Unsupported URI format in term: " + termString);
			    } else {
				    prefix = termStringURI.getScheme()+":"+termStringURI.getSchemeSpecificPart()+"#";
			    }
                 
			    Long btId = createBaseTerm(prefix,identifier, null);
			    baseTermMap.put(termString, btId);
				tick();
			    return btId;
			  
		  } catch (URISyntaxException e) {
			// ignore and move on to next case
		  }
		}
		
		Long btId = null;
		String[] termStringComponents = TermUtilities.getNdexQName(termString);
		if (termStringComponents != null && termStringComponents.length == 2) {
			// case 2: termString is of the form (NamespacePrefix:)*Identifier
			identifier = termStringComponents[1];
			prefix = termStringComponents[0];
			Long nsId = namespaceMap.get(prefix);

			if ( nsId !=null) {
			  btId = createBaseTerm(null, identifier, nsId);
			} else 
				btId = createBaseTerm(prefix + ":",identifier, null);
			baseTermMap.put(termString, btId);
			tick();
			return btId;
		} 
		
			// case 3: termString cannot be parsed, use it as the identifier.
			// so leave the prefix as null and create the baseterm
			identifier = termString;
	
		
		// create baseTerm in db
		Long id= createBaseTerm(null,identifier,null);
        this.baseTermMap.put(termString, id);
		   tick();
        return id;

	}
	
	private Long createBaseTerm(String prefix, String identifier, Long nsId) {
		Long termId = ndexdb.getNextId();
		
		ODocument btDoc = new ODocument(NdexClasses.BaseTerm)
		  .fields(NdexClasses.BTerm_P_name, identifier,
				  NdexClasses.Element_ID, termId,
		  		  NdexClasses.BTerm_P_prefix, prefix); 
		
		if ( nsId !=null)
			  btDoc.field(NdexClasses.BTerm_NS_ID, nsId);
 
		btDoc.save();
		OrientVertex basetermV = graph.getVertex(btDoc);
	//	networkVertex.getRecord().reload();
        networkVertex.addEdge(NdexClasses.Network_E_BaseTerms, basetermV);
		return termId;
	}

	private void addEdgeAttribute(EdgeAttributesElement e) throws NdexException{
		for ( String edgeSID : e.getPropertyOf()) {
		
		   Long edgeId = this.edgeSIDMap.get(edgeSID);
		   if ( edgeId == null) {
			  edgeId = createCXEdgeBySID(edgeSID);
			  undefinedEdgeId.add(edgeId);
		   }
		   
		   ODocument edgeDoc = getEdgeDocById(edgeId);

		   NdexPropertyValuePair newProps= new NdexPropertyValuePair(e.getSubnetwork(),
					 e.getName(),
					 (e.isSingleValue() ? e.getValue(): e.getValuesAsString()),
					 e.getDataType().toString());
		   List<NdexPropertyValuePair> props =edgeDoc.field(NdexClasses.ndexProperties);
			if ( props == null)
				props = new ArrayList<>(1);
			 
			props.add(newProps);
			edgeDoc.field(NdexClasses.ndexProperties,props).save();

			tick();
		}
	}
	
	
	private void addNodeAttribute(NodeAttributesElement e) throws NdexException{
		for ( String nodeSID : e.getPropertyOf()) {
		
		   Long nodeId = this.nodeSIDMap.get(nodeSID);
		   if ( nodeId == null) {
			  nodeId = createCXNodeBySID(nodeSID);
			  undefinedNodeId.add(nodeId);
		   }
		   
		   ODocument nodeDoc = getNodeDocById(nodeId);

		   String propName = e.getName();
		   if (propName.equals(NdexClasses.Node_P_name)) {      //  node name
			   if ( e.getDataType() != ATTRIBUTE_DATA_TYPE.STRING) {
				   new DuplicateObjectException("Node id " + nodeSID + " has wrong data type for attribute name: " + e.getValues().toString());
			   }
			   nodeDoc.field(NdexClasses.Node_P_name,e.getValue()).save();
		   } else if ( propName.equals(NdexClasses.Node_P_represents)){    // represents
			   if ( !e.isSingleValue()) {
				   new DuplicateObjectException("Node id " + nodeSID + " has 0 or more than 1 represents: " + e.getValues().toString());
			   }
			   Long btId = getBaseTermId ( e.getValue());
			   nodeDoc.fields(NdexClasses.Node_P_represents, btId,
					          NdexClasses.Node_P_representTermType,NdexClasses.BaseTerm).save();
			   
		   } else if ( propName.equals(NdexClasses.Node_P_alias)) {       // aliases
			   if (!e.getValues().isEmpty()) {
				   Set<Long> aliases = new TreeSet<>();
				   for ( String v : e.getValues()) {
					   aliases.add(getBaseTermId(v));
				   }
				   
				   nodeDoc.field(NdexClasses.Node_P_alias, aliases).save();
			   } 
		   } else if ( propName.equals(NdexClasses.Node_P_relatedTo)) {       // relateTo
			   if (!e.getValues().isEmpty()) {
				   Set<Long> relateTo = new TreeSet<>();
				   for ( String v : e.getValues()) {
					   relateTo.add(getBaseTermId(v));
				   }
				   
				   nodeDoc.field(NdexClasses.Node_P_relatedTo, relateTo).save();
			   } 
		   }  else {

			   NdexPropertyValuePair newProps= new NdexPropertyValuePair(e.getSubnetwork(),
						 e.getName(),
						 (e.isSingleValue() ? e.getValue(): e.getValuesAsString()),
						 e.getDataType().toString());
			   List<NdexPropertyValuePair> props =nodeDoc.field(NdexClasses.ndexProperties);
				if ( props == null)
					props = new ArrayList<>(1);
					
				props.add(newProps);
				nodeDoc.field(NdexClasses.ndexProperties,props).save();

				tick();
		   }
		   
		   tick();
		}
	}

	/**
	 * Get the id of the base term if it was already created. Othewise creates it and return its id.
	 * @param termString
	 * @return
	 * @throws NdexException
	 */
	private Long getBaseTermId(String termString) throws NdexException {
		Long btId = baseTermMap.get(termString);
		if ( btId == null) {
			btId = createBaseTerm(termString);
		}
		return btId;
	}

    
	
	@Override
	public void close() throws Exception {
		graph.shutdown();
	}
	
	private void tick() {
		counter ++;
		if ( counter % 5000 == 0 )  graph.commit();
		if ( counter %10000 == 0 )
			System.out.println("Loaded " + counter + " element in CX");
		
	}
	
	
	private void abortTransaction() throws ObjectNotFoundException, NdexException {
		logger.warn("AbortTransaction has been invoked from CX loader.");

		logger.info("Deleting partial network "+ uuid + " in order to rollback in response to error");
		networkDoc.field(NdexClasses.ExternalObj_isDeleted, true).save();
		graph.commit();
		
		Task task = new Task();
		task.setTaskType(TaskType.SYSTEM_DELETE_NETWORK);
		task.setResource(uuid.toString());
		NdexServerQueue.INSTANCE.addSystemTask(task);
		logger.info("Partial network "+ uuid + " is deleted.");
	}
	
}
