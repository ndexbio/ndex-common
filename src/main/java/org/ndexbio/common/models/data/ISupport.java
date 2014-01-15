package org.ndexbio.common.models.data;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface ISupport extends IMetadataObject
{
    @Property("jdexId")
    public void setJdexId(String jdexId);

    @Property("jdexId")
    public String getJdexId();

    @Property("text")
    public void setText(String text);

    @Property("text")
    public String getText();

    @Adjacency(label = "citationSupports", direction = Direction.IN)
    public void setSupportCitation(ICitation citation);

    @Adjacency(label = "citationSupports", direction = Direction.IN)
    public ICitation getSupportCitation();
    
    @Adjacency(label = "edgeSupports", direction = Direction.IN)
    public void addNdexEdge(IEdge edge);

    @Adjacency(label = "edgeSupports", direction = Direction.IN)
    public Iterable<IEdge> getNdexEdges();
    
    @Adjacency(label = "edgeSupports", direction = Direction.IN)
    public void removeNdexEdge(IEdge edge);
}
