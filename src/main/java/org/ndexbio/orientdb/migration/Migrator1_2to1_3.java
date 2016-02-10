package org.ndexbio.orientdb.migration;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.task.Configuration;

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.command.OCommandResultListener;

public class Migrator1_2to1_3 {
	
	static final Logger logger = Logger.getLogger(Migrator1_2to1_3.class.getName());

	private String srcDbPath;
//	private String destDbPath;

	OPartitionedDatabasePool srcPool; 
	ODatabaseDocumentTx srcConnection;
	ODatabaseDocumentTx descConn;
	
	NdexDatabase targetDB;
	
	long counter;
	
	public Migrator1_2to1_3(String srcPath) throws NdexException {
		srcDbPath = srcPath;
		
		srcPool = new OPartitionedDatabasePool(srcDbPath , "admin","admin",5);

		srcConnection = srcPool.acquire();
		
		targetDB = NdexDatabase.createNdexDatabase("http://pulic.ndexbio.org/",
				"plocal:/opt/ndex/orientdb/databases/ndex",
    			"admin","admin", 5);
		
		descConn = targetDB.getAConnection();
		
	}
	
	private void copyNamespaces() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM namespace";
		List<ODocument> rs = srcConnection.query(new OSQLSynchQuery<ODocument>(query));
		
