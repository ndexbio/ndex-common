package org.ndexbio.common.models.data;

import java.util.Map;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;

public interface IMetadataObject extends VertexFrame
{
    @Property("metadata")
    public void addMetadata(String metadataKey, String metadataValue);

    @Property("metadata")
    public Map<String, String> getMetadata();
    
    @Property("metadata")
    public void removeMetadata(String metadataKey);
    
    @Property("metaterms")
    public void addMetaterm(String metatermKey, IBaseTerm metatermValue);

    @Property("metaterms")
    public Map<String, IBaseTerm> getMetaterms();
    
    @Property("metaterms")
    public void removeMetaterm(String metatermKey);
}
