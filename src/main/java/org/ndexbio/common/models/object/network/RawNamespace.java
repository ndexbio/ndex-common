package org.ndexbio.common.models.object.network;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.ndexbio.common.models.data.INamespace;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawNamespace implements Comparable<RawNamespace> 
{
    private String _prefix;
    private String _uri;
    
    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param namespace The Namespace with source data.
    **************************************************************************/
    public RawNamespace (/*UUID networkID, */String prefix, String URI)
    {
        
        this._prefix = prefix;
        this._uri = URI;
     //   this.setNetworkID(networkID);
    }
    
    public String getPrefix()
    {
        return _prefix;
    }
    
    public String getURI()
    {
        return this._uri;
    }
    
	@Override
	public int compareTo(RawNamespace arg0) {
	
		if ( _uri == null ) {
			if (arg0.getURI() == null) {
				return _prefix.compareTo(arg0.getPrefix());
			}
			return -1;
		}
		if ( arg0.getURI() == null) return 1;
		
		int c = _uri.compareTo(arg0.getURI());
		if ( c != 0)
			return c;
		
		if ( this._prefix == null ) {
			if ( arg0.getPrefix() == null )
				return 0;
			
			return -1;
		}
		
		if ( arg0.getPrefix() == null )
				return 1;
		
		return _prefix.compareTo ( arg0.getPrefix());
	}
    
    @Override
	public int hashCode() {
    	if (_uri !=null)
    		return this._uri.hashCode();
    	return _prefix.hashCode();
    }
    
    @Override
	public boolean equals ( Object arg0) {
    	if (arg0 instanceof RawNamespace)
    		return compareTo((RawNamespace)arg0)==0;
    	return false;
    }

}
