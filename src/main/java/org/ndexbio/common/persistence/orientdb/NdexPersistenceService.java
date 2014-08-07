package org.ndexbio.common.persistence.orientdb;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ValidationException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.object.network.RawCitation;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.models.object.network.RawSupport;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.model.object.NdexProperty;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.network.Namespace;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;


/*
 * An implementation of the NDExPersistenceService interface that uses a 
 * in-memory cache to provide persistence for new ndex doain objects
 * 
 * 
 * Utilizes a Google Guava cache that uses Jdexids as keys and VertexFrame implementations 
 * as values
 */

public class NdexPersistenceService  {
	
	public static final String defaultCitationType="URI";
	public static final String pmidPrefix = "pmid:";


	private static final long CACHE_SIZE =  200000L;
    private static final Logger logger = Logger.getLogger(NdexPersistenceService.class.getName());
	// key is the element_id of a BaseTerm, value is the id of the node which this BaseTerm represents
    private Map<Long, Long> baseTermNodeIdMap;
	// Store the mapping in memory to have better performance.
	private Map<String, Long> baseTermStrMap;
	private NdexDatabase database;
	// key is the edge id which this term reifed.
    //private LoadingCache<Long, ReifiedEdgeTerm> reifiedEdgeTermCache;
 // key is the edge id which this term reifed.
    private Map<Long,Long>  edgeIdReifiedEdgeTermIdMap;
	private LoadingCache<Long, ODocument>  elementIdCache;
	// maps an external node id to new node id created in Ndex.
    private Map<Long, Long> externalIdNodeMap; 
	//key is a function term Id, value is the node id which uses 
    // that function as represents term
    private Map<Long,Long> functionTermIdNodeIdMap;
	private OrientGraph graph;
	private ODatabaseDocumentTx  localConnection;  //all DML will be in this connection, in one transaction.

	// maps a node name to Node Id.
    private Map<String, Long> namedNodeMap;
	private Map<RawNamespace, Namespace> namespaceMap;
	private Network network;

	//	private static Joiner idJoiner = Joiner.on(":").skipNulls();
	
	// key is the full URI or other fully qualified baseTerm as a string.
  //	private LoadingCache<String, BaseTerm> baseTermStrCache;

	private NetworkDAO  networkDAO;
	
    private ODocument networkDoc;
    
	private OrientVertex networkVertex;
    
    private ODocument ownerDoc;
    
    private Map<String, Namespace> prefixMap;

    private Map<RawCitation, Long>           rawCitationMap;
    
    
    private Map<FunctionTerm, Long> rawFunctionTermFunctionTermIdMap; 
    
    
    
    //private LoadingCache<RawSupport, Support>     rawSupportCache;
    private Map<RawSupport, Long>  rawSupportMap;
    
    // key is a "rawFunctionTerm", which has element id as -1. This table
    // matches the key to a functionTerm that has been stored in the db.
    
    private Map<Long, Long> reifiedEdgeTermIdNodeIdMap;
  //  private LoadingCache<Long, Node> reifiedEdgeTermNodeCache;
    
    //key is the name of the node. This cache is for loading simple SIF 
    // for now
//    private LoadingCache<String, Node> namedNodeCache;
    
    private final Stopwatch stopwatch;
    
    private Map<String, Namespace> URINamespaceMap;
    
    /*
     * Currently, the procces flow of this class is:
     * 
     * 1. create object 
     * 2. Create New network
     */
    
