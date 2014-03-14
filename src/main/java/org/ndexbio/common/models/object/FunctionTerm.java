package org.ndexbio.common.models.object;

import java.util.Map;

import org.ndexbio.common.models.data.IFunctionTerm;
import org.ndexbio.common.models.data.ITerm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Maps;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionTerm extends Term
{
    private String _termFunction;
    private Map<String, String> _parameters;

    
    
    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public FunctionTerm()
    {
        super();
        this._parameters = Maps.newHashMap();
        this.setTermType("Function");
    }

    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param iFunctionTerm The Term with source data.
    **************************************************************************/
    public FunctionTerm(IFunctionTerm iFunctionTerm)
    {
    	this._parameters = Maps.newHashMap();
        this.setTermType("Function");
        this.setTermFunction(iFunctionTerm.getTermFunc().getJdexId());

        Integer parameterIndex = new Integer(0);
        
        /*
         * mod 14Mar2014 used ordered term parameters from ITerm to complete
         * the parameter list. 
         * TODO: validate that the same jdexid also exists in the term parameter list
         *       decide what to do when it doesn't
         */
       for (String orderedParmId : iFunctionTerm.getTermOrderedParameterIds()) {
    	   this.getParameters().put(parameterIndex.toString(), orderedParmId);
    	   parameterIndex++; //pseudo key for ordering
       }
        
       // deprecated - ordered parameter list
      //  for (final ITerm entry : iFunctionTerm.getTermParameters())
      //  {         
       //     this.getParameters().put(parameterIndex.toString(), entry.getJdexId());
       //     parameterIndex++; //pseudo key for ordering
       // }
    }

    public Map<String, String> getParameters()
    {
        return _parameters;
    }

    public void setParameters(Map<String, String> parameters)
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
