package org.ndexbio.common.models.object.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.ndexbio.common.models.data.ISupport;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Support extends MetadataObject
{
    private String _jdexId;
    private String _text;
    private String _citation;



    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public Support()
    {
        super();
    }

    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param support The Support with source data.
    **************************************************************************/
    public Support(ISupport support)
    {
        super(support);

        _jdexId = support.getJdexId();
        _text = support.getText();
        _citation = support.getSupportCitation().getJdexId();
    }
 
    public String getJdexId()
    {
        return _jdexId;
    }

    public void setJdexId(String jdexId)
    {
        _jdexId = jdexId;
    }

    public String getText()
    {
        return _text;
    }

    public void setText(String text)
    {
        _text = text;
    }

	public String getCitation() {
		return _citation;
	}

	public void setCitation(String _citation) {
		this._citation = _citation;
	}
    
    
}
