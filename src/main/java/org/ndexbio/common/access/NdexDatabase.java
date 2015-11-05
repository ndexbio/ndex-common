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
package org.ndexbio.common.access;


import java.util.Arrays;
import java.util.logging.Logger;

import org.cxio.aspects.datamodels.EdgeAttributesElement;
import org.cxio.aspects.datamodels.EdgesElement;
import org.cxio.aspects.datamodels.NetworkAttributesElement;
import org.cxio.aspects.datamodels.NodeAttributesElement;
import org.cxio.aspects.datamodels.NodesElement;
import org.ndexbio.model.cx.CitationElement;
import org.ndexbio.model.cx.EdgeCitationLinksElement;
import org.ndexbio.model.cx.EdgeSupportLinksElement;
import org.ndexbio.model.cx.FunctionTermElement;
import org.ndexbio.model.cx.NamespacesElement;
import org.ndexbio.model.cx.NdexNetworkStatus;
import org.ndexbio.model.cx.NodeCitationLinksElement;
import org.ndexbio.model.cx.NodeSupportLinksElement;
import org.ndexbio.model.cx.Provenance;
import org.ndexbio.model.cx.ReifiedEdgeElement;
import org.ndexbio.model.cx.SupportElement;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.orientdb.NdexSchemaManager;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
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
	
	public static  String[] NdexSupportedAspects ={NodesElement.ASPECT_NAME,EdgesElement.ASPECT_NAME,NetworkAttributesElement.ASPECT_NAME,
			NodeAttributesElement.ASPECT_NAME, EdgeAttributesElement.ASPECT_NAME, CitationElement.ASPECT_NAME, SupportElement.ASPECT_NAME,
			EdgeCitationLinksElement.ASPECT_NAME, EdgeSupportLinksElement.ASPECT_NAME, NodeCitationLinksElement.ASPECT_NAME,
			NodeSupportLinksElement.ASPECT_NAME, FunctionTermElement.ASPECT_NAME, NamespacesElement.ASPECT_NAME, NdexNetworkStatus.ASPECT_NAME,
			Provenance.ASPECT_NAME,ReifiedEdgeElement.ASPECT_NAME};
	
	private NdexDatabase(String HostURI, String dbURL, String dbUserName,
			String dbPassword, int size) throws NdexException {
		
		// check if the db exists, if not create it.
		try ( ODatabaseDocumentTx odb = new ODatabaseDocumentTx(dbURL)) {
			if ( !odb.exists() ) 
				odb.create();
		}
		
		Arrays.sort(NdexSupportedAspects) ;
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
	
	/**
	 * This function create a NDEx database object. It connects to the specified back end database if it exists, otherwise it will create one and connect to it. 
	 * @param HostURI  The URI of this NDEX server. It will be used to construct URIs for the networks that are created in this database.
	 * @param dbURL   Specify where the database is and what protocol we should use to connect to it.
	 * @param dbUserName   the account that administrator that backend database.
	 * @param dbPassword
	 * @param size
	 * @return
	 * @throws NdexException
	 */
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
	
    public synchronized long  getNextId(ODatabaseDocumentTx callingConnection) {
    	
    	if ( batchCounter == blockSize) {
    		vdoc.reload();
        	internalCounterBase = vdoc.field(seqField);
    	    batchCounter = 0 ;
            vdoc = vdoc.field(seqField, internalCounterBase + blockSize).save();
            this.ndexDatabase.activateOnCurrentThread();
            dictionary.put(sequenceKey, vdoc);
        	ndexDatabase.commit();
       // 	System.out.println("New batch in id sequence:" + internalCounterBase );
        	callingConnection.activateOnCurrentThread();
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
    		ODatabaseRecordThreadLocal.INSTANCE.set(INSTANCE.ndexDatabase);
    		INSTANCE.ndexDatabase.commit();
    		INSTANCE.ndexDatabase.close();
    		INSTANCE.pool.close();
    		INSTANCE.pool = null;
    		INSTANCE = null;
    		logger.info("Database closed.");
    	} else 
    		logger.info("Database is already closed.");
    }
    
    public ODatabaseDocumentTx getAConnection() throws NdexException {
  
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


 
}
