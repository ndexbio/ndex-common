package org.ndexbio.common.persistence.orientdb;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.OrientdbDAO;
import org.ndexbio.common.models.object.network.RawCitation;
import org.ndexbio.common.models.object.network.RawEdge;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.models.object.network.RawSupport;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.task.NdexServerQueue;

import com.google.common.base.Preconditions;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;


/*
 * An implementation of the NDExPersistenceService interface that uses a 
 * in-memory cache to provide persistence for new ndex doain objects
 * 
 * 
 * Utilizes a Google Guava cache that uses Jdexids as keys and VertexFrame implementations 
 * as values
 */

public class NdexPersistenceService extends PersistenceService {
	
	public static final String defaultCitationType="URI";
	public static final String pmidPrefix = "pmid:";


	// key is the element_id of a BaseTerm, value is the id of the node which this BaseTerm represents
    private Map<Long, Long> baseTermNodeIdMap;
	// Store the mapping in memory to have better performance.
//	private NdexDatabase database;
 // key is the edge id which this term reified.
    private Map<Long,Long>  edgeIdReifiedEdgeTermIdMap;
	// maps an external node id to new node id created in Ndex.
    private Map<String, Long> externalIdNodeMap; 
	//key is a function term Id, value is the node id which uses 
    // that function as represents term
    private Map<Long,Long> functionTermIdNodeIdMap;

	// maps a node name to Node Id.
    private Map<String, Long> namedNodeMap;

    private Map<RawEdge, Long> edgeMap;
    
    private String ownerAccount;
    
    private Map<RawCitation, Long>           rawCitationMap;
    
    
    private Map<FunctionTerm, Long> rawFunctionTermFunctionTermIdMap; 
    
    private Map<RawSupport, Long>  rawSupportMap;
    
    // key is a "rawFunctionTerm", which has element id as -1. This table
    // matches the key to a functionTerm that has been stored in the db.
    
    private Map<Long, Long> reifiedEdgeTermIdNodeIdMap;
  //  private LoadingCache<Long, Node> reifiedEdgeTermNodeCache;
    
    //key is the name of the node. This cache is for loading simple SIF 
    // for now
//    private LoadingCache<String, Node> namedNodeCache;
    
    
    /*
     * Currently, the procces flow of this class is:
     * 
     * 1. create object 
     * 2. Create New network
     */
    
	public NdexPersistenceService(NdexDatabase db) throws NdexException {
		super(db);

		this.network = null;
		this.ownerAccount = null;
		
		this.rawCitationMap  = new TreeMap <> ();
        this.baseTermNodeIdMap = new TreeMap <> ();
		this.namedNodeMap  = new TreeMap <> ();
		this.reifiedEdgeTermIdNodeIdMap = new HashMap<>(100);
		this.edgeIdReifiedEdgeTermIdMap = new HashMap<>(100);
		this.rawFunctionTermFunctionTermIdMap = new TreeMap<> ();
		this.rawSupportMap  = new TreeMap<> ();
		this.edgeMap = new TreeMap<>();
		this.functionTermIdNodeIdMap = new HashMap<>(100);
		// intialize caches.
		
		externalIdNodeMap = new TreeMap<>(); 

	    logger = Logger.getLogger(NdexPersistenceService.class.getName());

	}

	
	public NdexPersistenceService(NdexDatabase db, UUID networkID) throws NdexException  {
		super(db);
		
		this.networkDoc = this.networkDAO.getNetworkDocByUUID(networkID);
		this.networkVertex = graph.getVertex(this.networkDoc);
		this.network = NetworkDAO.getNetworkSummary(networkDoc);
		
		
		this.rawCitationMap  = new TreeMap <> ();
        this.baseTermNodeIdMap = new TreeMap <> ();
		this.namedNodeMap  = new TreeMap <> ();
		this.reifiedEdgeTermIdNodeIdMap = new HashMap<>(100);
		this.edgeIdReifiedEdgeTermIdMap = new HashMap<>(100);
		this.rawFunctionTermFunctionTermIdMap = new TreeMap<> ();
		this.rawSupportMap  = new TreeMap<> ();
		this.functionTermIdNodeIdMap = new HashMap<>(100);
		// intialize caches.
		
		externalIdNodeMap = new TreeMap<>(); 

	    logger = Logger.getLogger(NdexPersistenceService.class.getName());

	}
	
	
	public void abortTransaction() throws ObjectNotFoundException, NdexException {
		System.out.println(this.getClass().getName()
				+ ".abortTransaction has been invoked.");

		//localConnection.rollback();
//		graph.rollback();
		
		// make sure everything relate to the network is deleted.
		//localConnection.begin();
		logger.info("Deleting partial network "+ network.getExternalId().toString() + " in order to rollback in response to error");
		this.networkDAO.logicalDeleteNetwork(network.getExternalId().toString());
		//localConnection.commit();
		graph.commit();
		Task task = new Task();
		task.setTaskType(TaskType.SYSTEM_DELETE_NETWORK);
		task.setResource(network.getExternalId().toString());
		NdexServerQueue.INSTANCE.addSystemTask(task);
		logger.info("Partial network "+ network.getExternalId().toString() + " is deleted.");
	}
	
