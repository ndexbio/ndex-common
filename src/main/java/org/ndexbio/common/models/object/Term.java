package org.ndexbio.common.models.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ndexbio.common.helpers.TermDeserializer;
import org.ndexbio.common.models.data.ITerm;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = TermDeserializer.class)
public abstract class Term extends NdexObject
{
    private String _termType;

    
    
    public Term(ITerm term)
    {
        super(term);
    }

    public Term()
    {
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
