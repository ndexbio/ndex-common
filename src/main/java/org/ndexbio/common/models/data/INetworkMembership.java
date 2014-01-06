package org.ndexbio.common.models.data;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("networkMembership")
public interface INetworkMembership extends IMembership
{
    @Adjacency(label = "accountNetworks", direction = Direction.IN)
    public IAccount getMember();

    @Adjacency(label = "accountNetworks", direction = Direction.IN)
    public void setMember(IAccount member);
    
    @Adjacency(label = "networkMemberships", direction = Direction.IN)
    public INetwork getNetwork();

    @Adjacency(label = "networkMemberships", direction = Direction.IN)
    public void setNetwork(INetwork network);
}
