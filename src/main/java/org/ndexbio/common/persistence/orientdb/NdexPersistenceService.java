package org.ndexbio.common.persistence.orientdb;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ValidationException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.UserOrientdbDAO;
import org.ndexbio.model.object.SearchParameters;
import org.ndexbio.common.models.object.SearchResult;
import org.ndexbio.common.models.object.network.RawCitation;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.models.object.network.RawSupport;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.model.object.MembershipType;
import org.ndexbio.model.object.NdexProperty;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.network.Namespace;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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

	private NdexDatabase database;
    private NetworkDAO  networkDAO;
	private ODatabaseDocumentTx  localConnection;  //all DML will be in this connection, in one transaction.
	private OrientGraph graph;
	private Network network;
	private User user;
	private ODocument networkDoc;
	private OrientVertex networkVertex; 
	private Map<String, Namespace> prefixMap;
	private Map<String, Namespace> URINamespaceMap;

	private static final Logger logger = Logger.getLogger(NdexPersistenceService.class.getName());
	private static final long CACHE_SIZE = 100000L;
	private final Stopwatch stopwatch;
	private long commitCounter = 0L;
	private static Joiner idJoiner = Joiner.on(":").skipNulls();
	
	// key is the full URI or other fully qualified baseTerm as a string.
	private LoadingCache<String, BaseTerm> baseTermStrCache;
    private LoadingCache<RawNamespace, Namespace> rawNamespaceCache;
    private LoadingCache<RawCitation, Citation>   rawCitationCache;
    private LoadingCache<RawSupport, Support>     rawSupportCache;
    
    // key is the element_id of a BaseTerm
    private LoadingCache<Long, Node> baseTermNodeCache;

    private LoadingCache<Long, Node> functionTermNodeCache;
    
    // key is the edge id which this term reifed.
    private LoadingCache<Long, ReifiedEdgeTerm> reifiedEdgeTermCache;
    
    private LoadingCache<Long, Node> reifiedEdgeTermNodeCache;
    /*
     * Currently, the procces flow of this class is:
     * 
     * 1. create object 
     * 2. Create New network
     */
    
	public NdexPersistenceService(NdexDatabase db) {
		this.database = db;
		this.localConnection = this.database.getAConnection();
		this.graph = new OrientGraph(this.localConnection);
//		graph.setAutoStartTx(false);
		this.networkDAO = new NetworkDAO(localConnection,true);
		prefixMap = new HashMap<String,Namespace>();
		URINamespaceMap = new HashMap<String,Namespace>();
		this.stopwatch = Stopwatch.createUnstarted();

		
		// intialize caches.
		
		rawNamespaceCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<RawNamespace, Namespace>() {
				   @Override
				   public Namespace load(RawNamespace key) throws NdexException {
					return findOrCreateNamespace(key);
				   }
			    });

		rawCitationCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<RawCitation, Citation>() {
				   @Override
				   public Citation load(RawCitation key) throws NdexException {
					return findOrCreateCitation(key);
				   }
			    });

        rawSupportCache =  CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<RawSupport, Support>() {
				   @Override
				   public Support load(RawSupport key) throws NdexException {
					return findOrCreateSupport(key);
				   }
			    }); 
		
		baseTermStrCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<String, BaseTerm>() {
				   @Override
				   public BaseTerm load(String key) throws NdexException, ExecutionException {
					return findOrCreateBaseTerm(key);
				   }
			    });

		baseTermNodeCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<Long, Node>() {
				   @Override
				   public Node load(Long key) throws NdexException, ExecutionException {
					return findOrCreateNodeFromBaseTermId(key);
				   }
			    });

		
		functionTermNodeCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<Long, Node>() {
				   @Override
				   public Node load(Long key) throws NdexException, ExecutionException {
					return findOrCreateNodeFromFunctionTermId(key);
				   }
			    });

		reifiedEdgeTermCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<Long, ReifiedEdgeTerm>() {
				   @Override
				   public ReifiedEdgeTerm load(Long key) throws NdexException, ExecutionException {
					return findOrCreateReifiedEdgeTermFromEdgeId(key);
				   }
				 });

		reifiedEdgeTermNodeCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<Long, Node>() {
				   @Override
				   public Node load(Long key) throws NdexException, ExecutionException {
					return findOrCreateNodeFromReifiedTermId(key);
				   }
			    });
		
		
	}

	private ReifiedEdgeTerm findOrCreateReifiedEdgeTermFromEdgeId(Long key) {
		ReifiedEdgeTerm eTerm = networkDAO.findReifiedEdgeTermByEdgeId(key);
		if (eTerm != null)	
			return eTerm;
		
		// create new term
		eTerm = new ReifiedEdgeTerm();
		eTerm.setEdgeId(key);
		eTerm.setId(this.database.getNextId());
		
		ODocument eTermdoc = new ODocument ();
		eTermdoc.field(NdexClasses.Element_ID, eTerm.getId())
		.save();
		
		OrientVertex etV = graph.getVertex(eTermdoc);
		ODocument edgeDoc = networkDAO.getDocumentByElementId(NdexClasses.Edge, key);
		
		OrientVertex edgeV = graph.getVertex(edgeDoc);
		etV.addEdge(NdexClasses.ReifedEdge_E_edge, edgeV);
		networkVertex.addEdge(NdexClasses.Network_E_ReifedEdgeTerms,
				etV);
		
		return eTerm;
	}
				
	
				
	//TODO: need to add membership etc later. Need to 
	public void createNewNetwork(String ownerName, String networkTitle, String version) throws Exception {
		Preconditions.checkNotNull(ownerName,"A network owner name is required");
		Preconditions.checkNotNull(networkTitle,"A network title is required");
		
		createNetwork(networkTitle,version);

		// find the network owner in the database
		user =  findUserByAccountName(ownerName);
		if( null == user){
			logger.severe("User " +ownerName +" is not registered in the database/");
			throw new NdexException("User " +ownerName +" is not registered in the database");
		}
				
	//	Membership membership = createNewMember(ownerName, network.getExternalId());
	//	network.getMembers().add(membership);

		logger.info("A new NDex network titled: " +network.getName()
				+" owned by " +ownerName +" has been created");

	}


	// IBaseTerm cache