	// alias is treated as a baseTerm
	public void addAliasToNode(long nodeId, String[] aliases) throws ExecutionException, NdexException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		for (String alias : aliases) {
			Long b= this.getBaseTermId(alias);
		    Long repNodeId = this.baseTermNodeIdMap.get(b);
			if ( repNodeId != null && repNodeId.equals(nodeId)) {
//		    	logger.warning("Alias '" + alias + "' is also the represented base term of node " + 
//			    nodeId +". Alias ignored.");
		    } else {
		    	ODocument bDoc = elementIdCache.get(b);
		    	OrientVertex bV = graph.getVertex(bDoc);
		    	nodeV.addEdge(NdexClasses.Node_E_alias, bV);
		    	elementIdCache.put(b, bV.getRecord());
		    }
		    
		}
		
		elementIdCache.put(nodeId, nodeV.getRecord());
	}

	
	// alias is treated as a baseTerm
	public void addAliasToNode(long nodeId, long baseTermId) throws ExecutionException, NdexException {
		ODocument nodeDoc = elementIdCache.get(nodeId);

	    Long repNodeId = this.baseTermNodeIdMap.get(baseTermId);
		if ( repNodeId != null && repNodeId.equals(nodeId)) {
	    	logger.info("Base term ID " + baseTermId  + " is also the represented base term of node " + 
		    nodeId +". Alias ignored.");
	    	return;
	    } 
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		ODocument bTermDoc = elementIdCache.get(baseTermId);
		if(!bTermDoc.getClassName().equals(NdexClasses.BaseTerm))
			throw new NdexException ("Element " + baseTermId + " is not an instance of  baseTerm. It is " + 
							bTermDoc.getClassName() );
    	OrientVertex bV = graph.getVertex(bTermDoc);
		nodeV.addEdge(NdexClasses.Node_E_alias, bV);

		elementIdCache.put(nodeId, nodeV.getRecord());
		elementIdCache.put(baseTermId, bV.getRecord());
	}
	
				
	public void addCitationToElement(long elementId, Long citationId, String className) throws ExecutionException, NdexException{
		ODocument elementRec = elementIdCache.get(elementId);
		OrientVertex nodeV = graph.getVertex(elementRec);
		
		ODocument citationRec = elementIdCache.get(citationId);
		OrientVertex citationV = graph.getVertex(citationRec);
		
		if ( className.equals(NdexClasses.Node) ) {
 	       	nodeV.addEdge(NdexClasses.Node_E_citations, graph.getVertex(citationV));
		} else if ( className.equals(NdexClasses.Edge) ) {
			nodeV.addEdge(NdexClasses.Edge_E_citations, graph.getVertex(citationV));
		} else {
			throw new NdexException ("Citation can only be added to node or edges of network, can't added to " + className);
		}
		
		elementIdCache.put(citationId, citationV.getRecord());
		ODocument o = nodeV.getRecord();
//		o.reload();
		elementIdCache.put(elementId, o);
	}
	
	public void addSupportToElement(long elementId, Long supportId, String className) throws ExecutionException, NdexException {
		ODocument elementRec = elementIdCache.get(elementId);
		OrientVertex nodeV = graph.getVertex(elementRec);
		
		ODocument supportRec = elementIdCache.get(supportId);
		OrientVertex supportV = graph.getVertex(supportRec);
		
		if ( className.equals(NdexClasses.Node) ) {
			nodeV.addEdge(NdexClasses.Node_E_supports, graph.getVertex(supportV));
 	       	
		} else if ( className.equals(NdexClasses.Edge) ) {
			nodeV.addEdge(NdexClasses.Edge_E_supports, graph.getVertex(supportV));
		} else {
			throw new NdexException ("Support can only be added to node or edges of network, can't added to " + className);
		}
		
		elementIdCache.put(supportId, supportV.getRecord());
		
		ODocument o = nodeV.getRecord();
		elementIdCache.put(elementId, o);
	}
	
	
	//TODO: generalize this function so that createEdge(....) can use it.
	public void addMetaDataToNode (Long subjectNodeId, Long supportId, Long citationId,  Map<String,String> annotations) 
			throws ExecutionException, NdexException {
        
		ODocument nodeDoc = this.elementIdCache.get(subjectNodeId);
    	OrientVertex nodeVertex = graph.getVertex(nodeDoc);
		        
        if ( supportId != null) {
			ODocument supportDoc = this.elementIdCache.get(supportId);
	    	OrientVertex supportV = graph.getVertex(supportDoc);
	    	nodeVertex.addEdge(NdexClasses.Node_E_supports, supportV);
	    	this.elementIdCache.put(supportId, supportV.getRecord());
        }

	    if (citationId != null) {
			ODocument citationDoc = elementIdCache.get(citationId) ;
	    	OrientVertex citationV = graph.getVertex(citationDoc);
	    	nodeVertex.addEdge(NdexClasses.Node_E_citations, citationV);
	    	this.elementIdCache.put(citationId, citationV.getRecord());
	    	
	    }

		if ( annotations != null) {
			for (Map.Entry<String, String> e : annotations.entrySet()) {
                OrientVertex pV = this.createNdexPropertyVertex(new NdexPropertyValuePair(e.getKey(),e.getValue()));
                nodeVertex.addEdge(NdexClasses.E_ndexProperties, pV);
                
/*                NdexPropertyValuePair p = new NdexPropertyValuePair();
                p.setPredicateString(e.getKey());
                p.setDataType(e.getValue()); */
			}
		}
		
//		nodeDoc.reload();
		this.elementIdCache.put(subjectNodeId, nodeVertex.getRecord());

	}

	
	private OrientVertex addPropertyToVertex(OrientVertex v, NdexPropertyValuePair p) 
			throws ExecutionException, NdexException {

        OrientVertex pV = this.createNdexPropertyVertex(p);
        
       	v.addEdge(NdexClasses.E_ndexProperties, pV);
       	return v;
	}

	// related term is assumed to be a base term
	public void addRelatedTermToNode(long nodeId, String[] relatedTerms) throws ExecutionException, NdexException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		for (String rT : relatedTerms) {
			Long bID= this.getBaseTermId(rT);
		
			addRelatedTermToNode( nodeV, bID);
		}
		
		elementIdCache.put(nodeId, nodeV.getRecord());
	}

	// related term is assumed to be a base term
	public void addRelatedTermToNode(long nodeId, long baseTermId ) throws ExecutionException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		addRelatedTermToNode( nodeV, baseTermId );
		
		elementIdCache.put(nodeId, nodeV.getRecord());
	}
	
	// related term is assumed to be a base term but its not clear that 
	// we actually constrain the type of the related term
	private void addRelatedTermToNode(OrientVertex nodeV, long baseTermId ) throws ExecutionException {
		ODocument bDoc = elementIdCache.get(baseTermId);
		OrientVertex bV = graph.getVertex(bDoc);
		nodeV.addEdge(NdexClasses.Node_E_relateTo, bV);
		elementIdCache.put(baseTermId, bV.getRecord());
	}
	
	
	/**
	 *  Look up in the current context, if an edge with the same subject,predicate and object exists, return that edge,
	 *  otherwise create a new edge and return the id of the new edge.  
	 * @param subjectNodeId
	 * @param objectNodeId
	 * @param predicateId
	 * @param supportId
	 * @param citationId
	 * @param annotation
	 * @return
	 * @throws NdexException
	 * @throws ExecutionException
	 */
	public Long getEdge(Long subjectNodeId, Long objectNodeId, Long predicateId, 
			 Long supportId, Long citationId, Map<String,String> annotation ) throws NdexException, ExecutionException {
		RawEdge rawEdge = new RawEdge(subjectNodeId, predicateId, objectNodeId );
		Long edgeId = this.edgeMap.get(rawEdge);
		if ( edgeId != null) return edgeId;
		
		return createEdge(subjectNodeId, objectNodeId, predicateId, supportId, citationId, annotation);
	}	

	/**
	 *  Create an edge in the database.
	 * @param subjectNodeId
	 * @param objectNodeId
	 * @param predicateId
	 * @param supportId
	 * @param citationId
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
                    OrientVertex pV = this.createNdexPropertyVertex(
                    		new NdexPropertyValuePair(e.getKey(),e.getValue()));
                    edgeVertex.addEdge(NdexClasses.E_ndexProperties, pV);
                    
                    NdexPropertyValuePair p = new NdexPropertyValuePair();
                    p.setPredicateString(e.getKey());
                    p.setDataType(e.getValue());
				}
			
			}

			networkVertex.addEdge(NdexClasses.Network_E_Edges, edgeVertex);
			OrientVertex predicateV = graph.getVertex(predicateDoc);
			edgeVertex.addEdge(NdexClasses.Edge_E_predicate, predicateV);
			OrientVertex objectV = graph.getVertex(objectNodeDoc);
			edgeVertex.addEdge(NdexClasses.Edge_E_object, objectV);
			OrientVertex subjectV = graph.getVertex(subjectNodeDoc);
			subjectV.addEdge(NdexClasses.Edge_E_subject, edgeVertex);

		    network.setEdgeCount(network.getEdgeCount()+1);
		    
		    if (citationId != null) {
				ODocument citationDoc = elementIdCache.get(citationId) ; 
				if (citationDoc == null) 
					throw new NdexException ("Citation Id:" + citationId + " was not found in elementIdCache.");
		    	OrientVertex citationV = graph.getVertex(citationDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_citations, citationV);
	//	    	this.elementIdCache.put(citationId, citationV.getRecord());
		    }
		    
		    if ( supportId != null) {
				ODocument supportDoc =elementIdCache.get(supportId);
		    	OrientVertex supportV = graph.getVertex(supportDoc);
		    	Object e = edgeVertex.addEdge(NdexClasses.Edge_E_supports, supportV);
		    	if ( e == null) 
		    		throw new NdexException("Orient db error. Failed to create edge.");
	//	    	this.elementIdCache.put(supportId, supportV.getRecord());
		    }
		    
		    elementIdCache.put(edgeId, edgeVertex.getRecord());
		    elementIdCache.put(subjectNodeId, subjectV.getRecord() );
		    elementIdCache.put(objectNodeId, objectV.getRecord());
		    elementIdCache.put(predicateId, predicateV.getRecord());
		    edgeMap.put(new RawEdge(subjectNodeId, predicateId, objectNodeId), edgeId);
			return edgeId;
		} 
		throw new NdexException("Null value for one of the parameter when creating Edge.");
	}
	
	public Edge createEdge(Long subjectNodeId, Long objectNodeId, Long predicateId, 
			 Support support, Citation citation, List<NdexPropertyValuePair> properties,
			 List<SimplePropertyValuePair> presentationProps )
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
			OrientVertex edgeVertex = this.graph.getVertex(edgeDoc);
			
			this.addPropertiesToVertex(edgeVertex, properties, presentationProps);
			
			this.networkVertex.addEdge(NdexClasses.Network_E_Edges, edgeVertex);
			OrientVertex predicateV = this.graph.getVertex(predicateDoc);
			edgeVertex.addEdge(NdexClasses.Edge_E_predicate, predicateV);
			OrientVertex objectV = this.graph.getVertex(objectNodeDoc);
			edgeVertex.addEdge(NdexClasses.Edge_E_object, objectV);
			OrientVertex subjectV = this.graph.getVertex(subjectNodeDoc);
			subjectV.addEdge(NdexClasses.Edge_E_subject, edgeVertex);

		    this.network.setEdgeCount(this.network.getEdgeCount()+1);
		    
		    // add citation.
		    if (citation != null) {
				ODocument citationDoc = this.elementIdCache.get(citation.getId());
		    	OrientVertex citationV = this.graph.getVertex(citationDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_citations, citationV);
		    	
		    	edge.getCitationIds().add(citation.getId());
		    	this.elementIdCache.put(citation.getId(),citationV.getRecord());
		    }
		    
		    if ( support != null) {
				ODocument supportDoc = this.elementIdCache.get(support.getId());
		    	OrientVertex supportV = this.graph.getVertex(supportDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_supports, supportV);
		    	
		    	edge.getSupportIds().add(support.getId());
		    	this.elementIdCache.put(support.getId(), supportV.getRecord());
		    	
		    }
		    
//		    edgeDoc.reload();
		    elementIdCache.put(edge.getId(), edgeVertex.getRecord());
		    elementIdCache.put(subjectNodeId, subjectV.getRecord());
		    elementIdCache.put(objectNodeId, objectV.getRecord());
		    elementIdCache.put(predicateId, predicateV.getRecord());
			return edge;
		} 
		throw new NdexException("Null value for one of the parameter when creating Edge.");
	}

	


	public void createNamespace2(String prefix, String URI) throws NdexException {
		RawNamespace r = new RawNamespace(prefix, URI);
		getNamespace(r);
	}
	
/*	
    private ODocument createNdexPropertyDoc(String key, String value) {
		ODocument pDoc = new ODocument(NdexClasses.NdexProperty);
		pDoc.field(NdexClasses.ndexProp_P_predicateStr,key)
		   .field(NdexClasses.ndexProp_P_value, value)
		   .save();
		return pDoc;
		

	}
*/
    private NetworkSummary createNetwork(String title, String version, UUID uuid){
    	logger.info("Creating network with UUID:" + uuid.toString());
		this.network = new NetworkSummary();
		this.network.setExternalId(uuid);
		this.network.setURI(NdexDatabase.getURIPrefix()+ "/network/"+ uuid.toString());
		this.network.setName(title);
		this.network.setVisibility(VisibilityType.PRIVATE);
		this.network.setIsLocked(false);
		this.network.setIsComplete(false);

        
		this.networkDoc = new ODocument (NdexClasses.Network)
		  .fields(NdexClasses.Network_P_UUID,this.network.getExternalId().toString(),
		  	NdexClasses.ExternalObj_cTime, this.network.getCreationTime(),
		  	NdexClasses.ExternalObj_mTime, this.network.getModificationTime(),
		  	NdexClasses.ExternalObj_isDeleted, false,
		  	NdexClasses.Network_P_name, this.network.getName(),
		  	NdexClasses.Network_P_desc, "",
		  	NdexClasses.Network_P_isLocked, this.network.getIsLocked(),
		  	NdexClasses.Network_P_isComplete, this.network.getIsComplete(),
		  	NdexClasses.Network_P_visibility, this.network.getVisibility().toString(),
		  	NdexClasses.Network_P_cacheId, this.network.getReadOnlyCacheId(),
		  	NdexClasses.Network_P_readOnlyCommitId, this.network.getReadOnlyCommitId());

		if ( version != null) {
			this.network.setVersion(version);
			this.networkDoc.field(NdexClasses.Network_P_version, version);
		}
			
		this.networkDoc =this.networkDoc.save();
		
		this.networkVertex = this.graph.getVertex(getNetworkDoc());
		
		return this.network;
	}

	
	/*
	 * public method to allow xbel parsing components to rollback the
	 * transaction and close the database connection if they encounter an error
	 * situation
	 */
