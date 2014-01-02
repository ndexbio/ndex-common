package org.ndexbio.orientdb.persistence;

import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.IFunctionTerm;
import org.ndexbio.common.models.data.IGroup;
import org.ndexbio.common.models.data.IGroupInvitationRequest;
import org.ndexbio.common.models.data.IGroupMembership;
import org.ndexbio.common.models.data.IJoinGroupRequest;
import org.ndexbio.common.models.data.INetworkAccessRequest;
import org.ndexbio.common.models.data.INetworkMembership;
import org.ndexbio.common.models.data.IUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
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

	public OrientDBNoTxConnectionService() {
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
						.withClass(IFunctionTerm.class).build());

		_ndexDatabase = ODatabaseDocumentPool.global().acquire(
				"remote:localhost/ndex", "admin", "admin");

		// _ndexDatabase = ODatabaseDocumentPool.global().acquire(
		// Configuration.getInstance().getProperty("OrientDB-URL"),
		// Configuration.getInstance().getProperty("OrientDB-Username"),
		// Configuration.getInstance().getProperty("OrientDB-Password"));

		_orientDbGraph = _graphFactory
				.create((OrientBaseGraph) new OrientGraphNoTx(_ndexDatabase));
		OrientDBSchemaManager.INSTANCE.init(_orientDbGraph.getBaseGraph());
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
		logger.info("Connection to OrientDB closed");
	}

}