/*	private RemovalListener<Long, IBaseTerm> baseTermListener = new RemovalListener<Long, IBaseTerm>() {

		public void onRemoval(RemovalNotification<Long, IBaseTerm> removal) {
			logger.info("IBaseTerm removed from cache key= "
					+ removal.getKey().toString() + " "
					+ removal.getCause().toString());

		}

	};

	private LoadingCache<Long, IBaseTerm> baseTermCache = CacheBuilder
			.newBuilder().maximumSize(CACHE_SIZE)
			.expireAfterAccess(240L, TimeUnit.MINUTES)
			.removalListener(baseTermListener)
			.build(new CacheLoader<Long, IBaseTerm>() {
				@Override
				public IBaseTerm load(Long key) throws Exception {
					return ndexService._orientDbGraph.addVertex(
							"class:baseTerm", IBaseTerm.class);

				}
			});
*/
	// IFunctionTerm cache
/*	private RemovalListener<Long, IFunctionTerm> functionTermListener = new RemovalListener<Long, IFunctionTerm>() {

		public void onRemoval(RemovalNotification<Long, IFunctionTerm> removal) {
			logger.info("IFunctionTerm removed from cache key= "
					+ removal.getKey().toString() + " "
					+ removal.getCause().toString());

		}

	};
	
	private LoadingCache<Long, IFunctionTerm> functionTermCache = CacheBuilder
			.newBuilder().maximumSize(CACHE_SIZE)
			.expireAfterAccess(240L, TimeUnit.MINUTES)
			.removalListener(functionTermListener)
			.build(new CacheLoader<Long, IFunctionTerm>() {
				@Override
				public IFunctionTerm load(Long key) throws Exception {
					return ndexService._orientDbGraph.addVertex(
							"class:functionTerm", IFunctionTerm.class);
				}

			});

	// IReifiedEdgeTerm cache
	private RemovalListener<Long, IReifiedEdgeTerm> reifiedEdgeListener = new RemovalListener<Long, IReifiedEdgeTerm>() {

		public void onRemoval(RemovalNotification<Long, IReifiedEdgeTerm> removal) {
			logger.info("IReifiedEdgeTerm removed from cache key= "
					+ removal.getKey().toString() + " "
					+ removal.getCause().toString());

		}

	};
	
	private LoadingCache<Long, IReifiedEdgeTerm> reifiedEdgeTermCache = CacheBuilder
			.newBuilder().maximumSize(CACHE_SIZE)
			.expireAfterAccess(240L, TimeUnit.MINUTES)
			.removalListener(reifiedEdgeListener)
			.build(new CacheLoader<Long, IReifiedEdgeTerm>() {
				@Override
				public IReifiedEdgeTerm load(Long key) throws Exception {
					return ndexService._orientDbGraph.addVertex(
							"class:reifiedEdgeTerm", IReifiedEdgeTerm.class);
				}

			});
	
	// INamespace cache
	private RemovalListener<Long, Namespace> namespaceListener = new RemovalListener<Long, Namespace>() {

		@Override
		public void onRemoval(RemovalNotification<Long, Namespace> removal) {
			logger.info("INamespace removed from cache key= "
					+ removal.getKey().toString() + " "
					+ removal.getCause().toString());

		}

	};

	private LoadingCache<Long, Namespace> namespaceCache = CacheBuilder
			.newBuilder().maximumSize(CACHE_SIZE)
			.expireAfterAccess(240L, TimeUnit.MINUTES)
			.removalListener(namespaceListener)
			.build(new CacheLoader<Long, Namespace>() {
				@Override
				//TODO: Check to make sure Namespaces are persisted when network is persisted. 
				 // and we are not storing them here. Risk: over cache size limit will have problem in data.
				public Namespace load(Long key) throws Exception {
					Namespace ns = new Namespace();
					 ns.setId(key);
					 
					return ns; 
				}

			});
*/


	private Namespace findOrCreateNamespace(RawNamespace key) throws NdexException {
		Namespace ns = networkDAO.getNamespace(key.getPrefix(), 
				key.getURI(), network.getExternalId());

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
		nsDoc.field("prefix", key.getPrefix())
		  .field("uri", ns.getUri())
		  .field("id", ns.getId())
//		  .field("in_" + NdexClasses.Network_E_Namespace ,networkDoc,OType.LINK)
		  .save();
		
/*		String nsField = "out_" + NdexClasses.Network_E_Namespace;
		Collection<ODocument> s1 = networkDoc.field(nsField);
		s1.add(nsDoc);
		
		networkDoc.field(nsField, s1, OType.LINKSET)  
		.save();  */
        
		OrientVertex nsV = graph.getVertex(nsDoc);
		OrientVertex networkV = graph.getVertex(getNetworkDoc());
		networkV.addEdge(NdexClasses.Network_E_Namespace, nsV);
		
		if (ns.getPrefix() != null) 
			prefixMap.put(ns.getPrefix(), ns);
		
		if ( ns.getUri() != null) 
			URINamespaceMap.put(ns.getUri(), ns);
		return ns; 
		
	}

	
	private Citation findOrCreateCitation(RawCitation key) {
		Citation citation = networkDAO.getCitation(key.getTitle(), 
				key.getIdType(), key.getIdentifier(), network.getExternalId());

		if ( citation != null ) {
	        return citation;
		}
		
		// persist the citation object in db.
		citation = new Citation();
		citation.setId(database.getNextId());
		citation.setTitle(key.getTitle());
		citation.setContributors(key.getContributors());
		
		NdexProperty p = new NdexProperty();
		p.setPredicateString(key.getIdType());
		p.setValue(key.getIdentifier());
		citation.getProperties().add(p);
		
		ODocument pDoc = createNdexPropertyDoc(key.getIdType(),key.getIdentifier());
		
		ODocument citationDoc = new ODocument(NdexClasses.Citation);
		citationDoc.field(NdexClasses.Element_ID, citation.getId())
		  .field(NdexClasses.Citation_P_title, key.getTitle())
		  .field(NdexClasses.Citaion_P_contributors, key.getContributors(), OType.EMBEDDEDLIST)
		  .save();
		
        
		OrientVertex pV = graph.getVertex(pDoc);
		OrientVertex citationV = graph.getVertex(citationDoc);
		OrientVertex networkV = graph.getVertex(getNetworkDoc());
		citationV.addEdge(NdexClasses.E_ndexProperties, pV);
		networkV.addEdge(NdexClasses.Network_E_Citations, citationV);

		return citation; 
		
	}
	

	private Support findOrCreateSupport(RawSupport key) {
		Support support = networkDAO.getSupport(key.getSupportText(),key.getCitationId());

		if ( support != null ) {
	        return support;
		}
		
		// persist the support object in db.
		support = new Support();
		support.setId(database.getNextId());
		support.setCitation(key.getCitationId());
		support.setText(key.getSupportText());

		ODocument supportDoc = new ODocument(NdexClasses.Support);
		supportDoc.field(NdexClasses.Element_ID, support.getId())
		   .field(NdexClasses.Support_P_text, key.getSupportText())	
		   .save();

		ODocument citationDoc = networkDAO.getDocumentByElementId(NdexClasses.Citation, key.getCitationId());
        
		OrientVertex supportV = graph.getVertex(supportDoc);
		OrientVertex citationV = graph.getVertex(citationDoc);
		OrientVertex networkV = graph.getVertex(getNetworkDoc());
		supportV.addEdge(NdexClasses.Support_E_citation, citationV);
		networkV.addEdge(NdexClasses.Network_E_Supports, supportV);

		return support; 
		
	}
	
	
	
	private BaseTerm findOrCreateBaseTerm(String termString) throws NdexException, ExecutionException {
		// case 1 : termString is a URI
		// example: http://identifiers.org/uniprot/P19838
		// treat the last element in the URI as the identifier and the rest as
		// the namespace URI
		// find or create the namespace based on the URI
		// when creating, set the prefix based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
		//
		BaseTerm iBaseTerm = null;
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
			
			    // search in db to find the base term
			
			    iBaseTerm = networkDAO.getBaseTerm(fragment,namespace.getId());
			    if (iBaseTerm != null)
			       return iBaseTerm;
			
			    // create baseTerm in db
			    return createBaseTerm(fragment, namespace.getId());
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
			
			
			iBaseTerm = networkDAO.getBaseTerm(identifier,namespace.getId());
			if (iBaseTerm != null)
			   return iBaseTerm;
			
			// create baseTerm in db
			return createBaseTerm(identifier, namespace.getId());
		}

		// case 3: termString cannot be parsed, use it as the identifier.
		// find or create the namespace for prefix "LOCAL" and use that as the
		// namespace.

		iBaseTerm = networkDAO.getBaseTerm(termString,-1);
		if (iBaseTerm != null)
			   return iBaseTerm;
			
			// create baseTerm in db
     	return createBaseTerm(termString, -1);
		
	}
	
	private BaseTerm createBaseTerm(String localTerm, long nsId) {
		BaseTerm bterm = new BaseTerm();
		bterm.setId(database.getNextId());
		bterm.setName(localTerm);
		
		ODocument btDoc = new ODocument(NdexClasses.BaseTerm);
		btDoc.field(NdexClasses.BTerm_P_name, localTerm)
		  .field(NdexClasses.Element_ID, bterm.getId());
		btDoc.save();

		OrientVertex basetermV = graph.getVertex(btDoc);
		
		
		if ( nsId >= 0) {
  		  bterm.setNamespace(nsId);

  		  ODocument nsDoc = networkDAO.getNamespaceDocByEId(nsId); 
  		  
  		  OrientVertex nsV = graph.getVertex(nsDoc);
  		
  		  basetermV.addEdge(NdexClasses.BTerm_E_Namespace, nsV);
		}
		  
        networkVertex.addEdge(NdexClasses.Network_E_BaseTerms, basetermV);
		return bterm;
	}

	
	private Node findOrCreateNodeFromBaseTermId(Long bTermId) {
		Node node = networkDAO.findNodeByBaseTermId(bTermId.longValue());
		
		if (node != null) 
			return node;
		
		// otherwise insert Node.
		node = new Node();
		node.setId(database.getNextId());

		ODocument termDoc = networkDAO.getDocumentByElementId(NdexClasses.BaseTerm, bTermId.longValue());
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc.field(NdexClasses.Element_ID, node.getId())
		   .save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		nodeV.addEdge(NdexClasses.Node_E_represents, graph.getVertex(termDoc));
		
		network.setNodeCount(network.getNodeCount()+1);
		
		return node;
	}
	
	 
	
	private Node findOrCreateNodeFromFunctionTermId(Long fTermId) {
		Node node = networkDAO.findNodeByFunctionTermId(fTermId.longValue());
		
		if (node != null) 
			return node;
		
		// otherwise insert Node.
		node = new Node();
		node.setId(database.getNextId());

		ODocument termDoc = networkDAO.getDocumentByElementId(NdexClasses.FunctionTerm, fTermId.longValue());
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc.field(NdexClasses.Element_ID, node.getId())
		   .save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		nodeV.addEdge(NdexClasses.Node_E_represents, graph.getVertex(termDoc));
		
		network.setNodeCount(network.getNodeCount()+1);
		
		return node;
	}


	private Node findOrCreateNodeFromReifiedTermId(Long key) {
		Node node = networkDAO.findNodeByReifiedEdgeTermId(key.longValue());
		
		if (node != null) 
			return node;
		
		// otherwise insert Node.
		node = new Node();
		node.setId(database.getNextId());

		ODocument termDoc = networkDAO.getDocumentByElementId(NdexClasses.ReifiedEdgeTerm,
				key.longValue());
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc.field(NdexClasses.Element_ID, node.getId())
		   .save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		nodeV.addEdge(NdexClasses.Node_E_represents, graph.getVertex(termDoc));
		
		network.setNodeCount(network.getNodeCount()+1);
		
		return node;
	}
	
	/*	

	public ICitation findOrCreateICitation(Long jdexId)
			throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return citationCache.get(jdexId);
	}

	public IEdge findOrCreateIEdge(Long jdexId) throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return edgeCache.get(jdexId);
	}

	public INode findOrCreateINode(Long jdexId) throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return nodeCache.get(jdexId);
	}

	public ISupport findOrCreateISupport(Long jdexId) throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return supportCache.get(jdexId);
	}

*/

	public Network getCurrentNetwork() {
		return this.network;
	}
	
