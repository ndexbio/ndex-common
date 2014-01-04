package org.ndexbio.common.models.data;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface INode extends IMetadataObject
{
    @Property("jdexId")
    public void setJdexId(String jdexId);

    @Property("jdexId")
    public String getJdexId();

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(String name);

    @Adjacency(label = "nodeRepresents")
    public void setRepresents(ITerm term);

    @Adjacency(label = "nodeRepresents")
    public ITerm getRepresents();

}
