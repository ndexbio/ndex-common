package org.ndexbio.common.models.data;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.modules.typedgraph.TypeField;

@TypeField("termType")
public interface ITerm extends VertexFrame
{
    @Property("jdexId")
    public String getJdexId();

    @Property("jdexId")
    public void setJdexId(String jdexId);
    
    @Adjacency(label = "nodeRepresents", direction = Direction.IN)
    public void setRepresentedNode(INode node);

    @Adjacency(label = "nodeRepresents", direction = Direction.IN)
    public INode getRepresentedNode();
    
    @Adjacency(label = "nodeUnificationAliases", direction = Direction.IN)
    public void setUnificationNode(INode node);

    @Adjacency(label = "nodeUnificationAliases", direction = Direction.IN)
    public INode getUnificationNode();
    
    @Adjacency(label = "nodeRelationshipAliases", direction = Direction.IN)
    public void setRelationshipNode(INode node);

    @Adjacency(label = "nodeRelationshipAliases", direction = Direction.IN)
    public INode getRelationshipNode();

}
