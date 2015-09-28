/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
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

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.exceptions.*;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.Request;
import org.ndexbio.model.object.ResponseType;
import org.ndexbio.model.object.User;

import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class RequestDAO extends OrientdbDAO  {
	private static final Logger logger = Logger.getLogger(RequestDAO.class.getName());
	private OrientGraph graph;
	
	/**************************************************************************
	    * RequestDAO
	    * 
	    * @param db
	    *           Database instance from the Connection pool, should be opened
	    * @param graph
	    * 			OrientBaseGraph layer on top of db instance. 
	    **************************************************************************/
	public RequestDAO(ODatabaseDocumentTx dbConnection) {
		super(dbConnection);
		//this.db = graph.getRawGraph();
		this.graph = new OrientGraph(dbConnection, false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);

	}
	
/*	public RequestDAO(ODatabaseDocumentTx db, boolean autoStartTx) {
		super(db);
		this.graph = new OrientGraph(db, autoStartTx);
		//this.db = this.graph.getRawGraph();
	} */

	/**************************************************************************
	    * Creates a request. 
	    * 
	    * @param newRequest
	    *            The request to create.
	    * @param account
	    * 			Logged in user
	    * @throws IllegalArgumentException
	    *            Bad input.
	    * @throws DuplicateObjectException
	    *            The request is a duplicate.
	    * @throws NdexException
	    *            Duplicate requests or failed to create the request in the
	    *            database.
	    * @return The newly created request.
	    **************************************************************************/
	public Request createRequest(Request newRequest, User account)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException {
		Preconditions.checkArgument(null != newRequest,
				"A Request parameter is required");
		Preconditions.checkArgument( newRequest.getSourceUUID() != null
				&& !Strings.isNullOrEmpty( newRequest.getSourceUUID().toString() ),
				"A source UUID is required");
		Preconditions.checkArgument( !Strings.isNullOrEmpty( newRequest.getSourceName() ),
				"A source name is required");
		Preconditions.checkArgument( newRequest.getDestinationUUID() != null
				&& !Strings.isNullOrEmpty( newRequest.getDestinationUUID().toString() ),
				"A destination UUID is required");
		Preconditions.checkArgument( !Strings.isNullOrEmpty( newRequest.getDestinationName() ),
				"A destination name is required");
		Preconditions.checkArgument( newRequest.getPermission() != null,
				"A permission is required");
		Preconditions.checkArgument(account != null,
				"Must be logged in to make a request");
		
		// setup
		ODocument sourceAccount = this.getRecordByUUID(newRequest.getSourceUUID(), null);
		ODocument userAccount = this.getRecordByUUID(account.getExternalId(), NdexClasses.User);
		
		ODocument destinationResource;
		if( newRequest.getPermission().equals(Permissions.GROUPADMIN) || newRequest.getPermission().equals(Permissions.MEMBER)) {
			destinationResource = this.getRecordByUUID(newRequest.getDestinationUUID(), NdexClasses.Group);
			if(sourceAccount.getClassName().equals(NdexClasses.Group))
				throw new NdexException("Group cannot request access to group");
		} else
			destinationResource = this.getRecordByUUID(newRequest.getDestinationUUID(), NdexClasses.Network);
	
		if(sourceAccount.getClassName().equals(NdexClasses.Group))
			if(!checkPermission(userAccount.getIdentity(), sourceAccount.getIdentity(), Direction.OUT, 1, Permissions.GROUPADMIN))
				throw new NdexException("Not admin of specified group");
		
		// check for redundant requests
		this.checkForExistingRequest(userAccount, sourceAccount, destinationResource, newRequest.getPermission());
		
		newRequest.setExternalId(UUID.randomUUID());
		
		try {
			// create request object
			ODocument request = new ODocument(NdexClasses.Request);
			request.fields("sourceUUID", newRequest.getSourceUUID().toString(),
					"sourceName", newRequest.getSourceName(),
					"destinationUUID", newRequest.getDestinationUUID().toString(),
					"destinationName", newRequest.getDestinationName(),
					"message", newRequest.getMessage(),
					"requestPermission", newRequest.getPermission().name(),
					"response", ResponseType.PENDING ,
					NdexClasses.ExternalObj_cTime, newRequest.getCreationTime(),
					NdexClasses.ExternalObj_ID, newRequest.getExternalId().toString(),
					NdexClasses.ExternalObj_mTime, newRequest.getModificationTime(),
					NdexClasses.ExternalObj_isDeleted,false);
			
			request = request.save();
			
			// create links
			OrientVertex vRequest = this.graph.getVertex(request);
			OrientVertex vUser = this.graph.getVertex(userAccount);
			OrientVertex vResource = this.graph.getVertex(destinationResource);
			
			this.graph.addEdge(null, vUser, vRequest, NdexClasses.Request_E_requests);
			
			//need to reload?
			vUser.getRecord().reload();
			vRequest.getRecord().reload();
			
			for(Vertex v : vResource.getVertices(Direction.IN, Permissions.GROUPADMIN.name().toLowerCase(), Permissions.ADMIN.name().toLowerCase()) ) {
				if( ((OrientVertex) v).getRecord().getClassName().equals(NdexClasses.User) ) {
					vRequest.getRecord().reload();
					graph.addEdge(null, vRequest, v, NdexClasses.Request_E_requests);
				}
				
				for(Vertex vv : v.getVertices(Direction.IN, Permissions.GROUPADMIN.name().toLowerCase())) {
					vRequest.getRecord().reload();
					graph.addEdge(null, vRequest, vv, NdexClasses.Request_E_requests);
				}
				
			}
			
			logger.info("Created request:"
					+ "\n   source - " + newRequest.getSourceUUID()
					+ "\n   destination - " + newRequest.getDestinationUUID());

			return newRequest;
		} catch(Exception e) {
			logger.severe("Failed to create request:"
					+ "\n   source - " + newRequest.getSourceUUID()
					+ "\n   destination - " + newRequest.getDestinationUUID());
			throw new NdexException("Failed to create request");
		}

	}

	/**************************************************************************
	    * Deletes a request.
	    * 
	    * @param requestId
	    *            The request ID.
	    * @param account
	    * 			User object
	    * @throws IllegalArgumentException
	    *            Bad input.
	    * @throws ObjectNotFoundException
	    *            The request doesn't exist.
	    * @throws NdexException
	    *            Failed to delete the request from the database.
	    **************************************************************************/
	public void deleteRequest(UUID requestId, User account)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException {
		Preconditions.checkArgument( requestId != null,
				"A request id is required");
		Preconditions.checkArgument( account != null,
				"A user must be logged in");
		
		ODocument request = this.getRecordByUUID(requestId, NdexClasses.Request);
		ODocument user = this.getRecordByUUID(account.getExternalId(), NdexClasses.User);
		try {
			OrientVertex vRequest = graph.getVertex(request);
			
			boolean sender = false;
			for(Vertex v : vRequest.getVertices(Direction.IN) ) {
				if( ((OrientVertex) v).getIdentity().equals( user.getIdentity() ) )
					sender = true;
			}
			if(!sender)
				throw new NdexException("Not authorized to delete request");
			
			graph.removeVertex(vRequest);
			logger.info("Request deleted");
		} catch (NdexException e){
			throw e;
		} catch(Exception e) {
			logger.severe("Unable to delete request: " + e.getMessage());
			throw new NdexException("Failed to delete request");
		}

	}

	/**************************************************************************
	    * Gets a request by ID.
	    * 
	    * @param requestId
	    *           The request ID.
	    * @param account
	    * 			User object     
	    * @throws IllegalArgumentException
	    *           Bad input.
	    * @throws NdexException
	    *           Failed to query the database.
	    * @return The request.
	    **************************************************************************/
	public Request getRequest(UUID requestId, User account)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(requestId != null,
				"A request id is required");
		Preconditions.checkArgument(account != null,
				"Must be logged in to get a request");
		// TODO check if source UUID and account UUID match up
		
		ODocument request = this.getRecordByUUID(requestId, NdexClasses.Request);
		return RequestDAO.getRequestFromDocument(request);
		
	}

	/**************************************************************************
	    * Updates a request.
	    * 
	    * @param requestId
	    * 			UUID for request
	    * @param updatedRequest
	    *            The updated request information.
	    * @param account
	    * 		user associated request
	    * @throws IllegalArgumentException
	    *            Bad input.
	    * @throws NdexException
	    *            Failed to update the request in the database.
	    **************************************************************************/
	public void updateRequest(UUID requestId, Request updatedRequest, User account)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(null != updatedRequest,
				"A Request object is required");
		Preconditions.checkArgument(updatedRequest.getResponse().equals(ResponseType.ACCEPTED)
				|| updatedRequest.getResponse().equals(ResponseType.DECLINED),
				"A proper response type is required");
		Preconditions.checkArgument(account != null,
				"Must be logged in to update a request");

		ODocument request = this.getRecordByUUID(requestId, NdexClasses.Request);
		ODocument responder = this.getRecordByUUID(account.getExternalId(), NdexClasses.User);

		try {
			
			OrientVertex vRequest = this.graph.getVertex(request);
			
			boolean canModify = false;
			for(Vertex v : vRequest.getVertices(Direction.OUT, "requests")) {
				if( ((OrientVertex) v).getIdentity().equals(responder.getIdentity()) )
					canModify = true;
			}
			
			if(canModify) {
				logger.info("User credentials match with request");
				
				request.field("responder", account.getAccountName());
				request.field(NdexClasses.ExternalObj_mTime, new Date());
				if(updatedRequest.getPermission() != null) request.field("requestPermission", updatedRequest.getPermission().name());
				if(!Strings.isNullOrEmpty( updatedRequest.getResponseMessage() )) request.field("responseMessage", updatedRequest.getResponseMessage() );
				if(!Strings.isNullOrEmpty( updatedRequest.getResponse().name() )) request.field("response", updatedRequest.getResponse().name());
				request.field(NdexClasses.Request_P_responseTime, new Date());
				request.save();
				logger.info("Request has been updated. UUID : " + requestId.toString());
				
			} else {
				logger.severe("Account is not a recipient or sender of request");
				throw new NdexException(""); // message will not be saved
			}
			
		} catch (Exception e) {
			logger.severe("Unable to update request. UUID : " +  requestId.toString());
			throw new NdexException("Failed to update the request.");
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
	
	
	public static Request getRequestFromDocument(ODocument request) throws NdexException {
		try {
			Request result = new Request();
			
			Helper.populateExternalObjectFromDoc(result, request);
			result.setSourceName((String) request.field(NdexClasses.Request_P_sourceName));
			result.setSourceUUID( UUID.fromString( (String) request.field(NdexClasses.Request_P_sourceUUID) ) ); 
			result.setDestinationName((String) request.field("destinationName"));
			result.setDestinationUUID( UUID.fromString( (String) request.field("destinationUUID") ) );
			result.setMessage((String) request.field("message"));
			result.setPermission( Permissions.valueOf( (String) request.field("requestPermission") ) );
			result.setResponder((String) request.field("responder"));
			result.setResponse( ResponseType.valueOf( (String) request.field("response") ) );
			result.setResponseMessage((String) request.field("responseMessage"));
			Date d = request.field(NdexClasses.Request_P_responseTime);
			if ( d !=null )
				result.setResponseTime(new Timestamp (d.getTime()));

			return result;
		} catch (Exception e) {
			logger.severe(e.getMessage());
			throw new NdexException("Failed to retrieve request.");
		} 
	}
	
	private void checkForExistingRequest(ODocument user, ODocument source, ODocument destination, Permissions permission) 
			throws DuplicateObjectException, NdexException {
		
		OrientVertex vUser = this.graph.getVertex(user);
		for(Vertex v : vUser.getVertices(Direction.OUT, "requests") ) {
			if( ((OrientVertex) v).getRecord().field("destinationUUID").equals( destination.field("UUID") )
					&& ((OrientVertex) v).getRecord().field("sourceUUID").equals( source.field("UUID") )
					&& ((OrientVertex) v).getRecord().field("requestPermission").equals( permission.name() ) ) 
				throw new DuplicateObjectException("Request has already been issued");
		}
		
		if(checkPermission(source.getIdentity(), destination.getIdentity(), Direction.OUT, 1, permission))
			throw new NdexException("Access has already been granted");
		
	}
}
