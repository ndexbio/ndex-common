package org.ndexbio.common.persistence.orientdb;

import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.IFunctionTerm;
import org.ndexbio.common.models.data.IGroup;
import org.ndexbio.common.models.data.IGroupInvitationRequest;
import org.ndexbio.common.models.data.IGroupMembership;
import org.ndexbio.common.models.data.IJoinGroupRequest;
import org.ndexbio.common.models.data.INetworkAccessRequest;
import org.ndexbio.common.models.data.INetworkMembership;
import org.ndexbio.common.models.data.IReifiedEdgeTerm;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.orientdb.NdexSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.gremlingroovy.GremlinGroovyModule;
import com.tinkerpop.frames.modules.typedgraph.TypedGraphModuleBuilder;

/*
 * abstract class supporting common interactions with the orientdb database
 * specifically acquiring a database connection  initiating a graph module and
 * closing the database connection
 */

public class OrientDBNoTxConnectionService {
	private static final Logger logger = LoggerFactory
			.getLogger(OrientDBNoTxConnectionService.class);
	protected FramedGraphFactory _graphFactory = null;
	protected ODatabaseDocumentTx _ndexDatabase = null;
	protected FramedGraph<OrientBaseGraph> _orientDbGraph = null;
	private boolean setup;

	public OrientDBNoTxConnectionService() {
		this.setSetup(false);
		this.setupDatabase();
	}

	
	/**************************************************************************
	 * Opens a connection to OrientDB and initializes the OrientDB Graph ORM.
	 **************************************************************************/
	protected void setupDatabase() {
		// When starting up this application, tell OrientDB's global
		// configuration to close the storage; this is required here otherwise
		// OrientDB connection pooling doesn't work as expected
		// OGlobalConfiguration.STORAGE_KEEP_OPEN.setValue(false);

		_graphFactory = new FramedGraphFactory(new GremlinGroovyModule(),
				new TypedGraphModuleBuilder().withClass(IGroup.class)
						.withClass(IUser.class)
						.withClass(IGroupMembership.class)
						.withClass(INetworkMembership.class)
						.withClass(IGroupInvitationRequest.class)
						.withClass(IJoinGroupRequest.class)
						.withClass(INetworkAccessRequest.class)
						.withClass(IBaseTerm.class)
						.withClass(IReifiedEdgeTerm.class)
						.withClass(IFunctionTerm.class).build());

		_ndexDatabase = ODatabaseDocumentPool.global().acquire(
				"remote:localhost/ndex", "admin", "admin");

		// _ndexDatabase = ODatabaseDocumentPool.global().acquire(
		// Configuration.getInstance().getProperty("OrientDB-URL"),
		// Configuration.getInstance().getProperty("OrientDB-Username"),
		// Configuration.getInstance().getProperty("OrientDB-Password"));

		_orientDbGraph = _graphFactory
				.create((OrientBaseGraph) new OrientGraphNoTx(_ndexDatabase));
		NdexSchemaManager.INSTANCE.init(_orientDbGraph.getBaseGraph());
		//new OrientDBSchemaManager().init(_orientDbGraph.getBaseGraph());
		this.setSetup(true);
		logger.info("Connection to OrientDB established");
		
		

	}

	/**************************************************************************
	 * Cleans up the OrientDB resources. These steps are all necessary or
	 * OrientDB connections won't be released from the pool.
	 **************************************************************************/
	protected void teardownDatabase() {
		if (_graphFactory != null)
			_graphFactory = null;

		if (_ndexDatabase != null) {
			_ndexDatabase.close();
			_ndexDatabase = null;
		}

		if (_orientDbGraph != null) {
			_orientDbGraph.shutdown();
			_orientDbGraph = null;
		}
		this.setSetup(false);
		logger.info("Connection to OrientDB closed");
	}
	protected boolean isSetup() {
		return setup;
	}


	private void setSetup(boolean setup) {
		this.setup = setup;
	}
	public class OrientDBSchemaManager
	{
		public OrientDBSchemaManager() {
			
		}
	   

