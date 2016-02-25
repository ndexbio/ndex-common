package org.ndexbio.orientdb.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServerException;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.BasicNetworkDAO;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.common.models.dao.orientdb.OrientDBIterableSingleLink;
import org.ndexbio.common.models.dao.orientdb.OrientdbDAO;
import org.ndexbio.common.solr.NetworkGlobalIndexManager;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.TaskType;
import org.ndexbio.task.Configuration;

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.dictionary.ODictionary;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class Migrator1_2to1_3 {
	
	static final Logger logger = Logger.getLogger(Migrator1_2to1_3.class.getName());

	private static final Collection<ODocument> emptyDocs = new LinkedList<>();

	private String srcDbPath;
//	private String destDbPath;

	OPartitionedDatabasePool srcPool; 
	ODatabaseDocumentTx srcConnection;
	ODatabaseDocumentTx destConn;
		
	Long seqId ;
	
	NdexDatabase targetDB;
	
	OrientGraph graph;
	
	BasicNetworkDAO networkDao;
	
	NetworkDocDAO  dbDao;
	
	long counter;
	
	public Migrator1_2to1_3(String srcPath) throws NdexException, SolrServerException, IOException {
		
		NetworkGlobalIndexManager mgr = new NetworkGlobalIndexManager();
		mgr.createCoreIfNotExists();
		
		srcDbPath = "plocal:" + srcPath;
		
		srcPool = new OPartitionedDatabasePool(srcDbPath , "admin","admin",5);

		srcConnection = srcPool.acquire();
		
		ODictionary srcDictionary = srcConnection.getDictionary();
		ODocument vd = (ODocument) srcDictionary.get("NdexSeq");
		seqId = vd.field("f1");
		
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
	
	private void copyNamespaces() throws ObjectNotFoundException, NdexException {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM namespace where in_networkNS is not null";
		List<ODocument> rs = srcConnection.query(new OSQLSynchQuery<ODocument>(query));
		
		counter = 0;
		for (final ODocument nsDoc : rs )  {
			Long id = (Long) nsDoc.field("id");
			String prefix= nsDoc.field("prefix");
			String uri = nsDoc.field("uri");
			
			ODocument netDoc = nsDoc.field("in_networkNS");
			String uuid = netDoc.field(NdexClasses.ExternalObj_ID);
			
            destConn.activateOnCurrentThread();
            ODocument newDoc = new ODocument(NdexClasses.Namespace)
            			.field(NdexClasses.Element_ID,id, OType.LONG)
            			.field(NdexClasses.ns_P_prefix, prefix, OType.STRING)
            			.field(NdexClasses.ns_P_uri,uri, OType.STRING);
            newDoc.save();
            
			OrientVertex newV = graph.getVertex(newDoc);
			try {
				ODocument hDoc = dbDao.getRecordByUUIDStr(uuid,null);
	  			OrientVertex vNet = graph.getVertex(hDoc);
				graph.addEdge(null, vNet, newV, NdexClasses.Network_E_Namespace);
			} catch (ObjectNotFoundException e) {
				logger.info("Skipping creating network link for " + uuid );
//				continue;
			}
            counter++;
            if ( counter % 10000 == 0 ) {
            	destConn.commit();
            	logger.info( "Namespace commited " + counter + " records.");
            }
            srcConnection.activateOnCurrentThread();
		}
    	logger.info( "Namespace copy completed. Total record: " + counter);

	}
	

	private void copyBaseTerms() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM baseTerm where in_BaseTerms is not null";
        
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
	  							logger.warning("record if " + nsRid + " found in db for baseterm. ignore this baseterm: " + id);
	  							return true;
		  					} 
	  							
		  					ODocument ns = (ODocument)nsRid;
			  					
	  						nsId = ns.field(NdexClasses.Element_ID);
	  						prefix =ns.field(NdexClasses.ns_P_prefix);
	  						if ( prefix != null)
	  							prefix += ":";
		  						
		  					
		  				}
		  				
		  				Object  nsLink = nsDoc.field("in_BaseTerms");
		  				if ( !( nsLink instanceof ODocument) ) {
							logger.warning("ignoring baseterm " + id + " because network " + nsLink + " not found.");
		  					return true; 
						}	
		  				ODocument netDoc = (ODocument) nsLink;
		  				
		  				String uuid = netDoc.field(NdexClasses.ExternalObj_ID);
		  				
		  				destConn.activateOnCurrentThread();
		  				ODocument newbtDoc = new ODocument(NdexClasses.BaseTerm)
		              			.fields(NdexClasses.Element_ID,id,
		              			    NdexClasses.BTerm_P_name, name);
		  				if ( prefix!=null)
		              		newbtDoc.field(NdexClasses.BTerm_P_prefix, prefix, OType.STRING);
		  				if ( nsId !=null) {
		  					newbtDoc.field(NdexClasses.BTerm_NS_ID, nsId);
		  				}
		  				newbtDoc.save();
		  				
		  				OrientVertex newV = graph.getVertex(newbtDoc);
		  				try {
		  					ODocument hDoc = dbDao.getRecordByUUIDStr(uuid,null);
		  		  			OrientVertex vNet = graph.getVertex(hDoc);
		  					graph.addEdge(null, vNet, newV, NdexClasses.Network_E_BaseTerms);
		  				} catch (ObjectNotFoundException e) {
		  					logger.info("Skipping creating network link for " + uuid );
//		  					continue;
		  				} catch (NdexException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return false;
						}
		  				counter++;
		  				if ( counter % 10000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "baseTerm commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() {
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "Baseterm copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        srcConnection.activateOnCurrentThread();
        srcConnection.command(asyncQuery).execute(); 
        

	}

	private void copySupport() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM support where in_supports is not null";
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		  				Long id = (Long) doc.field(NdexClasses.Element_ID);
		  				String text= doc.field(NdexClasses.Support_P_text);
		  				if ( text == null) {  // skip this if the text is null
		  					logger.warning("empty support text. Support record" + id + " is ignored.");
		  					return true;
		  				}
		  				
		  				Object netRec = doc.field("in_supports");
		  				if ( ! (netRec instanceof ODocument)) {
		  					logger.warning("Support id" + id + " is not pointing to a network, ignoring it.");
		  					return true;
		  				}
		  					
		  				ODocument netDoc = (ODocument)netRec;
		  				String uuid = netDoc.field(NdexClasses.ExternalObj_ID);
		  				
		  				ODocument cDoc = doc.field("out_citeFrom");

		  				Long citationId = null;
		  				if ( cDoc != null) {
	  							citationId = cDoc.field(NdexClasses.Element_ID);
		  				
		  				}
		  				
		  				// get properties
		  				List<NdexPropertyValuePair> props = getProperties(doc);
		  				
		  				destConn.activateOnCurrentThread();
		  				
		  				ODocument newDoc = new ODocument(NdexClasses.Support)
		              			.fields(NdexClasses.Element_ID,id, 
		              					NdexClasses.Support_P_text, text,
		              					NdexClasses.Citation, citationId);
		              	
		  				if ( !props.isEmpty())
		              		newDoc.field(NdexClasses.ndexProperties, props);

		  				newDoc.save();
		  				
		  				// connect to network headnode.
		  				if ( !connectToNetworkHeadNode(newDoc, uuid, NdexClasses.Network_E_Supports))
		  					return false;
		  				
		  				counter++;
		  				if ( counter % 10000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "Support commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() {
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "Support copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        

	}

	private void copyCitations() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM citation where in_citations is not null";
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		  				Long id = (Long) doc.field(NdexClasses.Element_ID);
		  				String title= doc.field(NdexClasses.Citation_P_title);
		  				String idType = doc.field(NdexClasses.Citation_p_idType);
		  				String identifier = doc.field(NdexClasses.Citation_P_identifier);  
		  				List<String> authors = doc.field(NdexClasses.Citation_P_contributors);
		  				
		  				
		  				ODocument netDoc = doc.field("in_citations");
		  				String uuid = netDoc.field(NdexClasses.ExternalObj_ID);
		  				
		  				// get properties
		  				List<NdexPropertyValuePair> props = getProperties(doc);
		  				
		  				destConn.activateOnCurrentThread();
		  				
		  				ODocument newDoc = new ODocument(NdexClasses.Citation)
		              			.fields(NdexClasses.Element_ID,id, 
		              					NdexClasses.Citation_P_title, title, 
		              					NdexClasses.Citation_p_idType, idType,
		              					NdexClasses.Citation_P_identifier, identifier,
		              					NdexClasses.Citation_P_contributors, authors);
		              	
		  				if ( !props.isEmpty())
		              		newDoc.field(NdexClasses.ndexProperties, props);

		  				newDoc.save();
		  				
		  				// connect to network headnode.
		  				if ( !connectToNetworkHeadNode(newDoc, uuid, NdexClasses.Network_E_Citations))
		  					return false;
		  				
		  				counter++;
		  				if ( counter % 10000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "Citation commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "Citation copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        

	}
	
	private void copyReifiedEdgeElements() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM reifiedEdgeTerm where in_reifiedETerms is not null";
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		  				Long id = (Long) doc.field(NdexClasses.Element_ID);
		  		
		  				ODocument netDoc = doc.field("in_reifiedETerms");
		  				String uuid = netDoc.field(NdexClasses.ExternalObj_ID);
		  				
		  				
		  				destConn.activateOnCurrentThread();
		  				
		  				ODocument newDoc = new ODocument(NdexClasses.ReifiedEdgeTerm)
		              			.fields(NdexClasses.Element_ID,id);
		              	
		  				newDoc.save();
		  				
		  				// connect to network headnode.
		  				if ( !connectToNetworkHeadNode(newDoc, uuid, NdexClasses.Network_E_ReifiedEdgeTerms))
		  					return false;
		  				
		  				counter++;
		  				if ( counter % 5000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "ReifiedEdge term commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "Refied edge record copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        srcConnection.command(asyncQuery).execute(); 
        

	}
	
	private void copyReifiedEdgeLinks() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM reifiedEdgeTerm where in_reifiedETerms is not null and id > 1976699";
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		  				Long id = (Long) doc.field(NdexClasses.Element_ID);
		  		
		  				ODocument edgeDoc = doc.field("out_reify");
		  				Long edgeId = edgeDoc.field(NdexClasses.Element_ID);		  				
		  				
		  				destConn.activateOnCurrentThread();
		  				
	 					ODocument edgeNDoc;
	 					ODocument reifiedNDoc;
						try {
							edgeNDoc = dbDao.getDocumentByElementId(edgeId);
							reifiedNDoc = dbDao.getDocumentByElementId(id);
						} catch (NdexException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								logger.warning("Edge id not found in target db:" + edgeId );

								srcConnection.activateOnCurrentThread();

								return true;
						}
		  				OrientVertex edgeV = graph.getVertex(edgeNDoc);
		  				OrientVertex reifiedV = graph.getVertex(reifiedNDoc);
		  					
		  				graph.addEdge(null, reifiedV, edgeV, NdexClasses.ReifiedEdge_E_edge);

		  				counter++;
		  				if ( counter % 5000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "ReifiedEdge link commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "Refied edge linke copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        srcConnection.command(asyncQuery).execute(); 
        

	}
	
	private void copyFunctionTerms() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM functionTerm where out_FuncBaseTerm is not null";
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		  				Long id = (Long) doc.field(NdexClasses.Element_ID);
		  				
		  				Object btObj = doc.field("out_FuncBaseTerm");
		  				if ( !( btObj instanceof ODocument ) ) {
		  					logger.warning("function term " + id + " copy ignored. Not pointing to a valid base term. " + btObj.toString());
		  					return true;
		  				}	
		  				ODocument btDoc = doc.field("out_FuncBaseTerm");
		  				Long btId = btDoc.field(NdexClasses.Element_ID);
		  				

		  				ODocument netDoc = doc.field("in_FunctionTerms"); //(ODocument)netRec;
		  				String uuid = (netDoc == null) ? null  : ((String)netDoc.field ( NdexClasses.ExternalObj_ID  ) )  ;

		  				// get properties
		  				List<NdexPropertyValuePair> props = getProperties(doc);
		  				
		  				destConn.activateOnCurrentThread();
		  				
		  				ODocument newDoc = new ODocument(NdexClasses.FunctionTerm)
		              			.fields(NdexClasses.Element_ID,id, 
		              					NdexClasses.BaseTerm, btId 
		     //         					NdexClasses.Node_P_represents, rep,
		                                 );
		              	
		  				if ( !props.isEmpty())
		              		newDoc.field(NdexClasses.ndexProperties, props);

		  				newDoc.save();
		  				
		  				if ( uuid !=null) {
		  					// connect to network headnode.
		  					if ( !connectToNetworkHeadNode(newDoc, uuid, NdexClasses.Network_E_FunctionTerms))
		  						return false;
		  				}
		  				counter++;
		  				if ( counter % 10000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "FunctionTerm commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "FunctionTerm copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
       
        // now create the links for arguments.
        
		counter = 0;
		
		srcConnection.activateOnCurrentThread();
		OSQLAsynchQuery<ODocument> asyncQuery2 =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		  				Long id = (Long) doc.field(NdexClasses.Element_ID);
		  				
		  				List<Long> argIds = new ArrayList<>();
		  				for ( ODocument argDoc: getLinkedDocs(doc, "out_FuncArguments")) {
		  					if ( argDoc != null) {
		  						Long argId = argDoc.field(NdexClasses.Element_ID);
		  						argIds.add(argId);
		  					}
		  				}
		  				
		  				if ( argIds.isEmpty())
		  					return true;
		  				
		  				destConn.activateOnCurrentThread();
		  				
		  				ODocument funDoc;
						try {
							funDoc = dbDao.getDocumentByElementId(id);
						} catch (NdexException e) {
			//				e.printStackTrace();
							logger.warning("Element id " + id +" not found, ignoring this record.");

							srcConnection.activateOnCurrentThread();

							return true;
						} 
  			  			OrientVertex vFunc = graph.getVertex(funDoc);
  			  			
		  				for ( Long argId : argIds) {
		  					ODocument argDoc;
							try {
								argDoc = dbDao.getDocumentByElementId(argId);
							} catch (NdexException e) {
								e.printStackTrace();
								logger.warning("Element id " + id +" not found, ignoring this record.");
				  				srcConnection.activateOnCurrentThread();

								return true;
							}
		  				
		  					OrientVertex newV = graph.getVertex(argDoc);
//		  					try {
		  						graph.addEdge(null, vFunc, newV, NdexClasses.FunctionTerm_E_paramter);
	/*	  					} catch (ObjectNotFoundException e) {
		  						logger.info("Skipping creating network link for " + uuid );
//		  						continue;
		  					} catch (NdexException e) {
		  					// TODO Auto-generated catch block
		  					e.printStackTrace();
		  					return false; */
		  				}
		  				counter++;
		  				if ( counter % 10000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "FunctionTerm arguments commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "Function arguments completed. Total record: " + counter);
		            }
		            
		          });
		        
        srcConnection.activateOnCurrentThread();
        srcConnection.command(asyncQuery2).execute(); 
	}
	
	

	
	private void copyNodes() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM node where in_networkNodes is not null ";
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		  				Long id = (Long) doc.field(NdexClasses.Element_ID);
		  				String name= doc.field(NdexClasses.Node_P_name);
		//  				System.out.println("node id: " + id);
		  				
		  				ODocument netDoc = doc.field("in_networkNodes");
		  				String uuid = netDoc.field(NdexClasses.ExternalObj_ID);
		  				
		  				// get properties
		  				List<NdexPropertyValuePair> props = getProperties(doc);
		  				
		  				// get represents
		  				Long repId = null;
		  				String repType =null;
		  				ODocument repDoc = doc.field("out_represents");
		  				if ( repDoc !=null) {
		  					repId = repDoc.field(NdexClasses.Element_ID);
		  					repType = repDoc.getClassName();
		  				}
		  				// get aliases
		  				List<Long> aliases = new ArrayList<>();
		  				for ( ODocument alias : getLinkedDocs(doc, "out_alias") ) {
		  					aliases.add ((Long)alias.field(NdexClasses.Element_ID));
		  				}
		  				
		  				// get relatedTerms
		  				List<Long> relatedTo = new ArrayList<>();
		  				try {
		  					for ( Object relatedTerm : getLinkedObjs(doc, "out_relateTo") ) {
		  						if (relatedTerm instanceof ODocument) 
		  						    relatedTo.add ((Long)((ODocument)relatedTerm).field(NdexClasses.Element_ID));
		  						else {
		  						//	System.out.println(relatedTerm.getClass().getName());
		  						//	ODocument dd = new ODocument((ORecordId)relatedTerm);
		  						//	relatedTo.add((Long)dd.field(NdexClasses.Element_ID)) ;
		  						}
		  					}	
		  				} catch (ClassCastException e)	{
		  					System.err.println(e.getMessage() + " ");
		  					return false;
		  				}
		  				
		  				
		  				// get citations
		  				Set<Long> citationIds = new TreeSet<>();
		  				for ( ODocument cDoc : getLinkedDocs(doc, "out_nCitation") ) {
		  					Long cId = cDoc.field(NdexClasses.Element_ID);
		  					citationIds.add(cId);
		  				}
		  				
		  				//get supports
		  				Set<Long> supportIds = new TreeSet<> ();
		  				for ( ODocument sDoc : getLinkedDocs ( doc, "out_nSupport")) {
		  					Long sId = sDoc.field(NdexClasses.Element_ID);
		  					supportIds.add(sId);
		  				}		  				
		  				
		  				destConn.activateOnCurrentThread();
		  				
		  				ODocument newDoc = new ODocument(NdexClasses.Node)
		              			.fields(NdexClasses.Element_ID,id, 
		              					NdexClasses.Node_P_name, name);
		  				
		  				if ( repId != null) { 
		  					newDoc.fields(NdexClasses.Node_P_representTermType, repType,
		              					NdexClasses.Node_P_represents,repId);
		  				}
		  				if ( !props.isEmpty())
		              		newDoc.field(NdexClasses.ndexProperties, props);
		  				if ( !aliases.isEmpty()) {
		  					newDoc.field(NdexClasses.Node_P_alias, aliases);
		  				}
		  				
		  				if ( !relatedTo.isEmpty()){
		  					newDoc.field(NdexClasses.Node_P_relatedTo, relatedTo);
		  				}
		  				
		  				if ( !citationIds.isEmpty()) {
		  					newDoc.field(NdexClasses.Citation, citationIds);
		  				}
		  				
		  				if ( ! supportIds.isEmpty()) {
		  					newDoc.field(NdexClasses.Support, supportIds);
		  				}
		  				
		  				newDoc.save();
		  				
		  				// connect to network headnode.
		  				if ( !connectToNetworkHeadNode(newDoc, uuid, NdexClasses.Network_E_Nodes))
		  					return false;
		  				
		  				counter++;
		  				if ( counter % 10000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "Node commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "Node copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        

	}
	
	private void copyEdges() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM edge where in_networkEdges is not null and in_edgeSubject is not null and out_edgeObject is not null";
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		  				Long id = (Long) doc.field(NdexClasses.Element_ID);
		  				
		  				Object  netObj = doc.field("in_networkEdges");
		  				if ( !(netObj instanceof ODocument)) {
		  					logger.warning("edge: " + id + " not pointing to a network doc. ignoring it.");
		  					return true;
		  				}
		  					
		  				ODocument netDoc = (ODocument)netObj;
		  				String uuid = netDoc.field(NdexClasses.ExternalObj_ID);
		  				
		  				// get properties
		  				List<NdexPropertyValuePair> props = getProperties(doc);
		  				
		  				// get predicate
		  				Long predicateId = null;
		  				Object predObj = doc.field("out_edgePredicate");
		  				if ( predObj instanceof ORecordId ) {
		  					logger.warning("Invalid record id: " + predObj.toString() + " found for predicate doc link. Ignore this edge.");
		  					return true;
		  				}
		  					
		  				ODocument predDoc = (ODocument)predObj;
		  				if ( predDoc !=null) {
		  					predicateId = predDoc.field(NdexClasses.Element_ID);
		  				}
		  				// get subject node
		  				ODocument subjDoc = doc.field("in_edgeSubject");
		  				Long subjId = subjDoc.field(NdexClasses.Element_ID);
		  				
		  				// get object node
		  				ODocument objDoc = doc.field("out_edgeObject");
		  				Long objId = objDoc.field(NdexClasses.Element_ID);
		  				
		  				// get citations
		  				Set<Long> citationIds = new TreeSet<>();
		  				for ( ODocument cDoc : getLinkedDocs(doc, "out_eCitation") ) {
		  					Long cId = cDoc.field(NdexClasses.Element_ID);
		  					citationIds.add(cId);
		  				}
		  				
		  				//get supports
		  				Set<Long> supportIds = new TreeSet<> ();
		  				for ( ODocument sDoc : getLinkedDocs ( doc, "out_eSupport")) {
		  					Long sId = sDoc.field(NdexClasses.Element_ID);
		  					supportIds.add(sId);
		  				}
		  				
		  				destConn.activateOnCurrentThread();
		  				
		  				ODocument newDoc = new ODocument(NdexClasses.Edge)
		              			.fields(NdexClasses.Element_ID,id,
		              					NdexClasses.Edge_P_predicateId, predicateId);
		  				
		  				if ( !citationIds.isEmpty())
		  					newDoc.field(NdexClasses.Citation, citationIds);
		  				
		  				if ( !supportIds.isEmpty())
		  					newDoc.field(NdexClasses.Support, supportIds);
		  				
		  				if ( !props.isEmpty())
		              		newDoc.field(NdexClasses.ndexProperties, props);
		  				newDoc.save();
		  				
		  				// connect to network headnode.
		  				if ( !connectToNetworkHeadNode(newDoc, uuid, NdexClasses.Network_E_Edges))
		  					return false;
		  				
	  					OrientVertex newV = graph.getVertex(newDoc);

		  				// connect to nodes
	 					ODocument subjNDoc;
	 					ODocument objNDoc;
						try {
							subjNDoc = dbDao.getDocumentByElementId(subjId);
							objNDoc = dbDao.getDocumentByElementId(objId);
						} catch (NdexException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							logger.warning("subject or object node id not found:" + subjId + "/"+ objId);
							return true;
						}
	  					OrientVertex subjV = graph.getVertex(subjNDoc);
	  					OrientVertex objV = graph.getVertex(objNDoc);
