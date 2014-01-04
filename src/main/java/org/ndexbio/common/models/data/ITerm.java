package org.ndexbio.common.models.data;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeField;

@TypeField("termType")
public interface ITerm extends IMetadataObject
{
    @Property("jdexId")
    public String getJdexId();

    @Property("jdexId")
    public void setJdexId(String jdexId);
}
