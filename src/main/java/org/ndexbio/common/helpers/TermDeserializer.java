package org.ndexbio.common.helpers;

import java.io.IOException;
import org.ndexbio.common.models.object.BaseTerm;
import org.ndexbio.common.models.object.FunctionTerm;
import org.ndexbio.common.models.object.Namespace;
import org.ndexbio.common.models.object.Term;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

//TODO: Remove this class - it was only needed because of incomplete unit test data
public class TermDeserializer extends JsonDeserializer<Term>
{
    public TermDeserializer()
    {
        super();
    }
    
    
    
    @Override
    public Term deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException, JsonProcessingException
    {
        final ObjectMapper jsonMapper = new ObjectMapper();
        final JsonNode serializedTerm = jsonMapper.readTree(jsonParser);
        final JsonNode termType = serializedTerm.get("termType");
        
        if (termType != null)
        {
            if (termType.asText().equals("Base"))
                return populateBaseTerm(serializedTerm);
            else if (termType.asText().equals("Function"))
                return populateFunctionTerm(serializedTerm);
        }
        else
        {
            final JsonNode nameProperty = serializedTerm.get("name");
            if (nameProperty != null)
                return populateBaseTerm(serializedTerm);
            
            final JsonNode functionProperty = serializedTerm.get("termFunction");
            if (functionProperty != null)
                return populateFunctionTerm(serializedTerm);
        }
        
        throw context.mappingException("Unsupported term type.");
    }
    
    
    
    private BaseTerm populateBaseTerm(JsonNode serializedTerm)
    {
        final BaseTerm baseTerm = new BaseTerm();
        baseTerm.setName(serializedTerm.get("name").asText());
        
        if (serializedTerm.get("namespace") != null)
        {
            final JsonNode serializedNamespace = serializedTerm.get("namespace");
            
            final Namespace namespace = new Namespace();
            namespace.setPrefix(serializedNamespace.get("prefix").asText());
            namespace.setUri(serializedNamespace.get("uri").asText());
            
            baseTerm.setNamespace(namespace);
        }
        
        return baseTerm;
    }
    
    private FunctionTerm populateFunctionTerm(JsonNode serializedTerm)
    {
        final FunctionTerm functionTerm = new FunctionTerm();
        functionTerm.setTermFunction(serializedTerm.get("termFunction").asText());

        //TODO: Need to deserialize parameters, don't know what they look like yet
        
        return functionTerm;
    }
}
