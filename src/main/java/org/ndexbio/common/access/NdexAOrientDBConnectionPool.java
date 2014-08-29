package org.ndexbio.common.access;

import java.util.logging.Logger;

import org.ndexbio.common.helpers.Configuration;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class NdexAOrientDBConnectionPool {
	
	private static NdexAOrientDBConnectionPool INSTANCE = null;

	private static int poolSize = 1000;
	private static final String dbURLPropName  = "OrientDB-URL";
	private static final String dbUserPropName = "OrientDB-Username";
	
	private ODatabaseDocumentPool pool;
	
	
	private static final Logger logger = Logger
			.getLogger(NdexAOrientDBConnectionPool.class.getName());

	private NdexAOrientDBConnectionPool() {
          
		pool = new ODatabaseDocumentPool(	
		     Configuration.getInstance().getProperty(dbURLPropName),
             Configuration.getInstance().getProperty(dbUserPropName),
             Configuration.getInstance().getProperty("OrientDB-Password"));
	    pool.setup(1,poolSize);
	    
	    //TODO: check if we need to set a timeout value for it. Most likely not.
	    //OGlobalConfiguration.CLIENT_CONNECT_POOL_WAIT_TIMEOUT.setValue("3600000");
	    
	    logger.info("Connection pool to " 
	    		 + Configuration.getInstance().getProperty(dbUserPropName) +
	    		 "@" + Configuration.getInstance().getProperty(dbURLPropName) + " created.");
	}
	
	public static synchronized NdexAOrientDBConnectionPool getInstance() {
	      if(INSTANCE == null) {
	         INSTANCE = new NdexAOrientDBConnectionPool();
	      }
	      return INSTANCE;
	}

	public ODatabaseDocumentTx acquire() {
		ODatabaseDocumentTx conn = pool.acquire();
		
	    logger.info("Connection to " 
	    		 + Configuration.getInstance().getProperty(dbUserPropName) +
	    		 "@" + Configuration.getInstance().getProperty(dbURLPropName) + " acquired.");
	    return conn;
	}
   	
   public static synchronized void close() { 	
	 
       if ( INSTANCE != null) {	   
	     INSTANCE.pool.close();
	     INSTANCE=null;
       }
       logger.info("Connection pool to " 
	    		 + Configuration.getInstance().getProperty(dbUserPropName) +
	    		 "@" + Configuration.getInstance().getProperty(dbURLPropName) + " closed.");
   }
   
   
}
