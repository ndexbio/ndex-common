package org.ndexbio.common.models.dao.orientdb;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.object.NdexExternalObject;
import org.ndexbio.model.object.Permissions;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class Helper {
	
	public static NdexExternalObject populateExternalObjectFromDoc(NdexExternalObject obj, ODocument doc) {
		obj.setCreationDate((Date)doc.field(NdexClasses.ExternalObj_cDate));
		obj.setExternalId(UUID.fromString((String)doc.field(NdexClasses.Network_P_UUID)));
		obj.setModificationDate((Date)doc.field(NdexClasses.ExternalObj_mDate));
		
	    return obj;
	}

	
    /**
     * Get direct privilege between the account and a network. Indirect privileges are not included.  
     * @param networkUUID
     * @param accountUUID
     * @return the permission allowed between them. Null if no permissions are found.
     */
    public static Permissions getNetworkPermissionByAccout(ODatabaseDocumentTx db, String networkUUID, 
    				String accountUUID) {
        String query = "select $path from (traverse out_admin,out_write,out_read from (select * from " + NdexClasses.Account + 
          		" where UUID='"+ accountUUID + "')) where UUID = '"+ networkUUID + "'";

	    final List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>(query));

	    for ( ODocument d : result ) { 
	    	String s = d.field("$path");
	    	Pattern pattern = Pattern.compile("out_([a-z]+)");
	    	Matcher matcher = pattern.matcher(s);
	    	if (matcher.find())
	    	{
	    	    return Permissions.valueOf(matcher.group(1).toUpperCase());
	    	}  
	    	return null;
        }
    	
    	return null;
    }

    public static boolean isAdminOfNetwork(ODatabaseDocumentTx db, String networkUUID, 
			String accountUUID) {
    	String query = "select $path from (traverse out_admin,out_member,out_groupadmin from (select * from " + NdexClasses.Account + 
    			" where UUID='"+ accountUUID + "') while $depth < 3 ) where UUID = '"+ networkUUID + "'";

    	final List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>(query));

    	for ( ODocument d : result ) { 
    		String s = d.field("$path");
    		Pattern pattern = Pattern.compile("out_admin");
    		Matcher matcher = pattern.matcher(s);
    		if (matcher.find())
    		{
    			return true;
    		}  
    	}

    	return false;
    }
/*    
    public static boolean isAdminOfNetworkByAccountName(ODatabaseDocumentTx db, String networkUUID, 
			String accountName) {
    	String query = "select $path from (traverse out_admin,out_member,out_groupadmin from (select * from " + NdexClasses.Account + 
    			" where accountName='"+ accountName + "') while $depth < 3 ) where UUID = '"+ networkUUID + "'";

    	final List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>(query));

    	for ( ODocument d : result ) { 
    		String s = d.field("$path");
    		Pattern pattern = Pattern.compile("out_admin");
    		Matcher matcher = pattern.matcher(s);
    		if (matcher.find())
    		{
    			return true;
    		}  
    	}

    	return false;
    }
 */   
    /**
     * Check if an admin account exists on the given network other than the one specified in the parameter.
     *  Basically used to check if an admin edge are allowed to be removed between the network and given account. 
     * @param db
     * @param networkUUID
     * @param accountUUID
     * @return 
     */
    public static boolean canRemoveAdmin(ODatabaseDocumentTx db, String networkUUID, 
    				String accountUUID) {
    	
    	String query = "select count(*) as c from (traverse in_" + NdexClasses.E_admin + " from (select from " +
    	   NdexClasses.Network +" where UUID = '"+ networkUUID + "')) where UUID <> '"+ accountUUID +"'";

    	final List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>(query));

	    if ((long) result.get(0).field("c") > 1 ) return true;
    	return false;
    }

    public static boolean canRemoveAdminByAccount(ODatabaseDocumentTx db, String networkUUID, 
			String accountName) {

    	String query = "select count(*) as c from (traverse in_" + NdexClasses.E_admin + " from (select from " +
    			NdexClasses.Network +" where UUID = '"+ networkUUID + "')) where accountName <> '"+ accountName +"'";

    	final List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>(query));

    	if ((long) result.get(0).field("c") > 1 ) return true;
		return false;
    }

    
}
