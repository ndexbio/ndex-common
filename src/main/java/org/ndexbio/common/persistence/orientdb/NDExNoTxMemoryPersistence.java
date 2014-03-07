package org.ndexbio.common.persistence.orientdb;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.ndexbio.common.cache.NdexIdentifierCache;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.exceptions.ValidationException;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.data.*;
import org.ndexbio.common.models.object.Membership;
import org.ndexbio.common.models.object.Network;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.SearchResult;
import org.ndexbio.common.persistence.NDExPersistenceService;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
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
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * An implementation of the NDExPersistenceService interface that uses a 
 * in-memory cache to provide persistence for new ndex doain objects
 * 
 * 
 * Utilizes a Google Guava cache that uses Jdexids as keys and VertexFrame implementations 
 * as values
 */

public class NDExNoTxMemoryPersistence implements NDExPersistenceService {

	private OrientDBNoTxConnectionService ndexService;
	private Set<Long> jdexIdSet;
	private INetwork network;
	private IUser user;
	private static final Logger logger = LoggerFactory
			.getLogger(NDExNoTxMemoryPersistence.class);
	private static final Long CACHE_SIZE = 100000L;
	private final Stopwatch stopwatch;
	private long commitCounter = 0L;
	private static Joiner idJoiner = Joiner.on(":").skipNulls();

	public NDExNoTxMemoryPersistence() {
		ndexService = new OrientDBNoTxConnectionService();
		jdexIdSet = Sets.newHashSet();
		this.stopwatch = Stopwatch.createUnstarted();
	}

	// IBaseTerm cache
	private RemovalListener<Long, IBaseTerm> baseTermListener = new RemovalListener<Long, IBaseTerm>() {

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

	// IFunctionTerm cache
	private RemovalListener<Long, IFunctionTerm> functionTermListener = new RemovalListener<Long, IFunctionTerm>() {

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
	private RemovalListener<Long, INamespace> namespaceListener = new RemovalListener<Long, INamespace>() {

		public void onRemoval(RemovalNotification<Long, INamespace> removal) {
			logger.info("INamespace removed from cache key= "
					+ removal.getKey().toString() + " "
					+ removal.getCause().toString());

		}

	};

	private LoadingCache<Long, INamespace> namespaceCache = CacheBuilder
			.newBuilder().maximumSize(CACHE_SIZE)
			.expireAfterAccess(240L, TimeUnit.MINUTES)
			.removalListener(namespaceListener)
			.build(new CacheLoader<Long, INamespace>() {
				@Override
				public INamespace load(Long key) throws Exception {
					return ndexService._orientDbGraph.addVertex(
							"class:namespace", INamespace.class);
				}

			});

	// ICitation cache
	private RemovalListener<Long, ICitation> citationListener = new RemovalListener<Long, ICitation>() {

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
	public INamespace findNamespaceByPrefix(String prefix) {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(prefix),
				"A namespace prefix is required");
		String namespaceIdentifier = idJoiner.join("NAMESPACE", prefix);
		Preconditions.checkArgument(
				!NdexIdentifierCache.INSTANCE.isNovelIdentifier(namespaceIdentifier),
				"The namespace identifier " + namespaceIdentifier + " is not registered");
		try {
			Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache()
					.get(namespaceIdentifier);
			INamespace ns = this.namespaceCache.getIfPresent(jdexId);
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
	
	@Override
	public IReifiedEdgeTerm findOrCreateReifiedEdgeTerm(Long jdexId) throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return reifiedEdgeTermCache.get(jdexId);
	}

	public INamespace findOrCreateINamespace(Long jdexId)
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


	public INetwork getCurrentNetwork() {
		return this.network;
	}
	
	public INetwork createNetwork(){
		this.network = ndexService._orientDbGraph.addVertex(
				"class:network", INetwork.class);
		return this.network;
	}

	/*
	 * find the ITerm (either Base or Function) by jdex id
	 */

	public ITerm findChildITerm(Long jdexId) throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid JDExId is required");
		return Objects.firstNonNull(
				(ITerm) this.baseTermCache.getIfPresent(jdexId),
				(ITerm) this.functionTermCache.getIfPresent(jdexId));

	}

	public IUser getCurrentUser() {
		if (null == this.user) {
			this.user = ndexService._orientDbGraph.addVertex("class:user",
					IUser.class);
		}
		return this.user;
	}

	public INetworkMembership createNetworkMembership() {

		return ndexService._orientDbGraph.addVertex("class:networkMembership",
				INetworkMembership.class);
	}

	/*
	 * Returns a collection of IUsers based on search criteria
	 */

	public SearchResult<IUser> findUsers(SearchParameters searchParameters)
			throws NdexException {
		if (searchParameters.getSearchString() == null
				|| searchParameters.getSearchString().isEmpty())
			throw new ValidationException("No search string was specified.");
		else
			searchParameters.setSearchString(searchParameters.getSearchString()
					.toUpperCase().trim());

		final List<IUser> foundUsers = Lists.newArrayList();
		final SearchResult<IUser> result = new SearchResult<IUser>();
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

			List<ODocument> userDocumentList = ndexService._orientDbGraph
					.getBaseGraph().getRawGraph()
					.query(new OSQLSynchQuery<ODocument>(query));

			for (final ODocument document : userDocumentList)
				foundUsers.add(ndexService._orientDbGraph.getVertex(document,
						IUser.class));

			result.setResults(foundUsers);

			return result;
		} catch (Exception e) {
			ndexService._orientDbGraph.getBaseGraph().rollback();
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
		try {
			// ndexService._orientDbGraph.getBaseGraph().rollback();
			this.deleteNetwork();
			System.out.println("Deleting network in order to rollback in response to error");
		} finally {
			ndexService.teardownDatabase();
			System.out.println("Connection to orientdb database has been closed");
		}
	}

	/*
	 * public method to persist INetwork to the orientdb database using cache
	 * contents.
	 */

	public void persistNetwork() {
		try {
			network.setNdexEdgeCount((int) this.edgeCache.size());
			network.setNdexNodeCount((int) this.nodeCache.size());
			network.setIsComplete(true);
			System.out.println("The new network " + network.getName()
					+ " is complete");
		} catch (Exception e) {
			System.out.println("unexpected error in persist network...");
			e.printStackTrace();
		} finally {
			ndexService.teardownDatabase();
			System.out
					.println("Connection to orientdb database has been closed");
		}
	}


	public void networkProgressLogCheck() throws NdexException {
		commitCounter++;
		if (commitCounter % 1000 == 0) {
			logger.info("Checkpoint: Number of edges " + this.edgeCache.size());
		}

	}

	@Override
	public void deleteNetwork() {
		// TODO Implement deletion of network
		System.out
		.println("deleteNetwork called. Not yet implemented");
		
	}



}
