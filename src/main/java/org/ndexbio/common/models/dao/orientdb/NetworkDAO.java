package org.ndexbio.common.models.dao.orientdb;

import java.util.List;
import java.util.UUID;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.object.network.BaseTerm;
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
		     String query = "select from " + NdexClasses.Network + " where UUID='"+id.toString()+"'";
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
		     String query = "select from " + NdexClasses.BaseTerm + " where name ='"+baseterm+"'";
		     final List<ODocument> networks = ndexDatabase
						.query(new OSQLSynchQuery<ODocument>(query));
		     
		     if (networks.isEmpty())
		    	 return null;
             return getNetwork(networks.get(0));
		} finally {
   		  ndexDatabase.close();
		}
		
		
	}
	
	private BaseTerm getBaseTerm(ODocument o) {
		BaseTerm t = new BaseTerm();
		
	}
	

}
