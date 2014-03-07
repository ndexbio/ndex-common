package org.ndexbio.common.models.object;

import org.ndexbio.common.models.data.IReifiedEdgeTerm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReifiedEdgeTerm extends Term
{
    private String _termEdge;

    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public ReifiedEdgeTerm()
    {
        super();
        this.setTermType("ReifiedEdge");
    }

    /**************************************************************************
    * Populates the class (from the database) 
    * 
    * @param iReifiedEdgeTerm The Term with source data.
    **************************************************************************/
    public ReifiedEdgeTerm(IReifiedEdgeTerm iReifiedEdgeTerm)
    {
        this.setTermType("ReifiedEdge");
        this.setTermEdge(iReifiedEdgeTerm.getEdge().getJdexId());
    }

	public String getTermEdge() {
		return _termEdge;
	}

	public void setTermEdge(String termEdge) {
		this._termEdge = termEdge;
	}


}
