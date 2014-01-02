package org.ndexbio.orientdb.persistence;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.ndexbio.common.cache.NdexIdentifierCache;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.exceptions.ValidationException;
import org.ndexbio.orientdb.domain.*;
import org.ndexbio.rest.models.Membership;
import org.ndexbio.rest.models.Network;
import org.ndexbio.rest.models.SearchParameters;
import org.ndexbio.rest.models.SearchResult;
import org.ndexbio.service.helpers.RidConverter;

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

public class NDExMemoryPersistence implements NDExPersistenceService {

	 private OrientDBConnectionService ndexService;
	 private Set<Long> jdexIdSet ;
	 private INetwork network;
	 private IUser user;
	 private static final Logger logger = LoggerFactory.getLogger(NDExMemoryPersistence.class);
	 private static final Long CACHE_SIZE = 100000L;
	 private final Stopwatch stopwatch;
	 private long commitCounter  =0L;
	 
	 public NDExMemoryPersistence() {
		 ndexService = new OrientDBConnectionService();
		   jdexIdSet = Sets.newHashSet();
		   this.stopwatch = Stopwatch.createUnstarted();
	 }
	 
	 //IBaseTerm cache
	 private RemovalListener<Long,IBaseTerm> baseTermListener = new RemovalListener<Long, IBaseTerm>() {

		public void onRemoval(RemovalNotification<Long, IBaseTerm> removal) {
			logger.info("IBaseTerm removed from cache key= " +removal.getKey().toString()
					+" " +removal.getCause().toString());
			
		}
		 
	 };
	 private LoadingCache<Long, IBaseTerm> baseTermCache = CacheBuilder.newBuilder()
			 .maximumSize(CACHE_SIZE)
			 .expireAfterAccess(240L, TimeUnit.MINUTES)
			 .removalListener(baseTermListener)
			 .build(new CacheLoader<Long,IBaseTerm>() {
				@Override
				public IBaseTerm load(Long key) throws Exception {
					return ndexService._orientDbGraph.addVertex("class:baseTerm", IBaseTerm.class);
					
				}
			 });
	 
	 //IFunctionTerm cache
	 private RemovalListener<Long,IFunctionTerm> functionTermListener = new RemovalListener<Long, IFunctionTerm>() {

			public void onRemoval(RemovalNotification<Long, IFunctionTerm> removal) {
				logger.info("IBaseTerm removed from cache key= " +removal.getKey().toString()
						+" " +removal.getCause().toString());
				
			}
			 
		 };
	 private LoadingCache<Long, IFunctionTerm> functionTermCache = CacheBuilder.newBuilder()
			 .maximumSize(CACHE_SIZE)
			 .expireAfterAccess(240L, TimeUnit.MINUTES)
			 .removalListener(functionTermListener)
			 .build(new CacheLoader<Long,IFunctionTerm>() {
				@Override
				public IFunctionTerm load(Long key) throws Exception {
					return ndexService._orientDbGraph.addVertex("class:functionTerm", IFunctionTerm.class);
				}
				 
			 });
	 
	 //INamespace cache
	 private RemovalListener<Long,INamespace> namespaceListener = new RemovalListener<Long, INamespace>() {

			public void onRemoval(RemovalNotification<Long, INamespace> removal) {
				logger.info("INamespace removed from cache key= " +removal.getKey().toString()
						+" " +removal.getCause().toString());
				
			}
			 
		 };
		 
	 private LoadingCache<Long, INamespace> namespaceCache = CacheBuilder.newBuilder()
			 .maximumSize(CACHE_SIZE)
			 .expireAfterAccess(240L, TimeUnit.MINUTES)
			 .removalListener(namespaceListener)
			 .build(new CacheLoader<Long,INamespace>() {
				@Override
				public INamespace load(Long key) throws Exception {
					return  ndexService._orientDbGraph.addVertex("class:namespace", INamespace.class);
				}
				 
			 });
	 
	 
	 //ICitation cache
	 private RemovalListener<Long,ICitation> citationListener = new RemovalListener<Long, ICitation>() {

			public void onRemoval(RemovalNotification<Long, ICitation> removal) {
				logger.info("ICitiation removed from cache key= " +removal.getKey().toString()
						+" " +removal.getCause().toString());
				
			}
			 
		 };
	 private LoadingCache<Long, ICitation> citationCache = CacheBuilder.newBuilder()
			 .maximumSize(CACHE_SIZE)
			 .expireAfterAccess(240L, TimeUnit.MINUTES)
			 .removalListener(citationListener)
			 .build(new CacheLoader<Long,ICitation>() {
				@Override
				public ICitation load(Long key) throws Exception {
					return  ndexService._orientDbGraph.addVertex("class:citation", ICitation.class);
				}
				 
			 });
	 
