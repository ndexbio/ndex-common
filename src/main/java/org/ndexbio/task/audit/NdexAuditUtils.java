package org.ndexbio.task.audit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ndexbio.task.audit.network.NetworkIdentifier;


/*
 * Represents a collection of static utility methods 
 * to support network operations auditing
 */
public class NdexAuditUtils {
	
	private static final Log logger = LogFactory
			.getLog(NdexAuditUtils.class);

	public static enum AuditOperation {
		NETWORK_IMPORT, NETWORK_EXPORT, NETWORK_COPY, NETWORK_SUBSET
	};
	
	private static final String[] networkMetrics = {"edge count", "function term count",
		"base term count", "citation count", "reifiied edge terms", "node count", "support count"
	};
	
	public static List<String> getNetworkMetricsList = Arrays.asList(networkMetrics);
	
	public static NetworkIdentifier generateNetworkIdentifier( String networkName,  
			URI networkURI){
		UUID uuid = UUID.randomUUID();
		return new NetworkIdentifier(uuid, networkName, networkURI);
	}
	// use URI for localhost
	public static Optional<NetworkIdentifier> generateNetworkIdentifier( String networkName){
		
		 NetworkIdentifier networkId = null;
		try {
			URI networkURI  = new URI("http://localhost:8080/ndexbio/");
			UUID uuid = UUID.randomUUID();
			networkId=  new NetworkIdentifier(uuid, networkName, networkURI);
		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return Optional.of(networkId);
	}

}
