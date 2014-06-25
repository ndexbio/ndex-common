package org.ndexbio.common.models.object;

import java.util.List;

import org.ndexbio.common.models.object.network.BaseTerm;
import org.ndexbio.common.models.object.network.Citation;
import org.ndexbio.common.models.object.network.Edge;
import org.ndexbio.common.models.object.network.Namespace;
import org.ndexbio.common.models.object.network.Network;

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
