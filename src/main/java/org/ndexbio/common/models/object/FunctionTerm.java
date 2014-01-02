package org.ndexbio.common.models.object;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.IFunctionTerm;
import org.ndexbio.common.models.data.ITerm;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionTerm extends Term
{
    private String _termFunction;
    private Map<Integer, String> _parameters;

    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public FunctionTerm()
    {
        super();
        
        this.setTermType("Function");
    }

    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param iFunctionTerm The Term with source data.
    **************************************************************************/
    public FunctionTerm(IFunctionTerm iFunctionTerm)
    {
        this.setTermType("Function");
        this.setTermFunction(iFunctionTerm.getTermFunc().getJdexId());

        Integer parameterIndex = new Integer(0);
        Integer functionIndex =  new Integer(0);
        
        for (final ITerm entry : iFunctionTerm.getTermParameters())
        {
            if (entry instanceof IBaseTerm)
            {
                parameterIndex++;  //pseudo key for ordering
                IBaseTerm bt = (IBaseTerm)entry;
                this.getParameters().put(parameterIndex, bt.getName());
                
            }
            else if (entry instanceof IFunctionTerm)
            {
                functionIndex++; //pseudo key for ordering
                this.getParameters().put(functionIndex, entry.getJdexId());
            }
        }
    }



    public Map<Integer, String> getParameters()
    {
        return _parameters;
    }

    public void setParameters(Map<Integer, String> parameters)
    {
        _parameters = parameters;
    }

    public String getTermFunction()
    {
        return _termFunction;
    }

    public void setTermFunction(String termFunction)
    {
        _termFunction = termFunction;
    }
}
