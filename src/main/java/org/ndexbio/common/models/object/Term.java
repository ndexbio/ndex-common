package org.ndexbio.common.models.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ndexbio.common.helpers.TermDeserializer;
import org.ndexbio.common.models.data.ITerm;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = TermDeserializer.class)
public abstract class Term extends MetadataObject
{
    private String _termType;
    private String _representedNode;
    private String _aliasedNode;
    private String _relatedNode;

    
    
    public Term(ITerm term)
    {
        super(term);
        if (term.getRepresentedNode() != null)
        	_representedNode = term.getRepresentedNode().getJdexId();
        if (term.getUnificationNode() != null)
        	_aliasedNode = term.getUnificationNode().getJdexId();
        if (term.getRelationshipNode() != null)
        	_relatedNode = term.getRelationshipNode().getJdexId();
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

	public String getRepresentedNode() {
		return _representedNode;
	}

	public void setRepresentedNode(String _representedNode) {
		this._representedNode = _representedNode;
	}

	public String getAliasedNode() {
		return _aliasedNode;
	}

	public void setAliasedNode(String _aliasedNode) {
		this._aliasedNode = _aliasedNode;
	}

	public String getRelatedNode() {
		return _relatedNode;
	}

	public void setRelatedNode(String _relatedNode) {
		this._relatedNode = _relatedNode;
	}
    
    
}
