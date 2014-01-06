package org.ndexbio.common.models.data;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("groupMembership")
public interface IGroupMembership extends IMembership
{
	
    @Adjacency(label = "userGroups", direction = Direction.IN)
    public IUser getMember();

    @Adjacency(label = "userGroups", direction = Direction.IN)
    public void setMember(IUser member);
    
    @Adjacency(label = "groupMembers", direction = Direction.IN)
    public IGroup getGroup();

    @Adjacency(label = "groupMembers", direction = Direction.IN)
    public void setGroup(IGroup group);
}
