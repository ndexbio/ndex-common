package org.ndexbio.common.persistence;

import java.util.concurrent.ExecutionException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.*;
import org.ndexbio.common.models.object.Network;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.SearchResult;

/*
 * public interface representing all interactions with the underlying persistence implementation
 * this may be either direct interaction with a graph database or an in-memory domain
 * object caches
 */

public interface NDExPersistenceService {
	
	// find or create instances of domain objects
	public ITerm findChildITerm( Long jdexId) throws ExecutionException;
	public IBaseTerm findOrCreateIBaseTerm( Long jdexId) throws ExecutionException;
	public IFunctionTerm findOrCreateIFunctionTerm( Long jdexId) throws ExecutionException;
	public INamespace findOrCreateINamespace( Long jdexId) throws ExecutionException;
	public ICitation findOrCreateICitation( Long jdexId) throws ExecutionException;
	public IEdge findOrCreateIEdge( Long jdexId) throws ExecutionException;
	public INode findOrCreateINode( Long jdexId) throws ExecutionException;
	public ISupport findOrCreateISupport( Long jdexId) throws ExecutionException;
	public void networkProgressLogCheck() throws NdexException;
	
	public void persistNetwork();
	
	public boolean isEntityPersisted(Long jdexId);
	
	// Convenience methods
	// find an INamespace by its XBEL prefix
	public INamespace findNamespaceByPrefix(String prefix);
	public INetwork createNetwork() throws Exception;
	public INetwork getCurrentNetwork();
	public IUser getCurrentUser();
	public INetworkMembership  createNetworkMembership();
	
	public SearchResult<IUser> findUsers(SearchParameters searchParameters) throws NdexException;
	public void abortTransaction();
	public void deleteNetwork();
	
	
	
	
}
