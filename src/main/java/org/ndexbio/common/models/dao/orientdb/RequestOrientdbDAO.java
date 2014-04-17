package org.ndexbio.common.models.dao.orientdb;

import java.util.Date;
import java.util.List;

import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.dao.RequestDAO;
import org.ndexbio.common.models.data.IGroup;
import org.ndexbio.common.models.data.IGroupInvitationRequest;
import org.ndexbio.common.models.data.IGroupMembership;
import org.ndexbio.common.models.data.IJoinGroupRequest;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INetworkAccessRequest;
import org.ndexbio.common.models.data.INetworkMembership;
import org.ndexbio.common.models.data.IRequest;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.common.models.object.Permissions;
import org.ndexbio.common.models.object.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class RequestOrientdbDAO extends OrientdbDAO implements RequestDAO {
	private static final Logger logger = LoggerFactory
			.getLogger(RequestOrientdbDAO.class);

	private RequestOrientdbDAO() {super();}
	
	static RequestOrientdbDAO createInstance() { return new RequestOrientdbDAO(); }
	
	@Override
	public Request createRequest(Request newRequest)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException {
		Preconditions.checkArgument(null != newRequest,"A Request parameter is required");
		
	        final ORID fromRid = IdConverter.toRid(newRequest.getFromId());
	        final ORID toRid = IdConverter.toRid(newRequest.getToId());
	        
	        if (fromRid.equals(toRid))
	            throw new IllegalArgumentException("The 'from' and 'to' accounts of the request cannot be the same.");
	        
	        try
	        {
	            setupDatabase();
	            
	            final List<ODocument> existingRequests = _ndexDatabase.query(new OSQLSynchQuery<Integer>("SELECT COUNT(@RID) FROM Request WHERE out_fromUser = " + fromRid.toString() + " AND (out_toNetwork = " + toRid.toString() + " OR out_toGroup = " + toRid.toString() + ")"));
	            if (existingRequests == null || existingRequests.isEmpty())
	                throw new NdexException("Unable to get request count.");
	            else if ((long)existingRequests.get(0).field("COUNT") > 0)
	                throw new DuplicateObjectException("You have already made that request and cannot make another.");
	            
	            if (newRequest.getRequestType().equals("Group Invitation"))
	                createGroupInvitationRequest(fromRid, toRid, newRequest);
	            else if (newRequest.getRequestType().equals("Join Group"))
	                createJoinGroupRequest(fromRid, toRid, newRequest);
	            else if (newRequest.getRequestType().equals("Network Access"))
	                createNetworkAccessRequest(fromRid, toRid, newRequest);
	            else
	                throw new IllegalArgumentException("That request type isn't supported: " + newRequest.getRequestType() + ".");
	            
	            return newRequest;
	        }
	        catch (IllegalArgumentException | NdexException ne)
	        {
	            throw ne;
	        }
	        catch (Exception e)
	        {
	            logger.error("Failed to create a request.", e);
	            _orientDbGraph.getBaseGraph().rollback(); 
	            throw new NdexException("Failed to create your request.");
	        }
	        finally
	        {
	            teardownDatabase();
	        }
	}

	@Override
	public void deleteRequest(String requestId)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(requestId), "A request id is required");
		        
	        final ORID requestRid = IdConverter.toRid(requestId);

	        try
	        {
	            setupDatabase();
	            
	            final IRequest requestToDelete = _orientDbGraph.getVertex(requestRid, IRequest.class);
	            if (requestToDelete == null)
	                throw new ObjectNotFoundException("Request", requestId);
	            
	            _orientDbGraph.removeVertex(requestToDelete.asVertex());
	            _orientDbGraph.getBaseGraph().commit();
	        }
	        catch (ObjectNotFoundException onfe)
	        {
	            throw onfe;
	        }
	        catch (Exception e)
	        {
	            if (e.getMessage().indexOf("cluster: null") > -1)
	                throw new ObjectNotFoundException("Request", requestId);
	            
	            logger.error("Failed to delete request: " + requestId + ".", e);
	            _orientDbGraph.getBaseGraph().rollback(); 
	            throw new NdexException("Failed to delete the request.");
	        }
	        finally
	        {
	            teardownDatabase();
	        }

	}

	@Override
	public Request getRequest(String requestId)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(requestId), "A request id is required");
		 
	        final ORID requestRid = IdConverter.toRid(requestId);

	        try
	        {
	            setupDatabase();

	            final IRequest request = _orientDbGraph.getVertex(requestRid, IRequest.class);
	            if (request != null)
	                return new Request(request);
	        }
	        catch (Exception e)
	        {
	            logger.error("Failed to get request: " + requestId + ".", e);
	            throw new NdexException("Failed to get the request.");
	        }
	        finally
	        {
	            teardownDatabase();
	        }
	        
	        return null;
	}

	@Override
	public void updateRequest(Request updatedRequest) throws IllegalArgumentException, NdexException
	{
		Preconditions.checkArgument(null != updatedRequest, "A Request object is required");
	
    final ORID requestRid = IdConverter.toRid(updatedRequest.getId());

    try
    {
        setupDatabase();
        
        final IRequest requestToUpdate = _orientDbGraph.getVertex(requestRid, IRequest.class);
        if (requestToUpdate == null)
            throw new ObjectNotFoundException("Request", updatedRequest.getId());

        requestToUpdate.setResponder(updatedRequest.getResponder());
        requestToUpdate.setResponse(updatedRequest.getResponse());
        
        if (!updatedRequest.getResponse().equals("DECLINED"))
        {
            if (updatedRequest.getRequestType().equals("Group Invitation"))
                processGroupInvitation(updatedRequest);
            else if (updatedRequest.getRequestType().equals("Join Group"))
                processJoinGroup(updatedRequest);
            else if (updatedRequest.getRequestType().equals("Network Access"))
                processNetworkAccess(updatedRequest);
        }
        
        _orientDbGraph.getBaseGraph().commit();
    }
    catch (ObjectNotFoundException onfe)
    {
        throw onfe;
    }
    catch (Exception e)
    {
        if (e.getMessage().indexOf("cluster: null") > -1)
            throw new ObjectNotFoundException("Request", updatedRequest.getId());
        
        logger.error("Failed to update request: " + updatedRequest.getId() + ".", e);
        _orientDbGraph.getBaseGraph().rollback(); 
        throw new NdexException("Failed to update the request.");
    }
    finally
    {
        teardownDatabase();
    }

	}
	/**************************************************************************
	    * Creates a group invitation request. 
	    * 
	    * @param fromRid
	    *            The JID of the requesting group.
	    * @param toRid
	    *            The JID of the invited user.
	    * @param requestToCreate
	    *            The request data.
	    * @throws ObjectNotFoundException
	    *            The group or user wasn't found.
	    * @throws NdexException
	    *            Failed to create the request in the database.
	    **************************************************************************/
	    private void createGroupInvitationRequest(final ORID fromRid, final ORID toRid, final Request requestToCreate) throws ObjectNotFoundException, NdexException
	    {
	        final IGroup requestingGroup = _orientDbGraph.getVertex(fromRid, IGroup.class);
	        if (requestingGroup == null)
	            throw new ObjectNotFoundException("Group", fromRid);
	        
	        final IUser requestedUser = _orientDbGraph.getVertex(toRid, IUser.class);
	        if (requestedUser == null)
	            throw new ObjectNotFoundException("User", toRid);

	        final IGroupInvitationRequest newRequest = _orientDbGraph.addVertex("class:groupInvite", IGroupInvitationRequest.class);
	        newRequest.setMessage(requestToCreate.getMessage());
	        newRequest.setRequestTime(new Date());
	        newRequest.setFromGroup(requestingGroup);
	        newRequest.setToUser(requestedUser);
	        requestingGroup.addRequest(newRequest);
	        requestedUser.addRequest(newRequest);
	        
	        _orientDbGraph.getBaseGraph().commit();

	        requestToCreate.setId(IdConverter.toJid((ORID)newRequest.asVertex().getId()));
	    }
	    
	    /**************************************************************************
	    * Creates a join group request. 
	    * 
	    * @param fromRid
	    *            The JID of the requesting group.
	    * @param toRid
	    *            The JID of the invited user.
	    * @param requestToCreate
	    *            The request data.
	    * @throws ObjectNotFoundException
	    *            The group or user wasn't found.
	    * @throws NdexException
	    *            Failed to create the request in the database.
	    **************************************************************************/
	    private void createJoinGroupRequest(final ORID fromRid, final ORID toRid, final Request requestToCreate) throws ObjectNotFoundException, NdexException
	    {
	        final IUser requestOwner = _orientDbGraph.getVertex(fromRid, IUser.class);
	        if (requestOwner == null)
	            throw new ObjectNotFoundException("User", fromRid);
	        
	        final IGroup requestedGroup = _orientDbGraph.getVertex(toRid, IGroup.class);
	        if (requestedGroup == null)
	            throw new ObjectNotFoundException("Group", toRid);

	        final IJoinGroupRequest newRequest = _orientDbGraph.addVertex("class:joinGroup", IJoinGroupRequest.class);
	        newRequest.setMessage(requestToCreate.getMessage());
	        newRequest.setRequestTime(new Date());
	        newRequest.setFromUser(requestOwner);
	        newRequest.setToGroup(requestedGroup);
	        
	        requestOwner.addRequest(newRequest);
	        requestedGroup.addRequest(newRequest);
	        
	        _orientDbGraph.getBaseGraph().commit();

	        requestToCreate.setId(IdConverter.toJid((ORID)newRequest.asVertex().getId()));
	    }
	    
	    /**************************************************************************
	    * Creates a network access request. 
	    * 
	    * @param fromRid
	    *            The JID of the requesting group.
	    * @param toRid
	    *            The JID of the invited user.
	    * @param requestToCreate
	    *            The request data.
	    * @throws ObjectNotFoundException
	    *            The group or user wasn't found.
	    * @throws NdexException
	    *            Failed to create the request in the database.
	    **************************************************************************/
	    private void createNetworkAccessRequest(final ORID fromRid, final ORID toRid, final Request requestToCreate) throws ObjectNotFoundException, NdexException
	    {
	        final IUser requestOwner = _orientDbGraph.getVertex(fromRid, IUser.class);
	        if (requestOwner == null)
	            throw new ObjectNotFoundException("User", fromRid);
	        
	        final INetwork requestedNetwork = _orientDbGraph.getVertex(toRid, INetwork.class);
	        if (requestedNetwork == null)
	            throw new ObjectNotFoundException("Network", toRid);

	        final INetworkAccessRequest newRequest = _orientDbGraph.addVertex("class:networkAccess", INetworkAccessRequest.class);
	        newRequest.setMessage(requestToCreate.getMessage());
	        newRequest.setRequestTime(new Date());
	        newRequest.setFromUser(requestOwner);
	        newRequest.setToNetwork(requestedNetwork);
	        
	        requestOwner.addRequest(newRequest);
	        requestedNetwork.addRequest(newRequest);
	        
	        _orientDbGraph.getBaseGraph().commit();

	        requestToCreate.setId(IdConverter.toJid((ORID)newRequest.asVertex().getId()));
	    }

	    /**************************************************************************
	    * Adds a user to the group that invited them with read-only permissions.
	    * 
	    * @param requestToProcess
	    *            The request.
	    **************************************************************************/
	    private void processGroupInvitation(final Request requestToProcess) throws Exception
	    {
	        final ORID groupId = IdConverter.toRid(requestToProcess.getFromId());
	        final ORID userId = IdConverter.toRid(requestToProcess.getToId());
	        
	        IGroup group = _orientDbGraph.getVertex(groupId, IGroup.class);
	        IUser user = _orientDbGraph.getVertex(userId, IUser.class);
	        
	        IGroupMembership newMember = _orientDbGraph.addVertex("class:groupMembership", IGroupMembership.class);
	        newMember.setGroup(group);
	        newMember.setMember(user);
	        newMember.setPermissions(Permissions.valueOf(requestToProcess.getResponse()));
	        
	        group.addMember(newMember);
	    }

	    /**************************************************************************
	    * Adds a user to their requested group with read-only permissions.
	    * 
	    * @param requestToProcess
	    *            The request.
	    **************************************************************************/
	    private void processJoinGroup(final Request requestToProcess) throws Exception
	    {
	        final ORID groupId = IdConverter.toRid(requestToProcess.getToId());
	        final ORID userId = IdConverter.toRid(requestToProcess.getFromId());
	        
	        IGroup group = _orientDbGraph.getVertex(groupId, IGroup.class);
	        IUser user = _orientDbGraph.getVertex(userId, IUser.class);
	        
	        IGroupMembership newMember = _orientDbGraph.addVertex("class:groupMembership", IGroupMembership.class);
	        newMember.setGroup(group);
	        newMember.setMember(user);
	        newMember.setPermissions(Permissions.valueOf(requestToProcess.getResponse()));
	        
	        group.addMember(newMember);
	    }

	    /**************************************************************************
	    * Adds a user to a network's membership with read-only permissions.
	    * 
	    * @param requestToProcess
	    *            The request.
	    **************************************************************************/
	    private void processNetworkAccess(final Request requestToProcess) throws Exception
	    {
	        final ORID networkId = IdConverter.toRid(requestToProcess.getToId());
	        final ORID userId = IdConverter.toRid(requestToProcess.getFromId());
	        
	        INetwork network = _orientDbGraph.getVertex(networkId, INetwork.class);
	        IUser user = _orientDbGraph.getVertex(userId, IUser.class);
	        
	        INetworkMembership newMember = _orientDbGraph.addVertex("class:networkMembership", INetworkMembership.class);
	        newMember.setNetwork(network);
	        newMember.setMember(user);
	        newMember.setPermissions(Permissions.valueOf(requestToProcess.getResponse()));
	        
	        network.addMember(newMember);
	    }

}