	public NdexPersistenceService(NdexDatabase db) {
		this.database = db;
		this.localConnection = this.database.getAConnection();
//		this.localConnection.begin();
		this.graph = new OrientGraph(this.localConnection,false);
		
//		graph.setAutoStartTx(false);
		this.networkDAO = new NetworkDAO(localConnection,true);
		prefixMap = new HashMap<String,Namespace>();
		URINamespaceMap = new HashMap<String,Namespace>();
		this.network = null;
		this.ownerDoc = null;
		this.stopwatch = Stopwatch.createUnstarted();
		
		this.baseTermStrMap = new TreeMap <String, Long>();
		this.namespaceMap   = new TreeMap <RawNamespace, Namespace>();
		this.rawCitationMap  = new TreeMap <RawCitation, Long> ();
        this.baseTermNodeIdMap = new TreeMap <Long,Long> ();
		this.namedNodeMap  = new TreeMap <String, Long> ();
		this.reifiedEdgeTermIdNodeIdMap = new HashMap<Long,Long>(100);
		this.edgeIdReifiedEdgeTermIdMap = new HashMap<Long,Long>(100);
		this.rawFunctionTermFunctionTermIdMap = new TreeMap<FunctionTerm, Long> ();
		this.rawSupportMap  = new TreeMap<RawSupport, Long> ();
		this.functionTermIdNodeIdMap = new HashMap<Long,Long>(100);
		// intialize caches.
		
/*		rawCitationCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<RawCitation, Citation>() {
				   @Override
				   public Citation load(RawCitation key) throws NdexException {
					return findOrCreateCitation(key);
				   }
			    }); */

		
	/*	baseTermStrCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<String, BaseTerm>() {
				   @Override
				   public BaseTerm load(String key) throws NdexException, ExecutionException {
					return findOrCreateBaseTerm(key);
				   }
			    }); */

/*		baseTermNodeCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<Long, Node>() {
				   @Override
				   public Node load(Long key) throws NdexException, ExecutionException {
					return findOrCreateNodeFromBaseTermId(key);
				   }
			    });
*/
		
		
		elementIdCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE*5)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<Long, ODocument>() {
				   @Override
				   public ODocument load(Long key) throws NdexException, ExecutionException {
					ODocument o = networkDAO.getDocumentByElementId(key);
                    if ( o == null )
                    	throw new NdexException ("Document is not found for element id: " + key);
					return o;
				   }
			    });
		
		externalIdNodeMap = new TreeMap<Long,Long>(); 

	}

	public void abortTransaction() {
		System.out.println(this.getClass().getName()
				+ ".abortTransaction has been invoked.");

		//localConnection.rollback();
		graph.rollback();
		
		// make sure everything relate to the network is deleted.
		//localConnection.begin();
		deleteNetwork();
		//localConnection.commit();
		graph.commit();
		System.out.println("Deleting network in order to rollback in response to error");
	}
	
	// alias is treated as a baseTerm
	public void addAliasToNode(long nodeId, String[] aliases) throws ExecutionException, NdexException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		for (String alias : aliases) {
			Long b= this.getBaseTermId(alias);
		    Long repNodeId = this.baseTermNodeIdMap.get(b);
			if ( repNodeId != null && repNodeId.equals(nodeId)) {
		    	logger.warning("Alias '" + alias + "' is also the represented base term of node " + 
			    nodeId +". Alias ignored.");
		    } else {
		    	ODocument bDoc = elementIdCache.get(b);
		    	nodeV.addEdge(NdexClasses.Node_E_alias, graph.getVertex(bDoc));
		    }
		    
		}
		
//		nodeV.getRecord().reload();
		elementIdCache.put(nodeId, nodeV.getRecord());
	}
				
				
	public void addCitationToElement(long elementId, Long citationId, String className) throws ExecutionException {
		ODocument elementRec = elementIdCache.get(elementId);
		OrientVertex nodeV = graph.getVertex(elementRec);
		
		ODocument citationRec = elementIdCache.get(citationId);
		OrientVertex citationV = graph.getVertex(citationRec);
		
		if ( className.equals(NdexClasses.Node) ) {
 	       	nodeV.addEdge(NdexClasses.Node_E_ciations, graph.getVertex(citationV));
		} else if ( className.equals(NdexClasses.Edge) ) {
			nodeV.addEdge(NdexClasses.Edge_E_citations, graph.getVertex(citationV));
		}
		
		ODocument o = nodeV.getRecord();
//		o.reload();
		elementIdCache.put(elementId, o);
	}
	
	//TODO: generalize this function so that createEdge(....) can use it.
	public void addMetaDataToNode (Long subjectNodeId, Long supportId, Long citationId,  Map<String,String> annotations) throws ExecutionException {
        
		ODocument nodeDoc = this.elementIdCache.get(subjectNodeId);
    	OrientVertex nodeVertex = graph.getVertex(nodeDoc);
		        
        if ( supportId != null) {
			ODocument supportDoc = this.elementIdCache.get(supportId);
	    	OrientVertex supportV = graph.getVertex(supportDoc);
	    	nodeVertex.addEdge(NdexClasses.Node_E_supports, supportV);
	    	
        }

	    if (citationId != null) {
			ODocument citationDoc = elementIdCache.get(citationId) ;
	    	OrientVertex citationV = graph.getVertex(citationDoc);
	    	nodeVertex.addEdge(NdexClasses.Node_E_ciations, citationV);
	    	
	    }

		if ( annotations != null) {
			for (Map.Entry<String, String> e : annotations.entrySet()) {
				ODocument pDoc = this.createNdexPropertyDoc(e.getKey(),e.getValue());
                OrientVertex pV = graph.getVertex(pDoc);
                nodeVertex.addEdge(NdexClasses.E_ndexProperties, pV);
                
                NdexProperty p = new NdexProperty();
                p.setPredicateString(e.getKey());
                p.setDataType(e.getValue());
			}
		}
		
//		nodeDoc.reload();
		this.elementIdCache.put(subjectNodeId, nodeDoc);

	}

	
	private void addPropertiesToVertex (OrientVertex vertex, Collection<NdexProperty> properties, 
			Collection<NdexProperty> presentationProperties) {

		if ( properties != null) {
			for (NdexProperty e : properties) {
				ODocument pDoc = this.createNdexPropertyDoc(e.getPredicateString(),e.getValue());
				pDoc.field(NdexClasses.ndexProp_P_datatype, e.getDataType())
				.save();
               OrientVertex pV = graph.getVertex(pDoc);
               vertex.addEdge(NdexClasses.E_ndexProperties, pV);
			}
		
		}

		if ( presentationProperties !=null ) {
			for (NdexProperty e : presentationProperties) {
				ODocument pDoc = this.createNdexPropertyDoc(e.getPredicateString(),e.getValue());
				pDoc.field(NdexClasses.ndexProp_P_datatype, e.getDataType())
				.save();
               OrientVertex pV = graph.getVertex(pDoc);
               vertex.addEdge(NdexClasses.E_ndexPresentationProps, pV);
			}
		}
	}


	public void addPropertyToNode(long nodeId, NdexProperty p, boolean isPresentationProperty) throws ExecutionException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);

		ODocument pDoc = this.createNdexPropertyDoc(p.getPredicateString(),p.getValue());
        OrientVertex pV = graph.getVertex(pDoc);
        
        if ( isPresentationProperty) 
        	nodeV.addEdge(NdexClasses.E_ndexPresentationProps, pV);
        else
        	nodeV.addEdge(NdexClasses.E_ndexProperties, pV);
	}

	// alias is treated as a baseTerm
	public void addRelatedTermToNode(long nodeId, String[] relatedTerms) throws ExecutionException, NdexException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		for (String rT : relatedTerms) {
			Long bID= this.getBaseTermId(rT);
		
			ODocument bDoc = elementIdCache.get(bID);
			nodeV.addEdge(NdexClasses.Node_E_relateTo, graph.getVertex(bDoc));
		//	elementIdCache.put(b.getId(), bDoc);
		}
		
