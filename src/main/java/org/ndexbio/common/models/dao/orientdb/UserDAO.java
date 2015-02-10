package org.ndexbio.common.models.dao.orientdb;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.model.exceptions.NdexException;

import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
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

		ODocument userDoc = this.getRecordByUUID(id, NdexClasses.User);

		/*
		 * if( !this.getUserGroupMemberships(id, Permissions.ADMIN, 0,
		 * 5).isEmpty() || !this.getUserNetworkMemberships(id,
		 * Permissions.ADMIN, 0, 5).isEmpty() ) { throw new
		 * NdexException("Cannot orphan networks or groups"); }
		 */

		try {
//			boolean safe = true;

			// check if there is any network or group dependency
	        for (OIdentifiable networkDoc : new OTraverse()
	    		.field("out_"+ NdexClasses.E_admin )
	    		.target(userDoc)
	    		.predicate( new OSQLPredicate("$depth <= 1"))) {

	        	ODocument doc = (ODocument) networkDoc;

	        	if ( doc.getClassName().equals(NdexClasses.Network) ) {
	        		Boolean isDeleted = doc.field(NdexClasses.ExternalObj_isDeleted);
	        		if ( isDeleted == null || !isDeleted.booleanValue()) {
	        			String networkUUID = doc.field(NdexClasses.ExternalObj_ID);
	        			if ( !Helper.canRemoveAdmin(db, networkUUID, id.toString())) {
	        				throw new NdexException("Cannot orphan networks");
	        			}
	        				
	        		}
	        		
	        	}
	        }

	        for (OIdentifiable grpDoc : new OTraverse()
	    		.field("out_"+ NdexClasses.GRP_E_admin )
	    		.target(userDoc)
	    		.predicate( new OSQLPredicate("$depth <= 1"))) {

	        	ODocument doc = (ODocument) grpDoc;

	        	if ( doc.getClassName().equals(NdexClasses.Group) ) {
	        		Boolean isDeleted = doc.field(NdexClasses.ExternalObj_isDeleted);
	        		if ( isDeleted == null || !isDeleted.booleanValue()) {
	        			String grpUUID = doc.field(NdexClasses.ExternalObj_ID);
	        			if ( !Helper.canRemoveAdminOnGrp(db, grpUUID, id.toString())) {
	        				throw new NdexException("Cannot orphan groups");
	        			}
	        				
	        		}
	        		
	        	}
	        }

	        OrientVertex userV = graph.getVertex(userDoc);

	        //remove the group and network links
	        for (OIdentifiable networkDoc : new OTraverse()
	    		.field("out_"+ NdexClasses.E_admin )
	    		.target(userDoc)
	    		.predicate( new OSQLPredicate("$depth <= 1"))) {

	        	ODocument doc = (ODocument) networkDoc;

	        	if ( doc.getClassName().equals(NdexClasses.Network) ) {
	        		Boolean isDeleted = doc.field(NdexClasses.ExternalObj_isDeleted);
	        		if ( isDeleted == null || !isDeleted.booleanValue()) {
	        			OrientVertex networkV = graph.getVertex(doc);
	        			for ( Edge e : userV.getEdges(networkV, Direction.OUT, NdexClasses.E_admin)) {
	        				e.remove();
	        			}
	        		}
	        	}
	        }

	        for (OIdentifiable grpDoc : new OTraverse()
	    		.field("out_"+ NdexClasses.GRP_E_admin )
	    		.target(userDoc)
	    		.predicate( new OSQLPredicate("$depth <= 1"))) {

	        	ODocument doc = (ODocument) grpDoc;

	        	if ( doc.getClassName().equals(NdexClasses.Group) ) {
	        		Boolean isDeleted = doc.field(NdexClasses.ExternalObj_isDeleted);
	        		if ( isDeleted == null || !isDeleted.booleanValue()) {
	        			OrientVertex grpV = graph.getVertex(doc);
	        			for ( Edge e : userV.getEdges(grpV, Direction.OUT, NdexClasses.E_admin)) {
	        				e.remove();
	        			}
	        		}
	        	}
	        }
	        
	        // delete tasks an remove links
	        for (OIdentifiable taskDoc : new OTraverse()
	    		.field("in_"+ NdexClasses.Task_E_owner )
	    		.target(userDoc)
	    		.predicate( new OSQLPredicate("$depth <= 1"))) {

	        	ODocument doc = (ODocument) taskDoc;

	        	if ( doc.getClassName().equals(NdexClasses.Task) ) {
        			OrientVertex taskV = graph.getVertex(doc);
	        		for ( Edge e : userV.getEdges(taskV, Direction.IN, NdexClasses.Task_E_owner)) {
	        			e.remove();
	        		}
	        		taskV.reload();
	        		taskV.getRecord().field(NdexClasses.ExternalObj_isDeleted);
	        		taskV.save();
	        	}
	        }

			String accName = userDoc.field (NdexClasses.account_P_accountName);
			String email = userDoc.field(NdexClasses.User_P_emailAddress);
			
			userDoc.fields(NdexClasses.ExternalObj_isDeleted, true,
					NdexClasses.ExternalObj_mTime, new Date(),
					NdexClasses.account_P_accountName , null,
					NdexClasses.account_P_oldAcctName, accName,
					NdexClasses.User_P_emailAddress, null,
					NdexClasses.User_P_oldEmailAddress, email).save();
			
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