//	  					try {
	  						graph.addEdge(null, subjV, newV, NdexClasses.Edge_E_subject);
	  						graph.addEdge(null, newV, objV, NdexClasses.Edge_E_object);
/*	  					} catch (ObjectNotFoundException e) {
	  						logger.info("Skipping creating network link for " + uuid );
//	  						continue;
	  					} catch (NdexException e) {
	  					// TODO Auto-generated catch block
	  					e.printStackTrace();
	  					return false; */
	
		  				counter++;
		  				if ( counter % 10000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "Edge commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "Edge copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        

	}
	
	
	private void copyGroups() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM group where isDeleted=false";
        
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
		  				String websiteURL = doc.field("websiteURL");
		  				String accName = doc.field(NdexClasses.account_P_accountName);
		  				String grpName = doc.field("organizationName");
		  				
		  				List<String> members = new ArrayList<>();
		  				List<String> admins = new ArrayList<>();
		  				
		  				for (ODocument d : getLinkedDocs(doc, "in_member")) {
		  					members.add((String)d.field(NdexClasses.ExternalObj_ID));
		  				}
		  				
		  				for (ODocument d : getLinkedDocs(doc, "in_groupadmin")) {
		  					admins.add((String)d.field(NdexClasses.ExternalObj_ID));
		  				}
		  				
		  				destConn.activateOnCurrentThread();
		  				ODocument newDoc = new ODocument(NdexClasses.Group)
		              			.fields(NdexClasses.ExternalObj_ID, uuid,
		              					NdexClasses.ExternalObj_cTime, ct,
		              					NdexClasses.ExternalObj_mTime,mt,
		              					"isDeleted", false,
		              					NdexClasses.account_P_accountName, accName,
		              					"description", desc,
		              					"imageURL", imgURL,
		              					"websiteURL", websiteURL,
		              					NdexClasses.GRP_P_NAME,grpName );
		  				newDoc.save();
		  				
	  					OrientVertex newV = graph.getVertex(newDoc);
		  				
		  				for ( String id : members) {
		  					ODocument userDoc;
							try {
								userDoc = dbDao.getRecordByUUIDStr(id,NdexClasses.User);
			  					OrientVertex vUser = graph.getVertex(userDoc);
	  							graph.addEdge(null, vUser, newV, NdexClasses.GRP_E_member);
							} catch (ObjectNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								return false;
							} catch (NdexException e) {
								e.printStackTrace();
								return false;
							}

		  				}
		  				
		  				for ( String id : admins) {
		  					ODocument userDoc;
							try {
								userDoc = dbDao.getRecordByUUIDStr(id,NdexClasses.User);
			  					OrientVertex vAdmin = graph.getVertex(userDoc);
	  							graph.addEdge(null, vAdmin, newV, Permissions.GROUPADMIN.toString().toLowerCase());
							} catch (ObjectNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								return false;
							} catch (NdexException e) {
								e.printStackTrace();
								return false;
							}

		  				}

		  				counter++;
		  				if ( counter % 5000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "group commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "user class copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        
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
		  				
		  				destConn.activateOnCurrentThread();
		  				ODocument newbtDoc = new ODocument(NdexClasses.User)
		              			.fields(NdexClasses.ExternalObj_ID, uuid,
		              					NdexClasses.ExternalObj_cTime, ct,
		              					NdexClasses.ExternalObj_mTime,mt,
		              					NdexClasses.account_P_accountName, accName,
		              					"isDeleted", false,
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
		  					destConn.commit();
		  					logger.info( "User commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "user class copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        

	}
	
	
	private void copyNetworkHeadNodes() {
        srcConnection.activateOnCurrentThread();

        String query = "SELECT FROM network where isDeleted=false and isComplete=true";
        
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
		  				String name = doc.field(NdexClasses.Network_P_name);
		  				String version = doc.field(NdexClasses.Network_P_version);
		  				String visibility = doc.field(NdexClasses.Network_P_visibility);

		  				if (visibility.equals("DISCOVERABLE")){
		  					logger.info("Network "+ uuid + " visiblity changed from DISCOVERABLE to PRIVATE during migration." );
		  					visibility="PRIVATE";
		  				}	
		  				Integer edgeCount = doc.field(NdexClasses.Network_P_edgeCount);
		  				Integer nodeCount = doc.field(NdexClasses.Network_P_nodeCount);
		  				Long ROId	= doc.field(NdexClasses.Network_P_readOnlyCommitId);
		  				Long cacheId = doc.field(NdexClasses.Network_P_cacheId);
		  				
		  				String srcFormat = doc.field(NdexClasses.Network_P_source_format);
		  				String provenance = doc.field("provenance");
		  				
		  				List<NdexPropertyValuePair> props = getProperties(doc);
		  				
		  				
		  				List<String> edits = new ArrayList<>();
		  				List<String> reads = new ArrayList<>();
		  				List<String> admins = new ArrayList<>();
		  				
		  				for (ODocument d : getLinkedDocs(doc, "in_read")) {
		  					reads.add((String)d.field(NdexClasses.ExternalObj_ID));
		  				}
		  				
		  				for (ODocument d : getLinkedDocs(doc, "in_admin")) {
		  					admins.add((String)d.field(NdexClasses.ExternalObj_ID));
		  				}
		  				
		  				for (ODocument d : getLinkedDocs(doc, "in_write")) {
		  					edits.add((String)d.field(NdexClasses.ExternalObj_ID));
		  				}
		  				

		  				destConn.activateOnCurrentThread();
		  				ODocument newDoc = new ODocument(NdexClasses.Network)
		              			.fields(NdexClasses.ExternalObj_ID, uuid,
		              					NdexClasses.ExternalObj_cTime, ct,
		              					NdexClasses.ExternalObj_mTime,mt,
		              					"isDeleted", false,
		              					NdexClasses.Network_P_isComplete,true,
		              					"isLocked", false,
		              					NdexClasses.Network_P_readOnlyCommitId, ROId,
		              					NdexClasses.Network_P_cacheId, cacheId,
		              					NdexClasses.Network_P_desc, desc,
		              					NdexClasses.Network_P_name, name,
		              					NdexClasses.Network_P_edgeCount, edgeCount,
		              					NdexClasses.Network_P_nodeCount, nodeCount,
		              					NdexClasses.Network_P_provenance, provenance,
		              					NdexClasses.Network_P_source_format, srcFormat,
		              					NdexClasses.Network_P_version, version,
		              					NdexClasses.Network_P_visibility, visibility,
		              					NdexClasses.ndexProperties, props);
		  				newDoc.save();
		  				
	  					OrientVertex newV = graph.getVertex(newDoc);
		  				
		  				for ( String id : reads) {
		  					ODocument userDoc;
							try {
								userDoc = dbDao.getRecordByUUIDStr(id,null);
			  					OrientVertex vUser = graph.getVertex(userDoc);
	  							graph.addEdge(null, vUser, newV, NdexClasses.account_E_canRead);
							} catch (ObjectNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								return false;
							} catch (NdexException e) {
								e.printStackTrace();
								return false;
							}

		  				}
		  				
		  				for ( String id : edits) {
		  					ODocument userDoc;
							try {
								userDoc = dbDao.getRecordByUUIDStr(id,null);
			  					OrientVertex vUser = graph.getVertex(userDoc);
	  							graph.addEdge(null, vUser, newV, NdexClasses.account_E_canEdit);
							} catch (ObjectNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								return false;
							} catch (NdexException e) {
								e.printStackTrace();
								return false;
							}

		  				}
		  				
		  				for ( String id : admins) {
		  					ODocument userDoc;
							try {
								userDoc = dbDao.getRecordByUUIDStr(id,null);
			  					OrientVertex vAdmin = graph.getVertex(userDoc);
	  							graph.addEdge(null, vAdmin, newV, NdexClasses.E_admin);
							} catch (ObjectNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								return false;
							} catch (NdexException e) {
								e.printStackTrace();
								return false;
							}

		  				}

		  				counter++;
		  				if ( counter % 5000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "group commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "user class copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        

	}

	
	protected boolean connectToNetworkHeadNode(ODocument elementDoc, String uuid, String edgeName) {
		OrientVertex newV = graph.getVertex(elementDoc);
			try {
				ODocument hDoc = dbDao.getRecordByUUIDStr(uuid,null);
	  			OrientVertex vNet = graph.getVertex(hDoc);
				graph.addEdge(null, vNet, newV, edgeName);
			} catch (ObjectNotFoundException e) {
				logger.info("Skipping creating network link for " + uuid );
//				continue;
			} catch (NdexException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		return true;
	}
	
	protected static List<NdexPropertyValuePair> getProperties(ODocument doc) { 
	
		List<NdexPropertyValuePair> props = new ArrayList<>();
		for ( ODocument propDoc :  getLinkedDocs(doc, "out_ndexProps" )) {
			NdexPropertyValuePair p = getPropertyFromDoc (propDoc);
			if ( p !=null ) 
				props.add(p);
		}
		return props;
	}
	// direction "out_" or "in_"
	
	protected static Iterable<ODocument> getLinkedDocs(ODocument srcDoc, String edgeString) {	
	    	
		   Object f = srcDoc.field(edgeString);
		    	
		    	if ( f == null) return emptyDocs;
		    	
		    	if ( f instanceof ODocument)
		    		 return new OrientDBIterableSingleLink((ODocument)f);
		    	
		    	@SuppressWarnings("unchecked")
				Iterable<ODocument> iterable = (Iterable<ODocument>)f;
				return iterable;
		    	     
	}

	protected static Iterable<? extends Object> getLinkedObjs(ODocument srcDoc, String edgeString) {	
    	
		   Object f = srcDoc.field(edgeString);
		    	
		    	if ( f == null) return emptyDocs;
		    	
		    	if ( f instanceof ODocument)
		    		 return new OrientDBIterableSingleLink((ODocument)f);
		    	
		    	@SuppressWarnings("unchecked")
				Iterable<Object> iterable = (Iterable<Object>)f;
				return iterable;
		    	     
	}
	
	private static NdexPropertyValuePair getPropertyFromDoc (ODocument doc) {
		NdexPropertyValuePair result = new NdexPropertyValuePair();
		
		result.setValue( (String)doc.field("value"));
		result.setDataType((String)doc.field( "dType"));
		
		String predicateStr = doc.field("predicateStr");
		
		if ( predicateStr == null) {
			Object btObj = doc.field("out_prop");
			if ( ! (btObj instanceof ODocument)) {
				System.err.println("Record " + doc.getIdentity().toString() + " has field out_prop as a record id. skip it." );
				return null;
			}
			ODocument bt = (ODocument) btObj;
			predicateStr = bt.field("name");
			Object nsRid = bt.field("out_baseTermNS");
			String prefix = null;
			if ( nsRid != null) {
				if ( nsRid instanceof ORecordId) {
					logger.warning("record if " + nsRid + " found in db for baseterm. ignore this baseterm: " + 
								bt.getIdentity().toString());
				} else { 
					
					ODocument ns = (ODocument)nsRid;
					
					String uri = ns.field("uri");
					prefix =ns.field(NdexClasses.ns_P_prefix);
					if ( prefix != null)
						predicateStr = prefix +":" + predicateStr;
					else if ( uri !=null ) {
						predicateStr = uri + predicateStr;
					}
				}	
				
			}
		}
		result.setPredicateString(predicateStr);
		return result;
		
	}
	
	private void createSolrIndex() {
        destConn.activateOnCurrentThread();

        String query = "SELECT FROM network where isDeleted=false and isComplete=true";
        
		counter = 0;
		
		OSQLAsynchQuery<ODocument> asyncQuery =
				new OSQLAsynchQuery<ODocument>(query, new OCommandResultListener() { 
		            @Override 
		            public boolean result(Object iRecord) { 
		            	ODocument doc = (ODocument) iRecord;
		            	try {
							networkDao.createSolrIndex(doc);
							Thread.sleep(2000);
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
	
	private void copySquenceId() {
        destConn.activateOnCurrentThread();

        ODocument vdoc = new ODocument();
        vdoc = vdoc.field("f1", seqId).save();
        ODictionary dictionary = destConn.getDictionary();
        dictionary.put("NdexSeq", vdoc);
    	destConn.commit();
        
	}	
	private void copyExportTasks() {
        srcConnection.activateOnCurrentThread();

        String query = "select from task where isDeleted=false and taskType like 'EXPORT%' and status = 'COMPLETED' and out_ownedBy is not null";
        
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
		  				//String status = doc.field(NdexClasses.Task_P_status);
		  				String resource = doc.field(NdexClasses.Task_P_resource);
		  				Date startTime = doc.field(NdexClasses.Task_P_startTime);
		  				Date endTime = doc.field(NdexClasses.Task_P_endTime);
		  				String fmt = doc.field(NdexClasses.Task_P_fileFormat);
		  				String ownerUUID = doc.field("ownerUUID");
		  				if ( ownerUUID == null) {
		  					ODocument d = doc.field("out_ownedBy");
		  					ownerUUID = d.field(NdexClasses.ExternalObj_ID);
		  				}
		  				
		  				destConn.activateOnCurrentThread();
		  				ODocument newDoc = new ODocument(NdexClasses.Task)
		              			.fields(NdexClasses.ExternalObj_ID, uuid,
		              					NdexClasses.ExternalObj_cTime, ct,
		              					NdexClasses.ExternalObj_mTime,mt,
		              					"description", desc,
		              					NdexClasses.Task_P_status, "COMPLETED",
		              					NdexClasses.Task_P_priority, "MEDIUM",
		              					NdexClasses.Task_P_progress, 0,
		              					NdexClasses.Task_P_taskType, TaskType.EXPORT_NETWORK_TO_FILE.toString(),
		              					"isDeleted", false,
		              					NdexClasses.Task_P_resource,resource,
		              					NdexClasses.Task_P_startTime, startTime,
		              					NdexClasses.Task_P_endTime, endTime,
		              					NdexClasses.Task_P_fileFormat, fmt,
		              					"ownerUUID", ownerUUID);
		  				newDoc.save();
		  				
	  					OrientVertex newV = graph.getVertex(newDoc);
	  					
						try {
								ODocument userDoc = dbDao.getRecordByUUIDStr(ownerUUID,null);
			  					OrientVertex vUser = graph.getVertex(userDoc);
	  							graph.addEdge(null,  newV, vUser, NdexClasses.Task_E_owner);
						} catch (ObjectNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								return false;
						} catch (NdexException e) {
								e.printStackTrace();
								return false;
						}


		  				counter++;
		  				if ( counter % 5000 == 0 ) {
		  					destConn.commit();
		  					logger.info( "task commited " + counter + " records.");
		  				}
		  				srcConnection.activateOnCurrentThread();
		            	
		  				return true; 
		            } 
		   
		            @Override 
		            public void end() { 
		            	destConn.activateOnCurrentThread();
		            	destConn.commit();
		            	logger.info( "task class copy completed. Total record: " + counter);
		            }
		            
		          });
		        
        
        srcConnection.command(asyncQuery).execute(); 
        
	}
	
	
	private void closeAll() {
		srcConnection.activateOnCurrentThread();
		srcConnection.close();
		destConn.activateOnCurrentThread();
		graph.shutdown();
    	NdexDatabase.close();
	}
	
	public static void main(String[] args) throws NdexException, SolrServerException, IOException {
		
		if ( args.length != 1) {
			System.out.println("Usage: Migrator1_2to1_3 <Ndex 1.2 db path>\n\n example: \n\n Migrator1_2to1_3 /opt/ndex/orientdb/databases/ndex_1_2\n");
			
		}	
		//"plocal:/opt/ndex/orientdb/databases/ndex_1_2";
		Migrator1_2to1_3 migrator = new Migrator1_2to1_3(args[0]);
/*
		migrator.copySquenceId();

		migrator.copyUsers();
		migrator.copyGroups();
 		migrator.copyNetworkHeadNodes();
		migrator.copyExportTasks();
		
		migrator.copyNamespaces();
		
		migrator.copyBaseTerms();
		
		migrator.copySupport();

		
		migrator.copyCitations(); 
		
		migrator.copyFunctionTerms(); 
		
		migrator.copyReifiedEdgeElements(); */
		
		migrator.copyNodes();
	
		migrator.copyEdges();
  
		migrator.copyReifiedEdgeLinks();
		
		
		migrator.createSolrIndex();
		migrator.closeAll();
    	logger.info( "DB migration completed.");
	}
	
}
