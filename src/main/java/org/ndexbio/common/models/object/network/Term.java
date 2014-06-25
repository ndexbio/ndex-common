package org.ndexbio.common.models.object.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.ndexbio.common.helpers.TermDeserializer;
import org.ndexbio.common.models.data.ITerm;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = TermDeserializer.class)
public abstract class Term extends MetadataObject
{
    private String _termType;

    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public Term()
    {
    }  

    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param term The Term with source data.
    **************************************************************************/
    public Term(ITerm term)
    {
        super(term);
    }

    
    
    public String getTermType()
    {
        return _termType;
    }

    public void setTermType(String termType)
    {
        _termType = termType;
    }
}
