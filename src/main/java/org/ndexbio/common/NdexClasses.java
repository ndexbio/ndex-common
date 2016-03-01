/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
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
	public static final String OpaqueElement      = "opaqueElmt";
	
	// network related classes
	
	public static final String BaseTerm			  = "baseTerm";
	public static final String Citation			  = "citation";
	public static final String Edge				  = "CXedge";
	public static final String FunctionTerm 	  = "functionTerm";
	public static final String ReifiedEdgeTerm    = "reifiedEdgeTerm";
	public static final String Namespace          = "namespace";
	public static final String Network            = "network";
	public static final String Node               = "node";
	public static final String Provenance         = "Provenance";
	public static final String Subnetwork         = "Subnetwork";
	public static final String Support            = "support";
	
	//presentationProperty
	public static final String SimpleProp_P_name  = "name";
	public static final String SimpleProp_P_value = "value";
	
	//account edges
	public static final String E_admin               = Permissions.ADMIN.toString().toLowerCase();
    public static final String account_E_canRead     = Permissions.READ.toString().toLowerCase();
    public static final String account_E_canEdit     = Permissions.WRITE.toString().toLowerCase();
	public static final String account_P_accountName = "accountName";
    public static final String account_P_oldAcctName = "oldAcctName";
	
    public static final String Index_accountName = "index-user-username";
    public static final String Index_userEmail = "index-user-emailAddress";
    
    //extertnal object
    public static final String ExternalObj_ID    = "UUID";
    public static final String ExternalObj_cTime = "createdTime";
    public static final String ExternalObj_mTime = "modificationTime";
    public static final String ExternalObj_isDeleted = "isDeleted";


//    public static final String Index_externalID = "index-external-id";
    
	// network properties and edges.
    public static final String Network_P_UUID       = ExternalObj_ID;
    public static final String Network_P_name       = "name";
    public static final String Network_P_visibility = "visibility";
    public static final String Network_P_isLocked   = "isLocked";
    public static final String Network_P_isComplete = "isComplete";
    public static final String Network_P_desc       = "description";
    public static final String Network_P_version    = "version";
    public static final String Network_P_nodeCount  = "nodeCount";
    public static final String Network_P_edgeCount 	= "edgeCount";
    public static final String Network_P_provenance = "provenance";
    public static final String Network_P_source_format = "sourceFormat";  // Used internally. Will be convert to properties in the network model.
    public static final String Network_P_readOnlyCommitId = "ROId";
    public static final String Network_P_cacheId 	= "cacheId";
    public static final String Network_P_owner      = "owner";
    public static final String Network_P_metadata   = "aspectMetadata";
    public static final String Network_P_opaquEdgeTable = "opaqueAspects";
    
    public static final String Network_E_opaque_asp_prefix = "A_";
    
    public static final String Network_E_Namespace = "networkNS";
    public static final String Network_E_BaseTerms = "BaseTerms";
    public static final String Network_E_Nodes     = "networkNodes";
    public static final String Network_E_Edges     = "networkEdges";
    public static final String Network_E_FunctionTerms = "FunctionTerms";
    public static final String Network_E_Citations  = "citations";
    public static final String Network_E_Supports   = "supports";
    public static final String Network_E_ReifiedEdgeTerms = "reifiedETerms";    
    // element 
    public static final String Element_ID  = "id";
    public static final String Element_SID = "sid";
    
    public static final String Index_network_name_desc = "idx_network-name-desc";
    
    // propertiedObject
 //   public static final String E_ndexProperties        = "ndexProps";
 //   public static final String E_ndexPresentationProps = "ndexPresProp";
    
    
    // namespace 
    public static final String ns_P_prefix = "prefix";
    public static final String ns_P_uri    = "uri";
    
    // citation
    public static final String Citation_P_title        = "title";
    public static final String Citation_P_contributors = "authors";
    public static final String Citation_p_idType	   = "idType";
    public static final String Citation_P_identifier   = "identifier";
    //support
    public static final String Support_P_text     = "text";
