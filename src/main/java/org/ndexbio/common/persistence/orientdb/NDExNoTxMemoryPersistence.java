package org.ndexbio.common.persistence.orientdb;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.ndexbio.common.JdexIdService;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.cache.NdexIdentifierCache;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ValidationException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.UserOrientdbDAO;
import org.ndexbio.common.models.data.*;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.SearchResult;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.MembershipType;
import org.ndexbio.model.object.NdexProperty;
import org.ndexbio.common.persistence.NDExPersistenceService;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.network.Namespace;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;


/*
 * An implementation of the NDExPersistenceService interface that uses a 
 * in-memory cache to provide persistence for new ndex doain objects
 * 
 * 
 * Utilizes a Google Guava cache that uses Jdexids as keys and VertexFrame implementations 
 * as values
 */

public class NDExNoTxMemoryPersistence  {

	private NdexDatabase database;
    private NetworkDAO  networkDAO;
	ODatabaseDocumentTx  localConnection;  //all DML will be in this connection, in one transaction.
	private Set<Long> jdexIdSet;
	private Network network;
	private User user;
	private ODocument networkDoc;

	private static final Logger logger = Logger.getLogger(NDExNoTxMemoryPersistence.class.getName());
	private static final Long CACHE_SIZE = 100000L;
	private final Stopwatch stopwatch;
	private long commitCounter = 0L;
	private static Joiner idJoiner = Joiner.on(":").skipNulls();
	
	// key is the full URI or other fully qualified baseTerm as a string.
	private LoadingCache<String, BaseTerm> baseTermStrCache;
    private LoadingCache<RawNamespace, Namespace> rawNamespaceCache;

    /*
     * Currently, the procces flow of this class is:
     * 
     * 1. create object 
     * 2. Create New network
     */
    