//		nodeV.getRecord().reload();
		elementIdCache.put(nodeId, nodeV.getRecord());
	}
	

	public void commit () {
		//graph.commit();
		this.localConnection.commit();
		this.networkDoc.reload();
		this.networkVertex = graph.getVertex(networkDoc);
	//	logger.info("elementIdCachSize:" + elementIdCache.size());
	//	this.localConnection.begin();
	//	database.commit();
	}
	
	private Long createBaseTerm(String termString) throws NdexException, ExecutionException {
		// case 1 : termString is a URI
		// example: http://identifiers.org/uniprot/P19838
		// treat the last element in the URI as the identifier and the rest as
		// the namespace URI
		// find or create the namespace based on the URI
		// when creating, set the prefix based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
		//
		if ( termString.length() > 8 && termString.substring(0, 7).equalsIgnoreCase("http://") ) {
  		  try {
			URI termStringURI = new URI(termString);
//			String scheme = termStringURI.getScheme();
				String fragment = termStringURI.getFragment();
			
			    String prefix;
			    if ( fragment == null ) {
				    String path = termStringURI.getPath();
				    if (path != null && path.indexOf("/") != -1) {
					   fragment = path.substring(path.lastIndexOf('/') + 1);
					   prefix = termString.substring(0,
							termString.lastIndexOf('/') + 1);
				    } else
				       throw new NdexException ("Unsupported URI format in term: " + termString);
			    } else {
				    prefix = termStringURI.getScheme()+":"+termStringURI.getSchemeSpecificPart()+"#";
			    }
                 
			    RawNamespace rns = new RawNamespace(null, prefix);
			    Namespace namespace = getNamespace(rns);
			
			    // create baseTerm in db
			    Long id = createBaseTerm(fragment, namespace.getId());
		        this.baseTermStrMap.put(termString, id);
		        return id;
		  } catch (URISyntaxException e) {
			// ignore and move on to next case
		  }
		}
		// case 2: termString is of the form NamespacePrefix:Identifier
		// find or create the namespace based on the prefix
		// when creating, set the URI based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
		//
		String[] termStringComponents = termString.split(":");
		if (termStringComponents != null && termStringComponents.length == 2) {
			String identifier = termStringComponents[1];
			String prefix = termStringComponents[0];
			Namespace namespace = prefixMap.get(prefix);
			
			if ( namespace == null) {
				namespace = createLocalNamespaceforPrefix(prefix);
				logger.warning("Prefix '" + prefix + "' is not defined in the network. URI "+
				namespace.getUri()	+ " has been created for it by Ndex." );
			}
			
			// create baseTerm in db
			Long id= createBaseTerm(identifier, namespace.getId());
	        this.baseTermStrMap.put(termString, id);
	        return id;

		}

		// case 3: termString cannot be parsed, use it as the identifier.
		// find or create the namespace for prefix "LOCAL" and use that as the
		// namespace.

			// create baseTerm in db
    	Long id = createBaseTerm(termString, -1);
        this.baseTermStrMap.put(termString, id);
        return id;
		
	}

	private Long createBaseTerm(String localTerm, long nsId) throws ExecutionException {

//		BaseTerm bterm = new BaseTerm();
		Long termId = database.getNextId();
		
		ODocument btDoc = new ODocument(NdexClasses.BaseTerm)
		  .fields(NdexClasses.BTerm_P_name, localTerm,
				  NdexClasses.Element_ID, termId)
		  .save();

		OrientVertex basetermV = graph.getVertex(btDoc);
		
		if ( nsId >= 0) {

  		  ODocument nsDoc = elementIdCache.get(nsId); 
  		  
  		  OrientVertex nsV = graph.getVertex(nsDoc);
  		
  		  basetermV.addEdge(NdexClasses.BTerm_E_Namespace, nsV);
		}
		  
        networkVertex.addEdge(NdexClasses.Network_E_BaseTerms, basetermV);
//        btDoc.reload();
        elementIdCache.put(termId, btDoc);
		return termId;
	}
	
	
	/**
	 *  Create an edge in the database.
	 * @param subjectNodeId
	 * @param objectNodeId
	 * @param predicateId
	 * @param support
	 * @param citation
	 * @param annotation
	 * @return  The element id of the created edge.
	 * @throws NdexException
	 * @throws ExecutionException
	 */
	public Long createEdge(Long subjectNodeId, Long objectNodeId, Long predicateId, 
			 Long supportId, Long citationId, Map<String,String> annotation )
			throws NdexException, ExecutionException {
		if (null != objectNodeId && null != subjectNodeId && null != predicateId) {
			
			Long edgeId = database.getNextId(); 
			
			ODocument subjectNodeDoc = elementIdCache.get(subjectNodeId) ;
			ODocument objectNodeDoc  = elementIdCache.get(objectNodeId) ;
			ODocument predicateDoc   = elementIdCache.get(predicateId) ;
			
			ODocument edgeDoc = new ODocument(NdexClasses.Edge);
			edgeDoc = edgeDoc.field(NdexClasses.Element_ID, edgeId)
					.save();
			OrientVertex edgeVertex = graph.getVertex(edgeDoc);
			
			if ( annotation != null) {
				for (Map.Entry<String, String> e : annotation.entrySet()) {
					ODocument pDoc = this.createNdexPropertyDoc(e.getKey(),e.getValue());
                    OrientVertex pV = graph.getVertex(pDoc);
                    edgeVertex.addEdge(NdexClasses.E_ndexProperties, pV);
                    
                    NdexProperty p = new NdexProperty();
                    p.setPredicateString(e.getKey());
                    p.setDataType(e.getValue());
				}
			
			}

			networkVertex.addEdge(NdexClasses.Network_E_Edges, edgeVertex);
			edgeVertex.addEdge(NdexClasses.Edge_E_predicate, graph.getVertex(predicateDoc));
			edgeVertex.addEdge(NdexClasses.Edge_E_object, graph.getVertex(objectNodeDoc));
			graph.getVertex(subjectNodeDoc).addEdge(NdexClasses.Edge_E_subject, edgeVertex);

		    network.setEdgeCount(network.getEdgeCount()+1);
		    
		    if (citationId != null) {
				ODocument citationDoc = elementIdCache.get(citationId) ; 
		    	OrientVertex citationV = graph.getVertex(citationDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_citations, citationV);
		    	
		    }
		    
		    if ( supportId != null) {
				ODocument supportDoc =elementIdCache.get(supportId);
		    	OrientVertex supportV = graph.getVertex(supportDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_supports, supportV);
		    	
		    }
		    
		    elementIdCache.put(edgeId, edgeDoc);
			return edgeId;
		} 
		throw new NdexException("Null value for one of the parameter when creating Edge.");
	}
	
	public Edge createEdge(Long subjectNodeId, Long objectNodeId, Long predicateId, 
			 Support support, Citation citation, List<NdexProperty> properties, List<NdexProperty> presentationProps )
			throws NdexException, ExecutionException {
		if (null != objectNodeId && null != subjectNodeId && null != predicateId) {
			
			Edge edge = new Edge();
			edge.setId(database.getNextId());
			edge.setSubjectId(subjectNodeId);
			edge.setObjectId(objectNodeId);
			edge.setPredicateId(predicateId);
			
			ODocument subjectNodeDoc = elementIdCache.get(subjectNodeId) ;
			ODocument objectNodeDoc  = elementIdCache.get(objectNodeId) ;
			ODocument predicateDoc   = elementIdCache.get(predicateId) ;
			
			ODocument edgeDoc = new ODocument(NdexClasses.Edge);
			edgeDoc = edgeDoc.field(NdexClasses.Element_ID, edge.getId())
					.save();
			OrientVertex edgeVertex = graph.getVertex(edgeDoc);
			
			this.addPropertiesToVertex(edgeVertex, properties, presentationProps);
			
			networkVertex.addEdge(NdexClasses.Network_E_Edges, edgeVertex);
			edgeVertex.addEdge(NdexClasses.Edge_E_predicate, graph.getVertex(predicateDoc));
			edgeVertex.addEdge(NdexClasses.Edge_E_object, graph.getVertex(objectNodeDoc));
			graph.getVertex(subjectNodeDoc).addEdge(NdexClasses.Edge_E_subject, edgeVertex);

		    network.setEdgeCount(network.getEdgeCount()+1);
		    
		    if (citation != null) {
				ODocument citationDoc = elementIdCache.get(citation.getId());
		    	OrientVertex citationV = graph.getVertex(citationDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_citations, citationV);
		    	
		    	edge.getCitations().add(citation.getId());
		    }
		    
		    if ( support != null) {
				ODocument supportDoc = elementIdCache.get(support.getId());
		    	OrientVertex supportV = graph.getVertex(supportDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_supports, supportV);
		    	
		    	edge.getSupports().add(support.getId());
		    }
		    
//		    edgeDoc.reload();
		    elementIdCache.put(edge.getId(), edgeDoc);
			return edge;
		} 
		throw new NdexException("Null value for one of the parameter when creating Edge.");
	}

	
	private Namespace createLocalNamespaceforPrefix (String prefix) throws NdexException {
		String urlprefix = prefix.replace(' ', '_');
		return findOrCreateNamespace(
				new RawNamespace(prefix, "http://uri.ndexbio.org/ns/"+this.network.getExternalId()
						+"/" + urlprefix + "/"));
	}
	

	public void createNamespace(String prefix, String URI) throws NdexException {
		RawNamespace r = new RawNamespace(prefix, URI);
		getNamespace(r);
	}
	
	
    private ODocument createNdexPropertyDoc(String key, String value) {
		ODocument pDoc = new ODocument(NdexClasses.NdexProperty);
		pDoc.field(NdexClasses.ndexProp_P_predicateStr,key)
		   .field(NdexClasses.ndexProp_P_value, value)
		   .save();
		return pDoc;
		

	}

    private Network createNetwork(String title, String version, UUID uuid){
		this.network = new Network();
		this.network.setExternalId(uuid);
		this.network.setName(title);
		network.setVisibility(VisibilityType.PUBLIC);
		network.setIsLocked(false);
		network.setIsComplete(false);

		if ( version != null)
			this.network.setVersion(version);
        
		networkDoc = new ODocument (NdexClasses.Network)
		  .field(NdexClasses.Network_P_UUID,network.getExternalId().toString())
		  .field(NdexClasses.Network_P_cDate, network.getCreationDate())
		  .field(NdexClasses.Network_P_mDate, network.getModificationDate())
		  .field(NdexClasses.Network_P_name, network.getName())
		  .field(NdexClasses.Network_P_isLocked, network.getIsLocked())
		  .field(NdexClasses.Network_P_isComplete, network.getIsComplete())
		  .field(NdexClasses.Network_P_visibility, network.getVisibility().toString())
          .save();
		    
		networkVertex = graph.getVertex(getNetworkDoc());
		
		OrientVertex ownerV = graph.getVertex(ownerDoc);
		ownerV.addEdge(NdexClasses.E_admin, networkVertex);
		
		return this.network;
	}

	
	/*
	 * public method to allow xbel parsing components to rollback the
	 * transaction and close the database connection if they encounter an error
	 * situation
	 */

	public void createNewNetwork(String ownerName, String networkTitle, String version) throws Exception {
		createNewNetwork(ownerName, networkTitle, version,NdexUUIDFactory.INSTANCE.getNDExUUID() );
	}

	/*
	 * public method to persist INetwork to the orientdb database using cache
	 * contents.
	 */

	public void createNewNetwork(String ownerName, String networkTitle, String version, UUID uuid) throws NdexException  {
		Preconditions.checkNotNull(ownerName,"A network owner name is required");
		Preconditions.checkNotNull(networkTitle,"A network title is required");
		

		// find the network owner in the database
		ownerDoc =  findUserByAccountName(ownerName);
		if( null == ownerDoc){
			String message = "Account " +ownerName +" is not registered in the database"; 
			logger.severe(message);
			throw new NdexException(message);
		}
				
		createNetwork(networkTitle,version, uuid);

		logger.info("A new NDex network titled: " +network.getName()
				+" owned by " +ownerName +" has been created");
		
	}

