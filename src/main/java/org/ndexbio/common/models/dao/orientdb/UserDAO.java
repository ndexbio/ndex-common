package org.ndexbio.common.models.dao.orientdb;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.model.exceptions.NdexException;

import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class UserDAO extends UserDocDAO {

	private OrientGraph graph;
	private static final Logger logger = Logger.getLogger(UserDAO.class
			.getName());

	/*
	 * User operations can be achieved with Orient Document API methods. The
	 * constructor will need to accept a OrientGraph object if we wish to use
	 * the Graph API.
	 */

	public UserDAO(ODatabaseDocumentTx dbConnection) {
		super(dbConnection);
		this.graph = new OrientGraph(db, false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);

	}
	
	public UserDAO(ODatabaseDocumentTx dbConnection, OrientGraph graphdb) {
		super(dbConnection);
		this.graph = graphdb;
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);

	}

	/**************************************************************************
	 * Delete a user
	 * 
	 * @param id
	 *            UUID for User
	 * @throws NdexException
	 *             Attempting to access and delete an ODocument from the
	 *             database
	 **************************************************************************/
	public void deleteUserById(UUID id) throws NdexException,
			ObjectNotFoundException {
		Preconditions.checkArgument(null != id, "UUID required");

		ODocument user = this.getRecordById(id, NdexClasses.User);

		/*
		 * if( !this.getUserGroupMemberships(id, Permissions.ADMIN, 0,
		 * 5).isEmpty() || !this.getUserNetworkMemberships(id,
		 * Permissions.ADMIN, 0, 5).isEmpty() ) { throw new
		 * NdexException("Cannot orphan networks or groups"); }
		 */

		try {
			OrientVertex vUser = graph.getVertex(user);
			boolean safe = true;

			// TODO, simplfy by using actual edge directions and labels
			for (Edge e : vUser.getEdges(Direction.BOTH)) {/*
															 * ,
															 * Permissions.ADMIN
															 * .toString().
															 * toLowerCase() +
															 * " " +
															 * Permissions.
															 * GROUPADMIN
															 * .toString
															 * ().toLowerCase()
															 * ) ) {
															 */

				OrientVertex vResource = (OrientVertex) e
						.getVertex(Direction.IN);

				if (!(vResource.getRecord().getSchemaClass().getName()
						.equals(NdexClasses.Group) || vResource.getRecord()
						.getSchemaClass().getName().equals(NdexClasses.Network)))
					continue;
				safe = false;

				for (Edge ee : vResource.getEdges(Direction.BOTH/*
																 * ,
																 * Permissions.
																 * ADMIN
																 * .toString
																 * ().toLowerCase
																 * ()
																 */)) {
					if (!((OrientVertex) ee.getVertex(Direction.OUT))
							.equals(vUser)) {
						safe = true;
					}
				}

			}

			if (!safe)
				throw new NdexException("Cannot orphan groups or networks");

			String accName = user.field (NdexClasses.account_P_accountName);
			
			user.fields(NdexClasses.ExternalObj_isDeleted, true,
					NdexClasses.ExternalObj_mTime, new Date(),
					NdexClasses.account_P_accountName , null,
					NdexClasses.account_P_oldAcctName, accName).save();
			
		} catch (Exception e) {
			logger.severe("Could not delete user from the database");
			throw new NdexException(e.getMessage());
		}

	}


	@Override
	public void commit() {
		this.graph.commit();
	}
	
	@Override
	public void close() {
		this.graph.shutdown();
	}
	

}