	public NDExNoTxMemoryPersistence(NdexDatabase db) {
		database = db;
		localConnection = database.getAConnection();
		localConnection.begin();
		this.networkDAO = new NetworkDAO(db);
		jdexIdSet = Sets.newHashSet();
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

		baseTermStrCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<String, BaseTerm>() {
				   @Override
				   public BaseTerm load(String key) throws NdexException {
					return findOrCreateBaseTerm(key);
				   }
			    });
		
	}

	//TODO: need to add membership etc later. Need to 
	public void createNewNetwork(String ownerName, String networkTitle, String version) throws Exception {
		Preconditions.checkNotNull(ownerName,"A network owner name is required");
		Preconditions.checkNotNull(networkTitle,"A network title is required");
		
		createNetwork(networkTitle,version);
		network.setProperties(new ArrayList<NdexProperty>());
		network.setPresentationProperties(new ArrayList<NdexProperty>());
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
	// ICitation cache
/*	private RemovalListener<Long, ICitation> citationListener = new RemovalListener<Long, ICitation>() {

		public void onRemoval(RemovalNotification<Long, ICitation> removal) {
			logger.info("ICitiation removed from cache key= "
					+ removal.getKey().toString() + " "
					+ removal.getCause().toString());

		}

	};
	private LoadingCache<Long, ICitation> citationCache = CacheBuilder
			.newBuilder().maximumSize(CACHE_SIZE)
			.expireAfterAccess(240L, TimeUnit.MINUTES)
			.removalListener(citationListener)
			.build(new CacheLoader<Long, ICitation>() {
				@Override
				public ICitation load(Long key) throws Exception {
					return ndexService._orientDbGraph.addVertex(
							"class:citation", ICitation.class);
				}

			});

	// IEdge cache
	private RemovalListener<Long, IEdge> edgeListener = new RemovalListener<Long, IEdge>() {

		public void onRemoval(RemovalNotification<Long, IEdge> removal) {
			logger.info("IEdge removed from cache key= "
					+ removal.getKey().toString() + " "
					+ removal.getCause().toString());

		}

	};
	private LoadingCache<Long, IEdge> edgeCache = CacheBuilder.newBuilder()
			.maximumSize(CACHE_SIZE).expireAfterAccess(240L, TimeUnit.MINUTES)
			.removalListener(edgeListener)
			.build(new CacheLoader<Long, IEdge>() {
				@Override
				public IEdge load(Long key) throws Exception {
					return ndexService._orientDbGraph.addVertex("class:edge",
							IEdge.class);
				}

			});

	// INode cache
	private RemovalListener<Long, INode> nodeListener = new RemovalListener<Long, INode>() {

		public void onRemoval(RemovalNotification<Long, INode> removal) {
			logger.info("INode removed from cache key= "
					+ removal.getKey().toString() + " "
					+ removal.getCause().toString());

		}

	};
	private LoadingCache<Long, INode> nodeCache = CacheBuilder.newBuilder()
			.maximumSize(CACHE_SIZE).expireAfterAccess(240L, TimeUnit.MINUTES)
			.removalListener(nodeListener)
			.build(new CacheLoader<Long, INode>() {
				@Override
				public INode load(Long key) throws Exception {
					return ndexService._orientDbGraph.addVertex("class:node",
							INode.class);
				}

			});

	// ISupport cache
	private RemovalListener<Long, ISupport> supportListener = new RemovalListener<Long, ISupport>() {

		public void onRemoval(RemovalNotification<Long, ISupport> removal) {
			logger.info("ISupport removed from cache key= "
					+ removal.getKey().toString() + " "
					+ removal.getCause().toString());

		}

	};
	private LoadingCache<Long, ISupport> supportCache = CacheBuilder
			.newBuilder().maximumSize(CACHE_SIZE)
			.expireAfterAccess(240L, TimeUnit.MINUTES)
			.removalListener(supportListener)
			.build(new CacheLoader<Long, ISupport>() {
				@Override
				public ISupport load(Long key) throws Exception {
					return ndexService._orientDbGraph.addVertex(
							"class:support", ISupport.class);
				}

			});

	public boolean isEntityPersisted(Long jdexId) {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		return (this.jdexIdSet.contains(jdexId));

	}

	// To find a namespace by its prefix, first try to find a jdexid by looking
	// up the prefix in the identifier cache.
	// If a jdexid is found, then lookup the INamespace by jdexid in the
	// namespaceCache and return it.
	public Namespace findNamespaceByPrefix(String prefix) {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(prefix),
				"A namespace prefix is required");
		String namespaceIdentifier = idJoiner.join("NAMESPACE", prefix);
		Preconditions.checkArgument(
				!NdexIdentifierCache.INSTANCE.isNovelIdentifier(namespaceIdentifier),
				"The namespace identifier " + namespaceIdentifier + " is not registered");
		try {
			Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache()
					.get(namespaceIdentifier);
			Namespace ns = this.namespaceCache.getIfPresent(jdexId);
			return ns;
		} catch (ExecutionException e) {

			e.printStackTrace();
		}
		return null;
	}


	public IBaseTerm findOrCreateIBaseTerm(Long jdexId)
			throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return baseTermCache.get(jdexId);
	}

	public IFunctionTerm findOrCreateIFunctionTerm(Long jdexId)
			throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return functionTermCache.get(jdexId);
	}
	

	public IReifiedEdgeTerm findOrCreateIReifiedEdgeTerm(Long jdexId) throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return reifiedEdgeTermCache.get(jdexId);
	}
*/
	private Namespace findOrCreateNamespace(RawNamespace key) throws NdexException {
		Namespace ns = networkDAO.getNamespace(key.getPrefix(), 
				key.getURI(), network.getExternalId());
		
        if (key.getPrefix() !=null && key.getURI() !=null && 
       		 !ns.getUri().equals(key.getURI()))
       	   throw new NdexException("Namespace conflict: prefix " 
       		       + key.getPrefix() + " maps to  " + 
       			   ns.getUri() + " and " + key.getURI());

		if ( ns != null )
			return ns;
		
		// persist the Namespace in db.
		ns = new Namespace();
		ns.setPrefix(key.getPrefix());
		ns.setUri(key.getURI());
		ns.setId(database.getNextId());
		

		ODocument nsDoc = new ODocument(NdexClasses.Namespace);
		nsDoc.field("prefix", key.getPrefix())
		  .field("uri", ns.getUri())
		  .field("id", ns.getId())
		  .field("in_" + NdexClasses.Network_E_NAMESPACE ,networkDoc,OType.LINK)
		  .save();
		
		String nsField = "out_" + NdexClasses.Network_E_NAMESPACE;
		Collection<ODocument> s1 = networkDoc.field(nsField);
		s1.add(nsDoc);
		
		networkDoc.field(nsField, s1, OType.LINKSET)
		.save();

		return ns; 
		
	}

	private BaseTerm findOrCreateBaseTerm(String termString) throws NdexException {
		// case 1 : termString is a URI
		// example: http://identifiers.org/uniprot/P19838
		// treat the last element in the URI as the identifier and the rest as
		// the namespace URI
		// find or create the namespace based on the URI
		// when creating, set the prefix based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
		//
		BaseTerm iBaseTerm = null;
		try {
			URI termStringURI = new URI(termString);
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
			
//			iBaseTerm = persistenceService.findNodeBaseTerm(fragment,namespace);
			return iBaseTerm;
			

		} catch (URISyntaxException e) {
			// ignore and move on to next case
		}

		// case 2: termString is of the form NamespacePrefix:Identifier
		// find or create the namespace based on the prefix
		// when creating, set the URI based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
		//
		String[] termStringComponents = termString.split(":");
/*		if (termStringComponents != null && termStringComponents.length == 2) {
			String identifier = termStringComponents[1];
			String prefix = termStringComponents[0];
			INamespace namespace = findINamespace(null,
					prefix);
			iBaseTerm = this.networkService.findNodeBaseTerm(identifier,
					namespace);
			return iBaseTerm;
		}

		// case 3: termString cannot be parsed, use it as the identifier.
		// find or create the namespace for prefix "LOCAL" and use that as the
		// namespace.

		iBaseTerm = this.networkService.findNodeBaseTerm(termString,
				this.networkService.findINamespace(null, "LOCAL"));

		return iBaseTerm;
	*/	
		return null;
	}
	
	/*	
	public Namespace findOrCreateINamespace(Long jdexId)
			throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return namespaceCache.get(jdexId);
	}

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
	
	public ODocument getNetworkDoc() { return this.networkDoc; } 
	
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
        
//		ndexService._ndexDatabase.begin();
		
		networkDoc = new ODocument (NdexClasses.Network);
		networkDoc.field(NdexClasses.Network_P_UUID,network.getExternalId())
		  .field(NdexClasses.Network_P_cDate, network.getCreationDate())
		  .field(NdexClasses.Network_P_mDate, network.getModificationDate())
		  .field(NdexClasses.Network_P_name, network.getName())
		  .field(NdexClasses.Network_P_isLocked, network.getIsLocked())
		  .field(NdexClasses.Network_P_isComplete, network.getIsComplete())
		  .field(NdexClasses.Network_P_visibility, network.getVisibility().toString())
          .save();
		    
//		ndexService._ndexDatabase.commit();
		
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

		localConnection.rollback();
		
		// make sure everything relate to the network is deleted.
		localConnection.begin();
		deleteNetwork();
		localConnection.commit();
		System.out.println("Deleting network in order to rollback in response to error");
	}

	/*
	 * public method to persist INetwork to the orientdb database using cache
	 * contents.
	 */

	public void persistNetwork() {
		try {
	//		network.setEdgeCount((int) this.edgeCache.size());
	//		network.setNodeCount((int) this.nodeCache.size());
			network.setIsComplete(true);
			NdexIdentifierCache.INSTANCE.accessIdentifierCache().invalidateAll();
			NdexIdentifierCache.INSTANCE.accessTermCache().invalidateAll();
			JdexIdService.INSTANCE.reset();
			System.out.println("The new network " + network.getName()
					+ " is complete");
		} catch (Exception e) {
			System.out.println("unexpected error in persist network...");
			e.printStackTrace();
		} finally {
//			ndexService.teardownDatabase();
			localConnection.commit();
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

	public Namespace getNamespace(RawNamespace rns) throws NdexException {
		try {
			return this.rawNamespaceCache.get(rns);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			logger.severe(e.getMessage());
			throw new NdexException ("Error occured when getting namespace " + rns.getURI() + ". " + e.getMessage());
		}
	}
	
/*
	public Namespace findOrCreateNamespace(String uri, String prefix) throws Exception {
		String namespaceIdentifier = null;
		
		if (uri == null && prefix == null){
			prefix = "LOCAL";
			namespaceIdentifier = "NAMESPACE:LOCAL";
		} else if (prefix != null){
			namespaceIdentifier = idJoiner.join("NAMESPACE", prefix);
		} else if (uri != null){
			prefix = findPrefixForNamespaceURI(uri);
			if (prefix != null){
				namespaceIdentifier = idJoiner.join("NAMESPACE", prefix);				
			} else {
				namespaceIdentifier = idJoiner.join("NAMESPACE", uri);	
			}	
		}

		
		if (uri == null && prefix != null){
			uri = findURIForNamespacePrefix(prefix);
		}
		
		Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache().get(
				namespaceIdentifier);
		boolean persisted = isEntityPersisted(jdexId);
		Namespace iNamespace = findOrCreateINamespace(jdexId);
		if (persisted) return iNamespace;

		// Not persisted, fill out blank Namespace
		iNamespace.setId(jdexId);
		if (prefix == null) prefix = this.findPrefixForNamespaceURI(uri);
		
		if (prefix != null) 
			 iNamespace.setPrefix(prefix);
		if (uri != null) iNamespace.setUri(uri);
		network.getNamespaces().add(iNamespace);
		logger.info("Created namespace " + iNamespace.getPrefix() + " " + iNamespace.getUri());
		return iNamespace;
	}
*/

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

}
