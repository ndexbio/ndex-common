package org.ndexbio.common.persistence;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.IFunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.common.models.data.IReifiedEdgeTerm;
import org.ndexbio.common.models.data.ISupport;
import org.ndexbio.common.models.data.ITerm;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.SearchResult;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.network.Network;

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
	
	public Namespace findOrCreateNamespace(String URI, String prefix) throws Exception;
	public Namespace findOrCreateINamespace( Long jdexId) throws ExecutionException;
	public ICitation findOrCreateICitation( Long jdexId) throws ExecutionException;
	public IEdge findOrCreateIEdge( Long jdexId) throws ExecutionException;
	public INode findOrCreateINode( Long jdexId) throws ExecutionException;
	public ISupport findOrCreateISupport( Long jdexId) throws ExecutionException;
	public IReifiedEdgeTerm findOrCreateIReifiedEdgeTerm(Long jdexId) throws ExecutionException;
	public void networkProgressLogCheck() throws NdexException;
	
	public void persistNetwork();
	
	public boolean isEntityPersisted(Long jdexId);
	
	// Convenience methods
	// find an INamespace by its XBEL prefix
	public Namespace findNamespaceByPrefix(String prefix);
	public Network createNetwork() throws Exception;
	public Network getCurrentNetwork();
	public IUser getCurrentUser();
	public Membership  createNetworkMembership(String accountName, UUID networkUUID);
	
	
	public User findUserByAccountName(String accountName) throws NdexException;
	
	public SearchResult<IUser> findUsers(SearchParameters searchParameters) throws NdexException;
	public void abortTransaction();
	public void deleteNetwork();
	
	
	
	
	
}
