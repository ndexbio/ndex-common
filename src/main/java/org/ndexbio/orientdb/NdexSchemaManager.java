/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
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
package org.ndexbio.orientdb;

import java.util.logging.Logger;

import org.apache.log4j.spi.LoggerFactory;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.FunctionTerm;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

/*
 * mod 03Apr2014 
 * the init method should only be invoked once per JVM invocation
 * add a flag to avoid repeated class
 * 
 */

public class NdexSchemaManager
{
    public static final NdexSchemaManager INSTANCE = new NdexSchemaManager();
    
    private Boolean initialized = Boolean.FALSE;
    
    private static final String NdexDbVersion = "1.1";
    private static final String NdexDbVersionKey = "NdexDbVer";
    //name for the version field.
    private static final String NdexVField= "n1";
    
	private static final Logger logger = Logger.getLogger(NdexSchemaManager.class.getName());

    //TODO: type property might not be needed because we can get them from the vertex type.
    public synchronized void init(ODatabaseDocumentTx  orientDb) throws NdexException
    {
    	ODocument  versionDoc = orientDb.getDictionary().get(NdexDbVersionKey); 
    	if( versionDoc != null ) {
    	   if ( versionDoc.field(NdexVField).equals(NdexDbVersion))	
    		  return;
		   throw new NdexException("Another version ("+versionDoc.field(NdexVField)+ 
				") of Ndex database found in the database. Please drop it before creating a new one.");
    	}
        
        OrientBaseGraph orientDbGraph = new OrientGraph(orientDb);
        orientDbGraph.setAutoScaleEdgeType(true);
        orientDbGraph.setEdgeContainerEmbedded2TreeThreshold(40);
        orientDbGraph.setUseLightweightEdges(true);

        /**********************************************************************
        * Create base types first. 
        **********************************************************************/
        orientDbGraph.getRawGraph().commit();

        logger.info("Creating schema in db.");
        
        OSchema schema = orientDb.getMetadata().getSchema();
//        OClass  v = schema.getClass("V");
//        if (v == null) throw new NdexException("Class V not found.");
        OClass clsNdxExternalObj = schema.getClass(NdexClasses.NdexExternalObject);
        
        if (clsNdxExternalObj == null)
        {
        	clsNdxExternalObj =  orientDbGraph.createVertexType(NdexClasses.NdexExternalObject);
            clsNdxExternalObj.createProperty(NdexClasses.Network_P_UUID, OType.STRING);
            clsNdxExternalObj.createProperty(NdexClasses.ExternalObj_cTime, OType.DATETIME);
            clsNdxExternalObj.createProperty(NdexClasses.ExternalObj_mTime, OType.DATETIME);
            clsNdxExternalObj.createProperty(NdexClasses.ExternalObj_isDeleted, OType.BOOLEAN);
        }
        
        OClass clsAccount =schema.getClass(NdexClasses.Account);
        
        if (clsAccount == null)
        {
        	clsAccount =  orientDbGraph.createVertexType(NdexClasses.Account, clsNdxExternalObj);
        //	clsAccount. setAbstract(true);
        	clsAccount.createProperty("backgroundImage", OType.STRING);
        	clsAccount.createProperty("description", OType.STRING);
        	clsAccount.createProperty("foregroundImage", OType.STRING);
        	clsAccount.createProperty(NdexClasses.account_P_accountName, OType.STRING);
        	clsAccount.createProperty("password", OType.STRING);
        	clsAccount.createProperty("website", OType.STRING);
        	clsAccount.createProperty(NdexClasses.account_P_oldAcctName, OType.STRING);
        }

        logger.info("parent classes created.");

        /**********************************************************************
        * Then create inherited types and uninherited types. 
        **********************************************************************/
        OClass cls = schema.getClass(NdexClasses.Group);  
        if (cls == null)
        {
            OClass groupClass =  orientDbGraph.createVertexType(NdexClasses.Group, clsAccount);
            groupClass.createProperty("organizationName", OType.STRING);
        }
        
/* 
        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.NdexProperty);  
        if ( cls == null)
        {
        	cls =  orientDbGraph.createVertexType(NdexClasses.NdexProperty);
            cls.createProperty("value", OType.STRING);
            cls.createProperty("dataType", OType.STRING);
        }
*/        
        cls = schema.getClass(NdexClasses.Request);  
        if (cls == null)
        {
        	cls =  orientDbGraph.createVertexType(NdexClasses.Request,clsNdxExternalObj);
        	cls.createProperty(NdexClasses.Request_P_sourceUUID, OType.STRING);
        	cls.createProperty(NdexClasses.Request_P_sourceName, OType.STRING);
        	cls.createProperty("destinationUUID", OType.STRING);
        	cls.createProperty("destiniationName", OType.STRING);
        	cls.createProperty("message", OType.STRING);
            cls.createProperty("permission", OType.STRING);
            cls.createProperty("response", OType.STRING);
            cls.createProperty("responder", OType.STRING);
            cls.createProperty("responseMessage", OType.STRING);
            cls.createProperty(NdexClasses.Request_P_responseTime, OType.DATETIME);
        }

        
        cls = schema.getClass(NdexClasses.Task);  
        if (cls == null)
        {
            OClass taskClass = orientDbGraph.createVertexType(NdexClasses.Task, clsNdxExternalObj);
            taskClass.createProperty(NdexClasses.Task_P_status, OType.STRING);
            taskClass.createProperty(NdexClasses.Task_P_description, OType.STRING);
            taskClass.createProperty(NdexClasses.Task_P_priority, OType.STRING);
            taskClass.createProperty(NdexClasses.Task_P_progress, OType.INTEGER);
            taskClass.createProperty(NdexClasses.Task_P_taskType, OType.STRING);
            taskClass.createProperty(NdexClasses.Task_P_resource, OType.STRING);
            taskClass.createProperty(NdexClasses.Task_P_startTime, OType.DATETIME);
            taskClass.createProperty(NdexClasses.Task_P_endTime, OType.DATETIME);
            taskClass.createProperty(NdexClasses.Task_P_message, OType.STRING);
        }

        OClass userClass = schema.getClass(NdexClasses.User);  
        if (userClass == null)
        {
            userClass = orientDbGraph.createVertexType(NdexClasses.User, clsAccount);

            userClass.createProperty("firstName", OType.STRING);
            userClass.createProperty("lastName", OType.STRING);
            userClass.createProperty(NdexClasses.User_P_emailAddress, OType.STRING);
        }
        
        logger.info("supporting classes created.");
        
        // network data schema
        
        OClass nsClass = schema.getClass(NdexClasses.Namespace);
        if (nsClass == null)
        {
            nsClass = orientDbGraph.createVertexType(NdexClasses.Namespace);
            nsClass.createProperty(NdexClasses.Element_ID, OType.LONG);
            nsClass.createProperty(NdexClasses.ns_P_prefix, OType.STRING);
            nsClass.createProperty(NdexClasses.ns_P_uri, OType.STRING);
            
        }

        OClass bTermClass = schema.getClass(NdexClasses.BaseTerm);  
        if ( bTermClass == null)
        {
            bTermClass = orientDbGraph.createVertexType(NdexClasses.BaseTerm);
            bTermClass.createProperty(NdexClasses.BTerm_P_name, OType.STRING);

            bTermClass.createProperty(NdexClasses.Element_ID, OType.LONG);

            bTermClass.createProperty(NdexClasses.BTerm_NS_ID, OType.LONG);
        }

        OClass citationClass = schema.getClass(NdexClasses.Citation);  
        if (cls == null)
        {
            citationClass = orientDbGraph.createVertexType(NdexClasses.Citation);

            citationClass.createProperty("contributors", OType.EMBEDDEDLIST, OType.STRING);
            citationClass.createProperty(NdexClasses.Element_ID, OType.LONG);
            citationClass.createProperty("properties", OType.EMBEDDEDLIST);
 //           citationClass.createProperty("presentationProperties", OType.EMBEDDEDLIST);
            citationClass.createProperty("title", OType.STRING);
            
        }

        OClass supportClass = orientDbGraph.getVertexType(NdexClasses.Support);
        if (supportClass == null)
        {
            supportClass = orientDbGraph.createVertexType(NdexClasses.Support);
            supportClass.createProperty(NdexClasses.Element_ID, OType.LONG);
            supportClass.createProperty("text", OType.STRING);
            supportClass.createProperty(NdexClasses.Citation, OType.LONG);
            
        }

        
        OClass edgeClass = schema.getClass(NdexClasses.Edge);  
        if (edgeClass == null)
        {
            edgeClass = orientDbGraph.createVertexType(NdexClasses.Edge);
            edgeClass.createProperty(NdexClasses.Element_ID, OType.LONG);
            edgeClass.createProperty(NdexClasses.ndexProperties, OType.EMBEDDEDLIST);
//            edgeClass.createProperty("presentationProperties", OType.EMBEDDEDLIST);
            edgeClass.createProperty(NdexClasses.Edge_P_predicateId, OType.LONG);

            edgeClass.createProperty(NdexClasses.Citation, OType.EMBEDDEDSET);
            edgeClass.createProperty(NdexClasses.Support,OType.EMBEDDEDSET);

        }

        cls = schema.getClass(NdexClasses.FunctionTerm);  
        if (cls == null)
        {
            OClass functionTermClass = orientDbGraph.createVertexType(NdexClasses.FunctionTerm);
            functionTermClass.createProperty(NdexClasses.Element_ID, OType.LONG);
            functionTermClass.createProperty("functionTermOrderedParameters", OType.EMBEDDEDLIST);
            //functionTermClass.createProperty("textParameters", OType.EMBEDDEDSET);
            //functionTermClass.createIndex("functionTermLinkParametersIndex", OClass.INDEX_TYPE.NOTUNIQUE, "termParameters by value");
            functionTermClass.createProperty(NdexClasses.BaseTerm, OType.LONG);
            
            functionTermClass.createIndex("index-function-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
            functionTermClass.createIndex("idx-func-bterm",OClass.INDEX_TYPE.NOTUNIQUE, NdexClasses.BaseTerm);
        }

        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.ReifiedEdgeTerm);  
        if (orientDbGraph.getVertexType("reifiedEdgeTerm") == null)
        {
            OClass reifiedEdgeTermClass = orientDbGraph.createVertexType(NdexClasses.ReifiedEdgeTerm);
            reifiedEdgeTermClass.createProperty(NdexClasses.Element_ID, OType.LONG);

            reifiedEdgeTermClass.createIndex("index-reifiedEdge-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);

        }
        
        if (orientDbGraph.getVertexType(NdexClasses.Network) == null)
        {
            OClass networkClass = orientDbGraph.createVertexType(NdexClasses.Network,clsNdxExternalObj);
       
            networkClass.createProperty(NdexClasses.Network_P_desc, OType.STRING);
            networkClass.createProperty("edgeCount", OType.INTEGER);
       
            networkClass.createProperty(NdexClasses.ndexProperties, OType.EMBEDDEDLIST);

            networkClass.createProperty("nodeCount", OType.INTEGER);
      
            networkClass.createProperty(NdexClasses.Network_P_name,    OType.STRING);
            networkClass.createProperty(NdexClasses.Network_P_version, OType.STRING);
            
            networkClass.createProperty(NdexClasses.Network_E_Namespace, OType.LINKSET, nsClass);

            networkClass.createIndex(NdexClasses.Index_network_name_desc, "FULLTEXT", 
        			null, null, "LUCENE", new String[] { NdexClasses.ExternalObj_ID, NdexClasses.Network_P_name, NdexClasses.Network_P_desc});

        }

        OClass nodeClass = orientDbGraph.getVertexType(NdexClasses.Node);
        if (nodeClass == null)
        {
            nodeClass = orientDbGraph.createVertexType(NdexClasses.Node);
            nodeClass.createProperty(NdexClasses.Node_P_name, OType.STRING);
            nodeClass.createProperty(NdexClasses.Element_ID,  OType.LONG);
            nodeClass.createProperty(NdexClasses.ndexProperties, OType.EMBEDDEDLIST);
            
            nodeClass.createProperty(NdexClasses.Node_P_represents, OType.LONG);
            nodeClass.createProperty(NdexClasses.Node_P_representTermType, OType.STRING);
            
            nodeClass.createProperty(NdexClasses.Node_P_alias, OType.EMBEDDEDSET);
            nodeClass.createProperty(NdexClasses.Node_P_relateTo, OType.EMBEDDEDSET);
            nodeClass.createProperty(NdexClasses.Citation, OType.EMBEDDEDSET);
            nodeClass.createProperty(NdexClasses.Support, OType.EMBEDDEDSET);
            
         //   nodeClass.createIndex(NdexClasses.Index_node_name, OClass.INDEX_TYPE.NOTUNIQUE, 		NdexClasses.Node_P_name);
        }
        
/*        if (orientDbGraph.getVertexType(NdexClasses.Provenance) == null)
        {
           // OClass clss = 
            		orientDbGraph.createVertexType(NdexClasses.Provenance);
        }
        
        
        if (orientDbGraph.getVertexType(NdexClasses.Subnetwork) == null)
        {
            OClass clss = orientDbGraph.createVertexType(NdexClasses.Subnetwork);
            clss.createProperty(NdexClasses.Element_ID, OType.LONG);
            clss.createProperty("subnetworktype", OType.STRING);
            clss.createProperty("name", OType.STRING);
            clss.createProperty("properties", OType.EMBEDDEDLIST);
  //          clss.createProperty("presentationProperties", OType.EMBEDDEDLIST);
        } */
        logger.info("All classes created. creating indexes ...");

        clsNdxExternalObj.createIndex(NdexClasses.Index_UUID, OClass.INDEX_TYPE.UNIQUE, NdexClasses.Network_P_UUID);
    	clsAccount.createIndex(NdexClasses.Index_accountName, 
    			OClass.INDEX_TYPE.UNIQUE, 
    			NdexClasses.account_P_accountName);

       userClass.createIndex("index-user-emailAddress", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "emailAddress");
        nsClass.createIndex(NdexClasses.Index_ns_id, OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
        
        bTermClass.createIndex(NdexClasses.Index_BTerm_name, "FULLTEXT", null, null, "LUCENE", new String[] { NdexClasses.BTerm_P_name});
       bTermClass.createIndex(NdexClasses.Index_bterm_id, OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
       bTermClass.createIndex("index-baseterm-ns", OClass.INDEX_TYPE.NOTUNIQUE, NdexClasses.BTerm_NS_ID);

       citationClass.createIndex("index-citation-id", OClass.INDEX_TYPE.NOTUNIQUE, NdexClasses.Element_ID);
      
       supportClass.createIndex("index-support-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
       supportClass.createIndex(NdexClasses.Index_support_citation, OClass.INDEX_TYPE.NOTUNIQUE, NdexClasses.Citation);
      
       edgeClass.createIndex("index-edge-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
       edgeClass.createIndex("idx-edge-predicate", OClass.INDEX_TYPE.NOTUNIQUE, NdexClasses.Edge_P_predicateId);

       nodeClass.createIndex(NdexClasses.Index_node_id, OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
       nodeClass.createIndex(NdexClasses.Index_node_rep_id, OClass.INDEX_TYPE.NOTUNIQUE, NdexClasses.Node_P_represents);
       nodeClass.createIndex(NdexClasses.Index_node_name, "FULLTEXT",null, null, "LUCENE", new String[] { NdexClasses.Node_P_name});


        logger.info("All indexes are created.");

        orientDb.getMetadata().getSchema().save();
        
        versionDoc = new ODocument(NdexVField, NdexDbVersion);
        orientDb.getDictionary().put(NdexDbVersionKey, versionDoc);
        
		// add a system user
        orientDbGraph.commit();
        logger.info("graph commited.");

        // turn on initialized flag
        this.setInitialized(Boolean.TRUE);
    }

	public Boolean isInitialized() {
		return initialized;
	}

	private void setInitialized(Boolean initialized) {
		this.initialized = initialized;
	}
	
	
}
