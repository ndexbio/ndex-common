package org.ndexbio.common.models.object.network;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;

import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.common.models.data.ISupport;
import org.ndexbio.common.models.data.ITerm;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Node extends MetadataObject
{
    private String _name;
    private String _represents;
    private List<String> _aliases;
    private List<String> _relatedTerms;
    private List<String> _citations;
    private List<String> _supports;
    
    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public Node()
    {
        super();
        this.initializeCollections();
    }
    
    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param node The Node with source data.
    **************************************************************************/
    public Node(INode node)
    {
        super(node);

        this.initializeCollections();
        _name = node.getName();
     
        
        if (node.getRepresents() != null)
            _represents = node.getRepresents().getJdexId();
        
        for (final ITerm iTerm : node.getAliases())
            _aliases.add(iTerm.getJdexId());
        
        for (final ITerm iTerm : node.getRelatedTerms())
            _relatedTerms.add(iTerm.getJdexId());
        
        for (final ICitation iCitation : node.getCitations())
            _citations.add(iCitation.getJdexId());
        
        for (final ISupport iSupport : node.getSupports())
            _supports.add(iSupport.getJdexId());
    }
    
  
    /*
     * initialize class Collection fields
     */
    private void initializeCollections() {
    	this._aliases = Lists.newArrayList();
    	this._relatedTerms = Lists.newArrayList();
    	this._supports = Lists.newArrayList();
    	this._citations = Lists.newArrayList();
    }
    
    public List<String> getAliases()
    {
        return _aliases;
    }

    public void setAliases(List<String> aliases)
    {
        _aliases = aliases;
    }

    public String getName()
    {
        return _name;
    }
    
    public void setName(String name)
    {
        _name = name;
    }

    public List<String> getRelatedTerms()
    {
        return _relatedTerms;
    }

    public void setRelatedTerms(List<String> relatedTerms)
    {
        _relatedTerms = relatedTerms;
    }
    
    public String getRepresents()
    {
        return _represents;
    }
    
    public void setRepresents(String representsId)
    {
        _represents = representsId;
    }
}