//    public static final String Support_E_citation = "citeFrom";
    
    //BaseTerm
    public static final String BTerm_P_name        = "name";
    public static final String BTerm_P_prefix      = "prefix";
    public static final String BTerm_NS_ID   = "nsID";
    
    public static final String Index_BTerm_name    = "idx_baseterm_name";

    //ReifiedEdgeTerm
    public static final String ReifiedEdge_E_edge  ="reify";
    
    //FunctionTerm
//    public static final String FunctionTerm_E_baseTerm = "FuncBaseTerm";
    public static final String FunctionTerm_E_paramter = "FuncArguments";
    
    //node
    public static final String Node_P_name         = "name";
    public static final String Node_P_representTermType = "repType";
    
//    public static final String Node_E_citations     = "nCitation";
//    public static final String Node_E_supports     = "nSupport";
    public static final String Node_P_represents   = "represents";
    public static final String Node_P_alias		   = "alias";
    public static final String Node_P_relatedTo	   = "relatedTo";
    
    // index on primary keys.
    public static final String Index_ns_id = "index-namespace-id";
    public static final String Index_bterm_id = "index-baseterm-id";
    public static final String Index_citation_id = "index-citation-id";
    public static final String Index_support_id = "index-support-id";
    public static final String Index_node_id = "index-node-id";
    public static final String Index_edge_id = "index-edge-id";
    public static final String Index_function_id = "index-function-id";
    public static final String Index_reifiededge_id = "index-reifiedEdge-id";
    
    public static final String Index_node_name = "index-node-name";
    public static final String Index_node_rep_id = "index-node-rep-id";
    public static final String Index_UUID = "index-external-id";
    public static final String Index_support_citation = "idx-support-citation";
    
    public static final String Index_node_sid = "idx-node-sid";
    public static final String Index_edge_sid = "idx-edge-sid";
    public static final String Index_citation_sid = "idx-citation-sid";
    public static final String Index_support_sid = "idx-support-sid";
    
    //edge
    
//    public static final String Edge_E_predicate  = "edgePredicate";
    public static final String Edge_P_predicateId = "prdctId";
    public static final String Edge_E_subject    = "edgeSubject";
    public static final String Edge_E_object     = "edgeObject";
//    public static final String Edge_E_citations  = "eCitation";
//    public static final String Edge_E_supports   = "eSupport";
    
    // ndexProperty
    public static final String ndexProp_P_predicateStr  = "predicateStr";
    public static final String ndexProp_P_value         = "value";
    public static final String ndexProp_P_datatype		= "dType";
    public static final String ndexProp_P_predicateId   = "predicateId";
    public static final String ndexProp_P_valueId	    = "valueId";
//    public static final String ndexProp_E_predicate     = "prop";
    public static final String ndexProperties = "props";
    
    //Group
    
    public static final String GRP_E_admin = Permissions.GROUPADMIN.toString().toLowerCase();
    public static final String GRP_E_member = Permissions.MEMBER.toString().toLowerCase();
    public static final String GRP_P_NAME = "groupName";

    //user
    
//    public static final String user_E_memberOf        ="member";
//    public static final String user_E_grp_admin	      = "grpAdmin";
    public static final String User_P_oldEmailAddress = "oldEmail";
    public static final String User_P_emailAddress   = "emailAddress";
//    public static final String User_is_verified  = "isVerified";
    public static final String User_verification_code = "verificationCode";
        
    // task
    public static final String Task_P_description = "description";
    public static final String Task_P_status = "status";
    public static final String Task_P_priority = "priority";
    public static final String Task_P_progress = "progress";
    public static final String Task_P_taskType = "taskType";
    public static final String Task_P_resource = "resource";
    public static final String Task_P_fileFormat = "format";
    public static final String Task_P_startTime = "startTime";
    public static final String Task_P_endTime   = "endTime";
    public static final String Task_P_message   = "message";
    public static final String Task_P_attributes = "attributes";
    
    public static final String Task_E_owner   = "ownedBy";
    
    //request
    public static final String Request_P_sourceUUID = "sourceUUID";
    public static final String Request_P_sourceName = "sourceName";
    public static final String Request_P_responseTime = "responseTime";
    public static final String Request_E_requests   = "requests";
    
    //reserved NDEx property names
    
    //BEL namespace file record
    public static final String BELPrefix = "prefix";
    public static final String BELNamespaceFileContent = "content";
    
}

