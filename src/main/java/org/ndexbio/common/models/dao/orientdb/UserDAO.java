/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.common.models.dao.orientdb;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
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
			for ( ODocument doc : Helper.getDocumentLinks(userDoc,"out_", NdexClasses.E_admin )) {
		        	Boolean isDeleted = doc.field(NdexClasses.ExternalObj_isDeleted);
	        		if ( isDeleted == null || !isDeleted.booleanValue()) {
	        			String networkUUID = doc.field(NdexClasses.ExternalObj_ID);
	        			if ( !Helper.canRemoveAdmin(db, networkUUID, id.toString())) {
	        				throw new NdexException("Cannot orphan networks");
	        			}
	        				
	        		}
	        		
	        }
            
			for ( ODocument doc : Helper.getDocumentLinks(userDoc, "out_", NdexClasses.GRP_E_admin )) {
	    
	        		Boolean isDeleted = doc.field(NdexClasses.ExternalObj_isDeleted);
	        		if ( isDeleted == null || !isDeleted.booleanValue()) {
	        			String grpUUID = doc.field(NdexClasses.ExternalObj_ID);
	        			if ( !Helper.canRemoveAdminOnGrp(db, grpUUID, id.toString())) {
	        				throw new NdexException("Cannot orphan groups");
	        			}
	        				
	        		}

	        }

	        OrientVertex userV = graph.getVertex(userDoc);

	        //remove the group and network links
	        for ( ODocument doc : Helper.getDocumentLinks(userDoc, "out_", NdexClasses.E_admin)) {
	        
	        		Boolean isDeleted = doc.field(NdexClasses.ExternalObj_isDeleted);
	        		if ( isDeleted == null || !isDeleted.booleanValue()) {
	        			OrientVertex networkV = graph.getVertex(doc);
	        			for ( Edge e : userV.getEdges(networkV, Direction.OUT, NdexClasses.E_admin)) {
	        				e.remove();
	        			}
	        		}
	        }

	        for ( ODocument doc : Helper.getDocumentLinks(userDoc,"out_", NdexClasses.GRP_E_admin )) {
	  
	        		Boolean isDeleted = doc.field(NdexClasses.ExternalObj_isDeleted);
	        		if ( isDeleted == null || !isDeleted.booleanValue()) {
	        			OrientVertex grpV = graph.getVertex(doc);
	        			for ( Edge e : userV.getEdges(grpV, Direction.OUT, NdexClasses.E_admin)) {
	        				e.remove();
	        			}
	        		}
	        }
	        
	        for ( ODocument doc : Helper.getDocumentLinks(userDoc,"in_", NdexClasses.Task_E_owner)) {
        			OrientVertex taskV = graph.getVertex(doc);
	        		for ( Edge e : userV.getEdges(taskV, Direction.IN, NdexClasses.Task_E_owner)) {
	        			e.remove();
	        		}
	        		taskV.reload();
	        		taskV.getRecord().field(NdexClasses.ExternalObj_isDeleted);
	        		taskV.save();
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