	    public synchronized void init(OrientBaseGraph orientDbGraph)
	    {
	        orientDbGraph.getRawGraph().commit();
	        
	       

	        /**********************************************************************
	        * Create base types first. 
	        **********************************************************************/
	        if (orientDbGraph.getVertexType("account") == null)
	        {
	            OClass accountClass = orientDbGraph.createVertexType("account");
	            accountClass.createProperty("backgroundImage", OType.STRING);
	            accountClass.createProperty("createdDate", OType.DATE);
	            accountClass.createProperty("description", OType.STRING);
	            accountClass.createProperty("foregroundImage", OType.STRING);
	        }

	        if (orientDbGraph.getVertexType("membership") == null)
	        {
	            OClass membershipClass = orientDbGraph.createVertexType("membership");
	            membershipClass.createProperty("permissions", OType.STRING);
	        }

	        if (orientDbGraph.getVertexType("request") == null)
	        {
	            OClass requestClass = orientDbGraph.createVertexType("request");
	            requestClass.createProperty("message", OType.STRING);
	            requestClass.createProperty("requestTime", OType.DATETIME);
	        }

	        if (orientDbGraph.getVertexType("term") == null)
	        {
	            OClass termClass = orientDbGraph.createVertexType("term");
	            termClass.createProperty("jdexId", OType.STRING);
	        }

	        /**********************************************************************
	        * Then create inherited types and uninherited types. 
	        **********************************************************************/
	        if (orientDbGraph.getVertexType("baseTerm") == null)
	        {
	            OClass termClass = orientDbGraph.createVertexType("baseTerm", "term");
	            termClass.createProperty("name", OType.STRING);

	            termClass.createIndex("index-term-name", OClass.INDEX_TYPE.NOTUNIQUE, "name");
	        }

	        if (orientDbGraph.getVertexType("citation") == null)
	        {
	            OClass citationClass = orientDbGraph.createVertexType("citation");

	            citationClass.createProperty("contributors", OType.STRING);
	            citationClass.createProperty("identifier", OType.STRING);
	            citationClass.createProperty("jdexId", OType.STRING);
	            citationClass.createProperty("title", OType.STRING);
	            citationClass.createProperty("type", OType.STRING);
	        }

	        if (orientDbGraph.getVertexType("edge") == null)
	        {
	            OClass edgeClass = orientDbGraph.createVertexType("edge");
	            edgeClass.createProperty("jdexId", OType.STRING);
	        }

	        if (orientDbGraph.getVertexType("functionTerm") == null)
	        {
	            OClass functionTermClass = orientDbGraph.createVertexType("functionTerm", "term");
	            functionTermClass.createProperty("functionTermOrderedParameters", OType.EMBEDDEDLIST);
	           // functionTermClass.createProperty("termParameters", OType.EMBEDDEDMAP, OType.LINK);
	            //functionTermClass.createProperty("textParameters", OType.EMBEDDEDSET);

	            //functionTermClass.createIndex("functionTermLinkParametersIndex", OClass.INDEX_TYPE.NOTUNIQUE, "termParameters by value");
	        }
	        
	        if (orientDbGraph.getVertexType("reifiedEdgeTerm") == null)
	        {
	            OClass reifiedEdgeTermClass = orientDbGraph.createVertexType("reifiedEdgeTerm", "term");

	        }

	        if (orientDbGraph.getVertexType("group") == null)
	        {
	            OClass groupClass = orientDbGraph.createVertexType("group", "account");
	            groupClass.createProperty("name", OType.STRING);
	            groupClass.createProperty("organizationName", OType.STRING);
	            groupClass.createProperty("website", OType.STRING);

	            groupClass.createIndex("index-group-name", OClass.INDEX_TYPE.UNIQUE, "name");
	        }
	        
	        if (orientDbGraph.getVertexType("groupInvite") == null)
	            orientDbGraph.createVertexType("groupInvite", "request");

	        if (orientDbGraph.getVertexType("groupMembership") == null)
	            orientDbGraph.createVertexType("groupMembership", "membership");

	        if (orientDbGraph.getVertexType("joinGroup") == null)
	            orientDbGraph.createVertexType("joinGroup", "request");

	        if (orientDbGraph.getVertexType("namespace") == null)
	        {
	            OClass nameSpaceClass = orientDbGraph.createVertexType("namespace");
	            nameSpaceClass.createProperty("jdexId", OType.STRING);
	            nameSpaceClass.createProperty("prefix", OType.STRING);
	            nameSpaceClass.createProperty("uri", OType.STRING);
	        }

	        if (orientDbGraph.getVertexType("network") == null)
	        {
	            OClass networkClass = orientDbGraph.createVertexType("network");
	            networkClass.createProperty("copyright", OType.STRING);
	            networkClass.createProperty("description", OType.STRING);
	            networkClass.createProperty("edgeCount", OType.INTEGER);
	            networkClass.createProperty("format", OType.STRING);
	            networkClass.createProperty("nodeCount", OType.INTEGER);
	            networkClass.createProperty("source", OType.STRING);
	            networkClass.createProperty("title", OType.STRING);
	            networkClass.createProperty("version", OType.STRING);
	        }

	        if (orientDbGraph.getVertexType("networkAccess") == null)
	            orientDbGraph.createVertexType("networkAccess", "request");

	        if (orientDbGraph.getVertexType("networkMembership") == null)
	            orientDbGraph.createVertexType("networkMembership", "membership");

	        if (orientDbGraph.getVertexType("node") == null)
	        {
	            OClass nodeClass = orientDbGraph.createVertexType("node");
	            nodeClass.createProperty("name", OType.STRING);
	            nodeClass.createProperty("jdexId", OType.STRING);
	        }

	        if (orientDbGraph.getVertexType("support") == null)
	        {
	            OClass supportClass = orientDbGraph.createVertexType("support");
	            supportClass.createProperty("jdexId", OType.STRING);
	            supportClass.createProperty("text", OType.STRING);
	        }

	        if (orientDbGraph.getVertexType("task") == null)
	        {
	            OClass taskClass = orientDbGraph.createVertexType("task");
	            taskClass.createProperty("status", OType.STRING);
	            taskClass.createProperty("startTime", OType.DATETIME);
	        }

	        if (orientDbGraph.getVertexType("user") == null)
	        {
	            OClass userClass = orientDbGraph.createVertexType("user", "account");

	            userClass.createProperty("username", OType.STRING);
	            userClass.createProperty("password", OType.STRING);
	            userClass.createProperty("firstName", OType.STRING);
	            userClass.createProperty("lastName", OType.STRING);
	            userClass.createProperty("emailAddress", OType.STRING);
	            userClass.createProperty("website", OType.STRING);

	            userClass.createIndex("index-user-username", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "username");
	            userClass.createIndex("index-user-emailAddress", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "emailAddress");
	        }
	    }
	}
}
