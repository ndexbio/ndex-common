package org.ndexbio.common.persistence.orientdb;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Support;

import com.google.common.base.Preconditions;
import com.google.common.cache.LoadingCache;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class NdexNetworkCloneService extends PersistenceService {

	private static final Logger logger = Logger.getLogger(NdexNetworkCloneService.class.getName());

	private Network   srcNetwork;

	private NetworkSummary networkSummary;

	// key is the full URI or other fully qualified baseTerm as a string.
  //	private LoadingCache<String, BaseTerm> baseTermStrCache;


	private ODocument networkDoc;
	
    private ODocument ownerDoc;
    
    // all these mapping are for mapping from source Id to Ids in the newly persisted graph.
    private Map<Long, Long>  baseTermIdMap;
    private Map<Long, Long>  reifiedEdgeTermIdMap;
    private Map<Long, Long>  nodeIdMap; 
    private Map<Long, Long>  functionTermIdMap;
    private Map<Long, Long>  namespaceIdMap;
    private Map<Long, Long>  citationIdMap;
    private Map<Long, Long>  supportIdMap;
    private Map<Long, Long>  edgeIdMap;
    
    /*
     * Currently, the procces flow of this class is:
     * 
     * 1. create object 
     * 2. Create New network
     */
    
	public NdexNetworkCloneService(NdexDatabase db, Network sourceNetwork, String ownerAccountName)
			throws ObjectNotFoundException, NdexException {
        super(db);
		
		Preconditions.checkNotNull(sourceNetwork.getName(),"A network title is required");
		
		this.srcNetwork = sourceNetwork;
		this.networkSummary = new NetworkSummary();

		// find the network owner in the database
		UserDAO userdao = new UserDAO(localConnection, graph);
		ownerDoc = userdao.getRecordByAccountName(ownerAccountName, null) ;
		
		this.baseTermIdMap    = new HashMap <Long, Long>(1000);
		this.namespaceIdMap   = new HashMap <Long, Long>(1000);
		this.citationIdMap    = new HashMap <Long, Long> (1000);
		this.reifiedEdgeTermIdMap = new HashMap<Long,Long>(1000);
		this.nodeIdMap      = new HashMap<Long,Long>(1000);
		this.edgeIdMap      = new HashMap<Long, Long> ();
		this.supportIdMap   = new HashMap<Long, Long> (1000);
		this.functionTermIdMap = new HashMap<Long,Long>(1000);
		// intialize caches.

	}


	public NetworkSummary cloneNetwork() throws NdexException, ExecutionException {
		try {
			// need to keep this order because of the dependency between objects.
			cloneNetworkNode ();
			cloneNamespaces ();
			cloneBaseTerms ();
			cloneCitations();
			cloneSupports();
			cloneReifiedEdgeTermNodes(); // only clone the vertex itself.
			cloneFunctionTermVertex();
            cloneNodes(); 			
            cloneEdges();
            
			// process reifiedEdgeTerm and FunctionTerm
            createLinksforRefiedEdgeTerm();
            createLinksFunctionTerm();
		
			networkSummary.setIsLocked(false);
			networkSummary.setIsComplete(true);
		
			networkDoc.field(NdexClasses.Network_P_isComplete,true)
				.save();

			logger.info("The new network " + networkSummary.getName() + " is complete.");
			return this.networkSummary;
		} finally {
			this.localConnection.commit();
	//		localConnection.close();
	//		database.close();
		}
	}
	
	private void cloneNetworkNode()  {

		this.networkSummary.setExternalId( NdexUUIDFactory.INSTANCE.getNDExUUID());		
		this.networkSummary.setName(srcNetwork.getName());
		this.networkSummary.setEdgeCount(srcNetwork.getEdges().size());
		this.networkSummary.setNodeCount(srcNetwork.getNodes().size());

		networkDoc = new ODocument (NdexClasses.Network)
		  .fields(NdexClasses.Network_P_UUID,this.networkSummary.getExternalId().toString(),
		          NdexClasses.Network_P_cDate, networkSummary.getCreationDate(),
		          NdexClasses.Network_P_mDate, networkSummary.getModificationDate(),
		          NdexClasses.Network_P_name, srcNetwork.getName(),
		          NdexClasses.Network_P_edgeCount, networkSummary.getEdgeCount(),
		          NdexClasses.Network_P_nodeCount, networkSummary.getNodeCount(),
		          NdexClasses.Network_P_isLocked, false,
		          NdexClasses.Network_P_isComplete, false,
		          NdexClasses.Network_P_visibility, srcNetwork.getVisibility().toString());
		
		if ( srcNetwork.getDescription() != null) {
			networkDoc.field(NdexClasses.Network_P_desc,srcNetwork.getDescription());
			networkSummary.setDescription(srcNetwork.getDescription());
		}

		if ( srcNetwork.getVersion() != null) {
			networkDoc.field(NdexClasses.Network_P_version,srcNetwork.getVersion());
			networkSummary.setDescription(srcNetwork.getVersion());
		}
		
		networkDoc = networkDoc.save();
		
		networkVertex = graph.getVertex(networkDoc);
		
		addPropertiesToVertex(networkVertex, srcNetwork.getProperties(), srcNetwork.getPresentationProperties());
		
		OrientVertex ownerV = graph.getVertex(ownerDoc);
		ownerV.addEdge(NdexClasses.E_admin, networkVertex);
		
		logger.info("A new NDex network titled: " +srcNetwork.getName() +" has been created");
	}

	
	private void cloneNamespaces() throws NdexException {
		TreeSet<String> prefixSet = new TreeSet<String>();

		if ( srcNetwork.getNamespaces() != null) {
			for ( Namespace ns : srcNetwork.getNamespaces().values() ) {
				if ( ns.getPrefix() !=null && prefixSet.contains(ns.getPrefix()))
					throw new NdexException("Duplicated Prefix " + ns.getPrefix() + " found." );
				Long nsId = createNamespace(ns.getPrefix(), ns.getUri());
				this.namespaceIdMap.put(ns.getId(), nsId);
			}
		}
	}
	
	private void cloneBaseTerms() throws ExecutionException, NdexException {
		if ( srcNetwork.getBaseTerms()!= null) {
			for ( BaseTerm term : srcNetwork.getBaseTerms().values() ) {
				Long nsId = (long)-1 ;
				if ( term.getNamespace() >0 ) {
					nsId = namespaceIdMap.get(term.getNamespace());
					if ( nsId == null)  
						throw new NdexException ("Namespece Id " + term.getNamespace() + " is not found in name space list.");
				}
				Long baseTermId = createBaseTerm(term.getName(), nsId);
				this.baseTermIdMap.put(term.getId(), baseTermId);
			}
		}
	}

	private void cloneCitations() {
		if ( srcNetwork.getCitations()!= null) {
			for ( Citation citation : srcNetwork.getCitations().values() ) {
				Long citationId = this.createCitation(citation.getTitle(),
						citation.getIdType(), citation.getIdentifier(), 
						citation.getContributors(), citation.getProperties(), citation.getPresentationProperties());
				
				this.citationIdMap.put(citation.getId(), citationId);
			}
		}
	}

	private void cloneSupports() throws NdexException, ExecutionException {
		if ( srcNetwork.getSupports()!= null) {
			for ( Support support : srcNetwork.getSupports().values() ) {
				Long citationId = citationIdMap.get(support.getCitation());
				if ( citationId == null )
					throw new NdexException ("Citation Id " + support.getCitation() + " is not found in citation list.");
				Long supportId = createSupport(support.getText(),citationId);
				this.supportIdMap.put(support.getId(), supportId);
			}
		}
	}

	// we only clone the nodes itself. We added the edges in the second rournd
	private void cloneReifiedEdgeTermNodes() {
		if ( srcNetwork.getReifiedEdgeTerms()!= null) {
			for ( ReifiedEdgeTerm reifiedTerm : srcNetwork.getReifiedEdgeTerms().values() ) {
				Long reifiedEdgeTermId = this.database.getNextId();
				
				ODocument eTermdoc = new ODocument (NdexClasses.ReifiedEdgeTerm);
				eTermdoc = eTermdoc.field(NdexClasses.Element_ID, reifiedEdgeTermId)
						.save();

				elementIdCache.put(reifiedEdgeTermId, eTermdoc);
				this.reifiedEdgeTermIdMap.put(reifiedTerm.getId(), reifiedEdgeTermId);
			}
		}
	}


	private void cloneFunctionTermVertex() {
		if ( srcNetwork.getFunctionTerms()!= null) {
			for ( FunctionTerm functionTerm : srcNetwork.getFunctionTerms().values() ) {
				Long newFunctionTermId = this.database.getNextId();
				
				ODocument eTermdoc = new ODocument (NdexClasses.FunctionTerm)
				        .field(NdexClasses.Element_ID, newFunctionTermId)
						.save();

				elementIdCache.put(newFunctionTermId, eTermdoc);
				this.functionTermIdMap.put(functionTerm.getId(), newFunctionTermId);
					
			}
		}
	}

	private void cloneNodes() throws NdexException, ExecutionException {
		if ( srcNetwork.getNodes()!= null) {
			for ( Node node : srcNetwork.getNodes().values() ) {
				Long newNodeId = createNode(node);
				this.nodeIdMap.put(node.getId(), newNodeId);
			}
		}
	}

	private Long createNode (Node node) throws NdexException, ExecutionException {
		Long nodeId = database.getNextId();
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node)
		   .field(NdexClasses.Element_ID, nodeId);
		
		if ( node.getName()!= null) {
			nodeDoc = nodeDoc.field(NdexClasses.Node_P_name,node.getName());
		}
		nodeDoc= nodeDoc.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		if ( node.getRepresents() != null ) {
		   Long newRepId = null;	
		   String repType = node.getRepresentsTermType();
		   if ( repType.equals(NdexClasses.BaseTerm)) {
			 newRepId = baseTermIdMap.get(node.getRepresents()); 
		   } else if (repType.equals(NdexClasses.FunctionTerm)) {
			 newRepId = functionTermIdMap.get(node.getRepresents());  
		   } else 
			   newRepId = reifiedEdgeTermIdMap.get(node.getRepresents());
		   
		   if ( newRepId == null)
			   throw new NdexException ("Term id " + node.getRepresents() + "not found.");
		   
   		   ODocument termDoc = elementIdCache.get(newRepId); 
     	   nodeV.addEdge(NdexClasses.Node_E_represents, graph.getVertex(termDoc));
		   
		}
		
		if ( node.getAliases() != null) {
			for ( Long aliasId : node.getAliases()) {
				Long newAliasId = baseTermIdMap.get(aliasId);
				if ( newAliasId == null)
					throw new NdexException ("Base term id " + aliasId + " not found.");

				ODocument termDoc = elementIdCache.get(newAliasId); 
				nodeV.addEdge(NdexClasses.Node_E_alias,	graph.getVertex(termDoc));
			}
		}
		
		if ( node.getRelatedTerms() != null) {
			for ( Long relateToId : node.getRelatedTerms()) {
				Long newRelateToId = baseTermIdMap.get(relateToId);
				if ( newRelateToId == null)
					throw new NdexException ("Base term id " + relateToId + " not found.");

				ODocument termDoc = elementIdCache.get(newRelateToId); 
				nodeV.addEdge(NdexClasses.Node_E_relateTo,	graph.getVertex(termDoc));
			}
		}
		
		if ( node.getCitations() != null) {
			for ( Long citationId : node.getCitations()) {
				Long newCitationId = citationIdMap.get(citationId);
				if ( newCitationId == null)
					throw new NdexException ("Citation id " + citationId + " not found.");

				ODocument citationDoc = elementIdCache.get(newCitationId); 
				nodeV.addEdge(NdexClasses.Node_E_ciations,	graph.getVertex(citationDoc));
			}
		}
		
		if ( node.getSupports() != null) {
			for ( Long supportId : node.getSupports()) {
				Long newSupportId = supportIdMap.get(supportId);
				if ( newSupportId == null)
					throw new NdexException ("Support id " + supportId + " not found.");

				ODocument supportDoc = elementIdCache.get(newSupportId); 
				nodeV.addEdge(NdexClasses.Node_E_supports,	graph.getVertex(supportDoc));
			}
			
		}
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		
		this.addPropertiesToVertex(nodeV, node.getProperties(), node.getPresentationProperties());
		elementIdCache.put(nodeId, nodeDoc);
        return nodeId;		
	}
	
	private void cloneEdges() throws NdexException, ExecutionException {
		if ( srcNetwork.getEdges() != null) {
			for ( Edge edge : srcNetwork.getEdges().values()) {
				Long newEdgeId = createEdge(edge);
				edgeIdMap.put(edge.getId(), newEdgeId);
			}
		}
	}
	
	
	private Long createEdge(Edge edge) throws NdexException, ExecutionException {
		
		Long edgeId = database.getNextId();
		
		ODocument edgeDoc = new ODocument(NdexClasses.Edge)
		   .field(NdexClasses.Element_ID, edgeId)
           .save();
		
		OrientVertex edgeV = graph.getVertex(edgeDoc);
		
		{
        Long newSubjectId = nodeIdMap.get(edge.getSubjectId());
        if ( newSubjectId == null)
        	   throw new NdexException ("Node id " + edge.getSubjectId() + "not found.");
	   ODocument subjectDoc = elementIdCache.get(newSubjectId); 
	   graph.getVertex(subjectDoc).addEdge(NdexClasses.Edge_E_subject, edgeV);
		}
	   {
	   Long newObjectId = nodeIdMap.get(edge.getObjectId());
       if ( newObjectId == null)
    	   throw new NdexException ("Node id " + edge.getObjectId() + "not found.");
       ODocument objectDoc = elementIdCache.get(newObjectId); 
       edgeV.addEdge(NdexClasses.Edge_E_object, graph.getVertex(objectDoc));
	   }
	   
	   Long newPredicateId = baseTermIdMap.get(edge.getPredicateId());
 	   if ( newPredicateId == null)
			throw new NdexException ("Base term id " + edge.getPredicateId() + " not found.");
	   ODocument termDoc = elementIdCache.get(newPredicateId); 
       edgeV.addEdge(NdexClasses.Edge_E_predicate, graph.getVertex(termDoc));
	   
		if ( edge.getCitations() != null) {
			for ( Long citationId : edge.getCitations()) {
				Long newCitationId = citationIdMap.get(citationId);
				if ( newCitationId == null)
					throw new NdexException ("Citation id " + citationId + " not found.");

				ODocument citationDoc = elementIdCache.get(newCitationId); 
				edgeV.addEdge(NdexClasses.Edge_E_citations,	graph.getVertex(citationDoc));
			}
		}
		
		if ( edge.getSupports() != null) {
			for ( Long supportId : edge.getSupports()) {
				Long newSupportId = supportIdMap.get(supportId);
				if ( newSupportId == null)
					throw new NdexException ("Support id " + supportId + " not found.");

				ODocument supportDoc = elementIdCache.get(newSupportId); 
				edgeV.addEdge(NdexClasses.Edge_E_supports,	graph.getVertex(supportDoc));
			}
			
		}
		
		networkVertex.addEdge(NdexClasses.Network_E_Edges,edgeV);
		this.addPropertiesToVertex(edgeV, edge.getProperties(), edge.getPresentationProperties());
		elementIdCache.put(edgeId, edgeDoc);
        return edgeId;		
		
	}
	
	
	private void createLinksforRefiedEdgeTerm() throws NdexException, ExecutionException {
		if ( srcNetwork.getReifiedEdgeTerms()!= null) {
			for ( ReifiedEdgeTerm reifiedTerm : srcNetwork.getReifiedEdgeTerms().values() ) {
				Long newEdgeId = edgeIdMap.get(reifiedTerm.getEdgeId());
				if ( newEdgeId == null) 
					throw new NdexException ("Edge Id " + reifiedTerm.getEdgeId() + " not found in the system.");
				
				ODocument edgeDoc = elementIdCache.get(newEdgeId); 
				ODocument reifiedEdgeTermDoc = elementIdCache.get(reifiedTerm.getId());
				graph.getVertex(reifiedEdgeTermDoc).addEdge(
						NdexClasses.ReifedEdge_E_edge, graph.getVertex(edgeDoc));
			}
		}
	}

	private void createLinksFunctionTerm() throws NdexException, ExecutionException {
		if ( srcNetwork.getFunctionTerms()!= null) {
			for ( FunctionTerm functionTerm : srcNetwork.getFunctionTerms().values() ) {
				Long newFunctionId =findTermId(functionTerm.getFunctionTermId());
				if ( newFunctionId == null )
					throw new NdexException ("Term Id " + functionTerm.getFunctionTermId() + " is not found in Term list.");
				ODocument functionTermDoc = elementIdCache.get(functionTerm.getId());
				OrientVertex functionTermV = graph.getVertex(functionTermDoc);
				
				ODocument newFunctionNameDoc = elementIdCache.get(newFunctionId);
				functionTermV.addEdge(NdexClasses.FunctionTerm_E_baseTerm, graph.getVertex(newFunctionNameDoc));
				
				for ( Long argId : functionTerm.getParameters()) {
					Long newId = findTermId(argId);
					if ( newId == null)
						throw new NdexException ("Term Id " + argId + " is not found in any term list.");
				    ODocument argumentDoc = elementIdCache.get(newId);
				    functionTermV.addEdge(NdexClasses.FunctionTerm_E_paramter, graph.getVertex(argumentDoc));
				}
			}
		}
	}
	
	
	/*
	
	private void cloneFunctionTerms() throws NdexException, ExecutionException {
		if ( srcNetwork.getFunctionTerms()!= null) {
			for ( FunctionTerm functionTerm : srcNetwork.getFunctionTerms().values() ) {
			}
		}
	}
*/
	
    /**
     * Find the matching term ID from an old term Id. This function is only used for cloning function parameters.
     * @param oldId original id in the function parameter list.
     * @return new id 
     */
	private Long findTermId (Long oldId) {
		Long id = baseTermIdMap.get(oldId);
		if ( id != null) return id;
		id = functionTermIdMap.get(oldId);
		if ( id != null ) return id;
		return reifiedEdgeTermIdMap.get(oldId);
	}
}