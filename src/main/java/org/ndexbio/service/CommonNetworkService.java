package org.ndexbio.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.*;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.SearchResult;
import org.ndexbio.common.models.object.privilege.Permissions;
import org.ndexbio.common.persistence.NDExPersistenceService;
import org.ndexbio.common.persistence.NDExPersistenceServiceFactory;
import org.ndexbio.common.persistence.orientdb.NDExNoTxMemoryPersistence;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.NdexProperty;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.VisibilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

// TODO: THIS DOES NOT BELONG HERE. THIS CLASS IS APPLICATION-SPECIFIC TO THE SERVICE
//       AND THEREFORE SHOULD RESIDE IN THAT PACKAGE. COMMON SHOULD ONLY CONTAIN
//       CLASSES AND FUNCTIONALITY THAT ARE COMMON TO MULTIPLE PROJECTS


/*
 * abstract class containing functionality common to all network service classes
 */

public abstract class CommonNetworkService {

	protected NDExNoTxMemoryPersistence persistenceService;
	protected final Logger logger;

	protected CommonNetworkService() throws NdexException {
		this.persistenceService = NDExPersistenceServiceFactory.INSTANCE
				.getNDExPersistenceService();
		logger = LoggerFactory.getLogger(this.getClass());
		logger.info("connection service instantiated");
	}
	
	/*
	 * A consistent method for creating a network, registering ownership, and
	 * assigning it a title
	 * throws NdexException if supplied owner name is not in the database
	 */
	public final Network createNewNetwork(String ownerName, String networkTitle) throws Exception {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(ownerName),
				"A network owner name is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkTitle),
				"A network title is required");
		Network network = this.persistenceService.createNetwork();
		network.setProperties(new ArrayList<NdexProperty>());
		network.setPresentationProperties(new ArrayList<NdexProperty>());
		// find the network owner in the database
		User networkOwner =  this.persistenceService.findUserByAccountName(ownerName);
		if( null == networkOwner){
			logger.error("User " +ownerName +" is not registered in the database/");
			throw new NdexException("User " +ownerName +" is not registered in the database");
		}
				
		Membership membership = createNewMember(ownerName, network.getExternalId());
		network.setVisibility(VisibilityType.PUBLIC);
		network.setIsLocked(false);
		network.setIsComplete(false);
		network.getMembers().add(membership);
		network.setName(networkTitle);
		logger.info("A new NDex network titled: " +network.getName()
				+" owned by " +ownerName +" has been created");
		return network;
	}

	/*
 public INetwork createNetwork(Network newNetwork) throws Exception {
                Preconditions.checkArgument(null != newNetwork,
                                "A network model object is required");
                Preconditions.checkArgument(null != newNetwork.getMembers()
                                && newNetwork.getMembers().size() > 0,
                                "The network to create has no members specified.");

                try {

                        final Membership newNetworkMembership = newNetwork.getMembers()
                                        .get(0);
                        final ORID userRid = IdConverter.toRid(newNetworkMembership
                                        .getResourceId());

                        final IUser networkOwner = ndexService._orientDbGraph.getVertex(
                                        userRid, IUser.class);
                        if (networkOwner == null)
                                throw new ObjectNotFoundException("User",
                                                newNetworkMembership.getResourceId());

                        final INetwork network = ndexService._orientDbGraph.addVertex(
                                        "class:network", INetwork.class);

                        final INetworkMembership membership = ndexService._orientDbGraph
                                        .addVertex("class:networkMembership",
                                                        INetworkMembership.class);
                        membership.setPermissions(Permissions.ADMIN);
                        membership.setMember(networkOwner);
                        membership.setNetwork(network);
                        networkOwner.addNetwork(membership);
                        network.addMember(membership);
                        network.setIsPublic(false);
            network.getMetadata().put("Format", newNetwork.getMetadata().get("Format"));
            network.getMetadata().put("Source", newNetwork.getMetadata().get("Source"));
                        network.setName(newNetwork.getName());

                        this.network = network; // keep a copy in this repository
                        return network;
                } catch (Exception e) {
                        ndexService._orientDbGraph.getBaseGraph().rollback();
                        throw e;
                }
        }
	 */
	
	public final SearchResult<IUser> findUsers(SearchParameters searchParameters)
			throws NdexException {
		return this.persistenceService.findUsers(searchParameters);
	}	
	
	public final Network getCurrentNetwork() {
		return this.persistenceService.getCurrentNetwork();
	}

	public final IUser createNewUser(String username) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(username));
		IUser user = this.persistenceService.getCurrentUser();
		user.setUsername(username);
		return user;
	}

	public final Membership createNewMember(String accountName, UUID networkUUID) {
		return this.persistenceService.createNetworkMembership(accountName, networkUUID);
	}

	public final void persistNewNetwork() {
		this.persistenceService.persistNetwork();
	}

	public final void rollbackCurrentTransaction() {
		this.persistenceService.abortTransaction();
	}
	
	
	
	/*
	 * expose persistence service to subclasses to provide access to database 
	 * operations defined in service interface
	 */
	public final NDExNoTxMemoryPersistence getPersistenceService() {
		return this.persistenceService;
	}
	
/*	public void setFormat(String format) {
	    if (persistenceService.getCurrentNetwork().getMetadata() != null)
	        persistenceService.getCurrentNetwork().getMetadata().put("Format", format);	
	}
*/	
	public void setDescription(String description) {
		persistenceService.getCurrentNetwork().setDescription(description);	
		
	}
	
/*	public void setSource(String source) {
		if (persistenceService.getCurrentNetwork().getMetadata() != null)
	        persistenceService.getCurrentNetwork().getMetadata().put("Source", source);			
	} */
	
}
