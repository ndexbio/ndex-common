package org.ndexbio.common.models.object;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.ISupport;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Citation extends MetadataObject
{
    private List<String> _contributors;
    private String _identifier;
    private String _title;
    private String _type;
    private List<Support> _supports;



    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public Citation()
    {
        super();

        _contributors = new ArrayList<String>();
        _supports = new ArrayList<Support>();
    }

    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param citation The Citation with source data.
    **************************************************************************/
    public Citation(ICitation citation)
    {
        super(citation);

        _identifier = citation.getIdentifier();
        _title = citation.getTitle();
        _type = citation.getType();
        _contributors = citation.getContributors();
        _supports = new ArrayList<Support>();
        
        for (final ISupport support : citation.getSupports())
            _supports.add(new Support(support));
    }



    public List<String> getContributors()
    {
        return _contributors;
    }

    public void setContributors(List<String> contributors)
    {
        _contributors = contributors;
    }

    public String getIdentifier()
    {
        return _identifier;
    }
    
    public void setIdentifier(String identifier)
    {
        _identifier = identifier;
    }
    
    public List<Support> getSupports()
    {
        return _supports;
    }
    
    public void setSupports(List<Support> supports)
    {
        _supports = supports;
    }

    public String getTitle()
    {
        return _title;
    }

    public void setTitle(String title)
    {
        _title = title;
    }

    public String getType()
    {
        return _type;
    }

    public void setType(String type)
    {
        _type = type;
    }
}
