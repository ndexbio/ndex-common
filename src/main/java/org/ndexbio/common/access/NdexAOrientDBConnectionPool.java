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
package org.ndexbio.common.access;

import java.util.logging.Logger;

import org.ndexbio.model.exceptions.NdexException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class NdexAOrientDBConnectionPool {
	
//	private static NdexAOrientDBConnectionPool INSTANCE = null;

	
	private OrientGraphFactory pool;
	
	
	private static final Logger logger = Logger
			.getLogger(NdexAOrientDBConnectionPool.class.getName());

	private NdexAOrientDBConnectionPool(String dbURL, String dbUserName, String dbPassword, int size) {

		pool = new OrientGraphFactory(dbURL, dbUserName, dbPassword).setupPool(1,size);
		pool.setAutoScaleEdgeType(true);
		pool.setEdgeContainerEmbedded2TreeThreshold(40);
		pool.setUseLightweightEdges(true);
	    
	    logger.info("Connection pool to " + dbUserName + "@" + dbURL + " created.");
	}
	
/*	
	private static synchronized void createOrientDBConnectionPool (String dbURL, String dbUserName,
				String dbPassword, int size) {
	      if(INSTANCE == null) {
		         INSTANCE = new NdexAOrientDBConnectionPool(dbURL, dbUserName, dbPassword, size);
	      }
	}
	
	private static synchronized NdexAOrientDBConnectionPool getInstance() throws NdexException {
	      if(INSTANCE == null) {
	         throw new NdexException ("Connection pool is not created yet.");
	      }
	      return INSTANCE;
	}

	private ODatabaseDocumentTx acquire() {
		ODatabaseDocumentTx conn = pool.getDatabase();
		
	    return conn;
	}
   	
   private static synchronized void close() { 	
	 
       if ( INSTANCE != null) {	   
	     INSTANCE.pool.close();
	     INSTANCE=null;
         logger.info("Connection pool closed.");
       } else 
         logger.info("Connection pool already closed.");
   }
*/   
   
}
