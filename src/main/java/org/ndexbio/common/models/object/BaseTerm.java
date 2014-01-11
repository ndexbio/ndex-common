package org.ndexbio.common.models.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ndexbio.common.models.data.IBaseTerm;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseTerm extends Term
{
    private String _name;
    private Namespace _namespace;
    
    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public BaseTerm()
    {
        super();
        
        _namespace = new Namespace();
        _namespace.setPrefix("LOCAL");
        
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
            _namespace = new Namespace(baseTerm.getTermNamespace());
        else
        {
            _namespace = new Namespace();
            _namespace.setPrefix("Local");
        }
    }
    
    
    
    public String getName()
    {
        return _name;
    }

    public void setName(String termName)
    {
        _name = termName;
    }

    public Namespace getNamespace()
    {
        return _namespace;
    }

    public void setNamespace(Namespace namespace)
    {
        _namespace = namespace;
    }
}
