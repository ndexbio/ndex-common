package org.ndexbio.orientdb.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * Single to return an implementation of the NDExPersitenceService interface
 * For the Beta version this will be limited to an instance of the NDExMemoryPersistance class
 */
public enum NDExPersistenceServiceFactory {
	INSTANCE;
	private static final Logger logger = LoggerFactory.getLogger(NDExPersistenceServiceFactory.class);
	public NDExPersistenceService getNDExPersistenceService() {
		logger.info("NDExPersistenceServiceFactory  invoked...");
		return  NDExMemoryPersistence.INSTANCE;
	}
}
