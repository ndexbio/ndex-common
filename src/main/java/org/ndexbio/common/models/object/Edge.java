package org.ndexbio.common.models.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ndexbio.common.models.data.IEdge;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Edge extends MetadataObject
{
    private String _objectId;
    private String _predicateId;
    private String _subjectId;



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
    }

    

    public void setS(String s)
    {
        _subjectId = s;
    }

    public String getS()
    {
        return _subjectId;
    }

    public void setP(String p)
    {
        _predicateId = p;
    }

    public String getP()
    {
        return _predicateId;
    }

    public void setO(String o)
    {
        _objectId = o;
    }

    public String getO()
    {
        return _objectId;
    }

    public String getObjectId()
    {
        return _objectId;
    }

    public void setObjectId(String objectId)
    {
        _objectId = objectId;
    }

    public String getPredicateId()
    {
        return _predicateId;
    }

    public void setPredicateId(String predicateId)
    {
        _predicateId = predicateId;
    }

    public String getSubjectId()
    {
        return _subjectId;
    }

    public void setSubjectId(String subjectId)
    {
        _subjectId = subjectId;
    }
}
