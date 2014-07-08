package org.ndexbio.common.models.dao.orientdb;

import java.util.List;
import java.util.UUID;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NetworkDAO {
	
	private NdexDatabase db;
//	Network network;
	
	public NetworkDAO (NdexDatabase db) {
		this.db = db;
	}
	
/*	public NetworkDAO(NdexDatabase db, UUID networkID) {
		this.db = db;
		this.network = getNetworkById(networkID);
		
	} */
	
	public Network getNetworkById(UUID id) {
		ODatabaseDocumentTx ndexDatabase = db.getAConnection();
		try {
		     String query = "select from " + NdexClasses.Network + " where UUID='"
		                    +id.toString()+"'";
		     final List<ODocument> networks = ndexDatabase
						.query(new OSQLSynchQuery<ODocument>(query));
		     
		     if (networks.isEmpty())
		    	 return null;
             return getNetwork(networks.get(0));
//		} catch ()
//		{
		} finally {
   		  ndexDatabase.close();
		}
	}
	
	
	
	private Network getNetwork(ODocument n) {
		Network result = new Network();
		
		result.setExternalId(UUID.fromString((String)n.field("UUID")));
		result.setName((String)n.field("name"));
        //TODO: populate all fields.
		
		return result;
	}
	
	public BaseTerm getBaseTerm(UUID networkID, String baseterm, long namespaceId) {
     
		ODatabaseDocumentTx ndexDatabase = db.getAConnection();
		try {
		     String query = "select from " + NdexClasses.BaseTerm + 
		    		 " where name ='" + baseterm + "'";
		     final List<ODocument> networks = ndexDatabase
						.query(new OSQLSynchQuery<ODocument>(query));
		     
		     if (networks.isEmpty())
		    	 return null;
             return null; //getNetwork(networks.get(0));
		} finally {
   		  ndexDatabase.close();
		}
		
		
	}
	
	private static BaseTerm getBaseTerm(ODocument o) {
		BaseTerm t = new BaseTerm();
		return t;
	}
	
	public Namespace getNamespace(String prefix, String URI, UUID networkID ) {
		ODatabaseDocumentTx ndexDatabase = this.db.getAConnection();
		try {
			String query = "select from (traverse out_" +
		    		  NdexClasses.Network_E_NAMESPACE +" from (select from "
		    		  + NdexClasses.Network + " where " +
		    		  NdexClasses.Network_P_UUID + "='" + networkID + 
		    		  "')) where @class='"+  NdexClasses.Namespace + "' and ";
			if ( prefix != null) {
		      query = query + NdexClasses.ns_P_prefix + "='"+ prefix +"'";
			}   else {
			  query = query + NdexClasses.ns_P_uri + "='"+ URI +"'";	
			}	
		    final List<ODocument> nss = ndexDatabase
						.query(new OSQLSynchQuery<ODocument>(query));
		     
		     if (nss.isEmpty())
		    	 return null;
             Namespace result = getNamespace(nss.get(0));
             return result;
		} finally {
   		  ndexDatabase.close();
		}
	}
	
    private static Namespace getNamespace(ODocument ns) {
       Namespace rns = new Namespace();
       rns.setId((long)ns.field("id"));
       rns.setPrefix((String)ns.field(NdexClasses.ns_P_prefix));
       rns.setUri((String)ns.field(NdexClasses.ns_P_uri));
       return rns;
    } 
}
