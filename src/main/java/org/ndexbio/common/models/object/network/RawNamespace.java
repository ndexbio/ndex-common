package org.ndexbio.common.models.object.network;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.ndexbio.common.models.data.INamespace;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawNamespace implements Comparable<RawNamespace> 
{
    private String _prefix;
    private String _uri;
    private UUID networkID;
    
    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public RawNamespace()
    {
        super();
    }
    
    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param namespace The Namespace with source data.
    **************************************************************************/
    public RawNamespace (UUID networkID,String prefix, String URI)
    {
        
        this._prefix = prefix;
        this._uri = URI;
        this.setNetworkID(networkID);
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
        return this._uri;
    }
    
    public void setUri(String uri)
    {
        this._uri = uri;
    }

	@Override
	public int compareTo(RawNamespace arg0) {
		int c = this.networkID.compareTo ( arg0.getNetworkID());
		if ( c!= 0)
			return c;
		
		c = _uri.compareTo(arg0.getUri());
		if ( c != 0)
			return c;
		if ( this._prefix == null ) {
			if ( arg0.getPrefix() == null )
				return 0;
			else 
				return -1;
		}
		
		if ( arg0.getPrefix() == null )
				return 1;
		
		return _prefix.compareTo ( arg0.getPrefix());
	}
    
    public int hashCode() {
    	return this._uri.hashCode();
    }
    
    public boolean equqls ( Object arg0) {
    	if (arg0 instanceof RawNamespace)
    		return compareTo((RawNamespace)arg0)==0;
    	return false;
    }

	public UUID getNetworkID() {
		return this.networkID;
	}

	public void setNetworkID(UUID networkID) {
		this.networkID = networkID;
	}
}
