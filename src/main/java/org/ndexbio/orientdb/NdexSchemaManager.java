package org.ndexbio.orientdb;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.NdexException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
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
    
    private static final String NdexDbVersion = "1.0";
    private static final String NdexDbVersionKey = "NdexDbVer";
    //name for the version field.
    private static final String NdexVField= "n1";
    

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
//        orientDb.commit();
        
        OrientBaseGraph orientDbGraph = new OrientGraph(orientDb);

        /**********************************************************************
        * Create base types first. 
        **********************************************************************/
        orientDbGraph.getRawGraph().commit();

        OClass clsNdxExternalObj = orientDb.getMetadata().getSchema().getClass(NdexClasses.NdexExternalObject);
        
        if (clsNdxExternalObj == null)
        {
        	clsNdxExternalObj = orientDbGraph.createVertexType(NdexClasses.NdexExternalObject);
            clsNdxExternalObj.createProperty(NdexClasses.Network_P_UUID, OType.STRING);
            clsNdxExternalObj.createProperty("createdDate", OType.DATE);
            clsNdxExternalObj.createProperty("modificationDate", OType.DATE);
            
            clsNdxExternalObj.createIndex("index-external-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Network_P_UUID);
        }
        
        OClass clsAccount = orientDb.getMetadata().getSchema().getClass(NdexClasses.Account);
        
        if (clsAccount == null)
        {
        	clsAccount = orientDbGraph.createVertexType(NdexClasses.Account, clsNdxExternalObj);
        //	clsAccount. setAbstract(true);
        	clsAccount.createProperty("backgroundImage", OType.STRING);
        	clsAccount.createProperty("description", OType.STRING);
        	clsAccount.createProperty("foregroundImage", OType.STRING);
        	clsAccount.createProperty("accountName", OType.STRING);
        	clsAccount.createProperty("password", OType.STRING);
        	clsAccount.createProperty("website", OType.STRING);

        	clsAccount.createIndex("index-user-username", OClass.INDEX_TYPE.UNIQUE, "accountName");

        }


        /**********************************************************************
        * Then create inherited types and uninherited types. 
        **********************************************************************/
        OClass cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.Group);  
        if (cls == null)
        {
            OClass groupClass = orientDbGraph.createVertexType(NdexClasses.Group, clsAccount);
            groupClass.createProperty("organizationName", OType.STRING);
            groupClass.createProperty("type", OType.STRING);
            
//            groupClass.createIndex("index-group-name", OClass.INDEX_TYPE.UNIQUE, "name");
        }
        
 /*  removed in 1.0
        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.Membership);  
        if ( cls == null)
        {
        	cls = orientDbGraph.createVertexType(NdexClasses.Membership,clsNdxExternalObj);
            cls.createProperty("permissions", OType.STRING);
            cls.createProperty("membershipType", OType.STRING);
        //    cls.createProperty("type", OType.STRING);
        }
*/
        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.NdexProperty);  
        if ( cls == null)
        {
        	cls = orientDbGraph.createVertexType(NdexClasses.NdexProperty);
            cls.createProperty("value", OType.STRING);
            cls.createProperty("dataType", OType.STRING);
 //           cls.createProperty("type", OType.STRING);
        }

        
        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.Request);  
        if (cls == null)
        {
        	cls = orientDbGraph.createVertexType(NdexClasses.Request,clsNdxExternalObj);
        	cls.createProperty("sourceUUID", OType.STRING);
        	cls.createProperty("sourceName", OType.STRING);
        	cls.createProperty("destinationUUID", OType.STRING);
        	cls.createProperty("destiniationName", OType.STRING);
        	cls.createProperty("message", OType.STRING);
            cls.createProperty("requestTime", OType.DATETIME);
            cls.createProperty("response", OType.STRING);
            cls.createProperty("responder", OType.STRING);
            cls.createProperty("responseMessage", OType.STRING);
//            cls.createProperty("type", OType.STRING);
        }

        
        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.Task);  
        if (cls == null)
        {
            OClass taskClass = orientDbGraph.createVertexType(NdexClasses.Task, clsNdxExternalObj);
            taskClass.createProperty(NdexClasses.Task_P_status, OType.STRING);
            taskClass.createProperty(NdexClasses.Task_P_description, OType.STRING);
            taskClass.createProperty(NdexClasses.Task_P_priority, OType.STRING);
            taskClass.createProperty(NdexClasses.Task_P_progress, OType.INTEGER);
            taskClass.createProperty(NdexClasses.Task_P_taskType, OType.STRING);
            taskClass.createProperty(NdexClasses.Task_P_resource, OType.STRING);
   //         taskClass.createProperty("type", OType.STRING);
        }

        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.User);  
        if (cls == null)
        {
            OClass userClass = orientDbGraph.createVertexType(NdexClasses.User, clsAccount);

            userClass.createProperty("firstName", OType.STRING);
            userClass.createProperty("lastName", OType.STRING);
            userClass.createProperty("emailAddress", OType.STRING);

            userClass.createIndex("index-user-emailAddress", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "emailAddress");
//            userClass.createProperty("type", OType.STRING);

        }
        
        
        // network data schema
        
        OClass nsClass = orientDb.getMetadata().getSchema().getClass(NdexClasses.Namespace);
        if (nsClass == null)
        {
            nsClass = orientDbGraph.createVertexType("namespace");
            nsClass.createProperty(NdexClasses.Element_ID, OType.LONG);
            nsClass.createProperty(NdexClasses.ns_P_prefix, OType.STRING);
            nsClass.createProperty(NdexClasses.ns_P_uri, OType.STRING);
            
            nsClass.createIndex("index-namespace-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
        }

        OClass bTermClass = orientDb.getMetadata().getSchema().getClass(NdexClasses.BaseTerm);  
        if ( bTermClass == null)
        {
            bTermClass = orientDbGraph.createVertexType(NdexClasses.BaseTerm);
            bTermClass.createProperty(NdexClasses.BTerm_P_name, OType.STRING);

            bTermClass.createProperty(NdexClasses.Element_ID, OType.LONG);
           // bTermClass.createProperty("type", OType.STRING);

            bTermClass.createProperty(NdexClasses.BTerm_E_Namespace, OType.LINK, nsClass);
            
            bTermClass.createIndex("index-term-name", OClass.INDEX_TYPE.NOTUNIQUE, "name");
            bTermClass.createIndex("index-baseterm-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
            
        }

        OClass citationClass = orientDb.getMetadata().getSchema().getClass(NdexClasses.Citation);  
        if (cls == null)
        {
            citationClass = orientDbGraph.createVertexType(NdexClasses.Citation);

            citationClass.createProperty("contributors", OType.EMBEDDEDLIST, OType.STRING);
            citationClass.createProperty(NdexClasses.Element_ID, OType.LONG);
            citationClass.createProperty("properties", OType.EMBEDDEDLIST);
            citationClass.createProperty("presentationProperties", OType.EMBEDDEDLIST);
            citationClass.createProperty("title", OType.STRING);
          //  citationClass.createProperty("type", OType.STRING);
            
            citationClass.createIndex("index-citation-id", OClass.INDEX_TYPE.NOTUNIQUE, NdexClasses.Element_ID);
        }

        OClass supportClass = orientDbGraph.getVertexType(NdexClasses.Support);
        if (supportClass == null)
        {
            supportClass = orientDbGraph.createVertexType(NdexClasses.Support);
            supportClass.createProperty(NdexClasses.Element_ID, OType.LONG);
            supportClass.createProperty("text", OType.STRING);
            
            supportClass.createIndex("index-support-id", OClass.INDEX_TYPE.NOTUNIQUE, NdexClasses.Element_ID);
            
        }

        
        OClass edgeClass = orientDb.getMetadata().getSchema().getClass(NdexClasses.Edge);  
        if (edgeClass == null)
        {
            edgeClass = orientDbGraph.createVertexType(NdexClasses.Edge);
            edgeClass.createProperty(NdexClasses.Element_ID, OType.LONG);
            edgeClass.createProperty("properties", OType.EMBEDDEDLIST);
            edgeClass.createProperty("presentationProperties", OType.EMBEDDEDLIST);
        //    edgeClass.createProperty("type", OType.STRING);

            edgeClass.createProperty(NdexClasses.Edge_E_citations, OType.LINKSET, citationClass);

            edgeClass.createIndex("index-edge-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
        }

        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.FunctionTerm);  
        if (cls == null)
        {
            OClass functionTermClass = orientDbGraph.createVertexType(NdexClasses.FunctionTerm);
            functionTermClass.createProperty(NdexClasses.Element_ID, OType.LONG);
            functionTermClass.createProperty("functionTermOrderedParameters", OType.EMBEDDEDLIST);
            //functionTermClass.createProperty("textParameters", OType.EMBEDDEDSET);
            //functionTermClass.createIndex("functionTermLinkParametersIndex", OClass.INDEX_TYPE.NOTUNIQUE, "termParameters by value");
            
            functionTermClass.createIndex("index-function-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
            
        }

        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.ReifiedEdgeTerm);  
        if (orientDbGraph.getVertexType("reifiedEdgeTerm") == null)
        {
            OClass reifiedEdgeTermClass = orientDbGraph.createVertexType(NdexClasses.ReifiedEdgeTerm);
            reifiedEdgeTermClass.createProperty(NdexClasses.Element_ID, OType.LONG);
//            reifiedEdgeTermClass.createProperty("type", OType.STRING);

            reifiedEdgeTermClass.createIndex("index-reifiedEdge-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);

        }
        
        if (orientDbGraph.getVertexType(NdexClasses.Network) == null)
        {
            OClass networkClass = orientDbGraph.createVertexType(NdexClasses.Network);
       //     networkClass.createProperty("copyright", OType.STRING);
            networkClass.createProperty("description", OType.STRING);
            networkClass.createProperty("edgeCount", OType.INTEGER);
       //     networkClass.createProperty("format", OType.STRING);
            networkClass.createProperty("properties", OType.EMBEDDEDLIST);
            networkClass.createProperty("presentationProperties", OType.EMBEDDEDLIST);

            networkClass.createProperty("nodeCount", OType.INTEGER);
      //      networkClass.createProperty("source", OType.STRING);
            networkClass.createProperty(NdexClasses.Network_P_name,    OType.STRING);
            networkClass.createProperty(NdexClasses.Network_P_version, OType.STRING);
            networkClass.createProperty("highestElementId", OType.INTEGER);
            
            networkClass.createProperty(NdexClasses.Network_E_Namespace, OType.LINKSET, nsClass);
            
        }

        if (orientDbGraph.getVertexType(NdexClasses.Node) == null)
        {
            OClass nodeClass = orientDbGraph.createVertexType(NdexClasses.Node);
            nodeClass.createProperty(NdexClasses.Node_P_name, OType.STRING);
            nodeClass.createProperty(NdexClasses.Element_ID,  OType.LONG);
            nodeClass.createProperty("properties", OType.EMBEDDEDLIST);
            nodeClass.createProperty("presentationProperties", OType.EMBEDDEDLIST);
            
            nodeClass.createProperty(NdexClasses.Node_E_represents, OType.LINK, bTermClass);
            nodeClass.createProperty(NdexClasses.Edge_E_subject, OType.LINKSET, edgeClass);
            nodeClass.createProperty(NdexClasses.Edge_E_object, OType.LINKSET, edgeClass);
            
            nodeClass.createProperty(NdexClasses.Node_E_alias, OType.LINKSET, bTermClass);
            nodeClass.createProperty(NdexClasses.Node_E_relateTo, OType.LINKSET, bTermClass);
            nodeClass.createProperty(NdexClasses.Node_E_ciations, OType.LINKSET, citationClass);
            nodeClass.createProperty(NdexClasses.Node_E_supports, OType.LINKSET, supportClass);
            
            nodeClass.createIndex("index-node-id", OClass.INDEX_TYPE.UNIQUE, NdexClasses.Element_ID);
            nodeClass.createIndex("index-node-name", OClass.INDEX_TYPE.NOTUNIQUE, NdexClasses.Node_P_name);
        }

        
        if (orientDbGraph.getVertexType(NdexClasses.Provenance) == null)
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
            clss.createProperty("presentationProperties", OType.EMBEDDEDLIST);
        }


        orientDb.getMetadata().getSchema().save();
        
        versionDoc = new ODocument(NdexVField, NdexDbVersion);
        orientDb.getDictionary().put(NdexDbVersionKey, versionDoc);
        
		// add a system user
        orientDbGraph.commit();
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