		counter = 0;
		for (final ODocument nsDoc : rs )  {
			Long id = (Long) nsDoc.field("id");
			String prefix= nsDoc.field("prefix");
			String uri = nsDoc.field("uri");
            descConn.activateOnCurrentThread();
            ODocument newNSDoc = new ODocument(NdexClasses.Namespace)
            			.field(NdexClasses.Element_ID,id, OType.LONG)
            			.field(NdexClasses.ns_P_prefix, prefix, OType.STRING)
            			.field(NdexClasses.ns_P_uri,uri, OType.STRING);
            newNSDoc.save();
            counter++;
            if ( counter % 5000 == 0 ) {
            	descConn.commit();
            	logger.info( "Namespace commited " + counter + " records.");
            }
            srcConnection.activateOnCurrentThread();
		}
    	logger.info( "Namespace copy completed. Total record: " + counter);

	}
	

	private void copyBaseTerms() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM baseTerm";
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument nsDoc = (ODocument) iRecord;
		  				Long id = (Long) nsDoc.field(NdexClasses.Element_ID);
		  				String name= nsDoc.field(NdexClasses.BTerm_P_name);
		  				Object nsRid = nsDoc.field("out_baseTermNS");
		  				Long nsId = null;
		  				String prefix = null;
		  				if ( nsRid != null) {
		  					if ( nsRid instanceof ORecordId) {
		  						try {
		  							ODocument ns = new ODocument((ORecordId)nsRid);
		  					
		  							nsId = ns.field(NdexClasses.Element_ID);
		  							prefix =ns.field(NdexClasses.ns_P_prefix);
		  							if ( prefix != null)
		  								prefix += ":";
		  						} catch ( ORecordNotFoundException e) {
		  							logger.warning("record " + nsRid + " not found in db. ignore this namespace link.");
		  						}
		  					} else {
	  							ODocument ns = (ODocument)nsRid;
			  					
	  							nsId = ns.field(NdexClasses.Element_ID);
	  							prefix =ns.field(NdexClasses.ns_P_prefix);
	  							if ( prefix != null)
	  								prefix += ":";
		  						
		  					}
		  				}
		  				descConn.activateOnCurrentThread();
		  				ODocument newbtDoc = new ODocument(NdexClasses.BaseTerm)
		              			.field(NdexClasses.Element_ID,id, OType.LONG)
		              			.field(NdexClasses.BTerm_P_name, name, OType.STRING);
		  				if ( prefix!=null)
		              		newbtDoc.field(NdexClasses.BTerm_P_prefix, prefix, OType.STRING);
		  				if ( nsId !=null) {
		  					newbtDoc.field(NdexClasses.BTerm_NS_ID, nsId);
		  				}
		  				newbtDoc.save();
		  				counter++;
		  				if ( counter % 5000 == 0 ) {
		  					descConn.commit();
		  					logger.info( "baseTerm commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() {
		            	descConn.commit();
		            	logger.info( "Baseterm copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        

	}

	private void copySupport() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM support";
        
		counter = 0;
		
/*		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		  				Long id = (Long) doc.field(NdexClasses.Element_ID);
		  				String text= doc.field(NdexClasses.Support_P_text);
		  				if ( text == null)  // skip this if the text is null
		  					return true;
		  				
		  				Object nsRid = nsDoc.field("out_baseTermNS");
		  				Long nsId = null;
		  				String prefix = null;
		  				if ( nsRid != null) {
		  					if ( nsRid instanceof ORecordId) {
		  						try {
		  							ODocument ns = new ODocument((ORecordId)nsRid);
		  					
		  							nsId = ns.field(NdexClasses.Element_ID);
		  							prefix =ns.field(NdexClasses.ns_P_prefix);
		  							if ( prefix != null)
		  								prefix += ":";
		  						} catch ( ORecordNotFoundException e) {
		  							logger.warning("record " + nsRid + " not found in db. ignore this namespace link.");
		  						}
		  					} else {
	  							ODocument ns = (ODocument)nsRid;
			  					
	  							nsId = ns.field(NdexClasses.Element_ID);
	  							prefix =ns.field(NdexClasses.ns_P_prefix);
	  							if ( prefix != null)
	  								prefix += ":";
		  						
		  					}
		  				}
		  				descConn.activateOnCurrentThread();
		  				ODocument newbtDoc = new ODocument(NdexClasses.BaseTerm)
		              			.field(NdexClasses.Element_ID,id, OType.LONG)
		              			.field(NdexClasses.BTerm_P_name, name, OType.STRING);
		  				if ( prefix!=null)
		              		newbtDoc.field(NdexClasses.BTerm_P_prefix, prefix, OType.STRING);
		  				if ( nsId !=null) {
		  					newbtDoc.field(NdexClasses.BTerm_NS_ID, nsId);
		  				}
		  				newbtDoc.save();
		  				counter++;
		  				if ( counter % 5000 == 0 ) {
		  					descConn.commit();
		  					logger.info( "baseTerm commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	descConn.commit();
		            	logger.info( "Baseterm copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        
*/
	}

	
	private void copyUsers() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM user where isDeleted=false";
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		  				String uuid =  doc.field(NdexClasses.ExternalObj_ID);
		  				String desc = doc.field("description");
		  				Date ct = doc.field(NdexClasses.ExternalObj_cTime);
		  				Date mt = doc.field(NdexClasses.ExternalObj_mTime);
		  				String imgURL = doc.field("imageURL");
		  				String password = doc.field("password");
		  				String websiteURL = doc.field("websiteURL");
		  				String accName = doc.field(NdexClasses.account_P_accountName);
		  				String email = doc.field("emailAddress");
		  				String firstName = doc.field("firstName");
		  				String lastName = doc.field("lastName");
		  				
		  				descConn.activateOnCurrentThread();
		  				ODocument newbtDoc = new ODocument(NdexClasses.User)
		              			.fields(NdexClasses.ExternalObj_ID, uuid,
		              					NdexClasses.ExternalObj_cTime, ct,
		              					NdexClasses.ExternalObj_mTime,mt,
		              					NdexClasses.account_P_accountName, accName,
		              					"description", desc,
		              					"imageURL", imgURL,
		              					"password", password,
		              					"websiteURL", websiteURL,
		              					"emailAddress",email,
		              					"firstName", firstName,
		              					"lastName", lastName);
		  				newbtDoc.save();
		  				counter++;
		  				if ( counter % 5000 == 0 ) {
		  					descConn.commit();
		  					logger.info( "User commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	descConn.activateOnCurrentThread();
		            	descConn.commit();
		            	logger.info( "user class copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        

	}
	
	private void closeAll() {
		srcConnection.activateOnCurrentThread();
		srcConnection.close();
		descConn.activateOnCurrentThread();
    	NdexDatabase.close();
	}
	
	public static void main(String[] args) throws NdexException {
		Migrator1_2to1_3 migrator = new Migrator1_2to1_3("plocal:/opt/ndex/orientdb/databases/ndex_1_2");
		
		migrator.copyUsers();
		
	//	migrator.copyNamespaces();
		
	//	migrator.copyBaseTerms();
		
	//	migrator.copySupport();
		
		migrator.closeAll();
    	logger.info( "DB migration completed.");
	}
	
}
