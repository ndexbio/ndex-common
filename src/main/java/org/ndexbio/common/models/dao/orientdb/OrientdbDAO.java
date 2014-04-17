package org.ndexbio.common.models.dao.orientdb;

import java.util.NoSuchElementException;

import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.common.helpers.IdConverter;
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
import org.ndexbio.common.persistence.NdexOrientdbConnectionPool;
import org.ndexbio.orientdb.NdexSchemaManager;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.gremlingroovy.GremlinGroovyModule;
import com.tinkerpop.frames.modules.typedgraph.TypedGraphModuleBuilder;

public abstract class OrientdbDAO {
	protected FramedGraphFactory _graphFactory = null;
	protected ODatabaseDocumentTx _ndexDatabase = null;
	protected FramedGraph<OrientBaseGraph> _orientDbGraph = null;
	

	public OrientdbDAO() {
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

	}

	/**************************************************************************
	 * Opens a connection to OrientDB and initializes the OrientDB Graph ORM.
	 **************************************************************************/
	protected void setupDatabase() {

		
		// When starting up this application, tell OrientDB's global
		// configuration to close the storage; this is required here otherwise
		// OrientDB connection pooling doesn't work as expected
		// OGlobalConfiguration.STORAGE_KEEP_OPEN.setValue(false);

		_ndexDatabase = ODatabaseDocumentPool.global().acquire(
				Configuration.getInstance().getProperty("OrientDB-URL"),
				Configuration.getInstance().getProperty("OrientDB-Username"),
				Configuration.getInstance().getProperty("OrientDB-Password"));

		if (Boolean.parseBoolean(Configuration.getInstance().getProperty(
				"OrientDB-Use-Transactions")))
			_orientDbGraph = _graphFactory
					.create((OrientBaseGraph) new OrientGraph(_ndexDatabase));
		else
			_orientDbGraph = _graphFactory
					.create((OrientBaseGraph) new OrientGraphNoTx(_ndexDatabase));

		/*
		 * only initialize the ORM once
		 */
		if (!NdexSchemaManager.INSTANCE.isInitialized()) {
			NdexSchemaManager.INSTANCE.init(_orientDbGraph.getBaseGraph());
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

		if (_orientDbGraph != null) {
			_orientDbGraph.shutdown();
			_orientDbGraph = null;
		}
	}

	/*
	 * resolving a user is a common requirement across all DAO classes
	 * 
	 */

	protected IUser findIuserById(final String userId) {
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
		
		
	}

}
