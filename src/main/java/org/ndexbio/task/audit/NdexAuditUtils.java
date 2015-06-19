/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
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
