package org.ndexbio.common.models.dao.orientdb;

import java.util.List;
import java.util.UUID;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.Node;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLQuery;
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
    
    private static Node getNode(ODocument nodeDoc) {
    	Node n = new Node();

    	n.setId((long)nodeDoc.field(NdexClasses.Element_ID));
    	n.setName((String)nodeDoc.field(NdexClasses.Node_P_name));
    	return n;
    }
}
