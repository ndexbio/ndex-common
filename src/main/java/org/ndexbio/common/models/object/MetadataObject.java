package org.ndexbio.common.models.object;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.IMetadataObject;

public class MetadataObject extends NdexObject
{
    private Map<String, String> _metadata;
    private Map<String, BaseTerm> _metaterms;
    
    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public MetadataObject()
    {
        super();
        
        _metadata = new HashMap<String, String>();
        _metaterms = new HashMap<String, BaseTerm>();
    }
    
    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param node The Node with source data.
    **************************************************************************/
    public MetadataObject(IMetadataObject metadataObject)
    {
        super(metadataObject);

        _metadata = metadataObject.getMetadata();
        _metaterms = new HashMap<String, BaseTerm>();
        
        if (metadataObject.getMetaterms() != null)
        {
            for (Entry<String, IBaseTerm> metaterm : metadataObject.getMetaterms().entrySet())
                _metaterms.put(metaterm.getKey(), new BaseTerm(metaterm.getValue()));
        }
    }

    
    
    public Map<String, String> getMetadata()
    {
        return _metadata;
    }
    
    public void setMetadata(Map<String, String> metadata)
    {
        _metadata = metadata;
    }
    
    public Map<String, BaseTerm> getMetaterms()
    {
        return _metaterms;
    }
    
    public void setMetaterms(Map<String, BaseTerm> metaterms)
    {
        _metaterms = metaterms;
    }
}
