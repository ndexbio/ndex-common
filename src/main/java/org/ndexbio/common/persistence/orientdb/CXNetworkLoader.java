package org.ndexbio.common.persistence.orientdb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.cxio.aspects.datamodels.EdgesElement;
import org.cxio.aspects.datamodels.NodesElement;
import org.cxio.core.CxElementReader;
import org.cxio.core.CxReader;
import org.cxio.core.interfaces.AspectElement;
import org.cxio.core.interfaces.AspectFragmentReader;
import org.cxio.metadata.MetaData;
import org.cxio.metadata.MetaDataElement;
import org.cxio.util.Util;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.cx.aspect.GeneralAspectFragmentReader;
import org.ndexbio.common.cx.aspect.GeneralAspectFragmentWriter;
import org.ndexbio.common.models.dao.orientdb.BasicNetworkDAO;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.common.models.dao.orientdb.UserDocDAO;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.common.util.TermUtilities;
import org.ndexbio.model.cx.NamespacesElement;
import org.ndexbio.model.cx.NdexNetworkStatus;
import org.ndexbio.model.exceptions.DuplicateObjectException;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class CXNetworkLoader extends BasicNetworkDAO {
	
//	private static final long CORE_CACHE_SIZE = 100000L;
//	private static final long NON_CORE_CACHE_SIZE = 100000L;

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
	
	private NamespacesElement ns;
	
	private Map<String, MetaDataElement> metaData;
	
	public CXNetworkLoader(InputStream iStream,String ownerAccountName)  throws NdexException {
		super();
		this.inputStream = iStream;
		
		ndexdb = NdexDatabase.getInstance();
		
		ownerAcctName = ownerAccountName;
		metaData = new TreeMap<>();
		
		nodeSIDMap = new TreeMap<>();
		edgeSIDMap = new TreeMap<> ();
		citationSIDMap = new TreeMap<> ();
		supportSIDMap = new TreeMap<> ();
		this.namespaceMap = new TreeMap<>();
		this.baseTermMap = new TreeMap<>();
		ns = null;
		
		undefinedNodeId = new TreeSet<>();
		
		graph =  new OrientGraph(db,false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);
		
	}

	
	//TODO: will modify this function to return a CX version of NetworkSummary object.
	public void persistCXNetwork() throws IOException, ObjectNotFoundException, NdexException {
		
		NdexNetworkStatus netStatus = null;
		
		networkDoc = this.createNetworkHeadNode();
		networkVertex = graph.getVertex(networkDoc);
		
		try { 
		  Set<AspectFragmentReader> readers = Util.getAllAvailableAspectFragmentReaders();
		  readers.add(new GeneralAspectFragmentReader (NdexNetworkStatus.NAME,
				NdexNetworkStatus.class));
		
		  CxElementReader cxreader = CxElementReader.createInstance(inputStream, true,
				   readers);
		
		  for (MetaData md : cxreader.getMetaData()) {
			for ( MetaDataElement e : md.asListOfMetaDataElements() ) {
				String name = e.getName();
				System.out.println(name);
			}
			
		  }
		
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
			}

		}
		
		  // finalize the headnode
		  networkDoc.fields(NdexClasses.ExternalObj_mTime,new Timestamp(Calendar.getInstance().getTimeInMillis()),
				   NdexClasses.Network_P_isComplete,true );
		  networkDoc.save(); 
		
		  // set the admin
		
		  UserDocDAO userdao = new UserDocDAO(db);
		  ODocument ownerDoc = userdao.getRecordByAccountName(ownerAcctName, null) ;
		  OrientVertex ownerV = graph.getVertex(ownerDoc);
		  ownerV.addEdge(NdexClasses.E_admin, networkVertex);
		  
		  
		} catch (Throwable e) { 
			graph.removeVertex(graph.getVertex(networkDoc) );
			throw e;
		}
		
		graph.commit();
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
			
		}
		
	}
	
	

	private void saveNetworkStatus(NdexNetworkStatus status ) {
		if ( status.getEdgeCount()>=0)
			networkDoc.field(NdexClasses.Network_P_edgeCount,status.getEdgeCount());
		if ( status.getVersion() !=null)
			networkDoc.field(NdexClasses.Network_P_version, status.getVersion());
		if ( status.getNodeCount()>=0) {
			networkDoc.field(NdexClasses.Network_P_nodeCount, status.getNodeCount());
		}
		networkDoc.save();
	}
	
	private ODocument createNetworkHeadNode() {
		UUID u = NdexUUIDFactory.INSTANCE.createNewNDExUUID();
		ODocument doc = new ODocument (NdexClasses.Network)
				  .fields(NdexClasses.Network_P_UUID,u.toString(),
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
		
		new ODocument(NdexClasses.Node)
		   .fields(NdexClasses.Element_ID, nodeId,
				   NdexClasses.Element_SID, SID)
		   .save();
		Long oldId = nodeSIDMap.put(SID, nodeId);
		if ( oldId !=null)
			throw new DuplicateObjectException(NodesElement.NAME, SID);
		return nodeId;
	}
		

	private Long createCXEdge(EdgesElement ee) throws NdexException {
		Long edgeId = ndexdb.getNextId();
		
		String relation = ee.getRelationship();
		Long btId = baseTermMap.get(relation);
		if ( btId == null) {
			btId = createBaseTerm(relation);
		}
	
	    ODocument edgeDoc = new ODocument(NdexClasses.Edge)
		   .fields(NdexClasses.Element_ID, edgeId,
				   NdexClasses.Edge_P_predicateId, btId )
		   .save();
	    
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
		
		return edgeId;
	}
	
	
	
	
	private Long createBaseTerm(String termString) throws NdexException {
		
		// case 1 : termString is a URI
		// example: http://identifiers.org/uniprot/P19838
		// treat the last element in the URI as the identifier and the rest as
		// the namespace URI
		// find or create the namespace based on the URI
		// when creating, set the prefix based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
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
                 
			    Long btId = createBaseTerm(prefix,identifier);
			    baseTermMap.put(termString, btId);
			    return btId;
			  
		  } catch (URISyntaxException e) {
			// ignore and move on to next case
		  }
		}
		
		// case 2: termString is of the form (NamespacePrefix:)*Identifier
		// find or create the namespace based on the prefix
		// when creating, set the URI based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
		//
		
		String[] termStringComponents = TermUtilities.getNdexQName(termString);
		if (termStringComponents != null && termStringComponents.length == 2) {
			identifier = termStringComponents[1];
			prefix = termStringComponents[0];

		} else {

		// case 3: termString cannot be parsed, use it as the identifier.
		// find or create the namespace for prefix "LOCAL" and use that as the
		// namespace.
			identifier = termString;
			
		}
		
		// create baseTerm in db
		
		Long id= createBaseTerm(prefix,identifier);
        this.baseTermMap.put(termString, id);
        return id;

	}


	private Long createBaseTerm(String prefix, String identifier) {
		Long termId = ndexdb.getNextId();
		
		ODocument btDoc = new ODocument(NdexClasses.BaseTerm)
		  .fields(NdexClasses.BTerm_P_name, identifier,
				  NdexClasses.Element_ID, termId,
		  		  NdexClasses.BTerm_P_prefix, prefix); 
		  
		btDoc.save();
		OrientVertex basetermV = graph.getVertex(btDoc);
		networkVertex.getRecord().reload();
        networkVertex.addEdge(NdexClasses.Network_E_BaseTerms, basetermV);
		return termId;
	}
	
	@Override
	public void close() throws Exception {
		graph.shutdown();
	}
	
}
