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
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.exceptions.ValidationException;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.object.network.RawCitation;
import org.ndexbio.common.models.object.network.RawEdge;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.models.object.network.RawSupport;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.network.Namespace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.metadata.schema.OType;
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
 // key is the edge id which this term reifed.
    private Map<Long,Long>  edgeIdReifiedEdgeTermIdMap;
	// maps an external node id to new node id created in Ndex.
    private Map<String, Long> externalIdNodeMap; 
	//key is a function term Id, value is the node id which uses 
    // that function as represents term
    private Map<Long,Long> functionTermIdNodeIdMap;

	// maps a node name to Node Id.
    private Map<String, Long> namedNodeMap;

    private Map<RawEdge, Long> edgeMap;
    
	// key is the full URI or other fully qualified baseTerm as a string.
  //	private LoadingCache<String, BaseTerm> baseTermStrCache;

    private ODocument networkDoc;
//	protected OrientVertex networkVertex;
    
    private ODocument ownerDoc;
    

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
    
	public NdexPersistenceService(NdexDatabase db) {
		super(db);

		this.network = null;
		this.ownerDoc = null;
		
		this.rawCitationMap  = new TreeMap <RawCitation, Long> ();
        this.baseTermNodeIdMap = new TreeMap <Long,Long> ();
		this.namedNodeMap  = new TreeMap <String, Long> ();
		this.reifiedEdgeTermIdNodeIdMap = new HashMap<Long,Long>(100);
		this.edgeIdReifiedEdgeTermIdMap = new HashMap<Long,Long>(100);
		this.rawFunctionTermFunctionTermIdMap = new TreeMap<FunctionTerm, Long> ();
		this.rawSupportMap  = new TreeMap<RawSupport, Long> ();
		this.edgeMap = new TreeMap<RawEdge, Long>();
		this.functionTermIdNodeIdMap = new HashMap<Long,Long>(100);
		// intialize caches.
		
		externalIdNodeMap = new TreeMap<String,Long>(); 

	    logger = Logger.getLogger(NdexPersistenceService.class.getName());

	}

	
	public NdexPersistenceService(NdexDatabase db, UUID networkID) throws NdexException  {
		super(db);
		
		this.networkDoc = this.networkDAO.getNetworkDocByUUID(networkID);
		this.networkVertex = graph.getVertex(this.networkDoc);
		this.network = NetworkDAO.getNetworkSummary(networkDoc);
		
		
		this.rawCitationMap  = new TreeMap <RawCitation, Long> ();
        this.baseTermNodeIdMap = new TreeMap <Long,Long> ();
		this.namedNodeMap  = new TreeMap <String, Long> ();
		this.reifiedEdgeTermIdNodeIdMap = new HashMap<Long,Long>(100);
		this.edgeIdReifiedEdgeTermIdMap = new HashMap<Long,Long>(100);
		this.rawFunctionTermFunctionTermIdMap = new TreeMap<FunctionTerm, Long> ();
		this.rawSupportMap  = new TreeMap<RawSupport, Long> ();
		this.functionTermIdNodeIdMap = new HashMap<Long,Long>(100);
		// intialize caches.
		
		externalIdNodeMap = new TreeMap<String,Long>(); 

	    logger = Logger.getLogger(NdexPersistenceService.class.getName());

	}
	
	
	public void abortTransaction() {
		System.out.println(this.getClass().getName()
				+ ".abortTransaction has been invoked.");

		//localConnection.rollback();
		graph.rollback();
		
		// make sure everything relate to the network is deleted.
		//localConnection.begin();
		logger.info("Deleting partial network "+ network.getExternalId().toString() + " in order to rollback in response to error");
		this.networkDAO.deleteNetwork(network.getExternalId().toString());
		//localConnection.commit();
		graph.commit();
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
	    	nodeVertex.addEdge(NdexClasses.Node_E_ciations, citationV);
	    	this.elementIdCache.put(citationId, citationV.getRecord());
	    	
	    }

		if ( annotations != null) {
			for (Map.Entry<String, String> e : annotations.entrySet()) {
                OrientVertex pV = this.createNdexPropertyVertex(new NdexPropertyValuePair(e.getKey(),e.getValue()));
                nodeVertex.addEdge(NdexClasses.E_ndexProperties, pV);
                
                NdexPropertyValuePair p = new NdexPropertyValuePair();
                p.setPredicateString(e.getKey());
                p.setDataType(e.getValue());
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

	// alias is treated as a baseTerm
	public void addRelatedTermToNode(long nodeId, String[] relatedTerms) throws ExecutionException, NdexException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		for (String rT : relatedTerms) {
			Long bID= this.getBaseTermId(rT);
		
			ODocument bDoc = elementIdCache.get(bID);
			OrientVertex bV = graph.getVertex(bDoc);
			nodeV.addEdge(NdexClasses.Node_E_relateTo, bV);
			elementIdCache.put(bID, bV.getRecord());
		}
		
//		nodeV.getRecord().reload();
		elementIdCache.put(nodeId, nodeV.getRecord());
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
		    	OrientVertex citationV = graph.getVertex(citationDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_citations, citationV);
		    	this.elementIdCache.put(citationId, citationV.getRecord());
		    }
		    
		    if ( supportId != null) {
				ODocument supportDoc =elementIdCache.get(supportId);
		    	OrientVertex supportV = graph.getVertex(supportDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_supports, supportV);
		    	this.elementIdCache.put(supportId, supportV.getRecord());
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
		    	
		    	edge.getCitations().add(citation.getId());
		    	this.elementIdCache.put(citation.getId(),citationV.getRecord());
		    }
		    
		    if ( support != null) {
				ODocument supportDoc = this.elementIdCache.get(support.getId());
		    	OrientVertex supportV = this.graph.getVertex(supportDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_supports, supportV);
		    	
		    	edge.getSupports().add(support.getId());
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
    private NetworkSummary createNetwork(String title, String version, UUID uuid) throws NdexException{
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
		  	NdexClasses.Network_P_name, this.network.getName(),
		  	NdexClasses.Network_P_isLocked, this.network.getIsLocked(),
		  	NdexClasses.Network_P_isComplete, this.network.getIsComplete(),
		  	NdexClasses.Network_P_visibility, this.network.getVisibility().toString());

		if ( version != null) {
			this.network.setVersion(version);
			this.networkDoc.field(NdexClasses.Network_P_version, version);
		}
			
		this.networkDoc =this.networkDoc.save();
		
		this.networkVertex = this.graph.getVertex(getNetworkDoc());
		
		OrientVertex ownerV = this.graph.getVertex(this.ownerDoc);
		ownerV.addEdge(NdexClasses.E_admin, this.networkVertex);
		
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
			  func.getParameters().add( termId);
		}		  
	
		Long functionTermId = this.rawFunctionTermFunctionTermIdMap.get(func);
		if ( functionTermId != null) return functionTermId;
		
		functionTermId = createFunctionTerm(baseTermId, termList);
        this.rawFunctionTermFunctionTermIdMap.put(func, functionTermId);
        return functionTermId;
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
		OrientVertex termV = graph.getVertex(termDoc);
		nodeV.addEdge(NdexClasses.Node_E_represents, termV);
		
		network.setNodeCount(network.getNodeCount()+1);
		
		elementIdCache.put(nodeId, nodeV.getRecord());
		elementIdCache.put(funcTermId, termV.getRecord());
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
		etV.addEdge(NdexClasses.ReifedEdge_E_edge, edgeV);
		networkVertex.addEdge(NdexClasses.Network_E_ReifedEdgeTerms,
				etV);
		
		elementIdCache.put(reifiedEdgeTermId, etV.getRecord());
		elementIdCache.put(edgeId,edgeV.getRecord());
		this.edgeIdReifiedEdgeTermIdMap.put(edgeId, reifiedEdgeTermId);
		return reifiedEdgeTermId;
	}

	
	public Long getSupportId(String literal, Long citationId) throws ExecutionException {
		
		RawSupport r = new RawSupport(literal, citationId);

		Long supportId = this.rawSupportMap.get(r);

		if ( supportId != null ) return supportId;
		
		// persist the support object in db.
		supportId = createSupport(literal, citationId);
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
	
	public void setNetworkProperties(Collection<NdexPropertyValuePair> properties, 
			Collection<SimplePropertyValuePair> presentationProperties) throws NdexException, ExecutionException {
		addPropertiesToVertex ( networkVertex, properties, presentationProperties);
	}
	
	public void setNetworkProvenance(ProvenanceEntity e) throws JsonProcessingException {
	
		ObjectMapper mapper = new ObjectMapper();
		String provenanceString = mapper.writeValueAsString(e);
		// store provenance string
		this.networkDoc = this.networkDoc.field(NdexClasses.Network_P_provenance, provenanceString)
				.save();

	}
	
	public void setNetworkTitleAndDescription(String title, String description) {
	   if ( description != null ) {
		   this.network.setDescription(description);
		   this.networkDoc = this.networkDoc.field(NdexClasses.Network_P_desc, description).save();
	   }
	   
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

	public void setElementProperty(Long elementId, String key, String value, String type) throws ExecutionException, NdexException {
		ODocument elementDoc = this.elementIdCache.get(elementId);
		OrientVertex v = graph.getVertex(elementDoc);
		
		NdexPropertyValuePair p = new NdexPropertyValuePair(key,value);
		p.setDataType(type);
		v = this.addPropertyToVertex(v, p);
        elementDoc = v.getRecord();
        this.elementIdCache.put(elementId, elementDoc);
	}
	
	public void setElementPresentationProperty(Long elementId, String key, String value) throws ExecutionException {
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
