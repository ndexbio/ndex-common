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
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.model.object.network.Term;
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

	private NdexDatabase database;
    private NetworkDAO  networkDAO;
	private ODatabaseDocumentTx  localConnection;  //all DML will be in this connection, in one transaction.
	private OrientGraph graph;
	private Network network;
	private ODocument ownerDoc;
	private ODocument networkDoc;
	private OrientVertex networkVertex; 
	private Map<String, Namespace> prefixMap;
	private Map<String, Namespace> URINamespaceMap;

	private static final Logger logger = Logger.getLogger(NdexPersistenceService.class.getName());
	private static final long CACHE_SIZE = 100000L;
	private final Stopwatch stopwatch;
//	private long commitCounter = 0L;
//	private static Joiner idJoiner = Joiner.on(":").skipNulls();
	
	// key is the full URI or other fully qualified baseTerm as a string.
	private LoadingCache<String, BaseTerm> baseTermStrCache;
    private LoadingCache<RawNamespace, Namespace> rawNamespaceCache;
    private LoadingCache<RawCitation, Citation>   rawCitationCache;
    private LoadingCache<RawSupport, Support>     rawSupportCache;
    
    private LoadingCache<Long, ODocument>  elementIdCache;
    
    // key is the element_id of a BaseTerm
    private LoadingCache<Long, Node> baseTermNodeCache;

    private LoadingCache<Long, Node> functionTermNodeCache;
    
    // key is the edge id which this term reifed.
    private LoadingCache<Long, ReifiedEdgeTerm> reifiedEdgeTermCache;
    
    private LoadingCache<Long, Node> reifiedEdgeTermNodeCache;
    
    // key is a "rawFunctionTerm", which has element id as -1. This table
    // matches the key to a functionTerm that has been stored in the db.
    private LoadingCache<FunctionTerm, FunctionTerm> functionTermCache;
    
    //key is the name of the node. This cache is for loading simple SIF 
    // for now
    private LoadingCache<String, Node> namedNodeCache;
    
    private Map<Long, Node> externalIdNodeMap;
    
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
		this.network = null;
		this.ownerDoc = null;
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
				   public Support load(RawSupport key) throws NdexException, ExecutionException {
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
		
		functionTermCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<FunctionTerm, FunctionTerm>() {
				   @Override
				   public FunctionTerm load(FunctionTerm key) throws NdexException, ExecutionException {
					return findOrCreateFunctionTerm  (key);
				   }
			    });
		
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
		
		namedNodeCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE*5)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<String, Node>() {
				   @Override
				   public Node load(String key) throws NdexException, ExecutionException {
					   return findOrCreateNodeByName(key);
				   }
			    }); 

		
		externalIdNodeMap = new TreeMap<Long,Node>(); 

	}

	private Node findOrCreateNodeByName(String key) throws NdexException {
		List<Node> nodes = networkDAO.findNodesByName (key, network.getExternalId().toString());
		
		if ( !nodes.isEmpty()) {
			if (nodes.size() != 1) 
				throw new NdexException ("More then one node has name " + key + " in current network.");
			return nodes.get(0);
		}
		
		// otherwise insert Node.
		Node node = new Node();
		node.setId(database.getNextId());
		node.setName(key);

		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc =nodeDoc.field(NdexClasses.Element_ID, node.getId())
				.field(NdexClasses.Node_P_name, key)
				.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		
		network.setNodeCount(network.getNodeCount()+1);
		
		nodeDoc.reload();
		elementIdCache.put(node.getId(), nodeDoc);
		return node;
		
	}
	
	public Node getNodeByName(String name) throws ExecutionException {
		return this.namedNodeCache.get(name);
	}

	private ReifiedEdgeTerm findOrCreateReifiedEdgeTermFromEdgeId(Long key) throws ExecutionException {
		ReifiedEdgeTerm eTerm = networkDAO.findReifiedEdgeTermByEdgeId(key);
		if (eTerm != null)	
			return eTerm;
		
		// create new term
		eTerm = new ReifiedEdgeTerm();
		eTerm.setEdgeId(key);
		eTerm.setId(this.database.getNextId());
		
		ODocument eTermdoc = new ODocument ();
		eTermdoc = eTermdoc.field(NdexClasses.Element_ID, eTerm.getId())
				.save();
		
		OrientVertex etV = graph.getVertex(eTermdoc);
		ODocument edgeDoc = elementIdCache.get(key);
		
		OrientVertex edgeV = graph.getVertex(edgeDoc);
		etV.addEdge(NdexClasses.ReifedEdge_E_edge, edgeV);
		networkVertex.addEdge(NdexClasses.Network_E_ReifedEdgeTerms,
				etV);
		
		eTermdoc.reload();
		elementIdCache.put(eTerm.getId(), eTermdoc);
		return eTerm;
	}
				
				
	public void createNewNetwork(String ownerName, String networkTitle, String version) throws Exception {
		createNewNetwork(ownerName, networkTitle, version,NdexUUIDFactory.INSTANCE.getNDExUUID() );
	}
	
	public Node findOrCreateNodeByExternalId(Long id) {
		Node node = this.externalIdNodeMap.get(id);
		if ( node != null) return node;
		
		//create a node for this external id.
		node = new Node();
		node.setId(database.getNextId());

		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc =nodeDoc.field(NdexClasses.Element_ID, node.getId())
				.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		
		network.setNodeCount(network.getNodeCount()+1);
		
		nodeDoc.reload();
		elementIdCache.put(node.getId(), nodeDoc);
		
		externalIdNodeMap.put(id, node);
		return node;

	}

	
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
		nsDoc = nsDoc.field("prefix", key.getPrefix())
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
		
		nsDoc.reload();
		elementIdCache.put(ns.getId(),nsDoc);
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

		citationDoc.reload();
		elementIdCache.put(citation.getId(), citationDoc);
		return citation; 
		
	}
	

	private Support findOrCreateSupport(RawSupport key) throws ExecutionException {
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

		ODocument citationDoc = elementIdCache.get(key.getCitationId());
			//	networkDAO.getDocumentByElementId(NdexClasses.Citation, key.getCitationId());
        
		OrientVertex supportV = graph.getVertex(supportDoc);
		OrientVertex citationV = graph.getVertex(citationDoc);
		OrientVertex networkV = graph.getVertex(getNetworkDoc());
		supportV.addEdge(NdexClasses.Support_E_citation, citationV);
		networkV.addEdge(NdexClasses.Network_E_Supports, supportV);

		supportDoc.reload();
		elementIdCache.put(support.getId(), supportDoc);
		return support; 
		
	}
	
	// input parameter is a "rawFunctionTerm", which has element_id as -1;
	private FunctionTerm findOrCreateFunctionTerm(FunctionTerm func) throws ExecutionException {
		FunctionTerm f = networkDAO.getFunctionTerm(func);
		if ( f != null) return f;
		
		f = new FunctionTerm();
		f.setId(database.getNextId());
		f.setFunctionTermId(func.getFunctionTermId());
		f.setParameters(func.getParameters());
		
	    ODocument fTerm = new ODocument(NdexClasses.FunctionTerm);
	    fTerm.field(NdexClasses.Element_ID, f.getId())
	       .save();
	    
        OrientVertex fTermV = graph.getVertex(fTerm);
        
        ODocument bTermDoc = elementIdCache.get(func.getFunctionTermId()); 
        		//networkDAO.getDocumentByElementId(NdexClasses.BaseTerm, func.getFunctionTermId());
        fTermV.addEdge(NdexClasses.FunctionTerm_E_baseTerm, graph.getVertex(bTermDoc));
        
        for (Long id : func.getParameters()) {
        	ODocument o = elementIdCache.get(id);
        	fTermV.addEdge(NdexClasses.FunctionTerm_E_paramter, graph.getVertex(o));
        }
	    
        fTerm.reload();
        elementIdCache.put(f.getId(), fTerm);
        return f;
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
			
			    iBaseTerm = networkDAO.getBaseTerm(fragment,namespace.getId(), network.getExternalId().toString());
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
			
			if ( namespace == null) {
				namespace = createLocalNamespaceforPrefix(prefix);
				logger.warning("Prefix '" + prefix + "' is not defined in the network. URI "+
				namespace.getUri()	+ " has been created for it by Ndex." );
			}
			
			iBaseTerm = networkDAO.getBaseTerm(identifier,namespace.getId(),network.getExternalId().toString());
			if (iBaseTerm != null)
			   return iBaseTerm;
			
			// create baseTerm in db
			return createBaseTerm(identifier, namespace.getId());
		}

		// case 3: termString cannot be parsed, use it as the identifier.
		// find or create the namespace for prefix "LOCAL" and use that as the
		// namespace.

		iBaseTerm = networkDAO.getBaseTerm(termString,-1, network.getExternalId().toString());
		if (iBaseTerm != null)
			   return iBaseTerm;
			
			// create baseTerm in db
     	return createBaseTerm(termString, -1);
		
	}
	
	private Namespace createLocalNamespaceforPrefix (String prefix) throws NdexException {
		String urlprefix = prefix.replace(' ', '_');
		return findOrCreateNamespace(
				new RawNamespace(prefix, "http://uri.ndexbio.org/ns/"+this.network.getExternalId()
						+"/" + urlprefix + "/"));
	}
	
	private BaseTerm createBaseTerm(String localTerm, long nsId) throws ExecutionException {
		BaseTerm bterm = new BaseTerm();
		bterm.setId(database.getNextId());
		bterm.setName(localTerm);
		
		ODocument btDoc = new ODocument(NdexClasses.BaseTerm);
		btDoc = btDoc.field(NdexClasses.BTerm_P_name, localTerm)
		  .field(NdexClasses.Element_ID, bterm.getId())
		  .save();

		OrientVertex basetermV = graph.getVertex(btDoc);
		
		
		if ( nsId >= 0) {
  		  bterm.setNamespace(nsId);

  		  ODocument nsDoc = elementIdCache.get(nsId); 
  				  //networkDAO.getNamespaceDocByEId(nsId); 
  		  
  		  OrientVertex nsV = graph.getVertex(nsDoc);
  		
  		  basetermV.addEdge(NdexClasses.BTerm_E_Namespace, nsV);
		}
		  
        networkVertex.addEdge(NdexClasses.Network_E_BaseTerms, basetermV);
        btDoc.reload();
        elementIdCache.put(bterm.getId(), btDoc);
		return bterm;
	}

	
	private Node findOrCreateNodeFromBaseTermId(Long bTermId) throws ExecutionException {
		Node node = networkDAO.findNodeByBaseTermId(bTermId.longValue());
		
		if (node != null) 
			return node;
		
		// otherwise insert Node.
		node = new Node();
		node.setId(database.getNextId());

		ODocument termDoc = elementIdCache.get(bTermId); 
				//networkDAO.getDocumentByElementId(NdexClasses.BaseTerm, bTermId.longValue());
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc =nodeDoc.field(NdexClasses.Element_ID, node.getId())
		   .save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		nodeV.addEdge(NdexClasses.Node_E_represents, graph.getVertex(termDoc));
		
		network.setNodeCount(network.getNodeCount()+1);
		
		nodeDoc.reload();
		elementIdCache.put(node.getId(), nodeDoc);
		return node;
	}
	
	 
	
	private Node findOrCreateNodeFromFunctionTermId(Long fTermId) throws ExecutionException {
		Node node = networkDAO.findNodeByFunctionTermId(fTermId.longValue());
		
		if (node != null) 
			return node;
		
		// otherwise insert Node.
		node = new Node();
		node.setId(database.getNextId());

		ODocument termDoc = elementIdCache.get(fTermId); 
				//networkDAO.getDocumentByElementId(NdexClasses.FunctionTerm, fTermId.longValue());
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc = nodeDoc.field(NdexClasses.Element_ID, node.getId())
				.save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		nodeV.addEdge(NdexClasses.Node_E_represents, graph.getVertex(termDoc));
		
		network.setNodeCount(network.getNodeCount()+1);
		
		nodeDoc.reload();
		elementIdCache.put(node.getId(), nodeDoc);
		return node;
	}


	private Node findOrCreateNodeFromReifiedTermId(Long key) throws ExecutionException {
		Node node = networkDAO.findNodeByReifiedEdgeTermId(key.longValue());
		
		if (node != null) 
			return node;
		
		// otherwise insert Node.
		node = new Node();
		node.setId(database.getNextId());

		ODocument termDoc = elementIdCache.get(key); 
				//networkDAO.getDocumentByElementId(NdexClasses.ReifiedEdgeTerm,key.longValue());
		
		ODocument nodeDoc = new ODocument(NdexClasses.Node);

		nodeDoc = nodeDoc.field(NdexClasses.Element_ID, node.getId())
		   .save();
		
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		networkVertex.addEdge(NdexClasses.Network_E_Nodes,nodeV);
		nodeV.addEdge(NdexClasses.Node_E_represents, graph.getVertex(termDoc));
		
		network.setNodeCount(network.getNodeCount()+1);
		
		nodeDoc.reload();
		elementIdCache.put(node.getId(), nodeDoc);
		return node;
	}
	

	public Network getCurrentNetwork() {
		return this.network;
	}
	
	
    //TODO: change this function to private void once migrate to 1.0 -- cj
	public Network createNetwork(String title, String version, UUID uuid){
		this.network = new Network();
		this.network.setExternalId(uuid);
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
		
		OrientVertex ownerV = graph.getVertex(ownerDoc);
		ownerV.addEdge(NdexClasses.E_admin, networkVertex);
		
		return this.network;
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
	
	public void setNodeProperties(Long nodeId, Collection<NdexProperty> properties, 
			Collection<NdexProperty> presentationProperties) throws ExecutionException {
		ODocument nodeDoc = this.elementIdCache.get(nodeId);
		OrientVertex v = graph.getVertex(nodeDoc);
		addPropertiesToVertex ( v, properties, presentationProperties);
	}
	
	public void setNetworkProperties(Collection<NdexProperty> properties, 
			Collection<NdexProperty> presentationProperties) {
		addPropertiesToVertex ( networkVertex, properties, presentationProperties);
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
	
	public void createNamespace(String prefix, String URI) throws NdexException {
		RawNamespace r = new RawNamespace(prefix, URI);
		getNamespace(r);
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
	
	public FunctionTerm getFunctionTerm (Long baseTermId, List<Term> termList) throws ExecutionException {
		FunctionTerm rawFunctionTerm = new FunctionTerm();
		rawFunctionTerm.setFunctionTermId(baseTermId);
		
		for ( Term t : termList) {
		  rawFunctionTerm.getParameters().add(t.getId());
		}		  
		 
	    return this.functionTermCache.get(rawFunctionTerm);
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
	
	public Node getNodeByReifiedEdgeTerm(ReifiedEdgeTerm reifiedTerm) throws ExecutionException {
		return this.reifiedEdgeTermNodeCache.get(reifiedTerm.getId());
	}


	//TODO: generalize this function so that createEdge(....) can use it.
	public void addMetaDataToNode (Node subjectNode, Support support, Citation citation,  Map<String,String> annotations) {
        ODocument nodeDoc = networkDAO.getDocumentByElementId(NdexClasses.Node, subjectNode.getId());
    	OrientVertex nodeVertex = graph.getVertex(nodeDoc);
		        
        if ( support != null) {
			ODocument supportDoc = networkDAO.getDocumentByElementId(
					NdexClasses.Support, support.getId());
	    	OrientVertex supportV = graph.getVertex(supportDoc);
	    	nodeVertex.addEdge(NdexClasses.Node_E_supports, supportV);
	    	
	    	subjectNode.getSupports().add(support.getId());
        }

	    if (citation != null) {
			ODocument citationDoc = networkDAO.getDocumentByElementId(
					NdexClasses.Citation, citation.getId());
	    	OrientVertex citationV = graph.getVertex(citationDoc);
	    	nodeVertex.addEdge(NdexClasses.Node_E_ciations, citationV);
	    	
	    	subjectNode.getCitations().add(citation.getId());
	    }

		if ( annotations != null) {
			for (Map.Entry<String, String> e : annotations.entrySet()) {
				ODocument pDoc = this.createNdexPropertyDoc(e.getKey(),e.getValue());
                OrientVertex pV = graph.getVertex(pDoc);
                nodeVertex.addEdge(NdexClasses.E_ndexProperties, pV);
                
                NdexProperty p = new NdexProperty();
                p.setPredicateString(e.getKey());
                p.setDataType(e.getValue());
                subjectNode.getProperties().add(p);
			}
		}

	}
	
	public Edge createEdge(Node subjectNode, Node objectNode, BaseTerm predicate, 
			 Support support, Citation citation, Map<String,String> annotation )
			throws NdexException, ExecutionException {
		if (null != objectNode && null != subjectNode && null != predicate) {
			
			Edge edge = new Edge();
			edge.setId(database.getNextId());
			edge.setSubjectId(subjectNode.getId());
			edge.setObjectId(objectNode.getId());
			edge.setPredicateId(predicate.getId());
			
			ODocument subjectNodeDoc = elementIdCache.get(subjectNode.getId()) ;
					//networkDAO.getDocumentByElementId(NdexClasses.Node, subjectNode.getId());
			ODocument objectNodeDoc  = elementIdCache.get(objectNode.getId()) ;
					//networkDAO.getDocumentByElementId(NdexClasses.Node, objectNode.getId());
			ODocument predicateDoc   = elementIdCache.get(predicate.getId()) ;
					// networkDAO.getDocumentByElementId(NdexClasses.BaseTerm, predicate.getId());
			
			ODocument edgeDoc = new ODocument(NdexClasses.Edge);
			edgeDoc = edgeDoc.field(NdexClasses.Element_ID, edge.getId())
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
		    
		    elementIdCache.put(edge.getId(), edgeDoc);
			return edge;
		} 
		throw new NdexException("Null value for one of the parameter when creating Edge.");
	}
	

	public Edge createEdge(Node subjectNode, Node objectNode, BaseTerm predicate, 
			 Support support, Citation citation, List<NdexProperty> properties, List<NdexProperty> presentationProps )
			throws NdexException, ExecutionException {
		if (null != objectNode && null != subjectNode && null != predicate) {
			
			Edge edge = new Edge();
			edge.setId(database.getNextId());
			edge.setSubjectId(subjectNode.getId());
			edge.setObjectId(objectNode.getId());
			edge.setPredicateId(predicate.getId());
			
			ODocument subjectNodeDoc = elementIdCache.get(subjectNode.getId()) ;
					//networkDAO.getDocumentByElementId(NdexClasses.Node, subjectNode.getId());
			ODocument objectNodeDoc  = elementIdCache.get(objectNode.getId()) ;
					//networkDAO.getDocumentByElementId(NdexClasses.Node, objectNode.getId());
			ODocument predicateDoc   = elementIdCache.get(predicate.getId()) ;
					// networkDAO.getDocumentByElementId(NdexClasses.BaseTerm, predicate.getId());
			
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
		    
		    edgeDoc.reload();
		    elementIdCache.put(edge.getId(), edgeDoc);
			return edge;
		} 
		throw new NdexException("Null value for one of the parameter when creating Edge.");
	}
	
	
	
	
	public void commit () {
		graph.commit();
		this.networkDoc.reload();
		this.networkVertex = graph.getVertex(networkDoc);
	//	database.commit();
	}

	private ODocument getNetworkDoc() {
		return networkDoc;
	}

	
	private ODocument createNdexPropertyDoc(String key, String value) {
		ODocument pDoc = new ODocument(NdexClasses.NdexProperty);
		pDoc.field(NdexClasses.ndexProp_P_predicateStr,key)
		   .field(NdexClasses.ndexProp_P_value, value)
		   .save();
		return pDoc;
		

	}
	
	// alias is treated as a baseTerm
	public void addAliasToNode(long nodeId, String[] aliases) throws ExecutionException, NdexException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		for (String alias : aliases) {
			BaseTerm b= this.getBaseTerm(alias);
		
			ODocument bDoc = elementIdCache.get(b.getId());
			nodeV.addEdge(NdexClasses.Node_E_alias, graph.getVertex(bDoc));
		//	elementIdCache.put(b.getId(), bDoc);
		}
		
		nodeV.getRecord().reload();
		elementIdCache.put(nodeId, nodeV.getRecord());
	}
	
	// alias is treated as a baseTerm
	public void addRelatedTermToNode(long nodeId, String[] relatedTerms) throws ExecutionException, NdexException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		OrientVertex nodeV = graph.getVertex(nodeDoc);
		
		for (String rT : relatedTerms) {
			BaseTerm b= this.getBaseTerm(rT);
		
			ODocument bDoc = elementIdCache.get(b.getId());
			nodeV.addEdge(NdexClasses.Node_E_relateTo, graph.getVertex(bDoc));
		//	elementIdCache.put(b.getId(), bDoc);
		}
		
		nodeV.getRecord().reload();
		elementIdCache.put(nodeId, nodeV.getRecord());
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
	
	public void addCitationToElement(long elementId, Citation c, String className) throws ExecutionException {
		ODocument elementRec = elementIdCache.get(elementId);
		OrientVertex nodeV = graph.getVertex(elementRec);
		
		ODocument citationRec = elementIdCache.get(c.getId());
		OrientVertex citationV = graph.getVertex(citationRec);
		
		if ( className.equals(NdexClasses.Node) ) {
 	       	nodeV.addEdge(NdexClasses.Node_E_ciations, graph.getVertex(citationV));
		} else if ( className.equals(NdexClasses.Edge) ) {
			nodeV.addEdge(NdexClasses.Edge_E_citations, graph.getVertex(citationV));
		}
		
		ODocument o = nodeV.getRecord();
		o.reload();
		elementIdCache.put(elementId, o);
	}
	
	public void setNodeName(long nodeId, String name) throws ExecutionException {
		ODocument nodeDoc = elementIdCache.get(nodeId);
		
		nodeDoc = nodeDoc.field(NdexClasses.Node_P_name, name).save();
		
		nodeDoc.reload();
		elementIdCache.put(nodeId, nodeDoc);
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
}
