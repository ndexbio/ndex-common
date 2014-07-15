package org.ndexbio.common.models.object.privilege;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.orientechnologies.orient.core.record.impl.ODocument;

import org.ndexbio.common.models.data.IGroup;
import org.ndexbio.common.models.data.IGroupMembership;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INetworkMembership;
import org.ndexbio.common.models.data.IRequest;
import org.ndexbio.common.models.data.ITask;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.common.models.object.Request;
import org.ndexbio.common.models.object.Task;
import org.ndexbio.model.object.network.Network;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends Account
{
    private String _emailAddress;
    private String _firstName;
    private String _lastName;
    private List<Membership> _groupMemberships;
    private List<Membership> _networkMemberships;
    private List<Request> _requests;
    private List<Task> _tasks;
    private String _username;
    private List<Network> _workSurface;

    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public User()
    {
        super();

        initCollections();
    }
    
    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * Doesn't load owned Groups or Networks.
    * 
    * @param user The User with source data.
    **************************************************************************/
    @Deprecated
    public User(IUser user)
    {
        this(user, false);
    }
    
    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param user The User with source data.
    * @param loadEverything True to load owned Groups and Networks, false to
    *                       exclude them.
    **************************************************************************/
    @Deprecated
    public User(IUser user, boolean loadEverything)
    {
        super(user);

        initCollections();
        
        this.setCreatedDate(user.getCreatedDate());

        _emailAddress = user.getEmailAddress();
        _firstName = user.getFirstName();
        _lastName = user.getLastName();
        _username = user.getUsername();
        
        for (final INetwork onWorkSurface : user.getWorkSurface())
            _workSurface.add(new Network());
        
        for (final ITask ownedTask : user.getTasks())
            _tasks.add(new Task(ownedTask));
        
        for (final IRequest request : user.getRequests())
        {
            //Don't display requests to the user that the user responded to
            final Request userRequest = new Request(request);
            if (userRequest.getResponder() == null || !userRequest.getResponder().equals(this.getId()))
                _requests.add(userRequest);
        }

        if (loadEverything)
        {
            for (final IGroupMembership membership : user.getGroups())
            {
                _groupMemberships.add(new Membership((IGroup)membership.getGroup(), membership.getPermissions()));
                
                if (membership.getPermissions() == Permissions.ADMIN)
                {
                    for (final IRequest request : membership.getGroup().getRequests())
                    {
                        //Don't display requests to the user that the user responded to
                        final Request userRequest = new Request(request);
                        if (userRequest.getResponder() == null || !userRequest.getResponder().equals(this.getId()))
                            _requests.add(userRequest);
                    }
                }
            }
        
            for (final INetworkMembership membership : user.getNetworks())
            {
                _networkMemberships.add(new Membership((INetwork)membership.getNetwork(), membership.getPermissions()));
                
                if (membership.getPermissions() == Permissions.ADMIN)
                {
                    for (final IRequest request : membership.getNetwork().getRequests())
                    {
                        //Don't display requests to the user that the user responded to
                        final Request userRequest = new Request(request);
                        if (userRequest.getResponder() == null || !userRequest.getResponder().equals(this.getId()))
                            _requests.add(userRequest);
                    }
                }
            }
        }
    }
    
    
    
    public String getEmailAddress()
    {
        return _emailAddress;
    }

    public void setEmailAddress(String emailAddress)
    {
        _emailAddress = emailAddress;
    }

    public String getFirstName()
    {
        return _firstName;
    }

    public void setFirstName(String firstName)
    {
        _firstName = firstName;
    }

    public String getLastName()
    {
        return _lastName;
    }

    public void setLastName(String lastName)
    {
        _lastName = lastName;
    }

    public List<Membership> getGroups()
    {
        return _groupMemberships;
    }

    public void setGroups(List<Membership> groupMemberships)
    {
        _groupMemberships = groupMemberships;
    }

    public List<Membership> getNetworks()
    {
        return _networkMemberships;
    }

    public void setNetworks(List<Membership> networkMemberships)
    {
        _networkMemberships = networkMemberships;
    }
    
    public List<Request> getRequests()
    {
        return _requests;
    }
    
    public void setRequests(List<Request> requests)
    {
        _requests = requests;
    }
    
    public List<Task> getTasks()
    {
        return _tasks;
    }
    
    public void setTasks(List<Task> tasks)
    {
        _tasks = tasks;
    }

    public String getUsername()
    {
        return _username;
    }

    public void setUsername(String username)
    {
        _username = username;
    }

    public List<Network> getWorkSurface()
    {
        return _workSurface;
    }
    
    public void setWorkSurface(List<Network> workSurface)
    {
        _workSurface = workSurface;
    }

    

    /**************************************************************************
    * Initializes the collections. 
    **************************************************************************/
    private void initCollections()
    {
        _groupMemberships = new ArrayList<Membership>();
        _networkMemberships = new ArrayList<Membership>();
        _requests = new ArrayList<Request>();
        _tasks = new ArrayList<Task>();
        _workSurface = new ArrayList<Network>();
    }
}
