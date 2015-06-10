/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.common.models.dao.orientdb;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.*;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class Helper {

	private static final Collection<ODocument> emptyDocs = new LinkedList<ODocument>();
	
	/**
	 * Populate a NdexExternalObject using data from an ODocument object.
	 * @param obj The NdexExternaObject to be populated.
	 * @param doc
	 * @return the Pouplated NdexExernalObject.
	 */
	public static NdexExternalObject populateExternalObjectFromDoc(NdexExternalObject obj, ODocument doc) {
		obj.setExternalId(UUID.fromString((String)doc.field(NdexClasses.Network_P_UUID)));
		
		Date d = doc.field(NdexClasses.ExternalObj_cTime);
		obj.setCreationTime(new Timestamp(d.getTime()));
		d = doc.field(NdexClasses.ExternalObj_mTime);
		obj.setModificationTime(new Timestamp(d.getTime()));
        Boolean isDeleted = doc.field(NdexClasses.ExternalObj_isDeleted);
       	obj.setIsDeleted(isDeleted != null && isDeleted.booleanValue());
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
	    	return getNetworkPermissionFromOutPath(s);
        }
    	
    	return null;
    }

    public static Permissions getNetworkPermissionFromOutPath(String path) {
	    Pattern pattern = Pattern.compile("out_([a-z]+)");
	    Matcher matcher = pattern.matcher(path);
	    if (matcher.find())
	    {
	    	return Permissions.valueOf(matcher.group(1).toUpperCase());
	    }  
	    return null;
    }

    public static Permissions getNetworkPermissionFromInPath(String path) {
	    Pattern pattern = Pattern.compile("in_([a-z]+)");
	    Matcher matcher = pattern.matcher(path);
	    if (matcher.find())
	    {
	    	return Permissions.valueOf(matcher.group(1).toUpperCase());
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

    public static boolean checkPermissionOnNetworkByAccountName(ODatabaseDocumentTx db, String networkUUID, 
			String accountName, Permissions expectedPermission) {
    	String query = "select $path from (traverse out_admin,out_member,out_groupadmin,out_write,out_read from (select * from " + NdexClasses.Account + 
    			" where accountName='"+ accountName + "') while $depth < 3 ) where UUID = '"+ networkUUID + "'";

    	final List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>(query));

    	for ( ODocument d : result ) { 
    		String s = d.field("$path");
    		Pattern pattern = Pattern.compile("(out_admin|out_write|out_read)");
    		Matcher matcher = pattern.matcher(s);
    		if (matcher.find())
    		{
    			Permissions p = Permissions.valueOf(matcher.group(1).substring(4).toUpperCase());
    			if ( permissionSatisfied( expectedPermission, p))
    				return true;
    		}  
    	}

    	return false;
    }
    
    public static VisibilityType getNetworkVisibility(ODatabaseDocumentTx db, String networkUUID) {
    	String query = "select " + NdexClasses.Network_P_visibility + " from " + NdexClasses.Network + 
    			" where UUID='"+ networkUUID + "'";

    	final List<ODocument> result = db.query(new OSQLSynchQuery<ODocument>(query));

    	if ( result.isEmpty()) return null;
 
    	String s = result.get(0).field(NdexClasses.Network_P_visibility);
    	return VisibilityType.valueOf(s);
    	
    }

    
    /**
     * Check if the actual permission meets the required permission level.
     * @param requiredPermission
     * @param actualPermission
     * @return
     */
    public static boolean permissionSatisfied(Permissions requiredPermission, Permissions actualPermission) {
    	if ( actualPermission == Permissions.ADMIN) return true;
    	if ( actualPermission == Permissions.WRITE) {
    		if (requiredPermission == Permissions.ADMIN)
    			return false;
    		return true;
    	}
    	if ( actualPermission == Permissions.READ && requiredPermission == Permissions.READ) 
    			return true;
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

    public static boolean canRemoveAdminOnGrp(ODatabaseDocumentTx db, String grpUUID, 
			String accountUUID) {

    	String query = "select count(*) as c from (traverse in_" + NdexClasses.GRP_E_admin + " from (select from " +
    	NdexClasses.Group +" where UUID = '"+ grpUUID + "')) where UUID <> '"+ accountUUID +"'";

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

	public static NdexPropertyValuePair getNdexPropertyFromDoc(ODocument doc) {
		NdexPropertyValuePair p = new NdexPropertyValuePair();
		
		ODocument baseTermDoc = doc.field("out_" + NdexClasses.ndexProp_E_predicate);
		if ( baseTermDoc == null ) {
			p.setPredicateString((String)doc.field(NdexClasses.ndexProp_P_predicateStr));
		} else {
			p.setPredicateString(getBaseTermStrFromDocument (baseTermDoc));
			p.setPredicateId((long)baseTermDoc.field(NdexClasses.Element_ID));
		}
		
		p.setValue((String)doc.field(NdexClasses.ndexProp_P_value)) ;
    	p.setDataType((String)doc.field(NdexClasses.ndexProp_P_datatype));
		return p;
	}
	
	private static String getBaseTermStrFromDocument(ODocument doc) {
		ODocument nsDoc = doc.field("out_" + NdexClasses.BTerm_E_Namespace);
		String localName = doc.field(NdexClasses.BTerm_P_name);
		if ( nsDoc !=null) {
			String prefix = nsDoc.field(NdexClasses.ns_P_prefix);
			if ( prefix != null)
				return prefix + ":" + localName;
			return nsDoc.field(NdexClasses.ns_P_uri) + localName;
		}
		return localName;
	}
	
	public static SimplePropertyValuePair getSimplePropertyFromDoc(ODocument doc) {
		SimplePropertyValuePair p = new SimplePropertyValuePair();
		p.setName((String)doc.field(NdexClasses.SimpleProp_P_name));
		p.setValue((String)doc.field(NdexClasses.SimpleProp_P_value)) ;
    	
		return p;
	}


	
	public static ODocument createSimplePropertyDoc(SimplePropertyValuePair property) {
		ODocument pDoc = new ODocument(NdexClasses.SimpleProperty)
			.fields(NdexClasses.SimpleProp_P_name,property.getName(),
					NdexClasses.SimpleProp_P_value, property.getValue())
			.save();
		return  pDoc;
	}

	public static ODocument updateNetworkProfile(ODocument doc, NetworkSummary newSummary){
	
	   boolean needResetModificationTime = false;
	   
	   if ( newSummary.getName() != null) {
		 doc.field( NdexClasses.Network_P_name, newSummary.getName());
		 needResetModificationTime = true;
	   }
		
	  if ( newSummary.getDescription() != null) {
		doc.field( NdexClasses.Network_P_desc, newSummary.getDescription());
		needResetModificationTime = true;
	  }
	
	  if ( newSummary.getVersion()!=null ) {
		doc.field( NdexClasses.Network_P_version, newSummary.getVersion());
		needResetModificationTime = true;
	  }
	  
	  if ( newSummary.getVisibility()!=null )
		doc.field( NdexClasses.Network_P_visibility, newSummary.getVisibility());
	  
	  if (needResetModificationTime) 
	     doc.field(NdexClasses.ExternalObj_mTime, new Date());
      
	  doc.save();
	  return doc;
	}
	
	
	public static NetworkSourceFormat getSourceFormatFromNetworkDoc(ODocument networkDoc) {
		String s = networkDoc.field(NdexClasses.Network_P_source_format);
		if ( s == null)
			return null;
		return NetworkSourceFormat.valueOf(s);
	}


	//TODO: this is a quick fix. Need to review Orientdb string escape rules to properly implement it.
	public static String escapeOrientDBSQL(String str) {
		return str.replace("'", "\\'");
	}

    // Added by David Welker
    public static void populateProvenanceEntity(ProvenanceEntity entity, NetworkDAO dao, String networkId) throws NdexException
    {
        NetworkSummary summary = NetworkDAO.getNetworkSummary(dao.getRecordByUUIDStr(networkId, null));
        populateProvenanceEntity(entity, summary);
    }

    //Added by David Welker
    public static void populateProvenanceEntity(ProvenanceEntity entity, NetworkSummary summary) throws NdexException
    {

        List<SimplePropertyValuePair> entityProperties = new ArrayList<>();

        entityProperties.add( new SimplePropertyValuePair("edge count", Integer.toString( summary.getEdgeCount() )) );
        entityProperties.add( new SimplePropertyValuePair("node count", Integer.toString( summary.getNodeCount() )) );

        if ( summary.getName() != null)
            entityProperties.add( new SimplePropertyValuePair("dc:title", summary.getName()) );

        if ( summary.getDescription() != null)
            entityProperties.add( new SimplePropertyValuePair("description", summary.getDescription()) );

        if ( summary.getVersion()!=null )
            entityProperties.add( new SimplePropertyValuePair("version", summary.getVersion()) );

        entity.setProperties(entityProperties);
    }

    //Added by David Welker
    public static void addUserInfoToProvenanceEventProperties(List<SimplePropertyValuePair> eventProperties, User user)
    {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        if( firstName != null || lastName != null )
        {
            String name = "";
            if( firstName == null )
                name = lastName;
            else if( lastName == null )
                name = firstName;
            else
                name = firstName + " " + lastName;
            eventProperties.add( new SimplePropertyValuePair("user", name));
        }

        if( user.getAccountName() != null )
            eventProperties.add( new SimplePropertyValuePair("account name", user.getAccountName()) );
    }


    public static Iterable<ODocument> getNetworkElements(ODocument networkDoc, String elementEdgeString) {	
    	
    	Object f = networkDoc.field("out_"+ elementEdgeString);
    	
    	if ( f == null) return emptyDocs;
    	
    	if ( f instanceof ODocument)
    		 return new OrientDBIterableSingleLink((ODocument)f);
    	
    	return ((Iterable<ODocument>)f);
    	     
    }

    
    public static Iterable<ODocument> getDocumentLinks(ODocument doc, String direction, String elementEdgeString) {	
    	
    	Object f = doc.field(direction+ elementEdgeString);
    	
    	if ( f == null) return emptyDocs;
    	
    	if ( f instanceof ODocument)
    		 return new OrientDBIterableSingleLink((ODocument)f);
    	
    	return ((Iterable<ODocument>)f);
    	     
    }

}
