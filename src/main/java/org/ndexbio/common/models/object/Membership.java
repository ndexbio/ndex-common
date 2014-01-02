package org.ndexbio.common.models.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.orientechnologies.orient.core.id.ORID;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.data.IGroup;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.common.models.data.Permissions;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Membership
{
    private Permissions _memberPermissions;
    private String _resourceId;
    private String _resourceName;
    
    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public Membership()
    {
        
    }
    
    /**************************************************************************
    * Populates the class with user membership (from the database).
    * 
    * @param membership A key/value pair containing the object/permissions.
    **************************************************************************/
    public Membership(IUser user, Permissions permissions)
    {
        _memberPermissions = permissions;
        _resourceId = IdConverter.toJid((ORID)user.asVertex().getId());
        _resourceName = user.getFirstName() + " " + user.getLastName();
    }
    
    /**************************************************************************
    * Populates the class with group membership (from the database).
    * 
    * @param membership A key/value pair containing the object/permissions.
    **************************************************************************/
    public Membership(IGroup group, Permissions permissions)
    {
        _memberPermissions = permissions;
        _resourceId = IdConverter.toJid((ORID)group.asVertex().getId());
        _resourceName = group.getName();
    }
    
    /**************************************************************************
    * Populates the class with network membership (from the database).
    * 
    * @param membership A key/value pair containing the object/permissions.
    **************************************************************************/
    public Membership(INetwork network, Permissions permissions)
    {
        _memberPermissions = permissions;
        _resourceId = IdConverter.toJid((ORID)network.asVertex().getId());
        _resourceName = network.getTitle();
    }
    
    
    
    public Permissions getPermissions()
    {
        return _memberPermissions;
    }
    
    public void setPermissions(Permissions memberPermissions)
    {
        _memberPermissions = memberPermissions;
    }
    
    public String getResourceId()
    {
        return _resourceId;
    }
    
    public void setResourceId(String resourceId)
    {
        _resourceId = resourceId;
    }
    
    public String getResourceName()
    {
        return _resourceName;
    }
    
    public void setResourceName(String resourceName)
    {
        _resourceName = resourceName;
    }
}
