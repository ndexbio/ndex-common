package org.ndexbio.common.persistence;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.persistence.orientdb.NDExNoTxMemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * Single to return an implementation of the NDExPersitenceService interface
 * For the Beta version this will be limited to an instance of the NDExMemoryPersistance class
 */
public enum NDExPersistenceServiceFactory {
	INSTANCE;
	private static final Logger logger = LoggerFactory.getLogger(NDExPersistenceServiceFactory.class);
	public NDExNoTxMemoryPersistence getNDExPersistenceService() throws NdexException {
		logger.info("NDExPersistenceServiceFactory  invoked...");
		return  new NDExNoTxMemoryPersistence();
	}
}