	//IEdge cache
	 private RemovalListener<Long,IEdge> edgeListener = new RemovalListener<Long, IEdge>() {

			public void onRemoval(RemovalNotification<Long, IEdge> removal) {
				logger.info("IEdge removed from cache key= " +removal.getKey().toString()
						+" " +removal.getCause().toString());
				
			}
			 
		 };
		 private LoadingCache<Long, IEdge> edgeCache = CacheBuilder.newBuilder()
				 .maximumSize(CACHE_SIZE)
				 .expireAfterAccess(240L, TimeUnit.MINUTES)
				 .removalListener(edgeListener)
				 .build(new CacheLoader<Long,IEdge>() {
					@Override
					public IEdge load(Long key) throws Exception {
						return  ndexService._orientDbGraph.addVertex("class:edge", IEdge.class);
					}
					 
				 });
		 
		//INode cache
		 private RemovalListener<Long,INode> nodeListener = new RemovalListener<Long, INode>() {

				public void onRemoval(RemovalNotification<Long, INode> removal) {
					logger.info("INode removed from cache key= " +removal.getKey().toString()
							+" " +removal.getCause().toString());
					
				}
				 
			 };
		 private LoadingCache<Long, INode> nodeCache = CacheBuilder.newBuilder()
				 .maximumSize(CACHE_SIZE)
				 .expireAfterAccess(240L, TimeUnit.MINUTES)
				 .removalListener(nodeListener)
				 .build(new CacheLoader<Long,INode>() {
					@Override
					public INode load(Long key) throws Exception {
						return  ndexService._orientDbGraph.addVertex("class:node", INode.class);
					}
					 
				 });
		 
		//ISupport cache
		 private RemovalListener<Long,ISupport> supportListener = new RemovalListener<Long, ISupport>() {

				public void onRemoval(RemovalNotification<Long, ISupport> removal) {
					logger.info("ISupport removed from cache key= " +removal.getKey().toString()
							+" " +removal.getCause().toString());
					
				}
				 
			 };
		 private LoadingCache<Long, ISupport> supportCache = CacheBuilder.newBuilder()
				 .maximumSize(CACHE_SIZE)
				 .expireAfterAccess(240L, TimeUnit.MINUTES)
				
				 .build(new CacheLoader<Long,ISupport>() {
					@Override
					public ISupport load(Long key) throws Exception {
						return  ndexService._orientDbGraph.addVertex("class:support", ISupport.class);
					}
					 
				 });
	 
	 

