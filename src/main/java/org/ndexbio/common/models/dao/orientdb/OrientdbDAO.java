package org.ndexbio.common.models.dao.orientdb;

import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
//import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
//import org.ndexbio.common.models.dao.CommonDAOValues;
//import org.ndexbio.model.object.NewUser;
//import org.ndexbio.common.helpers.Configuration;
//import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.orientdb.NdexSchemaManager;




//import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;

public abstract class OrientdbDAO {
//	protected FramedGraphFactory _graphFactory = null;
	protected ODatabaseDocumentTx _ndexDatabase = null;
//	protected FramedGraph<OrientBaseGraph> _orientDbGraph = null;
	
	protected ODatabaseDocumentTx db;
	private static final Logger logger = Logger.getLogger(OrientdbDAO.class.getName());

	public OrientdbDAO( ){
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
	
	public OrientdbDAO(ODatabaseDocumentTx db) {
		this.db = db;
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

	protected ODocument getRecordById(UUID id, String orientClass) 
			throws ObjectNotFoundException, NdexException {
		
		try {
			OIndex<?> Idx = this.db.getMetadata().getIndexManager().getIndex("NdexExternalObject.UUID");
			OIdentifiable user = (OIdentifiable) Idx.get(id.toString()); // account to traverse by
			if(user == null) 
				throw new ObjectNotFoundException("Object with UUID ", id.toString());
			
			if( !( (ODocument) user.getRecord() ).getSchemaClass().getName().equals( orientClass ) )
				throw new NdexException("UUID is not for class " + orientClass);
			
			return (ODocument) user.getRecord();
			
		} catch (ObjectNotFoundException e) {
			logger.info("Object with UUID " + id + " does not exist");
			throw e;
		} catch (Exception e) {
			logger.info("Unexpected error on user retrieval by UUID");
			throw new NdexException(e.getMessage());
		}
		
	}
	
	protected ODocument getRecordByAccountName(String accountName, String orientClass) 
			throws ObjectNotFoundException, NdexException {
		
		try {
			OIndex<?> Idx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
			OIdentifiable user = (OIdentifiable) Idx.get(accountName); // account to traverse by
			if(user == null) 
				throw new ObjectNotFoundException("Account ", accountName);
			
			if( !( (ODocument) user.getRecord() ).getSchemaClass().getName().equals( orientClass ) )
				throw new NdexException("UUID is not for class " + orientClass);
			
			return (ODocument) user.getRecord();
			
		} catch (ObjectNotFoundException e) {
			logger.info("Account " + accountName + " does not exist");
			throw e;
		} catch (Exception e) {
			logger.info("Unexpected error on user retrieval by accountName");
			throw new NdexException(e.getMessage());
		}
		
	} 
	
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
