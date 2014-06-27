package org.ndexbio.orientdb;

import org.ndexbio.common.NdexClasses;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
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

    //TODO: type property might not be needed because we can get them from the vertex type.
    public synchronized void init(ODatabaseDocumentTx  orientDb)
    {
    	if(this.isInitialized()) {
    		return;
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
            clsNdxExternalObj.createProperty("UUID", OType.STRING);
            clsNdxExternalObj.createProperty("createdDate", OType.DATE);
            clsNdxExternalObj.createProperty("modificationDate", OType.DATE);
        }
        
        OClass clsAccount = orientDb.getMetadata().getSchema().getClass(NdexClasses.Account);
        
        if (clsAccount == null)
        {
        	clsAccount = orientDbGraph.createVertexType(NdexClasses.Account, clsNdxExternalObj);
        	clsAccount. setAbstract(true);
        	clsAccount.createProperty("backgroundImage", OType.STRING);
        	clsAccount.createProperty("description", OType.STRING);
        	clsAccount.createProperty("foregroundImage", OType.STRING);
        	clsAccount.createProperty("accountName", OType.STRING);
        	clsAccount.createProperty("password", OType.STRING);
        	clsAccount.createProperty("website", OType.STRING);

        	clsAccount.createIndex("index-user-username", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "accountName");

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
        
        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.Membership);  
        if ( cls == null)
        {
        	cls = orientDbGraph.createVertexType(NdexClasses.Membership,clsNdxExternalObj);
            cls.createProperty("permissions", OType.STRING);
            cls.createProperty("membershipType", OType.STRING);
        //    cls.createProperty("type", OType.STRING);
        }

        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.NdexProperty);  
        if ( cls == null)
        {
        	cls = orientDbGraph.createVertexType(NdexClasses.NdexProperty);
            cls.createProperty("value", OType.STRING);
            cls.createProperty("dataType", OType.STRING);
            cls.createProperty("type", OType.STRING);
        }

        
        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.Request);  
        if (cls == null)
        {
        	cls = orientDbGraph.createVertexType(NdexClasses.Request,clsNdxExternalObj);
        	cls.createProperty("message", OType.STRING);
            cls.createProperty("requestTime", OType.DATETIME);
            cls.createProperty("response", OType.STRING);
            cls.createProperty("responseMessage", OType.STRING);
            cls.createProperty("type", OType.STRING);
        }

        
        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.Task);  
        if (cls == null)
        {
            OClass taskClass = orientDbGraph.createVertexType(NdexClasses.Task, clsNdxExternalObj);
            taskClass.createProperty("status", OType.STRING);
            taskClass.createProperty("description", OType.STRING);
            taskClass.createProperty("priority", OType.STRING);
            taskClass.createProperty("progress", OType.STRING);
            taskClass.createProperty("taskType", OType.STRING);
            taskClass.createProperty("resource", OType.STRING);
            taskClass.createProperty("type", OType.STRING);
        }

        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.User);  
        if (cls == null)
        {
            OClass userClass = orientDbGraph.createVertexType(NdexClasses.User, clsAccount);

            userClass.createProperty("firstName", OType.STRING);
            userClass.createProperty("lastName", OType.STRING);
            userClass.createProperty("emailAddress", OType.STRING);

            userClass.createIndex("index-user-emailAddress", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "emailAddress");
            userClass.createProperty("type", OType.STRING);

        }
        
        
        // network data schema
        
        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.BaseTerm);  
        if (cls == null)
        {
            OClass termClass = orientDbGraph.createVertexType(NdexClasses.BaseTerm);
            termClass.createProperty("name", OType.STRING);

            termClass.createProperty("id", OType.LONG);
            termClass.createProperty("type", OType.STRING);

            termClass.createIndex("index-term-name", OClass.INDEX_TYPE.NOTUNIQUE, "name");
        }

        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.Citation);  
        if (cls == null)
        {
            OClass citationClass = orientDbGraph.createVertexType(NdexClasses.Citation);

            citationClass.createProperty("contributors", OType.EMBEDDEDLIST, OType.STRING);
            citationClass.createProperty("id", OType.LONG);
            citationClass.createProperty("properties", OType.EMBEDDEDLIST);
            citationClass.createProperty("presentationProperties", OType.EMBEDDEDLIST);
            citationClass.createProperty("title", OType.STRING);
            citationClass.createProperty("type", OType.STRING);
        }

        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.Edge);  
        if (cls == null)
        {
            OClass edgeClass = orientDbGraph.createVertexType(NdexClasses.Edge);
            edgeClass.createProperty("id", OType.LONG);
            edgeClass.createProperty("properties", OType.EMBEDDEDLIST);
            edgeClass.createProperty("presentationProperties", OType.EMBEDDEDLIST);
            edgeClass.createProperty("type", OType.STRING);
        }

        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.FunctionTerm);  
        if (cls == null)
        {
            OClass functionTermClass = orientDbGraph.createVertexType(NdexClasses.FunctionTerm);
            functionTermClass.createProperty("functionTermOrderedParameters", OType.EMBEDDEDLIST);
            //functionTermClass.createProperty("textParameters", OType.EMBEDDEDSET);
            //functionTermClass.createIndex("functionTermLinkParametersIndex", OClass.INDEX_TYPE.NOTUNIQUE, "termParameters by value");
        }

        cls = orientDb.getMetadata().getSchema().getClass(NdexClasses.ReifiedEdgeTerm);  
        if (orientDbGraph.getVertexType("reifiedEdgeTerm") == null)
        {
            OClass reifiedEdgeTermClass = orientDbGraph.createVertexType(NdexClasses.ReifiedEdgeTerm);
            reifiedEdgeTermClass.createProperty("id", OType.LONG);
//            reifiedEdgeTermClass.createProperty("type", OType.STRING);
        }
        
        if (orientDbGraph.getVertexType(NdexClasses.Namespace) == null)
        {
            OClass namespaceClass = orientDbGraph.createVertexType("namespace");
            namespaceClass.createProperty("id", OType.LONG);
            namespaceClass.createProperty("prefix", OType.STRING);
            namespaceClass.createProperty("uri", OType.STRING);
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
            networkClass.createProperty("name", OType.STRING);
            networkClass.createProperty("version", OType.STRING);
            networkClass.createProperty("highestElementId", OType.INTEGER);
        }

        if (orientDbGraph.getVertexType(NdexClasses.Node) == null)
        {
            OClass nodeClass = orientDbGraph.createVertexType(NdexClasses.Node);
            nodeClass.createProperty("name", OType.STRING);
            nodeClass.createProperty("id", OType.LONG);
            nodeClass.createProperty("properties", OType.EMBEDDEDLIST);
            nodeClass.createProperty("presentationProperties", OType.EMBEDDEDLIST);
        }

        
        if (orientDbGraph.getVertexType(NdexClasses.Provenance) == null)
        {
           // OClass clss = 
            		orientDbGraph.createVertexType(NdexClasses.Provenance);
        }
        
        
        if (orientDbGraph.getVertexType(NdexClasses.Subnetwork) == null)
        {
            OClass clss = orientDbGraph.createVertexType(NdexClasses.Subnetwork);
            clss.createProperty("id", OType.LONG);
            clss.createProperty("subnetworktype", OType.STRING);
            clss.createProperty("name", OType.STRING);
            clss.createProperty("properties", OType.EMBEDDEDLIST);
            clss.createProperty("presentationProperties", OType.EMBEDDEDLIST);
        }

        if (orientDbGraph.getVertexType(NdexClasses.Support) == null)
        {
            OClass supportClass = orientDbGraph.createVertexType(NdexClasses.Support);
            supportClass.createProperty("id", OType.LONG);
            supportClass.createProperty("text", OType.STRING);
        }


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
