package org.ndexbio.common.models.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ndexbio.common.models.data.INamespace;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Namespace extends MetadataObject
{
    private String _jdexId;
    private String _prefix;
    private String _uri;
    
    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public Namespace()
    {
        super();
    }
    
    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param namespace The Namespace with source data.
    **************************************************************************/
    public Namespace (INamespace namespace)
    {
        super(namespace);
        
        _jdexId = namespace.getJdexId();
        _prefix = namespace.getPrefix();
        _uri = namespace.getUri();
    }
    
    
    
    public String getJdexId()
    {
        return _jdexId;
    }
    
    public void setJdexId(String jdexId)
    {
        _jdexId = jdexId;
    }
    
    public String getPrefix()
    {
        return _prefix;
    }
    
    public void setPrefix(String prefix)
    {
        _prefix = prefix;
    }
    
    public String getUri()
    {
        return _uri;
    }
    
    public void setUri(String uri)
    {
        _uri = uri;
    }
}