//	private ODocument getNetworkDoc() { return this.networkDoc; } 
	
    //TODO: change this function to private void once migrate to 1.0 -- cj
	public Network createNetwork(String title, String version){
		this.network = new Network();
		this.network.setExternalId(NdexUUIDFactory.INSTANCE.getNDExUUID());
		this.network.setName(title);
		network.setVisibility(VisibilityType.PUBLIC);
		network.setIsLocked(false);
		network.setIsComplete(false);

		if ( version != null)
			this.network.setVersion(version);
        
		networkDoc = new ODocument (NdexClasses.Network);
		getNetworkDoc().field(NdexClasses.Network_P_UUID,network.getExternalId().toString())
		  .field(NdexClasses.Network_P_cDate, network.getCreationDate())
		  .field(NdexClasses.Network_P_mDate, network.getModificationDate())
		  .field(NdexClasses.Network_P_name, network.getName())
		  .field(NdexClasses.Network_P_isLocked, network.getIsLocked())
		  .field(NdexClasses.Network_P_isComplete, network.getIsComplete())
		  .field(NdexClasses.Network_P_visibility, network.getVisibility().toString())
          .save();
		    
		networkVertex = graph.getVertex(getNetworkDoc());
		
		return this.network;
	}

	/*
	 * find the ITerm (either Base, Function, or ReifiedEdge) by jdex id
	 */

