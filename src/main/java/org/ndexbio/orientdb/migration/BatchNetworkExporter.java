package org.ndexbio.orientdb.migration;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class BatchNetworkExporter implements AutoCloseable{
	
	
	private String srcDbPath;

	OPartitionedDatabasePool srcPool; 
	ODatabaseDocumentTx srcConnection;
//	ODatabaseDocumentTx destConn;
	
	public BatchNetworkExporter (String srcPath) throws NdexException {
		srcDbPath = "plocal:" + srcPath;
		
		OGlobalConfiguration.USE_WAL.setValue(false);
		OGlobalConfiguration.WAL_SYNC_ON_PAGE_FLUSH.setValue(false);
		
		srcPool = new OPartitionedDatabasePool(srcDbPath , "admin","admin",5,10);  // from 2.1.15
	//	srcConnection = srcPool.acquire();
		
		 NdexDatabase db = NdexDatabase.createNdexDatabase( "http://localhost/rest/",
				 	srcDbPath,
	    			"admin",
	    			"admin", 5);
		 srcConnection = db.getAConnection();
		 srcConnection.activateOnCurrentThread();
	}
	
	
	private void exportMainTable(String tableName, String additionalCondition ) throws Exception {
		
		String pathPrefix = "/opt/ndex/migration/";
		String query = "SELECT * FROM " +tableName + " where isDeleted = false " + additionalCondition;
			List<ODocument> rs = srcConnection.query(new OSQLSynchQuery<ODocument>(query));
			int counter = 0;
			long t1 = System.currentTimeMillis();
			try (FileWriter wtr = new FileWriter(pathPrefix+ tableName +".json" )) {
				wtr.write("[\n");
				
				for (ODocument doc : rs) {
					if ( counter != 0)
						wtr.write(",\n");
					counter++;
					
					//String uuidStr = doc.field("UUID");
					String jsonUser = doc.toJSON();
					wtr.write(jsonUser);
										
				}	
				wtr.write("]\n");
			}
			
		long t2 = System.currentTimeMillis();
		System.out.println("exported " + counter + " " + tableName +". Total time: " + (t2-t1)/1000.0);
	}
	
	
	private void exportNetworkTable() throws Exception {
		
		String pathPrefix = "/opt/ndex/migration/";
		String query = "SELECT * FROM network where isDeleted = false and isComplete = true and nodeCount > 0";
			List<ODocument> rs = srcConnection.query(new OSQLSynchQuery<ODocument>(query));
			int counter = 0;
			long t0 = System.currentTimeMillis();
			ObjectMapper mapper = new ObjectMapper();
			try (FileWriter wtr = new FileWriter(pathPrefix+ "network.json" )) {
				wtr.write("[\n");
				
				for (ODocument doc : rs) {
					long t1 = System.currentTimeMillis();
					if ( counter != 0)
						wtr.write(",\n");
					counter++;
					Map<String,Object> networkRec = new HashMap<>();
					String uuidStr = doc.field("UUID");
				
					networkRec.put("rid", doc.field("@rid").toString());
					networkRec.put("uuid", uuidStr);
					networkRec.put("createdTime", doc.field("createdTime"));
					networkRec.put("modificationTime", doc.field("modificationTime"));
					networkRec.put("description", doc.field(NdexClasses.Network_P_desc));
					networkRec.put(NdexClasses.Network_P_provenance, doc.field(NdexClasses.Network_P_provenance));
					networkRec.put(NdexClasses.Network_P_name, doc.field( NdexClasses.Network_P_name));
					networkRec.put("props", doc.field("props"));
					networkRec.put(NdexClasses.Network_P_edgeCount, doc.field( NdexClasses.Network_P_edgeCount));
					networkRec.put(NdexClasses.Network_P_nodeCount, doc.field( NdexClasses.Network_P_nodeCount));
					networkRec.put(NdexClasses.Network_P_source_format, doc.field( NdexClasses.Network_P_source_format));
					networkRec.put(NdexClasses.Network_P_version, doc.field( NdexClasses.Network_P_version));
					networkRec.put(NdexClasses.Network_P_visibility, doc.field( NdexClasses.Network_P_visibility));
					long roid = doc.field(NdexClasses.Network_P_cacheId);
					long commitId = doc.field(NdexClasses.Network_P_readOnlyCommitId);
					networkRec.put("readonly", (roid > 0  && commitId == roid))	;
					networkRec.put(NdexClasses.Network_P_version, doc.field( NdexClasses.Network_P_version));

					
					String jsonRec = mapper.writeValueAsString(networkRec)	;
					wtr.write(jsonRec);
					
					System.out.print("Exporting " + uuidStr + " -- " + counter + "\tedges: " + doc.field( NdexClasses.Network_P_edgeCount) +
							"\tnodes: " + doc.field( NdexClasses.Network_P_nodeCount));
					
					try (FileOutputStream out = new FileOutputStream(pathPrefix+ uuidStr + ".cx" )) {
						
						CXNetworkExporterV13 exporter = new CXNetworkExporterV13(uuidStr);
						exporter.writeNetworkInCX(out, true);
						exporter.close();
						long t2 = System.currentTimeMillis();
						System.out.println( "\ttotal: " + (t2-t1)/1000.0 + "(sec)");
					}
										
				}	
				wtr.write("]\n");
			}
			
		long t2 = System.currentTimeMillis();
		System.out.println("exported " + counter + " network records. Total time: " + (t2-t0)/1000.0);
	}
	
	
	public void close() {
		srcConnection.activateOnCurrentThread();
		if (srcConnection != null ) 
			srcConnection.close();
	//	destConn.activateOnCurrentThread();
    	NdexDatabase.close();
	}
	
public static void main(String[] args) throws Exception {
	
		try (BatchNetworkExporter ne = new  BatchNetworkExporter("/opt/ndex/orientdb/databases/ndex")) {
	
			  // test only
			
		//	  try (CXNetworkExporterV13 e = new CXNetworkExporterV13 ("4325a644-a531-11e6-9ed0-06603eb7f303")) {
			try (CXNetworkExporterV13 e = new CXNetworkExporterV13 ("8954341d-8e46-11e5-b435-06603eb7f303")) {
			  FileOutputStream out = new FileOutputStream("/tmp/outputcy.cx" );
			  e.writeNetworkInCX(out, true);
		      out.close();
			  }
				
			
			
			
			ne.exportMainTable("user","");
			ne.exportMainTable("task"," and taskType = 'EXPORT_NETWORK_TO_FILE' and status = 'COMPLETED'");
			ne.exportMainTable("request","");
			ne.exportMainTable("group","");
			ne.exportNetworkTable();
		
		}
	}
	

}
