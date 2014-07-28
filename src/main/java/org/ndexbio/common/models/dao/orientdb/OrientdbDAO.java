package org.ndexbio.common.models.dao.orientdb;

import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.orientdb.NdexSchemaManager;

import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public abstract class OrientdbDAO {
//	protected FramedGraphFactory _graphFactory = null;
	protected ODatabaseDocumentTx _ndexDatabase = null;
//	protected FramedGraph<OrientBaseGraph> _orientDbGraph = null;
	

	public OrientdbDAO() {
/*		_graphFactory = new FramedGraphFactory(new GremlinGroovyModule(),
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

		_ndexDatabase =  NdexAOrientDBConnectionPool.getInstance().acquire();

	/*	if (Boolean.parseBoolean(Configuration.getInstance().getProperty(
				"OrientDB-Use-Transactions")))
			_orientDbGraph = _graphFactory
					.create((OrientBaseGraph) new OrientGraph(_ndexDatabase));
		else
			_orientDbGraph = _graphFactory
					.create((OrientBaseGraph) new OrientGraphNoTx(_ndexDatabase));
*/
		/*
		 * only initialize the ORM once
		 */
		if (!NdexSchemaManager.INSTANCE.isInitialized()) {
			NdexSchemaManager.INSTANCE.init(_ndexDatabase);
		}
	}

	/*
	 * return the connection to the pool
	 */
	protected void teardownDatabase() {

		if (_ndexDatabase != null) {
			_ndexDatabase.close();
			_ndexDatabase = null;
		}
/*
		if (_orientDbGraph != null) {
			_orientDbGraph.shutdown();
			_orientDbGraph = null;
		} */
	}

	/*
	 * resolving a user is a common requirement across all DAO classes
	 * 
	 */

/*	protected IUser findIuserById(final String userId) {
		Preconditions.checkState(null != this._ndexDatabase && null != this._orientDbGraph,
				"findUserById invoked without database connection");	
		try {
			
			return  _orientDbGraph.getVertex(
					IdConverter.toRid(userId),
					IUser.class);
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
		
		
	} */

}
