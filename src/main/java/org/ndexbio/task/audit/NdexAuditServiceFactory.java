/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.task.audit;

import org.ndexbio.task.audit.network.NetworkOperationAuditService;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.logging.Log;

import org.apache.commons.logging.LogFactory;
/*
 * Represents a singleton implemented as as enum that is responsible for instantiating
 * a subclass of NdexAuditService
 * 
 */
public enum NdexAuditServiceFactory {
	INSTANCE;
	
	protected static final Log logger = LogFactory
			.getLog(NdexAuditServiceFactory.class);
	
	/*
	 * public method that returns an instance of NdexAuditService subclass based
	 * on the type of operation to be audited. The service instance is return encapsulated
	 * within an Optional object to avoid returning null if an unsupported operation is
	 * specified
	 */
		
	public Optional<NdexAuditService> getAuditServiceByOperation(String  networkName,
				NdexAuditUtils.AuditOperation oper){
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkName),
				"A network name is required");
		Preconditions.checkArgument(null != oper, "A valid operation is required");
		NdexAuditService service = null;
		
		if (oper.toString().startsWith("NETWORK")){
			service = new NetworkOperationAuditService(networkName, oper);
			logger.info("An instannce of " +service.getClass().getSimpleName() +" has been instantiated "
					+"for operation " +oper.toString());
		} else {
			logger.error("The operation " +oper.toString() + " is not supported");
		}
		return Optional.of(service);		
	}

}
