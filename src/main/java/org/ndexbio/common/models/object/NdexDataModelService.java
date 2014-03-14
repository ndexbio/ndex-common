package org.ndexbio.common.models.object;

/*
 * Represents a set of service operations to interact with NDEx model objects.
 * Implementations may utilize the REST service classes directly or through
 * HTTP-based invocation of NDEx RESTful operations
 * 
 */

public interface NdexDataModelService {
	public Network getNetworkById(String networkId);
	
	public Iterable<Citation> getCitationsByNetworkId(String networkId);
	public Network getSubnetworkByCitationId(String networkId, String citationId);
	public Iterable<Edge> getEdgesBySupportId(String supportId);
	public Iterable<Namespace> getNamespacesByNetworkId(String networkId);
	// internal & external annotations are persisted as namespaces
	public Iterable<Namespace> getInternalAnnotationsByNetworkId(String networkId);
	public Iterable<Namespace> getExternalAnnotationsByNetworkId(String networkId);

}
