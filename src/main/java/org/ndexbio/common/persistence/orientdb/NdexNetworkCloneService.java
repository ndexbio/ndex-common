/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.common.persistence.orientdb;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.common.util.TermUtilities;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSourceFormat;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.task.NdexServerQueue;

import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NdexNetworkCloneService extends PersistenceService {

	private Network   srcNetwork;

//	private NetworkSummary networkSummary;

	// key is the full URI or other fully qualified baseTerm as a string.
  //	private LoadingCache<String, BaseTerm> baseTermStrCache;


    private String ownerAccount;
    
    // all these mapping are for mapping from source Id to Ids in the newly persisted graph.
    private Map<Long, Long>  baseTermIdMap;
    private Map<Long, Long>  reifiedEdgeTermIdMap;
    private Map<Long, Long>  nodeIdMap; 
    private Map<Long, Long>  functionTermIdMap;
    private Map<Long, Long>  namespaceIdMap;
    private Map<Long, Long>  citationIdMap;
    private Map<Long, Long>  supportIdMap;
    private Map<Long, Long>  edgeIdMap;
    
	static Logger logger = LoggerFactory.getLogger(NdexNetworkCloneService.class);
    
    /*
     * Currently, the procces flow of this class is:
     * 
     * 1. create object 
     * 2. Create New network
     */
    
	public NdexNetworkCloneService(NdexDatabase db, final Network sourceNetwork, String ownerAccountName) throws NdexException {
        super(db);
		
		Preconditions.checkNotNull(sourceNetwork.getName(),"A network title is required");
		
		this.srcNetwork = sourceNetwork;
		this.network = new NetworkSummary();

		this.ownerAccount = ownerAccountName;
		this.baseTermIdMap    = new HashMap <>(sourceNetwork.getBaseTerms().size());
		this.namespaceIdMap   = new HashMap <>(sourceNetwork.getNamespaces().size());
		this.citationIdMap    = new HashMap <> (sourceNetwork.getCitations().size());
		this.reifiedEdgeTermIdMap = new HashMap<>(sourceNetwork.getReifiedEdgeTerms().size());
		this.nodeIdMap      = new HashMap<>(sourceNetwork.getNodeCount());
		this.edgeIdMap      = new HashMap<> (sourceNetwork.getEdgeCount());
		this.supportIdMap   = new HashMap<> (sourceNetwork.getSupports().size());
		this.functionTermIdMap = new HashMap<>(sourceNetwork.getFunctionTerms().size());
		// intialize caches.
	    //logger = Logger.getLogger(NdexPersistenceService.class.getName());
		
		network.setOwner(ownerAccountName);

	}


	/**
	 * 
	 * @return
	 * @throws NdexException
	 * @throws ExecutionException
	 */
	public NetworkSummary updateNetwork() throws NdexException, ExecutionException {
		try {
			
			// create new network and set the isComplete flag to false 
			cloneNetworkCore();
			
			this.network.setExternalId(this.srcNetwork.getExternalId());
			
			// get the old network head node
			ODocument srcNetworkDoc = networkDAO.getNetworkDocByUUID(this.srcNetwork.getExternalId());
			if (srcNetworkDoc == null)
				throw new NdexException("Network with UUID " + this.srcNetwork.getExternalId()
						+ " is not found in this server");
			
			// copy the permission from source to target.
			copyNetworkPermissions(srcNetworkDoc, networkVertex);
			
			this.localConnection.commit();
			
			//move the UUID from old network to new network, set new one's isComplete and set the old one to isDeleted.
			
			localConnection.begin();
			UUID newUUID = NdexUUIDFactory.INSTANCE.createNewNDExUUID();

			srcNetworkDoc.fields(NdexClasses.ExternalObj_ID, newUUID.toString(),
					  NdexClasses.ExternalObj_isDeleted,true).save();
			
			this.networkDoc.reload();
			// copy the creationTime and visibility
			networkDoc.fields(NdexClasses.ExternalObj_ID, this.srcNetwork.getExternalId(),
					NdexClasses.ExternalObj_cTime, srcNetworkDoc.field(NdexClasses.ExternalObj_cTime),
					NdexClasses.Network_P_visibility, srcNetworkDoc.field(NdexClasses.Network_P_visibility),
					NdexClasses.Network_P_isLocked,false,
					NdexClasses.ExternalObj_mTime, new Date() ,
					          NdexClasses.Network_P_isComplete,true)
			.save();
			localConnection.commit();
			
			
			// added a delete old network task.
			Task task = new Task();
			task.setTaskType(TaskType.SYSTEM_DELETE_NETWORK);
			task.setResource(newUUID.toString());
			NdexServerQueue.INSTANCE.addSystemTask(task);
			
			this.network.setIsLocked(false);
			return this.network;
		} finally {
			this.localConnection.commit();

		}
	}
	
	
	private void copyNetworkPermissions(ODocument srcNetworkDoc, OrientVertex targetNetworkVertex) {
		
		copyNetworkPermissionAux(srcNetworkDoc, targetNetworkVertex, NdexClasses.E_admin);
		copyNetworkPermissionAux(srcNetworkDoc, targetNetworkVertex, NdexClasses.account_E_canEdit);
		copyNetworkPermissionAux(srcNetworkDoc, targetNetworkVertex, NdexClasses.account_E_canRead);

	}
	
	private void copyNetworkPermissionAux(ODocument srcNetworkDoc, OrientVertex targetNetworkVertex, String permissionEdgeType) {
		
		for ( ODocument rec : Helper.getDocumentLinks(srcNetworkDoc, "in_", permissionEdgeType)) {
			OrientVertex userV = graph.getVertex(rec);
			targetNetworkVertex.reload();
			userV.addEdge(permissionEdgeType, targetNetworkVertex);
		}
		
	}
	
	private void cloneNetworkElements() throws NdexException, ExecutionException {
		try {
			logger.info("[start: cloning network; name='{}']", network.getName());
			// need to keep this order because of the dependency between objects.
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
		
			network.setIsLocked(false);
			network.setIsComplete(true);
		

			logger.info("Cloning network " + network.getName() + " is complete.");
		} finally {
			this.localConnection.commit();
			logger.info("[end: cloned network name='{}']", network.getName());
		}
		
	}

	public NetworkSummary cloneNetwork() throws NdexException, ExecutionException {
		try {
			logger.info("[start: cloning network]");
			cloneNetworkCore();
			
			networkDoc.field(NdexClasses.Network_P_isComplete,true)
			.field(NdexClasses.Network_P_owner, this.ownerAccount)
			.save();

			// find the network owner in the database
			UserDAO userdao = new UserDAO(localConnection, graph);
			ODocument ownerDoc = userdao.getRecordByAccountName(this.ownerAccount, null) ;
			OrientVertex ownerV = graph.getVertex(ownerDoc);
			ownerV.addEdge(NdexClasses.E_admin, networkVertex);
			this.localConnection.commit();
	
			return this.network;
		} catch (Exception e ) {
			this.abortTransaction();
			throw e;
		}finally {
			logger.info("[end: network name='{}' has been saved; UUID='{}']", network.getName(), network.getExternalId());
		}
	}
	
	
	/**
	 * clone the network without the permission links and isComplete flag.
	 * @throws NdexException
	 * @throws ExecutionException
	 */
	private void cloneNetworkCore() throws NdexException, ExecutionException {
			cloneNetworkNode ();

			cloneNetworkElements();
			
	//		cloneNetworkProperties();
			this.localConnection.commit();
	}
	
	
	private void cloneNetworkNode()  {

		this.network.setExternalId( NdexUUIDFactory.INSTANCE.createNewNDExUUID());	
		
//		logger.info("using network prefix: " + NdexDatabase.getURIPrefix() );
//		logger.info("Configuration is HostURI=" + Configuration.getInstance().getProperty("HostURI") );
		
		this.network.setURI(NdexDatabase.getURIPrefix() + "/network/"
					+ this.network.getExternalId().toString());
		this.network.setName(srcNetwork.getName());
		this.network.setEdgeCount(srcNetwork.getEdges().size());
		this.network.setNodeCount(srcNetwork.getNodes().size());

        Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
		networkDoc = new ODocument (NdexClasses.Network)
		  .fields(NdexClasses.Network_P_UUID,this.network.getExternalId().toString(),
		          NdexClasses.ExternalObj_cTime, now,
		          NdexClasses.ExternalObj_mTime, now,
		          NdexClasses.ExternalObj_isDeleted, false,
		          NdexClasses.Network_P_name, srcNetwork.getName(),
		          NdexClasses.Network_P_edgeCount, network.getEdgeCount(),
		          NdexClasses.Network_P_nodeCount, network.getNodeCount(),
		          NdexClasses.Network_P_isLocked, false,
		          NdexClasses.Network_P_isComplete, false,
		          NdexClasses.Network_P_cacheId, Long.valueOf(-1),
		          NdexClasses.Network_P_readOnlyCommitId, Long.valueOf(-1),
		          NdexClasses.ndexProperties, network.getProperties(),
		          NdexClasses.Network_P_visibility,
		          ( srcNetwork.getVisibility() == null ? 
		        		  VisibilityType.PRIVATE.toString()  : 
		        		  srcNetwork.getVisibility().toString()));
		
		if ( srcNetwork.getDescription() != null) {
			networkDoc.field(NdexClasses.Network_P_desc,srcNetwork.getDescription());
			network.setDescription(srcNetwork.getDescription());
		}

		if ( srcNetwork.getVersion() != null) {
			networkDoc.field(NdexClasses.Network_P_version,srcNetwork.getVersion());
			network.setVersion(srcNetwork.getVersion());
		}
		
		NetworkSourceFormat fmt = removeNetworkSourceFormat(srcNetwork);
		if ( fmt!=null)
			networkDoc.field(NdexClasses.Network_P_source_format, fmt.toString());
		
		networkDoc = networkDoc.save();
		
		networkVertex = graph.getVertex(networkDoc);
		
		logger.info("A new NDex network titled: " +srcNetwork.getName() +" has been created");
	}

/*	
	private void cloneNetworkProperties() throws NdexException, ExecutionException {


		Collection<NdexPropertyValuePair> newProps = 
				addPropertiesToVertex(networkVertex, srcNetwork.getProperties(), null, srcNetwork.getPresentationProperties(), true);
		
		this.network.getProperties().addAll(newProps);
//		this.network.getPresentationProperties().addAll(srcNetwork.getPresentationProperties());

		
	}
*/	
	
	private void cloneNamespaces() throws NdexException, ExecutionException {
		TreeSet<String> prefixSet = new TreeSet<>();

		if ( srcNetwork.getNamespaces() != null) {
			for ( Namespace ns : srcNetwork.getNamespaces().values() ) {
				if ( ns.getPrefix() !=null && prefixSet.contains(ns.getPrefix()))
					throw new NdexException("Duplicated Prefix " + ns.getPrefix() + " found." );
				Long nsId = getNamespace( new RawNamespace(ns.getPrefix(), ns.getUri())).getId();

				ODocument nsDoc = this.elementIdCache.get(nsId);
				nsDoc.field(NdexClasses.ndexProperties, ns.getProperties()).save();
				
				//OrientVertex nsV = graph.getVertex(nsDoc);
				//addPropertiesToVertex(nsV, ns.getProperties(),null, /*ns.getPresentationProperties(),*/ false);

				this.namespaceIdMap.put(ns.getId(), nsId);
			}
		}
	}
	
	private void cloneBaseTerms() throws NdexException {
		logger.info("[start: cloning baseterms; size={} baseterms]", 
				((srcNetwork.getBaseTerms() != null) ? srcNetwork.getBaseTerms().size() : 0));
		
		if ( srcNetwork.getBaseTerms()!= null) {
			for ( BaseTerm term : srcNetwork.getBaseTerms().values() ) {
				Long baseTermId = null;
				if ( term.getNamespaceId() >0 ) {
					Long nsId = namespaceIdMap.get(term.getNamespaceId());
					if ( nsId == null)  
						throw new NdexException ("Namespece Id " + term.getNamespaceId() + " is not found in name space list.");
					baseTermId = createBaseTerm(null, term.getName(), nsId);
				} else {
					baseTermId = createBaseTermWithoutNamespace(term.getName());
				}
				this.baseTermIdMap.put(term.getId(), baseTermId);
			}
		}
		logger.info("[end: cloned baseterms; size={} baseterms]", 
				((srcNetwork.getBaseTerms() != null) ? srcNetwork.getBaseTerms().size() : 0));
	}

	
	private Long createBaseTermWithoutNamespace(String termString) throws NdexException {
		
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
                 
			    return createBaseTerm(prefix,identifier,null);
			  
		  } catch (URISyntaxException e) {
			// ignore and move on to next case
		  }
		}
		
		String[] termStringComponents = TermUtilities.getNdexQName(termString);
		if (termStringComponents != null && termStringComponents.length == 2) {
			// case 2: termString is of the form (NamespacePrefix:)*Identifier
			identifier = termStringComponents[1];
			prefix = termStringComponents[0];
			
			return createBaseTerm(prefix + ":" , identifier, null);
		} 
		
			// case 3: termString cannot be parsed, use it as the identifier.
			// so leave the prefix as null and create the baseterm				
		
		   // create baseTerm in db
		return createBaseTerm(null,termString,null);
          
	
	}

	
	private void cloneCitations() {
		logger.info("[start: cloning citations; size={} citations]", 
				((srcNetwork.getCitations() != null) ? srcNetwork.getCitations().size() : 0));

		if ( srcNetwork.getCitations()!= null) {
			for ( Citation citation : srcNetwork.getCitations().values() ) {
				Long citationId = this.createCitation(citation.getTitle(),
						citation.getIdType(), citation.getIdentifier(), 
						citation.getContributors(), citation.getProperties()); 
				
				this.citationIdMap.put(citation.getId(), citationId);
			}
		}
		
		logger.info("[end: cloned citations; size={} citations]", 
				((srcNetwork.getCitations() != null) ? srcNetwork.getCitations().size() : 0));		
	}

	private void cloneSupports() throws NdexException {
		logger.info("[start: cloning supports; size={} supports]", 
				((srcNetwork.getSupports() != null) ? srcNetwork.getSupports().size() : 0));
		
		if ( srcNetwork.getSupports()!= null) {
			for ( Support support : srcNetwork.getSupports().values() ) {
				Long citationId = -1l;
				long srcCitationId = support.getCitationId();
				if ( srcCitationId != -1)
				    citationId = citationIdMap.get(srcCitationId);
				if ( citationId == null )
					throw new NdexException ("Citation Id " + support.getCitationId() + " is not found in citation list.");
				Long supportId = createSupport(support.getText(),citationId, support.getProperties());
				this.supportIdMap.put(support.getId(), supportId);
			}
		}
		logger.info("[end: cloned supports; size={} supports]", 
				((srcNetwork.getSupports() != null) ? srcNetwork.getSupports().size() : 0));
	}

	// we only clone the nodes itself. We added the edges in the second round
	private void cloneReifiedEdgeTermNodes() {
		if ( srcNetwork.getReifiedEdgeTerms()!= null) {
			for ( ReifiedEdgeTerm reifiedTerm : srcNetwork.getReifiedEdgeTerms().values() ) {
				Long reifiedEdgeTermId = this.database.getNextId(localConnection);
				
				ODocument eTermdoc = new ODocument (NdexClasses.ReifiedEdgeTerm);
				eTermdoc = eTermdoc.field(NdexClasses.Element_ID, reifiedEdgeTermId)
						.save();

				elementIdCache.put(reifiedEdgeTermId, eTermdoc);
				this.reifiedEdgeTermIdMap.put(reifiedTerm.getId(), reifiedEdgeTermId);
			}
		}
	}


	private void cloneFunctionTermVertex() {
		logger.info("[start: cloning term vertex; size={} term vertex]", 
				((srcNetwork.getFunctionTerms() != null) ? srcNetwork.getFunctionTerms().size() : 0)); 
		
		if ( srcNetwork.getFunctionTerms()!= null) {
			for ( FunctionTerm functionTerm : srcNetwork.getFunctionTerms().values() ) {
				Long newFunctionTermId = this.database.getNextId(localConnection);
				
				ODocument eTermdoc = new ODocument (NdexClasses.FunctionTerm)
				        .field(NdexClasses.Element_ID, newFunctionTermId)
						.save();

				elementIdCache.put(newFunctionTermId, eTermdoc);
				this.functionTermIdMap.put(functionTerm.getId(), newFunctionTermId);
					
			}
		}
		logger.info("[end: cloned term vertex; size={} term vertex]", 
				((srcNetwork.getFunctionTerms() != null) ? srcNetwork.getFunctionTerms().size() : 0)); 		
	}

	private void cloneNodes() throws NdexException {
		logger.info("[start: cloning nodes; size={} nodes]", 
				((srcNetwork.getNodes() != null) ? srcNetwork.getNodes().size() : 0));
		
		if ( srcNetwork.getNodes()!= null) {
			for ( Node node : srcNetwork.getNodes().values() ) {
				Long newNodeId = createNode(node);
				this.nodeIdMap.put(node.getId(), newNodeId);
			}
		}
		logger.info("[end: cloned nodes; size={} nodes]", 
				((srcNetwork.getNodes() != null) ? srcNetwork.getNodes().size() : 0));
	}

	private Long createNode (Node node) throws NdexException {
		Long nodeId = database.getNextId(localConnection);
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node)
		   .field(NdexClasses.Element_ID, nodeId);
		
		if ( node.getName()!= null) {
			nodeDoc = nodeDoc.field(NdexClasses.Node_P_name,node.getName());
		}
		
		List<NdexPropertyValuePair> props = node.getProperties(); 		
		if (  props != null && props.size()>0)
			nodeDoc = nodeDoc.field(NdexClasses.ndexProperties, props);
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

		   nodeDoc.fields(NdexClasses.Node_P_represents, newRepId,
				          NdexClasses.Node_P_representTermType, node.getRepresentsTermType());
		}
		
		Collection<Long> oldAliases = node.getAliases();
		if ( oldAliases != null && oldAliases.size() > 0) {
			Set<Long> newAliases = new HashSet<> (oldAliases.size());
			
			for ( Long aliasId : oldAliases) {
				Long newAliasId = baseTermIdMap.get(aliasId);
				if ( newAliasId == null)
					throw new NdexException ("Base term id " + aliasId + " not found.");
				newAliases.add(newAliasId);
			}
			
			nodeDoc.field(NdexClasses.Node_P_alias, newAliases);
		}
		
		Collection<Long> oldRelatedTerms = node.getRelatedTerms(); 
		if ( oldRelatedTerms != null && oldRelatedTerms.size() > 0 ) {
			Set<Long> newRelatedTerms = new HashSet<>(oldRelatedTerms.size());
			
			for ( Long relateToId : oldRelatedTerms) {
				Long newRelateToId = baseTermIdMap.get(relateToId);
				if ( newRelateToId == null)
					throw new NdexException ("Base term id " + relateToId + " not found.");
				newRelatedTerms.add(newRelateToId);
			}
			nodeDoc.field(NdexClasses.Node_P_relatedTo, newRelatedTerms);
		}
		
		Collection<Long> oldCitations = node.getCitationIds(); 
		if ( oldCitations != null && oldCitations.size() > 0 ) {
			Set<Long> newCitations = new HashSet<> (oldCitations.size());
			for ( Long citationId : oldCitations) {
				Long newCitationId = citationIdMap.get(citationId);
				if ( newCitationId == null)
					throw new NdexException ("Citation id " + citationId + " not found.");
				newCitations.add(newCitationId);
			}
			nodeDoc.field(NdexClasses.Citation,newCitations);
		}
		
		Collection<Long> oldSupports = node.getSupportIds(); 
		if ( oldSupports != null) {
			Collection<Long> newSupports = new HashSet<> (oldSupports.size());
			for ( Long supportId : node.getSupportIds()) {
				Long newSupportId = supportIdMap.get(supportId);
				if ( newSupportId == null)
					throw new NdexException ("Support id " + supportId + " not found.");

				newSupports.add(newSupportId);
			}
			nodeDoc.field(NdexClasses.Support,newSupports);
		}
		
		nodeDoc.field(NdexClasses.NdexProperty, node.getProperties());
		
		nodeDoc= nodeDoc.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		
		elementIdCache.put(nodeId, nodeDoc);
        return nodeId;		
	}
	
	
	
	
	private void cloneEdges() throws NdexException, ExecutionException {
		logger.info("[start: cloning edges; size={} edges]", 
				((srcNetwork.getEdges() != null) ? srcNetwork.getEdges().size() : 0));
		
		if ( srcNetwork.getEdges() != null) {
			int counter = 0;
			for ( Map.Entry<Long, Edge> e : srcNetwork.getEdges().entrySet()) {
				Edge edge = e.getValue();
				Long newEdgeId = createEdge(edge);
				edgeIdMap.put(edge.getId(), newEdgeId);
				if ( counter % 20000 == 0 ) {
					graph.commit();
					logger.info("committed " + counter + " edges.");
				}
				counter ++;
 			}
		}
		logger.info("[end: cloned edges; size={} edges]", 
				((srcNetwork.getEdges() != null) ? srcNetwork.getEdges().size() : 0));		
	}
	
	
	private Long createEdge(Edge edge) throws NdexException, ExecutionException {
		
		Long edgeId = database.getNextId(localConnection);
		
		Long newPredicateId = baseTermIdMap.get(edge.getPredicateId());
	 	if ( newPredicateId == null)
				throw new NdexException ("Base term id " + edge.getPredicateId() + " not found.");

	    ODocument edgeDoc = new ODocument(NdexClasses.Edge)
		   .fields(NdexClasses.Element_ID, edgeId,
				   NdexClasses.ndexProperties, edge.getProperties(),
				   NdexClasses.Edge_P_predicateId, newPredicateId );

	    Collection<Long> oldEdgeCitations = edge.getCitationIds() ; 
		if ( oldEdgeCitations != null && oldEdgeCitations.size() > 0) {
			
			Collection<Long> edgeCitations = new HashSet<> (oldEdgeCitations.size());
			if ( edge.getCitationIds() != null) {
				for ( Long citationId : oldEdgeCitations) {
					Long newCitationId = citationIdMap.get(citationId);
					if ( newCitationId == null)
						throw new NdexException ("Citation id " + citationId + " not found.");
					edgeCitations.add(newCitationId);
				}
			}

			edgeDoc.field(NdexClasses.Citation, edgeCitations );
		} 
		
		Collection<Long> oldSupports = edge.getSupportIds(); 
		if (  oldSupports != null && oldSupports.size() > 0 ) {
			Set<Long> newSupports = new HashSet<> (oldSupports.size());
			for ( Long supportId : oldSupports) {
				Long newSupportId = supportIdMap.get(supportId);
				if ( newSupportId == null)
					throw new NdexException ("Support id " + supportId + " not found.");

				newSupports.add(newSupportId);
			}

			edgeDoc.field(NdexClasses.Support, newSupports);
		}
		
        edgeDoc.save();
		OrientVertex edgeV = graph.getVertex(edgeDoc);

		Long newSubjectId = nodeIdMap.get(edge.getSubjectId());
        if ( newSubjectId == null)
        	   throw new NdexException ("Node id " + edge.getSubjectId() + "not found.");
	   
        ODocument subjectDoc = elementIdCache.get(newSubjectId); 
	   graph.getVertex(subjectDoc).addEdge(NdexClasses.Edge_E_subject, edgeV);
		
	   
	   Long newObjectId = nodeIdMap.get(edge.getObjectId());
       if ( newObjectId == null)
    	   throw new NdexException ("Node id " + edge.getObjectId() + "not found.");
       ODocument objectDoc = elementIdCache.get(newObjectId); 
       edgeV.addEdge(NdexClasses.Edge_E_object, graph.getVertex(objectDoc));
	   
		networkVertex.addEdge(NdexClasses.Network_E_Edges,edgeV);
		
		elementIdCache.put(edgeId, edgeDoc);
        return edgeId;		
		
	}
	
	
	private void createLinksforRefiedEdgeTerm() throws NdexException, ExecutionException {
		if ( srcNetwork.getReifiedEdgeTerms()!= null) {
			for ( ReifiedEdgeTerm reifiedTerm : srcNetwork.getReifiedEdgeTerms().values() ) {
				Long newReifiedEdgeId = this.reifiedEdgeTermIdMap.get(reifiedTerm.getId());
				if ( newReifiedEdgeId == null) 
					throw new NdexException("ReifiedEdgeTerm Id " + reifiedTerm.getId() + " not found.");
				
				Long newEdgeId = edgeIdMap.get(reifiedTerm.getEdgeId());
				if ( newEdgeId == null) 
					throw new NdexException ("Edge Id " + reifiedTerm.getEdgeId() + " not found in the system.");
				
				ODocument edgeDoc = elementIdCache.get(newEdgeId); 
				ODocument reifiedEdgeTermDoc = elementIdCache.get(newReifiedEdgeId);
				OrientVertex v = graph.getVertex(reifiedEdgeTermDoc); 
				v.addEdge(NdexClasses.ReifiedEdge_E_edge, graph.getVertex(edgeDoc));
				networkVertex.addEdge(NdexClasses.Network_E_ReifiedEdgeTerms,v);

			}
		}
	}

	private void createLinksFunctionTerm() throws NdexException, ExecutionException {
		logger.info("[start: create links function terms; size={} terms]", 
				((srcNetwork.getFunctionTerms() != null) ? srcNetwork.getFunctionTerms().size() : 0)); 
		
		if ( srcNetwork.getFunctionTerms()!= null) {
			for ( FunctionTerm functionTerm : srcNetwork.getFunctionTerms().values() ) {
				Long newFunctionId = this.functionTermIdMap.get(functionTerm.getId());
				if ( newFunctionId == null )
					throw new NdexException ("Function term Id " + functionTerm.getId() + " is not found in Term list.");
				Long newFunctionNameId = this.baseTermIdMap.get(functionTerm.getFunctionTermId());

				ODocument functionTermDoc = elementIdCache.get(newFunctionId);
				functionTermDoc = functionTermDoc.field(NdexClasses.BaseTerm, newFunctionNameId).save();
				OrientVertex functionTermV = graph.getVertex(functionTermDoc);
				
				for ( Long argId : functionTerm.getParameterIds()) {
					Long newId = findTermId(argId);
					if ( newId == null)
						throw new NdexException ("Term Id " + argId + " is not found in any term list.");
				    ODocument argumentDoc = elementIdCache.get(newId);
				    functionTermV.addEdge(NdexClasses.FunctionTerm_E_paramter, graph.getVertex(argumentDoc));
				}
			}
		}
		logger.info("[end: created links function terms; size={} terms]", 
				((srcNetwork.getFunctionTerms() != null) ? srcNetwork.getFunctionTerms().size() : 0));
	}
	
	
	
	private static NetworkSourceFormat removeNetworkSourceFormat(NetworkSummary nsummary) {
		List<NdexPropertyValuePair> props = nsummary.getProperties(); 
		
		for ( int i = 0 ; i < props.size(); i++) {
			NdexPropertyValuePair p = props.get(i);
			if ( p.getPredicateString().equals(NdexClasses.Network_P_source_format)) {
				NetworkSourceFormat fmt = NetworkSourceFormat.valueOf(p.getValue());
				props.remove(i);
				return fmt;
			}
		}
		return null;
	}
	
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
	
	private void abortTransaction() throws ObjectNotFoundException, NdexException {
		logger.warn("AbortTransaction has been invoked from CX loader.");

		logger.info("Deleting partial network "+ this.network.getExternalId().toString() + " in order to rollback in response to error");
		networkDoc.field(NdexClasses.ExternalObj_isDeleted, true).save();
		graph.commit();
		
		Task task = new Task();
		task.setTaskType(TaskType.SYSTEM_DELETE_NETWORK);
		task.setResource(this.network.getExternalId().toString());
		NdexServerQueue.INSTANCE.addSystemTask(task);
		logger.info("Partial network "+ this.network.getExternalId().toString() + " is deleted.");
	}
}
