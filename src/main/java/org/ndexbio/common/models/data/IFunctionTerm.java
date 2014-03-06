package org.ndexbio.common.models.data;

import java.util.List;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("Function")
public interface IFunctionTerm extends ITerm
{
    @Adjacency(label = "functionTermParameters")
    public Iterable<ITerm> getTermParameters();

    @Adjacency(label = "functionTermParameters")
    public void setTermParameters(Iterable<ITerm> termParameters);
    
    @Property("functionTermOrderedParameters")
    public List<String> getTermOrderedParameterIds();
   
    @Property("functionTermOrderedParameters")
    public void setTermOrderedParameterIds(List<String> orderedParameterIds);

    @Adjacency(label = "functionTermFunction")
    public IBaseTerm getTermFunc();

    @Adjacency(label = "functionTermFunction")
    public void setTermFunc(IBaseTerm term);
}
