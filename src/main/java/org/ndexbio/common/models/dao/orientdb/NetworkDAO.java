package org.ndexbio.common.models.dao.orientdb;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.model.object.NdexProperty;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.PropertyGraphEdge;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.model.object.network.PropertyGraphNode;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.model.object.network.VisibilityType;

import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NetworkDAO {
	
	private ODatabaseDocumentTx db;
	
	//flag to specify whether need to search in the current un-commited transaction. 
	//This is used to work around the problem that sql query doesn't search the current 
	// uncommited transaction in OriantDB.
	private boolean searchCurrentTx;  
	
	public NetworkDAO (ODatabaseDocumentTx db) {
		this.db = db;
		this.searchCurrentTx = false;
	}

	public NetworkDAO (ODatabaseDocumentTx db, boolean searchCurrentTransaction) {
		this.db = db;
		this.searchCurrentTx = searchCurrentTransaction;
	}

/*	public NetworkDAO(NdexDatabase db, UUID networkID) {
		this.db = db;
		this.network = getNetworkById(networkID);
		
	} */
	
	
	/**
	 * Returns a subnetwork based on a block of edges selected from the network specified by networkUUID 
	 *     based on the specified blockSize and number of blocks to skip. It is intended to be used to 
	 *     incrementally "page" through a network by edges and forms the basis for operations like incremental copy. 
	 *     The returned network is fully poplulated and 'self-sufficient', including all nodes, terms, supports, 
	 *     citations, and namespaces referenced by the edges. 
	 *     The query selects a number of edges specified by the 'blockSize' parameter, 
	 *     starting at an offset specified by the 'skipBlocks' parameter. 
	 * @param networkID
	 * @param skipBlocks
	 * @param blockSize
	 * @return the subnetwork as a Network Object.   
	 */
	public Network getNetwork (UUID networkID, int skipBlocks, int blockSize) {
		ODocument nDoc = getNetworkDocByUUID(networkID);
		
	    if (nDoc == null) return null;
	    
	    Network network = new Network();  //result holder
	    
	    int startPosition = skipBlocks * blockSize;
	    int counter = 0;
	    int endPosition = skipBlocks * blockSize + blockSize;
	    
	    // get Edges
	    Map<Long, Edge> edgeMap = network.getEdges();
	    
	    network.getNodes();
        for (OIdentifiable nodeDoc : new OTraverse()
      	              	.field("out_"+ NdexClasses.Network_E_Edges )
      	              	.target(nDoc)
                      	.predicate( new OSQLPredicate("$depth <= 1"))) {
  

            ODocument doc = (ODocument) nodeDoc;
         
            if ( doc.getClassName().equals(NdexClasses.Edge) ) {

            	if ( counter >= endPosition) break;

                counter ++;
            	
            	if ( counter >= startPosition )  {
              	   Edge e = new Edge();
            	 
            	   ODocument subDoc = doc.field(NdexClasses.Edge_E_subject);
            	   
            	   //TODO: populate the content in Network
/*            	   ODocument 
                   Edge nd = NetworkDAO.getEdge(doc);
                   if ( nd != null)
               	      edgeList.add(nd);
                   else
               	     throw new NdexException("Error occurred when getting edge information from db "+ doc); */
                }
            } 	
        }
        
		 return network; 
	}
	
	
	public Network getNetworkById(UUID id) {
		ODocument nDoc = getNetworkDocByUUID(id);
		
        return (nDoc!=null) ? getNetwork(nDoc): null;
	}
	
    private ODocument getNetworkDocByUUID(UUID id) {
	     String query = "select from " + NdexClasses.Network + " where UUID='"
                 +id.toString()+"'";
         final List<ODocument> networks = db.query(new OSQLSynchQuery<ODocument>(query));
  
         if (networks.isEmpty())
 	        return null;
         
    	return networks.get(0);
    }

    
	public PropertyGraphNetwork getProperytGraphNetworkById (UUID networkID, int skipBlocks, int blockSize) {
		ODocument nDoc = getNetworkDocByUUID(networkID);
		
	    if (nDoc == null) return null;
	    
	    PropertyGraphNetwork network = new PropertyGraphNetwork();
	    
	    int startPosition = skipBlocks * blockSize;
	    int counter = 0;
	    int endPosition = skipBlocks * blockSize + blockSize;
	    
	    // get Edges
        Map<Long,PropertyGraphNode> nodeMap = network.getNodes();
        Collection<PropertyGraphEdge> edgeList = network.getEdges();

        for (OIdentifiable nodeDoc : new OTraverse()
      	              	.field("out_"+ NdexClasses.Network_E_Edges )
      	              	.target(nDoc)
                      	.predicate( new OSQLPredicate("$depth <= 1"))) {
  

            ODocument doc = (ODocument) nodeDoc;
         
            if ( doc.getClassName().equals(NdexClasses.Edge) ) {

            	if ( counter >= endPosition) break;

                counter ++;
            	
            	if ( counter >= startPosition )  {
              	   
            	    PropertyGraphEdge e = NetworkDAO.getPropertyGraphEdge(doc);
        
            	    edgeList.add(e);
            	    
            	    if ( ! nodeMap.containsKey(e.getSubjectId())) {
            	        ODocument subDoc = doc.field("in_" +  NdexClasses.Edge_E_subject);
            	        PropertyGraphNode node = NetworkDAO.getPropertyGraphNode(subDoc);
            	        nodeMap.put(node.getId(),node);
            	    }    
            	    
            	    if ( ! nodeMap.containsKey(e.getObjectId())) {
                	    ODocument objDoc = doc.field("out_" + NdexClasses.Edge_E_object);
            	        PropertyGraphNode node = NetworkDAO.getPropertyGraphNode(objDoc);
            	        nodeMap.put(node.getId(),node);
            	    }
            	    
                }
            } 	
        }
        
        
   	    return network; 
		
	}
    
    
    
	public PropertyGraphNetwork getProperytGraphNetworkById(UUID id) throws NdexException {
		
		//TODO: populate namespace, citations and support
		
		ODocument networkDoc = getNetworkDocByUUID(id);
		
		if (networkDoc == null) return null;

		PropertyGraphNetwork network = new PropertyGraphNetwork();

        network.getProperties().add(new NdexProperty(PropertyGraphNetwork.uuid, id.toString()));
        
        Map<Long,PropertyGraphNode> nodeList = network.getNodes();
         for (OIdentifiable nodeDoc : new OTraverse()
       	    .field("out_"+ NdexClasses.Network_E_Nodes )
            .target(networkDoc)
            .predicate( new OSQLPredicate("$depth <= 1"))) {

              ODocument doc = (ODocument) nodeDoc;
          
              if ( doc.getClassName().equals(NdexClasses.Node)) {
          
                  PropertyGraphNode nd = NetworkDAO.getPropertyGraphNode(doc);
                  if ( nd != null)
                	  nodeList.put(nd.getId(),nd);
                  else
                	  throw new NdexException("Error occurred when getting node information from db "+ doc);
              }
         }
         
         Collection<PropertyGraphEdge> edgeList = network.getEdges();
         for (OIdentifiable nodeDoc : new OTraverse()
       	    .field("out_"+ NdexClasses.Network_E_Edges )
            .target(networkDoc)
            .predicate( new OSQLPredicate("$depth <= 1"))) {

              ODocument doc = (ODocument) nodeDoc;
          
              if ( doc.getClassName().equals(NdexClasses.Edge)) {
          
                  PropertyGraphEdge nd = NetworkDAO.getPropertyGraphEdge(doc);
                  if ( nd != null)
                	  edgeList.add(nd);
                  else
                	  throw new NdexException("Error occurred when getting edge information from db "+ doc);
              }
         }
         
		 return network; 
	}
	

	private static PropertyGraphEdge getPropertyGraphEdge(ODocument doc) {
		PropertyGraphEdge e = new PropertyGraphEdge();
		e.setId((long)doc.field(NdexClasses.Element_ID));
		
		ODocument s =  doc.field("in_"+NdexClasses.Edge_E_subject);
		e.setSubjectId((long) s.field(NdexClasses.Element_ID));
		
		ODocument predicateDoc = (ODocument)doc.field("out_"+NdexClasses.Edge_E_predicate);
		e.setPredicate((String)predicateDoc.field(NdexClasses.BTerm_P_name));
		
		e.setObjectId((long)
			    ((ODocument)doc.field("out_"+NdexClasses.Edge_E_object))
			        .field(NdexClasses.Element_ID));
		
		return e;
	}
	
    private static PropertyGraphNode getPropertyGraphNode(ODocument doc) {
    	PropertyGraphNode n = new PropertyGraphNode ();
        n.setId((long)doc.field(NdexClasses.Element_ID));    	
       
        //populate node name
        String name = doc.field(NdexClasses.Node_P_name);
        if ( name != null) {
        	n.getProperties().add(new NdexProperty(PropertyGraphNode.name, name));
        }
        
    	ODocument o = doc.field("out_" + NdexClasses.Node_E_represents);
    	String termId = o.field(NdexClasses.BTerm_P_name);
    	
    	ODocument nsDoc = o.field("out_"+NdexClasses.BTerm_E_Namespace);
    	NdexProperty p ; 
    	if ( nsDoc == null ) {
    		p = new NdexProperty( PropertyGraphNode.represents,termId );
    	} else {
    		String prefix = nsDoc.field(NdexClasses.ns_P_prefix);
    		p = new NdexProperty(PropertyGraphNode.represents, prefix + ":"+termId);
    	}	
   		n.getProperties().add(p);
   		
   		//TODO: populate citations etc.
   		
    	return n;
    }
	
	
	private static Network getNetwork(ODocument n) {
		
		Network result = new Network();
		
		SetNetworkSummary(n, result);

		//TODO: populate all fields.
		
		return result;
	}
	
	// first parameter is namespaceID, second parameter is base term
	final static private String baseTermQuery = "select from (traverse in_" + NdexClasses.BTerm_E_Namespace +
			  " from (select from " + NdexClasses.Namespace + " where " + 
			NdexClasses.Element_ID +"= ?)) where @class='" +NdexClasses.BaseTerm + "' and "+NdexClasses.BTerm_P_name +
	    		 " =?"; 
	final static private String baseTermQuery2 = "select from " + NdexClasses.BaseTerm +
			  " where out_" + NdexClasses.BTerm_E_Namespace + " is null and "+NdexClasses.BTerm_P_name +
	    		 " =?"; 
	
	final static private String baseTermQuery3 = "select from (traverse out_" + NdexClasses.Network_E_BaseTerms +
			" from (select from " + NdexClasses.Network + " where " +
  		  NdexClasses.Network_P_UUID + "= ?)) where @class='"+  NdexClasses.BaseTerm + "' and " + 
	       NdexClasses.BTerm_P_name +  " =?";
	// namespaceID < 0 means baseTerm has a local namespace
	public BaseTerm getBaseTerm(String baseterm, long namespaceID, String networkId) {
		List<ODocument> terms;
		
		if ( namespaceID >= 0 ) {
			//TODO: check out why this method throws exceptions from Orientdb.
		/*	OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(baseTermQuery);
			terms = db.command(query).execute((Long)namespaceID, baseterm); */
			String query = "select from (traverse in_" + NdexClasses.BTerm_E_Namespace +
			  " from (select from " + NdexClasses.Namespace + " where " + 
			NdexClasses.Element_ID +"= "+ namespaceID+ ")) where @class='" +NdexClasses.BaseTerm + 
			     "' and "+NdexClasses.BTerm_P_name +  " ='" + baseterm +"'";
			terms = db.query(new OSQLSynchQuery<ODocument>(query));
			
		} else {
/*			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(baseTermQuery3);
			terms = db.command(query).execute( baseterm, networkId); */
			String query = "select from (traverse out_" + NdexClasses.Network_E_BaseTerms +
					" from (select from " + NdexClasses.Network + " where " +
			  		  NdexClasses.Network_P_UUID + "= '" + networkId + "')) where @class='"+  NdexClasses.BaseTerm + "' and " + 
				       NdexClasses.BTerm_P_name +  " ='" + baseterm + "'";
			terms = db.query(new OSQLSynchQuery<ODocument>(query));
		}
		     
		if (terms.isEmpty())
		    	 return null;
             return getBaseTerm(terms.get(0));
             
       //TODO: need to check the current transaction.      
	}
	
	private static BaseTerm getBaseTerm(ODocument o) {
		BaseTerm t = new BaseTerm();
		t.setId((long)o.field(NdexClasses.Element_ID));
		t.setName((String)o.field(NdexClasses.BTerm_P_name));
		
		ODocument nsDoc = o.field("out_"+NdexClasses.BTerm_E_Namespace);
		if ( nsDoc != null) 
			t.setNamespace((long)nsDoc.field(NdexClasses.Element_ID));
		else
			t.setNamespace(-1);
		
		return t;
	}
	
	public ODocument getDocumentByElementId(String NdexClassName, long elementID) {
		String query = "select from " + NdexClassName + " where " + 
		        NdexClasses.Element_ID + "=" +elementID;
	        final List<ODocument> nss = db.query(new OSQLSynchQuery<ODocument>(query));
	  
	        if (!nss.isEmpty())
	  	       return nss.get(0);
	         
	         List<ORecordOperation> txOperations = db.getTransaction().getRecordEntriesByClass(NdexClassName);
	         for (ORecordOperation op : txOperations) {
	         	long id = ((ODocument) op.getRecord()).field(NdexClasses.Element_ID);
	         if (id == elementID)
	            return (ODocument) op.getRecord();
	         }
	         return null;
	}
	
	//TODO: make a better implementation for this function.
	public ODocument getDocumentByElementId(long elementID) {
		ODocument result = getDocumentByElementId(NdexClasses.Node, elementID);
		if ( result != null) return result;
		
		result = getDocumentByElementId(NdexClasses.Edge, elementID);
		if ( result != null) return result;
		
		result = getDocumentByElementId(NdexClasses.BaseTerm, elementID);
		if ( result != null) return result;

		result = getDocumentByElementId(NdexClasses.Citation, elementID);
		if ( result != null) return result;
		result = getDocumentByElementId(NdexClasses.FunctionTerm, elementID);
		if ( result != null) return result;
		result = getDocumentByElementId(NdexClasses.Namespace, elementID);
		if ( result != null) return result;
		result = getDocumentByElementId(NdexClasses.ReifiedEdgeTerm, elementID);
		if ( result != null) return result;
		result = getDocumentByElementId(NdexClasses.Support, elementID);
		if ( result != null) return result;
		return null;
	}
	
	public Namespace getNamespace(String prefix, String URI, UUID networkID ) {
			String query = "select from (traverse out_" +
		    		  NdexClasses.Network_E_Namespace +" from (select from "
		    		  + NdexClasses.Network + " where " +
		    		  NdexClasses.Network_P_UUID + "='" + networkID + 
		    		  "')) where @class='"+  NdexClasses.Namespace + "' and ";
			if ( prefix != null) {
		      query = query + NdexClasses.ns_P_prefix + "='"+ prefix +"'";
			}   else {
			  query = query + NdexClasses.ns_P_uri + "='"+ URI +"'";	
			}	
		    final List<ODocument> nss = db.query(new OSQLSynchQuery<ODocument>(query));
		     
		     if (nss.isEmpty())
		    	 return null;
             Namespace result = getNamespace(nss.get(0));
             return result;
	}
	
	
	public Citation getCitation(String title, String idType, String identifier, UUID networkID) {
		String query = "select from " + NdexClasses.Citation + " where " +
	    		  NdexClasses.Citation_P_title + "='" + title +"'";
	    final List<ODocument> citations = db.query(new OSQLSynchQuery<ODocument>(query));
	    
	    ODocument c = null;
        for (ODocument x : citations ) {
        	ODocument networkDoc = x.field("in_" + NdexClasses.Network_E_Citations);
        	String uuidStr = networkDoc.field(NdexClasses.Network_P_UUID);
        	if (networkID.toString().equals(uuidStr)) {

        		if ( identifier != null) {  // check identifier
        			for (OIdentifiable ndexPropertyDoc : new OTraverse()
        	       	    .field("out_"+ NdexClasses.E_ndexProperties )
        	            .target(x)
        	            .predicate( new OSQLPredicate("$depth <= 1"))) {

        	              ODocument doc = (ODocument) ndexPropertyDoc;
        	          
        	              if ( doc.getClassName().equals(NdexClasses.NdexProperty)) {
        	                  if ( doc.field(NdexClasses.ndexProp_P_predicateStr).equals(idType)
        	                		&& 
        	                	   doc.field(NdexClasses.ndexProp_P_value).equals(identifier)) {
        	                	  c = x;
        	                	  break;
        	                  }
        	              }
        	         }
        		} else {	
        		  c = x;
        		  break;
        		}
        	}
        }
        
        if (c == null) return null;
        
        // construct the object
        return getCitationFromDoc(c);
	}
	
	
	private Citation getCitationFromDoc(ODocument doc) {
		Citation result = new Citation();
		result.setId((long)doc.field(NdexClasses.Element_ID));
		result.setTitle((String)doc.field(NdexClasses.Citation_P_title));
		
		
        for (OIdentifiable propRec : new OTraverse()
   	                  .field("out_"+ NdexClasses.E_ndexProperties )
   	                  .target(doc)
   	                  .predicate( new OSQLPredicate("$depth <= 1"))) {

             ODocument propDoc = (ODocument) propRec;
                 
             if ( doc.getClassName().equals(NdexClasses.NdexProperty)) {
            	 result.getProperties().add(getNdexPropertyFromDoc(propDoc));
             }
        }

		return result;
	}
	
	
	private NdexProperty getNdexPropertyFromDoc(ODocument doc) {
		NdexProperty p = new NdexProperty();
		p.setPredicateString((String)doc.field(NdexClasses.ndexProp_P_predicateStr));
		p.setValue((String)doc.field(NdexClasses.ndexProp_P_value)) ;
		return p;
	}
	
	//TODO: change this to direct index access in the future.
	public ODocument getNamespaceDocByEId (long elementID) {
		String query = "select from " + NdexClasses.Namespace + " where " + 
	        NdexClasses.Element_ID + "=" +elementID;
        final List<ODocument> nss = db.query(new OSQLSynchQuery<ODocument>(query));
  
        if (!nss.isEmpty())
 	       return nss.get(0);
        
        List<ORecordOperation> txOperations = db.getTransaction().getRecordEntriesByClass(NdexClasses.Namespace);
        for (ORecordOperation op : txOperations) {
        	long id = ((ODocument) op.getRecord()).field(NdexClasses.Element_ID);
        if (id == elementID)
           return (ODocument) op.getRecord();
        }
        return null;
	}
	
    private static Namespace getNamespace(ODocument ns) {
       Namespace rns = new Namespace();
       rns.setId((long)ns.field("id"));
       rns.setPrefix((String)ns.field(NdexClasses.ns_P_prefix));
       rns.setUri((String)ns.field(NdexClasses.ns_P_uri));
       return rns;
    } 
    
    private static final String nodeQuery = "select from (traverse in_" + 
         NdexClasses.Node_E_represents + " from (select from "+ NdexClasses.BaseTerm + " where " +
         NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.Node +"'";
    
    public Node findNodeByBaseTermId(long baseTermID) {
		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(nodeQuery);
		List<ODocument> nodes = db.command(query).execute( baseTermID);
    	
		if (nodes.isEmpty())
			return null;
		
    	return getNode(nodes.get(0));
    }

    private static final String functionTermNodeQuery = 
    		"select from (traverse in_" + NdexClasses.Node_E_represents + 
    		" from (select from "+ NdexClasses.FunctionTerm + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.Node +"'";
       
    public Node findNodeByFunctionTermId(long functionTermID) {
   		OSQLSynchQuery<ODocument> query = 
   				new OSQLSynchQuery<ODocument>(functionTermNodeQuery);
   		List<ODocument> nodes = db.command(query).execute( functionTermID);
       	
   		if (nodes.isEmpty())
   			return null;
   		
       	return getNode(nodes.get(0));
    }
    
    private static final String reifedEdgeTermNodeQuery = 
    		"select from (traverse in_" + NdexClasses.Node_E_represents + 
    		" from (select from "+ NdexClasses.ReifiedEdgeTerm + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.Node +"'";

    public Node findNodeByReifiedEdgeTermId (long reifiedEdgeTermId) {
   		OSQLSynchQuery<ODocument> query = 
   				new OSQLSynchQuery<ODocument>(reifedEdgeTermNodeQuery);
   		List<ODocument> nodes = db.command(query).execute( reifiedEdgeTermId);
       	
   		if (nodes.isEmpty())
   			return null;
   		
       	return getNode(nodes.get(0));
    }
    
    private static final String refiedEdgeTermQuery = 
    		"select from (traverse in_" + NdexClasses.ReifedEdge_E_edge + 
    		" from (select from "+ NdexClasses.Edge + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ 
    		NdexClasses.ReifiedEdgeTerm +"'";
       
    public ReifiedEdgeTerm findReifiedEdgeTermByEdgeId(long edgeId) {
   		OSQLSynchQuery<ODocument> query = 
   				new OSQLSynchQuery<ODocument>(refiedEdgeTermQuery);
   		List<ODocument> nodes = this.db.command(query).execute( edgeId);
       	
   		if (!nodes.isEmpty()) {
   			ReifiedEdgeTerm t = new ReifiedEdgeTerm();
   			t.setId((long)nodes.get(0).field(NdexClasses.Element_ID));
   			t.setEdgeId(edgeId);
   			return t;
   		}

   		if ( this.searchCurrentTx ) {
	         List<ORecordOperation> txOperations = db.getTransaction().getRecordEntriesByClass(NdexClasses.Edge);
	         for (ORecordOperation op : txOperations) {
	         	long id = ((ODocument) op.getRecord()).field(NdexClasses.Element_ID);
	            if (id == edgeId) {
	            	for (OIdentifiable reifiedTRec : new OTraverse()
      	       	    			.field("in_"+ NdexClasses.ReifedEdge_E_edge )
      	       	    			.target((ODocument) op.getRecord())
      	       	    			.predicate( new OSQLPredicate("$depth <= 1"))) {

	       				ODocument doc = (ODocument) reifiedTRec;
       	          
	       				if ( doc.getClassName().equals(NdexClasses.ReifiedEdgeTerm)) {
	       		   				ReifiedEdgeTerm t = new ReifiedEdgeTerm();
	       		   				t.setId((long)doc.field(NdexClasses.Element_ID));
	       		   				t.setEdgeId(edgeId);
	       						return t;
	       					
	       				}
	       			}
	            }
	         }
   		}
       	return null;
    }
    /*
    private ReifiedEdgeTerm getReifiedTermFromDocument(ODocument doc) {
    	ReifiedEdgeTerm rt = new ReifiedEdgeTerm();
    	rt.setEdgeId(termEdge);
    } */
    
    public static Node getNode(ODocument nodeDoc) {
    	Node n = new Node();

    	n.setId((long)nodeDoc.field(NdexClasses.Element_ID));
    	n.setName((String)nodeDoc.field(NdexClasses.Node_P_name));
    	return n;
    }
    
 

    private static NetworkSummary SetNetworkSummary(ODocument doc, NetworkSummary nSummary) {
    	nSummary.setCreationDate((Date)doc.field(NdexClasses.Network_P_cDate));
    	nSummary.setExternalId(UUID.fromString((String)doc.field(NdexClasses.Network_P_UUID)));
    	nSummary.setName((String)doc.field(NdexClasses.Network_P_name));
    	nSummary.setDescription((String)doc.field(NdexClasses.Network_P_desc));
    	
    	nSummary.setModificationDate((Date)doc.field(NdexClasses.Network_P_mDate));
    	nSummary.setEdgeCount((int)doc.field(NdexClasses.Network_P_edgeCount));
    	nSummary.setNodeCount((int)doc.field(NdexClasses.Network_P_nodeCount));
    	nSummary.setVersion((String)doc.field(NdexClasses.Network_P_version));
        nSummary.setVisibility(VisibilityType.valueOf((String)doc.field(NdexClasses.Network_P_visibility)));

        return nSummary;
    }
    
    public static NetworkSummary getNetworkSummary(ODocument doc) {
    	NetworkSummary networkSummary = new NetworkSummary();
    	return SetNetworkSummary(doc,networkSummary);
    }
    
    public Citation getCitationById(long elementId) {
    	ODocument doc = getDocumentByElementId(NdexClasses.Citation,elementId);
    	if ( doc == null) return null;
    	return getCitationFromDoc(doc);
    	
    }
    
    public Support getSupport(String text, long citationId) {
		String query = "select from " + NdexClasses.Citation + " where " + 
		        NdexClasses.Element_ID + "=" + citationId;
	    final List<ODocument> citations = db.query(new OSQLSynchQuery<ODocument>(query));
	  
	    if ( !citations.isEmpty()) {
   			for (OIdentifiable supportRec : new OTraverse()
       	       	    .field("in_"+ NdexClasses.Support_E_citation )
       	            .target(citations.get(0))
       	            .predicate( new OSQLPredicate("$depth <= 1"))) {

   	              ODocument doc = (ODocument) supportRec;
        	          
        	      if ( doc.getClassName().equals(NdexClasses.Support)) {
        	         if ( doc.field(NdexClasses.Support_P_text).equals(text) ) {
        	             return getSupportFromDoc(doc);
        	         }
       	          }
        	}
	    }
        
    	if ( this.searchCurrentTx) {
	         List<ORecordOperation> txOperations = db.getTransaction().getRecordEntriesByClass(NdexClasses.Citation);
	         for (ORecordOperation op : txOperations) {
	         	long id = ((ODocument) op.getRecord()).field(NdexClasses.Element_ID);
	            if (id == citationId) {
	            	for (OIdentifiable supportRec : new OTraverse()
       	       	    			.field("in_"+ NdexClasses.Support_E_citation )
       	       	    			.target((ODocument) op.getRecord())
       	       	    			.predicate( new OSQLPredicate("$depth <= 1"))) {

	       				ODocument doc = (ODocument) supportRec;
        	          
	       				if ( doc.getClassName().equals(NdexClasses.Support)) {
	       					if ( doc.field(NdexClasses.Support_P_text).equals(text) ) {
	       						return getSupportFromDoc(doc);
	       					}
	       				}
	       			}
	        	    
	            }
	         }
    	}
    	return null;
    }
    
    private static Support getSupportFromDoc(ODocument doc) {
    	Support s = new Support();
    	s.setText((String)doc.field(NdexClasses.Support_P_text));
    	s.setId((long)doc.field(NdexClasses.Element_ID));
    	ODocument citationDoc = doc.field("out_" + NdexClasses.Support_E_citation);
    	s.setCitation((long)citationDoc.field(NdexClasses.Element_ID));
    	
    	return s;
    	
    }

    
    private static final String functionTermQuery = "select from (traverse in_" + 
            NdexClasses.FunctionTerm_E_baseTerm + " from (select from "+ NdexClasses.BaseTerm + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.FunctionTerm +"'";

    // input parameter is a "rawFunctionTerm", which as elementid = -1;
    // This function will find the correspondent FunctionTerm from db.
    public FunctionTerm getFunctionTerm(FunctionTerm func) {
   		OSQLSynchQuery<ODocument> query = 
   				new OSQLSynchQuery<ODocument>(functionTermQuery);
   		List<ODocument> nodes = db.command(query).execute( func.getFunctionTermId());
       	
   		if (nodes.isEmpty())
   			return null;
   	
   		// check the parameters.
   		for ( ODocument n : nodes ) {
   			int counter = 0;
   			for (OIdentifiable parameterRec : new OTraverse()
	       	    .field("out_"+ NdexClasses.FunctionTerm_E_paramter )
	            .target(n)
	            .predicate( new OSQLPredicate("$depth <= 1"))) {

             ODocument doc = (ODocument) parameterRec;
	         
             String clsName = doc.getClassName();
             if ( clsName.equals(NdexClasses.BaseTerm) ||
            	  clsName.equals(NdexClasses.ReifiedEdgeTerm) || 
            	  clsName.equals(NdexClasses.FunctionTerm)) {
            	 if ( doc.field(NdexClasses.Element_ID).equals(func.getParameters().get(counter)) ) {
            		 counter ++;
            	 } else 
            		 break;
	         
             }
   			}
   			if ( counter == func.getParameters().size()) {
   				FunctionTerm result = new FunctionTerm();
   				result.setId((long)n.field(NdexClasses.Element_ID));
   				result.setFunctionTermId(func.getFunctionTermId());
   				for (Long pid : func.getParameters()) 
   				  result.getParameters().add(pid);
   				
   				return result;
   				
   			}
   		}
   		
   		return null;
    }
    
/*    public FunctionTerm getFunctionTermFromDoc(ODocument doc) {
    	FunctionTerm result = new FunctionTerm();
    	result.setId((long)doc.field(NdexClasses.Element_ID));
    	
    	
    	return result;
    } */
    
}
