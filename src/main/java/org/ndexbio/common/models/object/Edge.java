package org.ndexbio.common.models.object;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.ISupport;
import org.ndexbio.common.models.data.ITerm;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Edge extends MetadataObject
{
    private String _objectId;
    private String _predicateId;
    private String _subjectId;
    private List<String> _citations;
    private List<String> _supports;



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
        _supports = new ArrayList<String>();
        
        for (final ICitation iCitation : edge.getCitations())
            _citations.add(iCitation.getJdexId());
        
        for (final ISupport iSupport : edge.getSupports())
            _supports.add(iSupport.getJdexId());
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

	public List<String> getCitations() {
		return _citations;
	}

	public void setCitations(List<String> _citations) {
		this._citations = _citations;
	}

	public List<String> getSupports() {
		return _supports;
	}

	public void setSupports(List<String> _supports) {
		this._supports = _supports;
	}
    
    
}
