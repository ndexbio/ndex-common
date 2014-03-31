package org.ndexbio.common.models.object;

import java.util.List;

/*
 * Represents a set of service operations to interact with NDEx model objects.
 * Implementations may utilize the REST service classes directly or through
 * HTTP-based invocation of NDEx RESTful operations
 * 
 */

public interface NdexDataModelService {
	public Network getNetworkById(String networkId);
	
	public List<Citation> getCitationsByNetworkId(String networkId);
	public Network getSubnetworkByCitationId(String networkId, String citationId);
	public List<Edge> getEdgesBySupportId(String supportId);
	public List<Namespace> getNamespacesByNetworkId(String networkId);
	// internal & external annotations are persisted as namespaces
	public List<Namespace> getInternalAnnotationsByNetworkId(String networkId);
	public List<Namespace> getExternalAnnotationsByNetworkId(String networkId);
	public List<BaseTerm> getBaseTermsByNamespace(String namespace, String networkId);

}
