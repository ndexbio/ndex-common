/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
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
package org.ndexbio.common.solr;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServerException;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.BasicNetworkDAO;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.task.Configuration;

import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;


public class ReIndexer {
	static final Logger logger = Logger.getLogger(ReIndexer.class.getName());
	
	ODatabaseDocumentTx destConn;
	
	Long seqId ;
	
	NdexDatabase targetDB;
	
	OrientGraph graph;
	
	BasicNetworkDAO networkDao;
	
	NetworkDocDAO  dbDao;
	
	long counter;
	
	public ReIndexer () throws NdexException, SolrServerException, IOException {
		NetworkGlobalIndexManager mgr = new NetworkGlobalIndexManager();
		mgr.createCoreIfNotExists();
		
		Configuration configuration = Configuration.getInstance();
		targetDB = NdexDatabase.createNdexDatabase( configuration.getHostURI(),
				configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd(), 5);

		System.out.println ( "Database object created.");
		
		destConn = targetDB.getAConnection();
		
		graph = new OrientGraph(destConn,false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);
		
		networkDao = new BasicNetworkDAO ( destConn);
		
		dbDao = new NetworkDocDAO ( destConn);

	}
	
	private void createSolrIndex(String UUID) {

        String query = "SELECT FROM network where isDeleted=false and isComplete=true" + 
              (UUID == null ? "" : (" and UUID='" + UUID + "'") );
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		            	try {
							networkDao.createSolrIndex(doc);
							Thread.sleep(500);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							logger.severe("Network " + doc.field(NdexClasses.ExternalObj_ID) + " solr index failed to create. Error:" + e.getMessage());
							e.printStackTrace();
							return true;
						}
		            	counter ++;
		  				if ( counter % 50 == 0 ) {
		  					logger.info(  counter + " Solr indexes created for networks.");
		  				}
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	logger.info( "Solr index creation completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        destConn.command(asyncQuery).execute(); 
        
	}
		
	private void closeAll() {
		graph.shutdown();
    	NdexDatabase.close();
	}
	
	
	public static void main(String[] args) throws NdexException, SolrServerException, IOException {
		if ( args.length !=1) {
			System.out.println("Usage: ReIndexer <UUID>\n\n" +  
						"UUID can be \"all\", which will apply command to all networks in db.");
			return;
		}
		
		ReIndexer worker = new ReIndexer ();
		
		String uuidStr = args[0];
		
		if ( uuidStr.equalsIgnoreCase("all"))
		   worker.createSolrIndex(null);
		else if ( UUID.fromString( uuidStr) != null)
		   worker.createSolrIndex(uuidStr);
		
		worker.closeAll();
	}

}