/*
	public void createNewNetwork(String ownerName, String networkTitle, String version) throws Exception {
		createNewNetwork(ownerName, networkTitle, version,NdexUUIDFactory.INSTANCE.getNDExUUID() );
	}
*/
	/*
	 * public method to persist INetwork to the orientdb database using cache
	 * contents.
	 */

	public void createNewNetwork(String ownerName, String networkTitle, String version) throws NdexException  {
		Preconditions.checkNotNull(ownerName,"A network owner name is required");
		Preconditions.checkNotNull(networkTitle,"A network title is required");
		

		UUID uuid = NdexUUIDFactory.INSTANCE.getNDExUUID() ;
		// find the network owner in the database
		this.ownerAccount = ownerName;

/*		ownerDoc =  findUserByAccountName(ownerName);
		if( null == ownerDoc){
			String message = "Account " +ownerName +" is not registered in the database"; 
			logger.severe(message);
			throw new NdexException(message);
		} */
				
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
/*	public void deleteNetwork() {
		// TODO Implement deletion of network
		System.out
		.println("deleteNetwork called. Not yet implemented");
		
		
	} */

	
	/**
	 * 
	 * @param id the node id that was assigned by external source. 
	 * @param name the name of the node. If the value is null, no node name will be 
	 *        created in Ndex.
	 * @return
	 */
	public Long findOrCreateNodeIdByExternalId(String id, String name) {
		Long nodeId = this.externalIdNodeMap.get(id);
		if ( nodeId != null) return nodeId;
		
		//create a node for this external id.
		
		nodeId = database.getNextId();

		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc.field(NdexClasses.Element_ID, nodeId);
		if ( name != null) 
			nodeDoc.field(NdexClasses.Node_P_name, name);
		
		nodeDoc = nodeDoc.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		
		network.setNodeCount(network.getNodeCount()+1);
		
		elementIdCache.put(nodeId, nodeV.getRecord());
		
		externalIdNodeMap.put(id, nodeId);
		return nodeId;

	}
	
	public Long getNodeIdByBaseTermId(Long bTermId) throws ExecutionException {
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
		OrientVertex bTermV = graph.getVertex(termDoc);
		nodeV.addEdge(NdexClasses.Node_E_represents, bTermV);
		
		network.setNodeCount(network.getNodeCount()+1);
		
		nodeDoc = nodeV.getRecord();
		elementIdCache.put(nodeId, nodeDoc);
		elementIdCache.put(bTermId, bTermV.getRecord());
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
			throw new NdexException("No accountName was specified.");


		final String query = "select * from " + NdexClasses.Account + 
				  " where accountName = '" + accountName + "'";
				
		List<ODocument> userDocumentList = localConnection
					.query(new OSQLSynchQuery<ODocument>(query));

		if ( ! userDocumentList.isEmpty()) {
				return userDocumentList.get(0);
				
		}
		return null;
	}

	/*
	 @Override
	protected Long createBaseTerm(String localTerm, long nsId) throws ExecutionException {

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
	        elementIdCache.put(termId, btDoc);
			return termId;
	 }

*/
	
	
	public Long getBaseTermId (Namespace namespace, String localTerm) throws NdexException, ExecutionException {
		if ( namespace.getPrefix() != null ) {
			return getBaseTermId(namespace.getPrefix()+":"+localTerm);
		}
		
		return getBaseTermId(namespace.getUri()+localTerm);
	}
	
	
	
	public Long getCitationId(String title, String idType, String identifier, 
			List<String> contributors) throws NdexException, ExecutionException {
		
		RawCitation rCitation = new RawCitation(title, idType, identifier, contributors);
		Long citationId = rawCitationMap.get(rCitation);

		if ( citationId != null ) {
	        return citationId;
		}
		
		// persist the citation object in db.
		citationId = createCitation(title, idType, identifier, contributors, null,null);
				
		rawCitationMap.put(rCitation, citationId);
		return citationId; 
	}
	
	public NetworkSummary getCurrentNetwork() {
		return this.network;
	}
	
	
	// input parameter is a "rawFunctionTerm", which has element_id as -1;
	public Long getFunctionTermId(Long baseTermId, List<Long> termList) throws ExecutionException {
		
		FunctionTerm func = new FunctionTerm();
		func.setFunctionTermId(baseTermId);
			
		for ( Long termId : termList) {
			  func.getParameterIds().add( termId);
		}		  
	
		Long functionTermId = this.rawFunctionTermFunctionTermIdMap.get(func);
		if ( functionTermId != null) return functionTermId;
		
		functionTermId = createFunctionTerm(baseTermId, termList);
        this.rawFunctionTermFunctionTermIdMap.put(func, functionTermId);
        return functionTermId;
	}
	
	
	public ODocument getNetworkDoc() {
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
		return this.getNodeIdByBaseTermId(id);
	}


	public Long getNodeIdByFunctionTermId(Long funcTermId) throws ExecutionException {
		Long nodeId = this.functionTermIdNodeIdMap.get(funcTermId) ;
		
		if (nodeId != null) return nodeId;
		
		// otherwise insert Node.
		nodeId = createNodeFromFunctionTermId(funcTermId);
		
		this.functionTermIdNodeIdMap.put(funcTermId, nodeId);
		return nodeId;
	}
	
	/**
	 * This function doesn't check if a node with same semantic meaning exists. It doesn't register the created 
	 * node in the lookup table either. The only external usage of this function is to create orphan node in XBEL networks 
	 * 
	 * @param funcTermId
	 * @return
	 */
	
	public Long createNodeFromFunctionTermId(Long funcTermId) throws ExecutionException {
		Long nodeId = database.getNextId();

		ODocument termDoc = elementIdCache.get(funcTermId); 
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc = nodeDoc.field(NdexClasses.Element_ID, nodeId)
				.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		OrientVertex termV = graph.getVertex(termDoc);
		nodeV.addEdge(NdexClasses.Node_E_represents, termV);
		
		network.setNodeCount(network.getNodeCount()+1);

		elementIdCache.put(nodeId, nodeV.getRecord());
		
		return nodeId;
		
	}
	
/*
	public Long createNodeFromFunctionTermId(Long funcTermId, Long citationId, Long supportId) throws ExecutionException {
		Long nodeId = database.getNextId();

		ODocument termDoc = elementIdCache.get(funcTermId); 
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc = nodeDoc.field(NdexClasses.Element_ID, nodeId)
				.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		OrientVertex termV = graph.getVertex(termDoc);
		nodeV.addEdge(NdexClasses.Node_E_represents, termV);
		
		network.setNodeCount(network.getNodeCount()+1);

		// adding support
		ODocument supportRec = elementIdCache.get(supportId);
		OrientVertex supportV = graph.getVertex(supportRec);
		
		nodeV.addEdge(NdexClasses.Node_E_supports, graph.getVertex(supportV));
		
		elementIdCache.put(supportId, supportV.getRecord());

		// adding citation
		ODocument citationRec = elementIdCache.get(citationId);
		OrientVertex citationV = graph.getVertex(citationRec);
		
	    nodeV.addEdge(NdexClasses.Node_E_ciations, graph.getVertex(citationV));
		
		elementIdCache.put(citationId, citationV.getRecord());

		
		elementIdCache.put(nodeId, nodeV.getRecord());
		return nodeId;
		
	}
*/	
	
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
		
		elementIdCache.put(nodeId, nodeV.getRecord());
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
		OrientVertex termV = graph.getVertex(termDoc);
		nodeV.addEdge(NdexClasses.Node_E_represents, termV);
		
		network.setNodeCount(network.getNodeCount()+1);
		
		elementIdCache.put(nodeId, nodeV.getRecord());
		elementIdCache.put(reifiedEdgeTermId, termV.getRecord());
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
		etV.addEdge(NdexClasses.ReifiedEdge_E_edge, edgeV);
		networkVertex.addEdge(NdexClasses.Network_E_ReifiedEdgeTerms,
				etV);
		
		elementIdCache.put(reifiedEdgeTermId, etV.getRecord());
		elementIdCache.put(edgeId,edgeV.getRecord());
		this.edgeIdReifiedEdgeTermIdMap.put(edgeId, reifiedEdgeTermId);
		return reifiedEdgeTermId;
	}

	
	public Long getSupportId(String literal, Long citationId) throws ExecutionException {
		
		RawSupport r = new RawSupport(literal, (citationId !=null ? citationId.longValue(): -1));

		Long supportId = this.rawSupportMap.get(r);

		if ( supportId != null ) return supportId;
		
		// persist the support object in db.
		supportId = createSupport(literal, citationId);
		this.rawSupportMap.put(r, supportId);
		return supportId; 
		
	}
	
	public void persistNetwork() throws NdexException {
		try {
			
			network.setIsComplete(true);
			getNetworkDoc().fields(NdexClasses.Network_P_isComplete,true,
					NdexClasses.Network_P_edgeCount, network.getEdgeCount(),
			        NdexClasses.Network_P_nodeCount, network.getNodeCount(),
			        NdexClasses.ExternalObj_mTime, Calendar.getInstance().getTime() )
			  .save();
			
			this.localConnection.commit();
			
			if ( this.ownerAccount != null) {
				networkVertex.reload();
				ODocument ownerDoc =  findUserByAccountName(this.ownerAccount);		
				OrientVertex ownerV = this.graph.getVertex(ownerDoc);
				
		
				for	(int retry = 0;	retry <	OrientdbDAO.maxRetries;	++retry)	{
					try	{
						ownerV.reload();
						ownerV.addEdge(NdexClasses.E_admin, this.networkVertex);
						break;
					} catch(ONeedRetryException	e)	{
						logger.warning("Retry - " + e.getMessage());
						//ownerV.reload();
//						networkVertex.reload();
					}
				}
			
				this.localConnection.commit();
			}

			logger.info("Finished loading network " + network.getName());
		} catch (Exception e) {
			e.printStackTrace();
			String msg = "unexpected error in persist network. Cause: " + e.getMessage();
			logger.severe(msg);
			throw new NdexException (msg);
		} /* finally {
			graph.shutdown();
	//		this.database.close();
			logger.info("Connection to orientdb database closed");
		} */
	}
	
	@Override
	public void close () {
		graph.shutdown();
		logger.info("Connection to orientdb database closed");
	}
	
	public void setNetworkProperties(Collection<NdexPropertyValuePair> properties, 
			Collection<SimplePropertyValuePair> presentationProperties) throws NdexException, ExecutionException {
		addPropertiesToVertex ( networkVertex, properties, presentationProperties);
        
		if ( properties != null )
			this.network.getProperties().addAll(properties);
		if ( presentationProperties != null ) 
			this.network.getPresentationProperties().addAll(presentationProperties);

	}
	

	
	public void setNetworkVisibility(VisibilityType visibility) {

		this.networkDoc = this.networkDoc.field(NdexClasses.Network_P_visibility, visibility)
				.save();

	}
	
	public void setNetworkTitleAndDescription(String title, String description) {

	   this.network.setDescription( description != null ? description: "");
	   this.networkDoc = this.networkDoc.field(NdexClasses.Network_P_desc, this.network.getDescription()).save();
	   
	   if ( title != null) {
		   this.network.setName(title);
		   this.networkDoc = this.networkDoc.field(NdexClasses.Network_P_name, title).save();
	   }
	   
	}
	
	public void setNodeName(long nodeId, String name) throws ExecutionException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		
		nodeDoc = nodeDoc.field(NdexClasses.Node_P_name, name).save();
		
		elementIdCache.put(nodeId, nodeDoc);
	}

	public void addElementProperty(Long elementId, String key, String value, String type) throws ExecutionException, NdexException {
		ODocument elementDoc = this.elementIdCache.get(elementId);
		OrientVertex v = graph.getVertex(elementDoc);
		
		NdexPropertyValuePair p = new NdexPropertyValuePair(key,value);
		p.setDataType(type);
		v = this.addPropertyToVertex(v, p);
        elementDoc = v.getRecord();
        this.elementIdCache.put(elementId, elementDoc);
	}
	
	public void addElementPresentationProperty(Long elementId, String key, String value) throws ExecutionException {
		ODocument elementDoc = this.elementIdCache.get(elementId);
		OrientVertex v = graph.getVertex(elementDoc);
		
		ODocument pDoc = this.createSimplePropertyDoc(key,value);
		pDoc = pDoc.save();
        OrientVertex pV = graph.getVertex(pDoc);
        v.addEdge(NdexClasses.E_ndexPresentationProps, pV);
        elementDoc = v.getRecord();
        this.elementIdCache.put(elementId, elementDoc);
	}
	
	public void setNodeProperties(Long nodeId, Collection<NdexPropertyValuePair> properties, 
			Collection<SimplePropertyValuePair> presentationProperties) throws ExecutionException, NdexException {
		ODocument nodeDoc = this.elementIdCache.get(nodeId);
		OrientVertex v = graph.getVertex(nodeDoc);
		addPropertiesToVertex ( v, properties, presentationProperties);
		this.elementIdCache.put(nodeId, v.getRecord());
	}
	
	public void setCitationProperties(Long citationId, Collection<NdexPropertyValuePair> properties, 
			Collection<SimplePropertyValuePair> presentationProperties) throws ExecutionException, NdexException {
		ODocument nodeDoc = this.elementIdCache.get(citationId);
		OrientVertex v = graph.getVertex(nodeDoc);
		addPropertiesToVertex ( v, properties, presentationProperties);
		this.elementIdCache.put(citationId, v.getRecord());
	}
	
	/**
	 *  create a represent edge from a node to a term.
	 * @param nodeId
	 * @param termId
	 * @throws ExecutionException 
	 */
	public void setNodeRepresentTerm(long nodeId, long termId) throws ExecutionException {
		ODocument nodeDoc = this.elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		ODocument termDoc = this.elementIdCache.get(termId);
		OrientVertex termV = graph.getVertex(termDoc);
		
		nodeV.addEdge(NdexClasses.Node_E_represents, termV);
		
		this.elementIdCache.put(nodeId, nodeV.getRecord());
		this.elementIdCache.put(termId, termV.getRecord());
	}
	
	public void updateNetworkSummary() throws ObjectNotFoundException, NdexException, ExecutionException {
	   networkDoc = Helper.updateNetworkProfile(networkDoc, network);
	   addPropertiesToVertex(this.networkVertex, 
			   network.getProperties(),network.getPresentationProperties());
	   
	   networkDoc = this.networkVertex.getRecord();
	}
	
	
	
}
