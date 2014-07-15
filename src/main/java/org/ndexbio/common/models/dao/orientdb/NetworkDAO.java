package org.ndexbio.common.models.dao.orientdb;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.PropertyGraphEdge;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.model.object.network.PropertyGraphNode;
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
//	Network network;
	
	public NetworkDAO (ODatabaseDocumentTx db) {
		this.db = db;
	}
	
/*	public NetworkDAO(NdexDatabase db, UUID networkID) {
		this.db = db;
		this.network = getNetworkById(networkID);
		
	} */
	
	public Network getNetworkById(UUID id) {
	     String query = "select from " + NdexClasses.Network + " where UUID='"
		                    +id.toString()+"'";
	     final List<ODocument> networks = db.query(new OSQLSynchQuery<ODocument>(query));
		     
	     if (networks.isEmpty())
		    	 return null;
         return getNetwork(networks.get(0));
	}
	
	
	public PropertyGraphNetwork getProperytGraphNetworkById(UUID id) throws NdexException {
		PropertyGraphNetwork network = new PropertyGraphNetwork();
		
	     String query = "select from " + NdexClasses.Network + " where UUID='"
                 +id.toString()+"'";
         final List<ODocument> networks = db.query(new OSQLSynchQuery<ODocument>(query));
  
         if (networks.isEmpty()) return null;
         
         network.setUuid(id);
        
         Collection<PropertyGraphNode> nodeList = network.getNodes();
         for (OIdentifiable nodeDoc : new OTraverse()
       	    .field("out_"+ NdexClasses.Network_E_Nodes )
            .target(networks)
            .predicate( new OSQLPredicate("$depth <= 1"))) {

              ODocument doc = (ODocument) nodeDoc;
          
              if ( doc.getClassName().equals(NdexClasses.Node)) {
          
                  PropertyGraphNode nd = NetworkDAO.getPropertyGraphNode(doc);
                  if ( nd != null)
                	  nodeList.add(nd);
                  else
                	  throw new NdexException("Error occurred when getting node information from db "+ doc);
              }
         }
         
         Collection<PropertyGraphEdge> edgeList = network.getEdges();
         for (OIdentifiable nodeDoc : new OTraverse()
       	    .field("out_"+ NdexClasses.Network_E_Edges )
            .target(networks)
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
		
		e.setSubjectId((long)
			    ((ODocument)doc.field("out_"+NdexClasses.Edge_E_object))
			        .field(NdexClasses.Element_ID));
		
		return e;
	}
	
    private static PropertyGraphNode getPropertyGraphNode(ODocument doc) {
    	PropertyGraphNode n = new PropertyGraphNode ();
        n.setId((long)doc.field(NdexClasses.Element_ID));    	
    	ODocument o = doc.field("out_" + NdexClasses.Node_E_represents);
    	String name = o.field(NdexClasses.BTerm_P_name);

    	n.setName(name);
    	return n;
    }
	
	
	private static Network getNetwork(ODocument n) {
		
		Network result = new Network();
		
		result.setExternalId(UUID.fromString((String)n.field("UUID")));
		result.setName((String)n.field("name"));
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
	
	// namespaceID < 0 means baseTerm has a local namespace
	public BaseTerm getBaseTerm(String baseterm, long namespaceID) {
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
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(baseTermQuery2);
			terms = db.command(query).execute( baseterm);
			
		}
		     
		if (terms.isEmpty())
		    	 return null;
             return getBaseTerm(terms.get(0));
	}
	
	private static BaseTerm getBaseTerm(ODocument o) {
		BaseTerm t = new BaseTerm();
		t.setId((long)o.field(NdexClasses.Element_ID));
		t.setName((String)o.field(NdexClasses.BTerm_P_name));
		
		ODocument nsDoc = o.field("out_"+NdexClasses.BTerm_E_Namespace);
		t.setNamespace((long)nsDoc.field(NdexClasses.Element_ID));
		
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
         NdexClasses.Node + " from (select from "+ NdexClasses.BaseTerm + " where " +
         NdexClasses.Element_ID + " = ?)) where @class=’"+ NdexClasses.Node +"’";
    
    public Node findNode(long baseTermID) {
		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(nodeQuery);
		List<ODocument> nodes = db.command(query).execute( baseTermID);
    	
		if (nodes.isEmpty())
			return null;
		
    	return getNode(nodes.get(0));
    }
    
    public static Node getNode(ODocument nodeDoc) {
    	Node n = new Node();

    	n.setId((long)nodeDoc.field(NdexClasses.Element_ID));
    	n.setName((String)nodeDoc.field(NdexClasses.Node_P_name));
    	return n;
    }
    
    public static NetworkSummary getNetworkSummary(ODocument doc) {
    	NetworkSummary networkSummary = new NetworkSummary();
    	
    	networkSummary.setCreationDate((Date)doc.field(NdexClasses.Network_P_cDate));
    	networkSummary.setExternalId(UUID.fromString((String)doc.field(NdexClasses.Network_P_UUID)));
    	networkSummary.setName((String)doc.field(NdexClasses.Network_P_name));
    	networkSummary.setDescription((String)doc.field(NdexClasses.Network_P_desc));
    	
    	networkSummary.setModificationDate((Date)doc.field(NdexClasses.Network_P_mDate));
    	networkSummary.setEdgeCount((int)doc.field(NdexClasses.Network_P_edgeCount));
    	networkSummary.setNodeCount((int)doc.field(NdexClasses.Network_P_nodeCount));
    	networkSummary.setVersion((String)doc.field(NdexClasses.Network_P_version));
        networkSummary.setVisibility(VisibilityType.valueOf((String)doc.field(NdexClasses.Network_P_visibility)));
    	
    	return networkSummary;
    }
}
