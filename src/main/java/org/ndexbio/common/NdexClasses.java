package org.ndexbio.common;
/**
 *  This Class contains the vertex types (OrientDB classes) used in Ndex database.
 *   
 * @author chenjing
 *
 */
public class NdexClasses {
	
	public static final String NdexExternalObject = "NdexExternalObject"; 
	public static final String Account            = "account";
	public static final String Group          	  = "group";
	public static final String Membership         = "membership";
	public static final String NdexProperty       = "NdexProperty";
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
	
	// network properties and edges.
    public static final String Network_P_UUID    = "UUID";
    public static final String Network_P_cDate   = "createdDate";
    public static final String Network_P_mDate   = "modificationDate";
    public static final String Network_P_name    = "name";
    public static final String Network_P_visibility = "visibility";
    public static final String Network_P_isLocked   = "isLocked";
    public static final String Network_P_isComplete = "isComplete";
    public static final String Network_P_desc    = "description";
    public static final String Network_P_version = "version";
    public static final String Network_P_nodeCount = "nodeCount";
    public static final String Network_P_edgeCount = "edgeCount";
    
    public static final String Network_E_Namespace = "networkNS";
    public static final String Network_E_BaseTerms = "BaseTerms";
    public static final String Network_E_Nodes     = "networkNodes";
    public static final String Network_E_Edges     = "networkedges";
    public static final String Network_E_FunctionTerms = "FunctionTerms";
    
    // element 
    public static final String Element_ID  = "id";
    
    // namespace 
    public static final String ns_P_prefix = "prefix";
    public static final String ns_P_uri    = "uri";
    
    //BaseTerm
    public static final String BTerm_P_name        = "name";
    public static final String BTerm_E_Namespace   = "baseTermNS";

    //node
    public static final String Node_P_name         = "name";
    
    public static final String Node_E_represents   = "represents";
    
    //edge
    
    public static final String Edge_E_predicate  = "edgePredicate";
    public static final String Edge_E_subject    = "edgeSubject";
    public static final String Edge_E_object     = "edgeObject";
}
