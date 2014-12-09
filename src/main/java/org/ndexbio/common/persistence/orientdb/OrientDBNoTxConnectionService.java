package org.ndexbio.common.persistence.orientdb;

import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.orientdb.NdexSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

/*
 * abstract class supporting common interactions with the orientdb database
 * specifically acquiring a database connection  initiating a graph module and
 * closing the database connection
 */

public class OrientDBNoTxConnectionService {
	private static final Logger logger = LoggerFactory
			.getLogger(OrientDBNoTxConnectionService.class);
	protected ODatabaseDocumentTx _ndexDatabase = null;
	private boolean setup;
	private OrientGraph graph;

	public OrientDBNoTxConnectionService() {
		this.setSetup(false);
		
	}

	
	/**************************************************************************
	 * Opens a connection to OrientDB and initializes the OrientDB Graph ORM.
	 * @throws NdexException 
	 **************************************************************************/
	protected void setupDatabase() throws NdexException {
		// When starting up this application, tell OrientDB's global
		// configuration to close the storage; this is required here otherwise
		// OrientDB connection pooling doesn't work as expected
		// OGlobalConfiguration.STORAGE_KEEP_OPEN.setValue(false);
/*
		FramedGraphFactory _graphFactory = new FramedGraphFactory(new GremlinGroovyModule(),
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
*/
		_ndexDatabase = NdexAOrientDBConnectionPool.getInstance().acquire(); 

		graph = new OrientGraph(_ndexDatabase,false);
		NdexSchemaManager.INSTANCE.init(_ndexDatabase);
		this.setSetup(true);
		logger.info("Connection to OrientDB established");
		
		

	}

	/**************************************************************************
	 * Cleans up the OrientDB resources. These steps are all necessary or
	 * OrientDB connections won't be released from the pool.
	 **************************************************************************/
	protected void teardownDatabase() {

		if (_ndexDatabase != null) {
			_ndexDatabase.close();
			_ndexDatabase = null;
		}

/*		if (_orientDbGraph != null) {
			_orientDbGraph.shutdown();
			_orientDbGraph = null;
		} */
		this.setSetup(false);
		logger.info("Connection to OrientDB closed");
	}
	protected boolean isSetup() {
		return setup;
	}


	private void setSetup(boolean setup) {
		this.setup = setup;
	}
	
	public OrientGraph getGraph() { return this.graph;}
}
