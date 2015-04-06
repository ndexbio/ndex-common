package org.ndexbio.task.service;

import java.util.Collection;
import java.util.List;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;

/*
 * Represents a set of service operations to interact with NDEx model objects.
 * Implementations may utilize the REST service classes directly or through
 * HTTP-based invocation of NDEx RESTful operations
 * 
 */

public interface NdexTaskModelService { 
	public Network getNetworkById(String networkId);
	
	public Collection<Citation> getCitationsByNetworkId(String networkId);
	public Network getSubnetworkByCitationId(String networkId, Long citationId); 
	public Network getNoCitationSubnetwork(String networkId) throws NdexException;
	public Network getOrphanSupportNetwork(String netwokrID) throws NdexException;    
	
	public Iterable<Edge> getEdgesBySupportId(String supportId);
	public Iterable<Namespace> getNamespacesByNetworkId(String networkId);
	// internal & external annotations are persisted as namespaces
	public Iterable<Namespace> getInternalAnnotationsByNetworkId(String networkId);
	public Iterable<Namespace> getExternalAnnotationsByNetworkId(String networkId);
	public Collection<BaseTerm> getBaseTermsByNamespace(String userId, String namespace, String networkId);

}