/*	public ITerm findChildITerm(Long jdexId) throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		
		ITerm term = (ITerm) this.baseTermCache.getIfPresent(jdexId);
		if (null != term) return term;
		term = (ITerm) this.functionTermCache.getIfPresent(jdexId);
		if (null != term) return term;
		term = (ITerm) this.reifiedEdgeTermCache.getIfPresent(jdexId);
		return term;
	}

	public IUser getCurrentUser() {
		if (null == this.user) {
			this.user = ndexService._orientDbGraph.addVertex("class:user",
					IUser.class);
		}
		return this.user;
	}


	public Membership createNetworkMembership(String accountName, UUID networkUUID) {

		Membership result = new Membership();
		
		UUID uuid = NdexUUIDFactory.INSTANCE.getNDExUUID();
		
		result.setExternalId(uuid);
		result.setPermissions( org.ndexbio.model.object.Permissions.ADMIN);
		result.setMembershipType(MembershipType.NETWORK);
		ndexService._ndexDatabase.begin();
		
		 ODocument membership = new ODocument(NdexClasses.Membership);
		    membership.field("membershipType", result.getMembershipType());
		    membership.field("permissions", result.getPermissions());
		    //TODO: need to turn this into a link
		    membership.field("resourceUUID", networkUUID);
		    membership.field("accountName", accountName);

		    membership.save();
   		    
			ndexService._ndexDatabase.commit();

		return result;
	}
*/
    /**
     * Find a user based on account name.
     * @param accountName
     * @return a User object when found, otherwise returns null.
     * @throws NdexException
     */

	public User findUserByAccountName(String accountName)
			throws NdexException
			{
		if (accountName == null	)
			throw new ValidationException("No accountName was specified.");


		final String query = "select * from " + NdexClasses.User + 
				  " where accountName = '" + accountName + "'";
				
		User user = null;
		List<ODocument> userDocumentList = localConnection
					.query(new OSQLSynchQuery<ODocument>(query));

		if ( ! userDocumentList.isEmpty()) {
				ODocument userDoc = userDocumentList.get(0);
				user = new User();
				user.setAccountName((String)userDoc.field("accountName"));
				
				//TODO: populate all fields in User class.
				
		}
		return user;
	}

	/*
	 * Returns a collection of IUsers based on search criteria
	 */


	public SearchResult<User> findUsers(SearchParameters searchParameters)
			throws NdexException {
		if (searchParameters.getSearchString() == null
				|| searchParameters.getSearchString().isEmpty())
			throw new ValidationException("No search string was specified.");
		else
			searchParameters.setSearchString(searchParameters.getSearchString()
					.toUpperCase().trim());

		final List<User> foundUsers = Lists.newArrayList();
		final SearchResult<User> result = new SearchResult<User>();
		result.setResults(foundUsers);

		// TODO: Remove these, they're unnecessary
		result.setPageSize(searchParameters.getTop());
		result.setSkip(searchParameters.getSkip());

		final int startIndex = searchParameters.getSkip()
				* searchParameters.getTop();

		String whereClause = " where username.toUpperCase() like '%"
				+ searchParameters.getSearchString()
				+ "%' OR lastName.toUpperCase() like '%"
				+ searchParameters.getSearchString()
				+ "%' OR firstName.toUpperCase() like '%"
				+ searchParameters.getSearchString() + "%'";

		final String query = "select from User " + whereClause
				+ " order by creation_date desc skip " + startIndex + " limit "
				+ searchParameters.getTop();

		try {

			List<ODocument> userDocumentList = localConnection
					.query(new OSQLSynchQuery<ODocument>(query));

			for (final ODocument document : userDocumentList)
				foundUsers.add(UserOrientdbDAO.getUserFromDocument(document));

			result.setResults(foundUsers);

			return result;
		} catch (Exception e) {
			throw new NdexException(e.getMessage());
		}

	}
	
	
	/*
	 * public method to allow xbel parsing components to rollback the
	 * transaction and close the database connection if they encounter an error
	 * situation
	 */

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

	/*
	 * public method to persist INetwork to the orientdb database using cache
	 * contents.
	 */

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
			commit();
			System.out
					.println("Connection to orientdb database has been closed");
		}
	}


	public void networkProgressLogCheck() throws NdexException {
		commitCounter++;
		if (commitCounter % 1000 == 0) {
			logger.info("Checkpoint: Number of edges " /*+this.edgeCache.size()*/);
		}

	}


	public void deleteNetwork() {
		// TODO Implement deletion of network
		System.out
		.println("deleteNetwork called. Not yet implemented");
		
	}

	/**
	 * Find or create a namespace object from database;
	 * @param rns
	 * @return
	 * @throws NdexException
	 */
	public Namespace getNamespace(RawNamespace rns) throws NdexException {
		try {
			if (rns.getPrefix() == null) {
				Namespace ns = URINamespaceMap.get(rns.getURI());
				if ( ns != null ) {
					return ns; 
				}
			}
			return this.rawNamespaceCache.get(rns);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			logger.severe(e.getMessage());
			throw new NdexException ("Error occured when getting namespace " + rns.getURI() + ". " + e.getMessage());
		}
	}
	
	public Citation getCitation(String title, String idType, String identifier, 
			List<String> contributors) throws NdexException {
		RawCitation rCitation = new RawCitation(title, idType, identifier, contributors);
		try {
			return this.rawCitationCache.get(rCitation);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			logger.severe(e.getMessage());
			throw new NdexException ("Error occured when getting citation " + rCitation.getTitle() + ". " + e.getMessage());
		}
	}
	
	public Support getSupport(String literal, long citationId) throws ExecutionException {
		RawSupport r = new RawSupport(literal, citationId);
		return this.rawSupportCache.get(r);
		
	}

	public ReifiedEdgeTerm getReifedEdgeTermForEdge(long edgeId) throws ExecutionException {
		return this.reifiedEdgeTermCache.get(edgeId);
	}
	
	public BaseTerm getBaseTerm(String termString) throws NdexException {
		try {
			return this.baseTermStrCache.get(termString);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			logger.severe(e.getMessage());
			throw new NdexException ("Error occured when getting BaseTerm" + termString + ". " + e.getMessage());
		}
	}
	
	public Node getNodeByBaseTerm(String termString) throws ExecutionException {
		BaseTerm t = this.baseTermStrCache.get(termString);
		return this.baseTermNodeCache.get(t.getId());
	}
	
	public Node getNodeByFunctionTerm(FunctionTerm funcTerm) throws ExecutionException {
		return this.functionTermNodeCache.get(funcTerm.getId());
	}
	
	public Node getNodeByReifiedEdgeTerm(FunctionTerm funcTerm) throws ExecutionException {
		return this.reifiedEdgeTermNodeCache.get(funcTerm.getId());
	}

	public Edge createEdge(Node subjectNode, Node objectNode, BaseTerm predicate, 
			 Support support, Citation citation, Map<String,String> annotation )
			throws NdexException {
		if (null != objectNode && null != subjectNode && null != predicate) {
			
			Edge edge = new Edge();
			edge.setId(database.getNextId());
			edge.setSubjectId(subjectNode.getId());
			edge.setObjectId(objectNode.getId());
			edge.setPredicateId(predicate.getId());
			
			ODocument subjectNodeDoc = networkDAO.getDocumentByElementId(NdexClasses.Node, subjectNode.getId());
			ODocument objectNodeDoc  = networkDAO.getDocumentByElementId(NdexClasses.Node, objectNode.getId());
			ODocument predicateDoc   = networkDAO.getDocumentByElementId(NdexClasses.BaseTerm, predicate.getId());
			
			ODocument edgeDoc = new ODocument(NdexClasses.Edge);
			edgeDoc.field(NdexClasses.Element_ID, edge.getId()).save();
			OrientVertex edgeVertex = graph.getVertex(edgeDoc);
			
			if ( annotation != null) {
				for (Map.Entry<String, String> e : annotation.entrySet()) {
					ODocument pDoc = this.createNdexPropertyDoc(e.getKey(),e.getValue());
                    OrientVertex pV = graph.getVertex(pDoc);
                    edgeVertex.addEdge(NdexClasses.E_ndexProperties, pV);
                    
                    NdexProperty p = new NdexProperty();
                    p.setPredicateString(e.getKey());
                    p.setDataType(e.getValue());
                    edge.getProperties().add(p);
				}
			
			}

			networkVertex.addEdge(NdexClasses.Network_E_Edges, edgeVertex);
			edgeVertex.addEdge(NdexClasses.Edge_E_predicate, graph.getVertex(predicateDoc));
			edgeVertex.addEdge(NdexClasses.Edge_E_object, graph.getVertex(objectNodeDoc));
			graph.getVertex(subjectNodeDoc).addEdge(NdexClasses.Edge_E_subject, edgeVertex);

		    network.setEdgeCount(network.getEdgeCount()+1);
		    
		    if (citation != null) {
				ODocument citationDoc = networkDAO.getDocumentByElementId(
						NdexClasses.Citation, citation.getId());
		    	OrientVertex citationV = graph.getVertex(citationDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_citations, citationV);
		    	
		    	edge.getCitations().add(citation.getId());
		    }
		    
		    if ( support != null) {
				ODocument supportDoc = networkDAO.getDocumentByElementId(
						NdexClasses.Support, support.getId());
		    	OrientVertex supportV = graph.getVertex(supportDoc);
		    	edgeVertex.addEdge(NdexClasses.Edge_E_supports, supportV);
		    	
		    	edge.getSupports().add(support.getId());
		    }
		    
			return edge;
		} 
		throw new NdexException("Null value for one of the parameter when creating Edge.");
	}
	
	
	public void commit () {
		graph.commit();
		database.commit();
	}

	public ODocument getNetworkDoc() {
		return networkDoc;
	}

	
	private ODocument createNdexPropertyDoc(String key, String value) {
		ODocument pDoc = new ODocument(NdexClasses.NdexProperty);
		pDoc.field(NdexClasses.ndexProp_P_predicateStr,key)
		   .field(NdexClasses.ndexProp_P_value, value)
		   .save();
		return pDoc;
		

	}
/*	public void setNetworkDoc(ODocument networkDoc) {
		this.networkDoc = networkDoc;
	}
*/	
/*	
	private static String findPrefixForNamespaceURI(String uri) {
		if (uri.equals("http://biopax.org/generated/group/")) return "GROUP";
		if (uri.equals("http://identifiers.org/uniprot/")) return "UniProt";
		if (uri.equals("http://purl.org/pc2/4/")) return "PathwayCommons2";
		//System.out.println("No Prefix for " + uri);
		
		return null;
	}
	
	// TODO: check if this function need to be the same as the function above 
	private static String findURIForNamespacePrefix(String prefix){
		if (prefix.equals("UniProt")) return "http://identifiers.org/uniprot/";
		return null;
	}
*/
}