/*
	public void networkProgressLogCheck() {
		commitCounter++;
		if (commitCounter % 1000 == 0) {
			logger.info("Checkpoint: Number of edges " + this.edgeCache.size());
		}

	}
*/

	/**
	 * performing delete the current network but not commiting it.
	 */
	public void deleteNetwork() {
		// TODO Implement deletion of network
		System.out
		.println("deleteNetwork called. Not yet implemented");
		
		
	}

	private Namespace findOrCreateNamespace(RawNamespace key) throws NdexException {
		Namespace ns = namespaceMap.get(key);

		if ( ns != null ) {
	        // check if namespace definitions are consistent
			if (key.getPrefix() !=null && key.getURI() !=null && 
	          		 !ns.getUri().equals(key.getURI()))
	          	   throw new NdexException("Namespace conflict: prefix " 
	          		       + key.getPrefix() + " maps to  " + 
	          			   ns.getUri() + " and " + key.getURI());

	        return ns;
		}
		
		if ( key.getPrefix() !=null && key.getURI() == null )
			throw new NdexException ("Prefix " + key.getPrefix() + " is not defined." );
		
		// persist the Namespace in db.
		ns = new Namespace();
		ns.setPrefix(key.getPrefix());
		ns.setUri(key.getURI());
		ns.setId(database.getNextId());
		

		ODocument nsDoc = new ODocument(NdexClasses.Namespace);
		nsDoc = nsDoc.field("prefix", key.getPrefix())
		  .field("uri", ns.getUri())
		  .field("id", ns.getId())
		  .save();
		
        
		OrientVertex nsV = graph.getVertex(nsDoc);
		OrientVertex networkV = graph.getVertex(getNetworkDoc());
		networkV.addEdge(NdexClasses.Network_E_Namespace, nsV);
		
		if (ns.getPrefix() != null) 
			prefixMap.put(ns.getPrefix(), ns);
		
		if ( ns.getUri() != null) 
			URINamespaceMap.put(ns.getUri(), ns);
		
		elementIdCache.put(ns.getId(),nsDoc);
		namespaceMap.put(key, ns);
		return ns; 
		
	}
	
	public Long findOrCreateNodeIdByExternalId(Long id) {
		Long nodeId = this.externalIdNodeMap.get(id);
		if ( nodeId != null) return nodeId;
		
		//create a node for this external id.
		
		nodeId = database.getNextId();

		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc =nodeDoc.field(NdexClasses.Element_ID, nodeId)
				.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		
		network.setNodeCount(network.getNodeCount()+1);
		
		elementIdCache.put(nodeId, nodeDoc);
		
		externalIdNodeMap.put(id, nodeId);
		return nodeId;

	}
	
	private Long findOrCreateNodeIdFromBaseTermId(Long bTermId) throws ExecutionException {
		Long nodeId = this.baseTermNodeIdMap.get(bTermId);
		
		if (nodeId != null) 
			return nodeId;
		
		// otherwise insert Node.
		nodeId = database.getNextId();

		ODocument termDoc = elementIdCache.get(bTermId); 
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc =nodeDoc.field(NdexClasses.Element_ID, nodeId)
		   .save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		nodeV.addEdge(NdexClasses.Node_E_represents, graph.getVertex(termDoc));
		
		network.setNodeCount(network.getNodeCount()+1);
		
//		nodeDoc.reload();
		elementIdCache.put(nodeId, nodeDoc);
		this.baseTermNodeIdMap.put(bTermId, nodeId);
		return nodeId;
	}
	
	
	/**
     * Find a user based on account name.
     * @param accountName
     * @return ODocument object that hold data for this user account
     * @throws NdexException
     */

	private ODocument findUserByAccountName(String accountName)
			throws NdexException
			{
		if (accountName == null	)
			throw new ValidationException("No accountName was specified.");


		final String query = "select * from " + NdexClasses.Account + 
				  " where accountName = '" + accountName + "'";
				
		List<ODocument> userDocumentList = localConnection
					.query(new OSQLSynchQuery<ODocument>(query));

		if ( ! userDocumentList.isEmpty()) {
				return userDocumentList.get(0);
				
		}
		return null;
	}
	
	
	/**
	 * Find or create a base term from a string, and return its identifier.
	 * @param termString
	 * @return
	 * @throws NdexException
	 * @throws ExecutionException
	 */
	public Long getBaseTermId(String termStringRaw) throws NdexException, ExecutionException {
		
        String termString = termStringRaw;		
		if ( termStringRaw.length() > 8 && termStringRaw.substring(0, 7).equalsIgnoreCase("http://") ) {
	  		  try {
				URI termStringURI = new URI(termStringRaw);
				String fragment = termStringURI.getFragment();
				
			    String prefix;
			    if ( fragment == null ) {
					    String path = termStringURI.getPath();
					    if (path != null && path.indexOf("/") != -1) {
						   fragment = path.substring(path.lastIndexOf('/') + 1);
						   prefix = termStringRaw.substring(0,
								termStringRaw.lastIndexOf('/') + 1);
					    } else
					       throw new NdexException ("Unsupported URI format in term: " + termStringRaw);
			    } else {
					    prefix = termStringURI.getScheme()+":"+termStringURI.getSchemeSpecificPart()+"#";
			    }
	                 
			    Namespace ns = this.URINamespaceMap.get(prefix);
			    if ( ns != null && ns.getPrefix() != null) {
			    	termString =  ns.getPrefix() + ":" + fragment;
			    }
			  } catch (URISyntaxException e) {
				// ignore and move on to next case
			  }
		}		
		
		Long termId = this.baseTermStrMap.get(termString);
		if ( termId != null) {
			return termId;
		}
	    return this.createBaseTerm(termString);	
	}
	
	public Long getCitationId(String title, String idType, String identifier, 
			List<String> contributors) throws NdexException {
		
		RawCitation rCitation = new RawCitation(title, idType, identifier, contributors);
		Long citationId = rawCitationMap.get(rCitation);

		if ( citationId != null ) {
	        return citationId;
		}
		
		// persist the citation object in db.
		citationId = database.getNextId();

		ODocument citationDoc = new ODocument(NdexClasses.Citation)
		  .fields(
				NdexClasses.Element_ID, citationId,
		        NdexClasses.Citation_P_title, title,
		        NdexClasses.Citation_p_idType, idType,
		        NdexClasses.Citation_P_identifier, identifier)
		  .field(NdexClasses.Citation_P_contributors, contributors, OType.EMBEDDEDLIST)
		  .save();
        
		OrientVertex citationV = graph.getVertex(citationDoc);
		OrientVertex networkV = graph.getVertex(getNetworkDoc());
		networkV.addEdge(NdexClasses.Network_E_Citations, citationV);

		elementIdCache.put(citationId, citationDoc);
		rawCitationMap.put(rCitation, citationId);
		return citationId; 
		
	}
	
	public Network getCurrentNetwork() {
		return this.network;
	}
	
	
	// input parameter is a "rawFunctionTerm", which has element_id as -1;
	public Long getFunctionTermId(Long baseTermId, List<Long> termList) throws ExecutionException {
		
		FunctionTerm func = new FunctionTerm();
		func.setFunctionTermId(baseTermId);
			
		for ( Long termId : termList) {
			  func.getParameters().add( termId);
		}		  
	
		Long functionTermId = this.rawFunctionTermFunctionTermIdMap.get(func);
		if ( functionTermId != null) return functionTermId;
		
		functionTermId = database.getNextId(); 
		
	    ODocument fTerm = new ODocument(NdexClasses.FunctionTerm);
	    fTerm = fTerm.field(NdexClasses.Element_ID, functionTermId)
	       .save();
	    
        OrientVertex fTermV = graph.getVertex(fTerm);
        
        ODocument bTermDoc = elementIdCache.get(func.getFunctionTermId()); 
        fTermV.addEdge(NdexClasses.FunctionTerm_E_baseTerm, graph.getVertex(bTermDoc));
        
        for (Long id : func.getParameters()) {
        	ODocument o = elementIdCache.get(id);
        	fTermV.addEdge(NdexClasses.FunctionTerm_E_paramter, graph.getVertex(o));
        }
	    
        elementIdCache.put(functionTermId, fTerm);
        this.rawFunctionTermFunctionTermIdMap.put(func, functionTermId);
        return functionTermId;
	}
	
	/**
	 * Find or create a namespace object from database;
	 * @param rns
	 * @return
	 * @throws NdexException
	 */
	public Namespace getNamespace(RawNamespace rns) throws NdexException {
		if (rns.getPrefix() == null) {
			Namespace ns = URINamespaceMap.get(rns.getURI());
			if ( ns != null ) {
				return ns; 
			}
		}
		
		if (rns.getURI() == null) {
			Namespace ns = this.prefixMap.get(rns.getPrefix()); 
			if ( ns != null ) {
				return ns; 
			}
		}
		
		return findOrCreateNamespace(rns);
	}
	
	private ODocument getNetworkDoc() {
		return networkDoc;
	}
	
	/**
	 * Create or Find a node from a baseTerm.
	 * @param termString
	 * @return
	 * @throws ExecutionException
	 * @throws NdexException
	 */
	public Long getNodeIdByBaseTerm(String termString) throws ExecutionException, NdexException {
		Long id = this.getBaseTermId(termString);
		return this.findOrCreateNodeIdFromBaseTermId(id);
	}


	public Long getNodeIdByFunctionTermId(Long funcTermId) throws ExecutionException {
		Long nodeId = this.functionTermIdNodeIdMap.get(funcTermId) ;
		
		if (nodeId != null) return nodeId;
		
		// otherwise insert Node.
		nodeId = database.getNextId();

		ODocument termDoc = elementIdCache.get(funcTermId); 
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc = nodeDoc.field(NdexClasses.Element_ID, nodeId)
				.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		nodeV.addEdge(NdexClasses.Node_E_represents, graph.getVertex(termDoc));
		
		network.setNodeCount(network.getNodeCount()+1);
		
		elementIdCache.put(nodeId, nodeDoc);
		this.functionTermIdNodeIdMap.put(funcTermId, nodeId);
		return nodeId;
	}
	
	public Long getNodeIdByName(String key) {
		Long nodeId = this.namedNodeMap.get(key);
		
		if ( nodeId !=null ) {
			return nodeId;
		}
		
		// otherwise insert Node.
		nodeId = database.getNextId();

		ODocument nodeDoc = new ODocument(NdexClasses.Node)
		        .fields(NdexClasses.Element_ID, nodeId,
		        		NdexClasses.Node_P_name, key)
				.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		
		network.setNodeCount(network.getNodeCount()+1);
		
		elementIdCache.put(nodeId, nodeDoc);
		this.namedNodeMap.put(key,nodeId);
		return nodeId;
		
	}

	
	public Long getNodeIdByReifiedEdgeTermId(Long reifiedEdgeTermId) throws ExecutionException {
		Long nodeId = this.reifiedEdgeTermIdNodeIdMap.get(reifiedEdgeTermId); 

		if (nodeId != null) 
			return nodeId;
		
		// otherwise insert Node.
		nodeId = database.getNextId();

		ODocument termDoc = elementIdCache.get(reifiedEdgeTermId); 
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc = nodeDoc.field(NdexClasses.Element_ID, nodeId)
		   .save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		nodeV.addEdge(NdexClasses.Node_E_represents, graph.getVertex(termDoc));
		
		network.setNodeCount(network.getNodeCount()+1);
		
		elementIdCache.put(nodeId, nodeDoc);
		this.reifiedEdgeTermIdNodeIdMap.put(reifiedEdgeTermId, nodeId);
		return nodeId;
	}
	
	
	public Long getReifiedEdgeTermIdFromEdgeId(Long edgeId) throws ExecutionException {
		Long reifiedEdgeTermId = this.edgeIdReifiedEdgeTermIdMap.get(edgeId);
				
		if (reifiedEdgeTermId != null) 	return reifiedEdgeTermId;
		
		// create new term
		reifiedEdgeTermId = this.database.getNextId();
		
		ODocument eTermdoc = new ODocument (NdexClasses.ReifiedEdgeTerm);
		eTermdoc = eTermdoc.field(NdexClasses.Element_ID, reifiedEdgeTermId)
				.save();
		
		OrientVertex etV = graph.getVertex(eTermdoc);
		ODocument edgeDoc = elementIdCache.get(edgeId);
		
		OrientVertex edgeV = graph.getVertex(edgeDoc);
		etV.addEdge(NdexClasses.ReifedEdge_E_edge, edgeV);
		networkVertex.addEdge(NdexClasses.Network_E_ReifedEdgeTerms,
				etV);
		
		elementIdCache.put(reifiedEdgeTermId, eTermdoc);
		this.edgeIdReifiedEdgeTermIdMap.put(edgeId, reifiedEdgeTermId);
		return reifiedEdgeTermId;
	}

	
	public NetworkSummary getSummaryOfCurrentNetwork() {
		NetworkSummary summary = new NetworkSummary ();
		
		summary.setCreationDate(network.getCreationDate());
		summary.setDescription(network.getDescription());
		summary.setEdgeCount(network.getEdgeCount());
		summary.setExternalId(network.getExternalId());
		summary.setIsComplete(network.getIsComplete());
		summary.setIsLocked(network.getIsLocked());
		summary.setModificationDate(network.getModificationDate());
		summary.setName(network.getName());
		summary.setNodeCount(network.getNodeCount());
		summary.setVersion(network.getVersion());
		summary.setVisibility(network.getVisibility());
		
		return summary;
	}

	
	public Long getSupportId(String literal, Long citationId) throws ExecutionException {
		
		RawSupport r = new RawSupport(literal, citationId);

		Long supportId = this.rawSupportMap.get(r);

		if ( supportId != null ) return supportId;
		
		// persist the support object in db.
		supportId =database.getNextId() ;

		ODocument supportDoc = new ODocument(NdexClasses.Support)
		   .fields(NdexClasses.Element_ID, supportId,
		           NdexClasses.Support_P_text, literal)	
		   .save();

		ODocument citationDoc = elementIdCache.get(citationId);
        
		OrientVertex supportV = graph.getVertex(supportDoc);
		OrientVertex citationV = graph.getVertex(citationDoc);
		OrientVertex networkV = graph.getVertex(getNetworkDoc());
		supportV.addEdge(NdexClasses.Support_E_citation, citationV);
		networkV.addEdge(NdexClasses.Network_E_Supports, supportV);

		elementIdCache.put(supportId, supportDoc);
		this.rawSupportMap.put(r, supportId);
		return supportId; 
		
	}
	
	public void persistNetwork() {
		try {
			
			network.setIsComplete(true);
			getNetworkDoc().field(NdexClasses.Network_P_isComplete,true)
			  .field(NdexClasses.Network_P_edgeCount, network.getEdgeCount())
			  .field(NdexClasses.Network_P_nodeCount, network.getNodeCount())
			  .save();
			
			
			System.out.println("The new network " + network.getName()
					+ " is complete");
		} catch (Exception e) {
			System.out.println("unexpected error in persist network...");
			e.printStackTrace();
		} finally {
			this.localConnection.commit();
			localConnection.close();
			this.database.close();
			System.out
					.println("Connection to orientdb database has been closed");
		}
	}
	
	public void setNetworkProperties(Collection<NdexProperty> properties, 
			Collection<NdexProperty> presentationProperties) {
		addPropertiesToVertex ( networkVertex, properties, presentationProperties);
	}
	
	public void setNetworkTitleAndDescription(String title, String description) {
	   if ( description != null ) {
		   this.network.setDescription(description);
		   this.networkDoc.field(NdexClasses.Network_P_desc, title).save();
	   }
	   
	   if ( title != null) {
		   this.network.setName(title);
		   this.networkDoc.field(NdexClasses.Network_P_name, title).save();
	   }
	   
	}
	
	public void setNodeName(long nodeId, String name) throws ExecutionException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		
		nodeDoc = nodeDoc.field(NdexClasses.Node_P_name, name).save();
		
		elementIdCache.put(nodeId, nodeDoc);
	}
	
	public void setNodeProperties(Long nodeId, Collection<NdexProperty> properties, 
			Collection<NdexProperty> presentationProperties) throws ExecutionException {
		ODocument nodeDoc = this.elementIdCache.get(nodeId);
		OrientVertex v = graph.getVertex(nodeDoc);
		addPropertiesToVertex ( v, properties, presentationProperties);
	}
	
	/**
	 *  create a represent edge from a node to a term.
	 * @param nodeId
	 * @param TermId
	 * @throws ExecutionException 
	 */
	public void setNodeRepresentTerm(long nodeId, long termId) throws ExecutionException {
		ODocument nodeDoc = this.elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		ODocument termDoc = this.elementIdCache.get(termId);
		OrientVertex termV = graph.getVertex(termDoc);
		
		nodeV.addEdge(NdexClasses.Node_E_represents, termV);
	}
}
