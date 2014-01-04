package org.ndexbio.common.models.data;

import com.tinkerpop.frames.Property;

public interface INamespace extends IMetadataObject
{
    @Property("jdexId")
    public String getJdexId();

    @Property("jdexId")
    public void setJdexId(String jdexId);

    @Property("prefix")
    public String getPrefix();

    @Property("prefix")
    public void setPrefix(String prefix);

    @Property("uri")
    public void setUri(String uri);

    @Property("uri")
    public String getUri();
}
