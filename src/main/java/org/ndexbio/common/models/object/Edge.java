package org.ndexbio.common.models.object;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.IEdge;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Edge extends MetadataObject
{
    private String _objectId;
    private String _predicateId;
    private String _subjectId;
    private List<String> _citations;



    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public Edge()
    {
        super();
    }

    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param edge The Edge with source data.
    **************************************************************************/
    public Edge(IEdge edge)
    {
        super(edge);

        _subjectId = edge.getSubject().getJdexId();
        _predicateId = edge.getPredicate().getJdexId();
        _objectId = edge.getObject().getJdexId();
        
        _citations = new ArrayList<String>();
        
        for (final ICitation iCitation : edge.getCitations())
            _citations.add(iCitation.getJdexId());
    }

    


    public List<String> getCitations()
    {
        return _citations;
    }

    public void setCitations(List<String> citations)
    {
        _citations = citations;
    }

    public String getO()
    {
        return _objectId;
    }

    public void setO(String objectId)
    {
        _objectId = objectId;
    }

    public String getP()
    {
        return _predicateId;
    }

    public void setP(String predicateId)
    {
        _predicateId = predicateId;
    }
    
    public String getS()
    {
        return _subjectId;
    }

    public void setS(String subjectId)
    {
        _subjectId = subjectId;
    }
}