	public boolean isEntityPersisted(Long jdexId) {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() >0,
				"A valid JDExId is required");
		return (this.jdexIdSet.contains(jdexId));
		
	}
	
	
	
	// To find a namespace by its prefix, first try to find a jdexid by looking up the prefix in the identifier cache.
	// If a jdexid is found, then lookup the INamespace by jdexid in the namespaceCache and return it.
	public INamespace findNamespaceByPrefix(String prefix) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(prefix), "A namespace prefix is required");
		Preconditions.checkArgument(!NdexIdentifierCache.INSTANCE.isNovelIdentifier(prefix),
				"The namespace prefix " + prefix +" is not registered");
		try {
			Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache().get(prefix);
			INamespace ns = this.namespaceCache.getIfPresent(jdexId);
			return ns;
		} catch (ExecutionException e) {
			
			e.printStackTrace();
		}
		return null;
	}
	
	// find 


	public IBaseTerm findOrCreateIBaseTerm(Long jdexId)
			throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() >0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return baseTermCache.get(jdexId);
	}


	
	public IFunctionTerm findOrCreateIFunctionTerm(Long jdexId)
			throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() >0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return functionTermCache.get(jdexId);
	}


	
	public INamespace findOrCreateINamespace(Long jdexId)
			throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() >0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return namespaceCache.get(jdexId);
	}


	
	public ICitation findOrCreateICitation(Long jdexId)
			throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() >0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return citationCache.get(jdexId);
	}

	
	public IEdge findOrCreateIEdge(Long jdexId) throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() >0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return edgeCache.get(jdexId);
	}


	
	public INode findOrCreateINode(Long jdexId) throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() >0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return nodeCache.get(jdexId);
	}


	
	public ISupport findOrCreateISupport(Long jdexId) throws ExecutionException {
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() >0,
				"A valid JDExId is required");
		this.jdexIdSet.add(jdexId);
		return supportCache.get(jdexId);
	}


	
	 public INetwork createNetwork(Network newNetwork) throws Exception
	    {
	        Preconditions.checkArgument(null != newNetwork,"A network model object is required");
	        Preconditions.checkArgument(null != newNetwork.getMembers() && 
	        		newNetwork.getMembers().size() > 0,
	        		"The network to create has no members specified.");
	        
	        try
	        {
	           
	            final Membership newNetworkMembership = newNetwork.getMembers().get(0);
	            final ORID userRid = RidConverter.convertToRid(newNetworkMembership.getResourceId());

	            final IUser networkOwner =  ndexService._orientDbGraph.getVertex(userRid, IUser.class);
	            if (networkOwner == null)
	                throw new ObjectNotFoundException("User", newNetworkMembership.getResourceId());

	            final INetwork network =  ndexService._orientDbGraph.addVertex("class:network", INetwork.class);

	            final INetworkMembership membership =  ndexService._orientDbGraph.addVertex("class:networkMembership", INetworkMembership.class);
	            membership.setPermissions(Permissions.ADMIN);
	            membership.setMember(networkOwner);
	            membership.setNetwork(network);
	            networkOwner.addNetwork(membership);
	            network.addMember(membership);
	            network.setIsPublic(false);
	            network.setFormat(newNetwork.getFormat());
	            network.setSource(newNetwork.getSource());
	            network.setTitle(newNetwork.getTitle());
	            
	           this.network = network;  // keep a copy in this repository
	            return network;
	        }catch (Exception e)
	        {
	        	 ndexService._orientDbGraph.getBaseGraph().rollback();
	            throw e;
	        }
	    }
	    
		public INetwork getCurrentNetwork() {
	    	if (null == this.network){
	    		this.network = ndexService._orientDbGraph.addVertex("class:network", INetwork.class);
	    	}
	    	
	    	return this.network;}

	    /*
	     * find the ITerm (either Base or Function) by jdex id
	     */
		
		public ITerm findChildITerm(Long jdexId) throws ExecutionException {
			Preconditions.checkArgument(null != jdexId && jdexId.longValue() >0,
					"A valid JDExId is required");
			return Objects.firstNonNull((ITerm) this.baseTermCache.getIfPresent(jdexId),
					(ITerm) this.functionTermCache.getIfPresent(jdexId));
	
		}


		
		public IUser getCurrentUser() {
			if (null == this.user) {
				this.user = ndexService._orientDbGraph.addVertex("class:user", IUser.class);
			}
			return this.user;
		}


		
		public INetworkMembership createNetworkMembership() {
			
			return ndexService._orientDbGraph.addVertex("class:networkMembership", INetworkMembership.class);
		}
	
		/*
		 * Returns a collection of IUsers based on search criteria
		 */
		
		 public SearchResult<IUser> findUsers(SearchParameters searchParameters) throws NdexException
		    {
		        if (searchParameters.getSearchString() == null || searchParameters.getSearchString().isEmpty())
		            throw new ValidationException("No search string was specified.");
		        else
		            searchParameters.setSearchString(searchParameters.getSearchString().toUpperCase().trim());
		        
		        final List<IUser> foundUsers = Lists.newArrayList();
		        final SearchResult<IUser> result = new SearchResult<IUser>();
		        result.setResults(foundUsers);
		        
		        //TODO: Remove these, they're unnecessary
		        result.setPageSize(searchParameters.getTop());
		        result.setSkip(searchParameters.getSkip());
		        
		        final int startIndex = searchParameters.getSkip() * searchParameters.getTop();

		        String whereClause = " where username.toUpperCase() like '%" + searchParameters.getSearchString()
		                    + "%' OR lastName.toUpperCase() like '%" + searchParameters.getSearchString()
		                    + "%' OR firstName.toUpperCase() like '%" + searchParameters.getSearchString() + "%'";

		        final String query = "select from User " + whereClause
		                + " order by creation_date desc skip " + startIndex + " limit " + searchParameters.getTop();
		        
		        try
		        {
		           
		            
		            List<ODocument> userDocumentList = ndexService._orientDbGraph
		                .getBaseGraph()
		                .getRawGraph()
		                .query(new OSQLSynchQuery<ODocument>(query));
		            
		            for (final ODocument document : userDocumentList)
		                foundUsers.add(ndexService._orientDbGraph.getVertex(document, IUser.class));
		    
		            result.setResults(foundUsers);
		            
		            return result;
		        }
		        catch (Exception e)
		        {
		        	ndexService._orientDbGraph.getBaseGraph().rollback(); 
		            throw new NdexException(e.getMessage());
		        }
		        
		    }
		/*
		 * public method to allow xbel parsing components to rollback the transaction and 
		 * close the database connection if they encounter an error situation
		 */

		
		public void abortTransaction() {
			System.out.println(this.getClass().getName() +".abortTransaction has been invoked.");
			try {
				ndexService._orientDbGraph.getBaseGraph().rollback();
				System.out.println("The current orientdb transaction has been rolled back");
			} finally {
				ndexService.teardownDatabase();
				System.out.println("Connection to orientdb database has been closed");
			}
		}


	
		/*
		 * public method to persist INetwork to the orientdb database
		 * using cache contents.
		 */

		
		public void persistNetwork() {
			try {

				ndexService._orientDbGraph.getBaseGraph().commit();
				System.out.println("The new network " +network.getTitle() 
						+" has been committed");
			} catch (Exception e) {
				ndexService._orientDbGraph.getBaseGraph().rollback();
				System.out.println("The current orientdb transaction has been rolled back");
				e.printStackTrace();
			} finally {
				ndexService.teardownDatabase();
				System.out.println("Connection to orientdb database has been closed");
			}
		}
		
		private void addISupports() {
			for(ISupport support : this.supportCache.asMap().values()){
				this.network.addSupport(support);
			}
			//this.supportCache.invalidateAll();
		}
		
		private void addICitations() {
			for(ICitation citation : this.citationCache.asMap().values()){
				this.network.addCitation(citation);
			}
			//this.citationCache.invalidateAll();
		}
		
		private void addIEdges() {
			for (IEdge edge : this.edgeCache.asMap().values()) {
				this.network.addNdexEdge(edge);
			}
			this.network.setNdexEdgeCount(this.edgeCache.asMap().size());
			//this.edgeCache.invalidateAll();
		}
		
		
		
		private void addINodes() {
			for (INode in : this.nodeCache.asMap().values()){
				this.network.addNdexNode(in);
			}
			this.network.setNdexNodeCount(this.nodeCache.asMap().size());
			//this.nodeCache.invalidateAll();
		}
		
		private void addINamespaces() {
			for(INamespace ns : this.namespaceCache.asMap().values()){
				this.network.addNamespace(ns);
			}
			
			// clear namespace cache
			//this.namespaceCache.invalidateAll();
		}
		
		private void addITerms() {		
			for (IBaseTerm bt : this.baseTermCache.asMap().values() ){
				this.network.addTerm(bt);
			}
			//this.baseTermCache.invalidateAll();
			for(IFunctionTerm ft : this.functionTermCache.asMap().values()){
				this.network.addTerm(ft);
			}	
			//this.functionTermCache.invalidateAll();
		}



		@Override
		/*
		 * mod 20Dec2013
		 * time how long commits take
		 */
		public void commitCurrentNetwork() throws NdexException {
			commitCounter++;
			if(null != this.getCurrentNetwork() ) {
				if (commitCounter %10000 == 0) {
					this.stopwatch.start();
					ndexService._orientDbGraph.getBaseGraph().commit();
					this.stopwatch.stop();
					
					logger.info("Network commit required "
							+ this.stopwatch.elapsed(TimeUnit.MILLISECONDS)
							+ " milliseconds. Number of edges " +this.edgeCache.size());
					this.stopwatch.reset();
				}
			} else {
				throw new NdexException("Attempt to commit non-existent network.");
			}
			
		}



		@Override
		public void deleteNetwork() {
			// TODO Auto-generated method stub
			
		}


		


		



	






}
