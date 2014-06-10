package org.ndexbio.common.models.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.ndexbio.common.models.data.IBaseTerm;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseTerm extends Term
{
    private String _name;
    private String _namespace;
    
    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public BaseTerm()
    {
        super();
        
        this.setTermType("Base");
    }

    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param baseTerm
    *            The Term with source data.
    **************************************************************************/
    public BaseTerm(IBaseTerm baseTerm)
    {
        super(baseTerm);
        
        this.setTermType("Base");
        _name = baseTerm.getName();
        
        if (baseTerm.getTermNamespace() != null)
            _namespace = baseTerm.getTermNamespace().getJdexId();

    }
    
    
    public BaseTerm(String name, String jdexId, String namespaceId) {
    	super();
        
        this.setTermType("Base");
        _name = name;
        _namespace = namespaceId;
        this.setId(jdexId);
	}

	public String getName()
    {
        return _name;
    }

    public void setName(String termName)
    {
        _name = termName;
    }

    public String getNamespace()
    {
        return _namespace;
    }

    public void setNamespace(String namespace)
    {
        _namespace = namespace;
    }
}
