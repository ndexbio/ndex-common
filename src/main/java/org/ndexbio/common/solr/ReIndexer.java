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
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.dictionary.ODictionary;
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
							Thread.sleep(1000);
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
