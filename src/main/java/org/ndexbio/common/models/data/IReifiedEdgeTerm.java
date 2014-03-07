package org.ndexbio.common.models.data;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("ReifiedEdge")
public interface IReifiedEdgeTerm extends ITerm
{
    @Adjacency(label = "reifiedEdgeTermEdge")
    public IEdge getEdge();

    @Adjacency(label = "reifiedEdgeTermEdge")
    public void setEdge(IEdge edge);
    
}
