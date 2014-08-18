package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.Request;
import org.ndexbio.model.object.RequestType;
import org.ndexbio.model.object.ResponseType;
import org.ndexbio.model.object.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class RequestDAO extends OrientdbDAO  {
	private static final Logger logger = LoggerFactory.getLogger(RequestDAO.class);
	private ODatabaseDocumentTx db;
	private OrientBaseGraph graph;
	
	/**************************************************************************
	    * RequestDAO
	    * 
	    * @param db
	    *           Database instance from the Connection pool, should be opened
	    * @param graph
	    * 			OrientBaseGraph layer on top of db instance. 
	    **************************************************************************/
	public RequestDAO(ODatabaseDocumentTx db, OrientBaseGraph graph) {
		super(db);
		this.db = db;
		this.graph = graph;
	}

	public Request createRequest(Request newRequest, User account)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException {
		Preconditions.checkArgument(null != newRequest,
				"A Request parameter is required");
		Preconditions.checkArgument( newRequest.getDestinationUUID() != null
				&& !Strings.isNullOrEmpty( newRequest.getDestinationUUID().toString() ),
				"A destination UUID is required");
		Preconditions.checkArgument( !Strings.isNullOrEmpty( newRequest.getDestinationName() ),
				"A destination name is required");
		Preconditions.checkArgument( !newRequest.getRequestType().equals(null),
				"A request type is required");
		Preconditions.checkArgument(account != null,
				"Must be logged in to make a request");
		
		// setup
		ODocument sourceAccount = this.getRecordById(account.getExternalId(), NdexClasses.User);
		ODocument destinationResource;
		if( newRequest.getRequestType().equals(RequestType.NetworkAccess) )
			destinationResource = this.getRecordById(newRequest.getDestinationUUID(), NdexClasses.Network);
		else
			destinationResource = this.getRecordById(newRequest.getDestinationUUID(), NdexClasses.Group);
		
		// check for redundant requests
		this.checkForExistingRequest(sourceAccount, destinationResource);
		
		newRequest.setDestinationName(account.getAccountName());
		newRequest.setDestinationUUID(account.getExternalId());
		newRequest.setExternalId(UUID.randomUUID());
		
		try {
			// create request object
			ODocument request = new ODocument(NdexClasses.Request);
			request.field("sourceUUID", account.getExternalId().toString());
			request.field("sourceName", account.getAccountName());
			request.field("destinationUUID", newRequest.getDestinationUUID().toString());
			request.field("destinationName", newRequest.getDestinationName());
			request.field("message", newRequest.getMessage());
			request.field("requestType", newRequest.getRequestType().toString());
			request.field("createdDate", newRequest.getCreationDate());
			request.field("UUID", newRequest.getExternalId().toString());
			request.save();
			
			// create links
			OrientVertex vRequest = this.graph.getVertex(request);
			OrientVertex vSource = this.graph.getVertex(sourceAccount);
			OrientVertex vResource = this.graph.getVertex(destinationResource);
			
			this.graph.addEdge(null, vSource, vRequest, NdexClasses.Request);
			
			for(Vertex v : vResource.getVertices(Direction.IN, "groupadmin", "admin") ) {
				graph.addEdge(null, vRequest, v, NdexClasses.Request);
			}
		
		} catch(Exception e) {
			throw new NdexException("Failed to create request");
		}

		return newRequest;
	}

	public void deleteRequest(UUID requestId, User account)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException {
		Preconditions.checkArgument( requestId != null,
				"A request id is required");
		Preconditions.checkArgument( account != null,
				"A user must be logged in");
		
		ODocument request = this.getRecordById(requestId, NdexClasses.Request);
		
		try {
			request.delete();
		} catch(Exception e) {
			throw new NdexException("Failed to delete request");
		}

	}

	public Request getRequest(UUID requestId, User account)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(requestId != null,
				"A request id is required");
		Preconditions.checkArgument(account != null,
				"Must be logged in to get a request");
		// TODO check if source UUID and account UUID match up
		
		ODocument request = this.getRecordById(requestId, NdexClasses.Request);
		return this.getRequestFromDocument(request);
		
	}

	public void updateRequest(UUID requestId, Request updatedRequest, User account)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(null != updatedRequest,
				"A Request object is required");
		Preconditions.checkArgument(account != null,
				"Must be logged in to update a request");

		ODocument request = this.getRecordById(requestId, NdexClasses.Request);
		ODocument responder = this.getRecordById(account.getExternalId(), NdexClasses.User);

		try {
			
			OrientVertex vRequest = this.graph.getVertex(request);
			
			boolean canModify = false;
			for(Vertex v : vRequest.getVertices(Direction.BOTH, NdexClasses.Request)) {
				if( ((OrientVertex) v).getIdentity().equals(responder.getIdentity()) )
					canModify = true;
			}
			
			if(canModify) {
				
				if(!Strings.isNullOrEmpty( updatedRequest.getMessage() )) request.field("message", updatedRequest.getMessage());
				request.field("responder", account.getExternalId());
				request.field("modificationDate", new Date());
				if(!Strings.isNullOrEmpty( updatedRequest.getResponseMessage() )) request.field("responseMessage", updatedRequest.getResponseMessage() );
				if(!Strings.isNullOrEmpty( updatedRequest.getResponse().name() )) request.field("response", updatedRequest.getResponse().name());
				
			} else {
				throw new NdexException(""); // message will not be saved
			}
			
		} catch (Exception e) {
			throw new NdexException("Failed to update the request.");
		} 
	}

	public List<Request> getSentRequest(User account, int skipBlocks, int BlockSize) 
			throws ObjectNotFoundException, NdexException {
		Preconditions.checkArgument(account != null,
				"Must be logged in");
		final List<Request> requests = new ArrayList<Request>();
		
		ODocument user = this.getRecordById(account.getExternalId(), NdexClasses.User);
		
		try {
			OrientVertex vUser = this.graph.getVertex(user);
		
			for(Vertex v : vUser.getVertices(Direction.OUT, NdexClasses.Request)) {
				requests.add( this.getRequestFromDocument( ((OrientVertex) v).getRecord() ) );
			}
			
			return requests;
		} catch(Exception e){
			throw new NdexException("Unable to retrieve sent requests");
		}
	}
	
	public List<Request> getPendingRequest(User account, int skipBlocks, int BlockSize) 
			throws ObjectNotFoundException, NdexException {
		Preconditions.checkArgument(account != null,
				"Must be logged in");
		final List<Request> requests = new ArrayList<Request>();
		
		ODocument user = this.getRecordById(account.getExternalId(), NdexClasses.User);
		
		try {
		OrientVertex vUser = this.graph.getVertex(user);
	
		for(Vertex v : vUser.getVertices(Direction.OUT, NdexClasses.Request)) {
			requests.add( this.getRequestFromDocument( ((OrientVertex) v).getRecord() ) );
		}
		
		return requests;
		} catch(Exception e){
			throw new NdexException("Unable to retrieve sent requests");
		}
	}
	
	private Request getRequestFromDocument(ODocument request) throws NdexException {
		try {
			Request result = new Request();
			result.setSourceName((String) request.field("sourceName"));
			result.setSourceUUID( UUID.fromString( (String) request.field("sourceUUID") ) ); 
			result.setDestinationName((String) request.field("destinationName"));
			result.setDestinationUUID( UUID.fromString( (String) request.field("destinationUUID") ) );
			result.setMessage((String) request.field("message"));
			result.setRequestType( RequestType.valueOf( (String) request.field("requestType") ) );
			result.setResponder((String) request.field("responder"));
			result.setResponse( ResponseType.valueOf( (String) request.field("response") ) );
			result.setResponseMessage((String) request.field("responseMessage"));
			result.setExternalId( UUID.fromString( (String) request.field("UUID") ) );
			result.setCreationDate( (Date) request.field("createdDate") );
			result.setModificationDate( (Date) request.field("modificationDate") );
			return result;
		} catch (Exception e) {
			throw new NdexException("Failed to retrieve request.");
		} 
	}
	
	private void checkForExistingRequest(ODocument source, ODocument destination) 
			throws DuplicateObjectException, NdexException {
		// could have been implemented with sql commands
		
		OrientVertex vSource = this.graph.getVertex(source);
	
		for(Vertex v : vSource.getVertices(Direction.OUT, NdexClasses.Request) ) {
			if( ((OrientVertex) v).getRecord().field("UUID").equals( destination.field("UUID") ) ) 
				throw new DuplicateObjectException("Request has been made");
		}
		
		//TODO should delete if there is no way to specify what permission one should get. 
		String permissions[]  = {
								Permissions.ADMIN.name().toLowerCase(),
								Permissions.WRITE.name().toLowerCase(),
								Permissions.READ.name().toLowerCase(),
								Permissions.GROUPADMIN.name().toLowerCase(),
								Permissions.MEMBER.name().toLowerCase()
								};
		
		for(Vertex v : vSource.getVertices(Direction.OUT, permissions)) {
			if( ((OrientVertex) v).getIdentity().equals(destination.getIdentity()) )
				throw new NdexException("Access has already been granted");
		}
	}
}
