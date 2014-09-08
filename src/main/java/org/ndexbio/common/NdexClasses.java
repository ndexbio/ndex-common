package org.ndexbio.common;

import org.ndexbio.model.object.Permissions;

/**
 *  This Class contains the vertex types (OrientDB classes) used in Ndex database.
 *   
 * @author chenjing
 *
 */
public interface NdexClasses {
	
	public static final String NdexExternalObject = "NdexExternalObject"; 
	public static final String Account            = "account";
	public static final String Group          	  = "group";
	public static final String Membership         = "membership";
	public static final String NdexProperty       = "NdexProperty";
	public static final String SimpleProperty     = "SimpleProperty";
	public static final String Request            = "request";
	public static final String Task				  = "task";
	public static final String User				  = "user";
	
	// network related classes
	
	public static final String BaseTerm			  = "baseTerm";
	public static final String Citation			  = "citation";
	public static final String Edge				  = "edge";
	public static final String FunctionTerm 	  = "functionTerm";
	public static final String ReifiedEdgeTerm    = "reifiedEdgeTerm";
	public static final String Namespace          = "namespace";
	public static final String Network            = "network";
	public static final String Node               = "node";
	public static final String Provenance         = "Provenance";
	public static final String Subnetwork         = "Subnetwork";
	public static final String Support            = "support";
	
	//presentationProperty
	public static final String SimpleProp_P_name = "name";
	public static final String SimpleProp_P_value = "value";
	
	//account edges
	public static final String E_admin               = Permissions.ADMIN.toString().toLowerCase();
    public static final String account_E_canRead     = Permissions.READ.toString().toLowerCase();
    public static final String account_E_canEdit     = Permissions.WRITE.toString().toLowerCase();
	public static final String account_P_accountName = "accountName";
	
    public static final String Index_accountName = "index-user-username";
    
    //extertnal object
    public static final String ExternalObj_ID = "UUID";
    public static final String ExternalObj_cDate = "createdDate";
    public static final String ExternalObj_mDate = "modificationDate";

    public static final String Index_externalID = "index-external-id";
    
	// network properties and edges.
    public static final String Network_P_UUID    = ExternalObj_ID;
    public static final String Network_P_name    = "name";
    public static final String Network_P_visibility = "visibility";
    public static final String Network_P_isLocked   = "isLocked";
    public static final String Network_P_isComplete = "isComplete";
    public static final String Network_P_desc    = "description";
    public static final String Network_P_version = "version";
    public static final String Network_P_nodeCount = "nodeCount";
    public static final String Network_P_edgeCount = "edgeCount";
    public static final String Network_P_provenance = "provenance";
    
    public static final String Network_E_Namespace = "networkNS";
    public static final String Network_E_BaseTerms = "BaseTerms";
    public static final String Network_E_Nodes     = "networkNodes";
    public static final String Network_E_Edges     = "networkEdges";
    public static final String Network_E_FunctionTerms = "FunctionTerms";
    public static final String Network_E_Citations  = "citations";
    public static final String Network_E_Supports   = "supports";
    public static final String Network_E_ReifedEdgeTerms = "reifiedETerms";    
    // element 
    public static final String Element_ID  = "id";
    
    // propertiedObject
    public static final String E_ndexProperties = "ndexProps";
    public static final String E_ndexPresentationProps = "ndexPresProp";
    
    
    // namespace 
    public static final String ns_P_prefix = "prefix";
    public static final String ns_P_uri    = "uri";
    
    // citation
    public static final String Citation_P_title       = "title";
    public static final String Citation_P_contributors = "authors";
    public static final String Citation_p_idType	="idType";
    public static final String Citation_P_identifier ="identifier";
    //support
    public static final String Support_P_text     = "text";
    public static final String Support_E_citation = "citeFrom";
    
    //BaseTerm
    public static final String BTerm_P_name        = "name";
    public static final String BTerm_E_Namespace   = "baseTermNS";

    //ReifiedEdgeTerm
    public static final String ReifedEdge_E_edge  ="reify";
    
    //FunctionTerm
    public static final String FunctionTerm_E_baseTerm = "FuncBaseTerm";
    public static final String FunctionTerm_E_paramter = "FuncArguments";
    
    //node
    public static final String Node_P_name         = "name";
    
    public static final String Node_E_ciations     = "nCitation";
    public static final String Node_E_supports     = "nSupport";
    public static final String Node_E_represents   = "represents";
    public static final String Node_E_alias		   = "alias";
    public static final String Node_E_relateTo	   = "relateTo";
    
    public static final String Index_node_id = "index-node-id";
    public static final String Index_node_name = "index-node-name";
    //edge
    
    public static final String Edge_E_predicate  = "edgePredicate";
    public static final String Edge_E_subject    = "edgeSubject";
    public static final String Edge_E_object     = "edgeObject";
    public static final String Edge_E_citations  = "eCitation";
    public static final String Edge_E_supports   = "eSupport";
    
    // ndexProperty
    public static final String ndexProp_P_predicateStr = "predicateStr";
    public static final String ndexProp_P_value        = "value";
    public static final String ndexProp_P_datatype		= "dType";
    public static final String ndexProp_P_predicateId  = "predicateId";
    public static final String ndexProp_P_valueId	= "valueId";
    public static final String ndexProp_E_predicate = "prop";
    
    //user

    public static final String user_E_memberOf        ="member";
    public static final String user_E_grp_admin	      = "grpAdmin";
    
    
    // task
    public static final String Task_P_description = "description";
    public static final String Task_P_status = "status";
    public static final String Task_P_priority = "priority";
    public static final String Task_P_progress = "progress";
    public static final String Task_P_taskType = "taskType";
    public static final String Task_P_resource = "resource";
    public static final String Task_P_fileFormat = "format";
    
    public static final String Task_E_owner   = "ownedBy";
    
    //Group
    
    public static final String GRP_E_admin = Permissions.GROUPADMIN.toString().toLowerCase();
    public static final String GRP_E_member = Permissions.MEMBER.toString().toLowerCase();
    
    //request
    public static final String Request_P_sourceUUID = "sourceUUID";
    public static final String Request_P_sourceName = "sourceName";
    
}

