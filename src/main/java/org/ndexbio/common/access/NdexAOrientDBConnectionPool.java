package org.ndexbio.common.access;

import java.util.logging.Logger;

import org.ndexbio.model.exceptions.NdexException;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class NdexAOrientDBConnectionPool {
	
	private static NdexAOrientDBConnectionPool INSTANCE = null;

	
	private OrientGraphFactory pool;
	
	
	private static final Logger logger = Logger
			.getLogger(NdexAOrientDBConnectionPool.class.getName());

	private NdexAOrientDBConnectionPool(String dbURL, String dbUserName, String dbPassword, int size) {

		pool = new OrientGraphFactory(dbURL, dbUserName, dbPassword).setupPool(1,size);
	    
	    logger.info("Connection pool to " + dbUserName + "@" + dbURL + " created.");
	}
	
	public static synchronized void createOrientDBConnectionPool (String dbURL, String dbUserName,
				String dbPassword, int size) {
	      if(INSTANCE == null) {
		         INSTANCE = new NdexAOrientDBConnectionPool(dbURL, dbUserName, dbPassword, size);
	      }
	}
	
	public static synchronized NdexAOrientDBConnectionPool getInstance() throws NdexException {
	      if(INSTANCE == null) {
	         throw new NdexException ("Connection pool is not created yet.");
	      }
	      return INSTANCE;
	}

	public ODatabaseDocumentTx acquire() {
		ODatabaseDocumentTx conn = pool.getDatabase();
		
	    return conn;
	}
   	
   public static synchronized void close() { 	
	 
       if ( INSTANCE != null) {	   
	     INSTANCE.pool.close();
	     INSTANCE=null;
       }
       logger.info("Connection pool closed.");
   }
   
   
}
