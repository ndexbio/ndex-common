package org.ndexbio.common.access;


import java.util.logging.Logger;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.orientdb.NdexSchemaManager;

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.dictionary.ODictionary;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class NdexDatabase {
	
	private static NdexDatabase INSTANCE = null;
	
	private OPartitionedDatabasePool pool;
	
	private ODatabaseDocumentTx ndexDatabase;  // this connection is used for transactions in this database object.
	
	private ODictionary dictionary;
	
	private static final String sequenceKey="NdexSeq";

	private static final String seqField= "f1";
	
	private int batchCounter;
	
	private long internalCounterBase;

	private static final int blockSize = 80;  
	
	private ODocument vdoc;
	
	static private String URIPrefix = null;
	
	private static final Logger logger = Logger
			.getLogger(NdexAOrientDBConnectionPool.class.getName());
	
	private static long currentId = System.currentTimeMillis(); 
	
	private NdexDatabase(String HostURI, String dbURL, String dbUserName,
			String dbPassword, int size) throws NdexException {
		
		pool = new OPartitionedDatabasePool(dbURL, dbUserName, dbPassword,size);
	    
	    logger.info("Connection pool to " + dbUserName + "@" + dbURL + " ("+ size + ") created.");

	    ndexDatabase = pool.acquire();
	    
		dictionary = ndexDatabase.getDictionary();
		
		NdexSchemaManager.INSTANCE.init(ndexDatabase);
		vdoc = (ODocument) dictionary.get(sequenceKey);
		if (vdoc == null ) {
			ndexDatabase.commit();
			internalCounterBase = 1;
			vdoc = new ODocument(seqField, internalCounterBase);  // + blockSize); // ids start with 1.
			vdoc = vdoc.save();
			dictionary.put(sequenceKey, vdoc);
			ndexDatabase.commit();	
		} 
		batchCounter=blockSize;
		
		URIPrefix = HostURI;
		
	}
	
	public static synchronized long getCommitId () {
		return currentId++;
	}
	
	public static synchronized NdexDatabase createNdexDatabase (String HostURI, String dbURL, String dbUserName,
			String dbPassword, int size) throws NdexException {
		if(INSTANCE == null) {
	         INSTANCE = new NdexDatabase(HostURI, dbURL, dbUserName, dbPassword, size);
	         return INSTANCE;
		} 
		
		throw new NdexException("Database has arlready been  opened.");
		
	}

	
	
	public static synchronized NdexDatabase getInstance() throws NdexException {
	      if(INSTANCE == null) {
	         throw new NdexException ("Connection pool is not created yet.");
	      }
	      return INSTANCE;
	}

	
	static public String getURIPrefix ()  {
		return URIPrefix;

	}
	
    public synchronized long  getNextId() {
    	
    	if ( batchCounter == blockSize) {
    		vdoc.reload();
        	internalCounterBase = vdoc.field(seqField);
    	    batchCounter = 0 ;
            vdoc = vdoc.field(seqField, internalCounterBase + blockSize).save();
            dictionary.put(sequenceKey, vdoc);
        	commit();
       // 	System.out.println("New batch in id sequence:" + internalCounterBase );
    	}
    	long rvalue = internalCounterBase + batchCounter;
    	batchCounter++;
        
    	return rvalue;
    }
    
    public synchronized void resetIdCounter() {
    	vdoc.field(seqField,0);
    }
    
    public static synchronized void close () {
    	if ( INSTANCE != null ) {
    		logger.info("Closing database.");
    		INSTANCE.ndexDatabase.commit();
    		INSTANCE.ndexDatabase.close();
    		INSTANCE.pool.close();
    		INSTANCE.pool = null;
    		INSTANCE = null;
    		logger.info("Database closed.");
    	} else 
    		logger.info("Database is already closed.");
    }
    
    /**
     * This function commit the metadata changes in the data. It doesn't comment any connections in the pool.
     */
    public void commit() {
    	ndexDatabase.commit();
    }
    
    public ODatabaseDocumentTx getAConnection() throws NdexException {
//    	logger.info("available conn: " + pool.getAvailableConnections());
  
    	for ( int i = 0 ; i < 3000; i ++) {
    		try { 
    			return pool.acquire();
    		} catch (java.lang.IllegalStateException e) {
    			if ( e.getMessage().equals("You have reached maximum pool size for given partition")) {
					logger.warning("DB connection pool is full, wait and retry " + i);
    				try {
						Thread.sleep(150);
					} catch (InterruptedException e1) {
						throw new NdexException ("Interrupted in getAConnection.");
					}
    			} else 
    				throw e;
    		}
    	}
    	throw new NdexException ("Timeout in getting db connection from pool.");
    }

  /*  
    public OrientGraph getGraph() {
    	OrientGraph graph = pool.getTx();
    	 
 		graph.setAutoScaleEdgeType(true);
 		graph.setEdgeContainerEmbedded2TreeThreshold(40);
 		graph.setUseLightweightEdges(true);

    	return graph;
    	 
    }  */

 
}
