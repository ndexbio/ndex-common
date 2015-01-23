package org.ndexbio.xgmml.parser.handler;



import java.util.Collection;

import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleListAttributeDone extends AbstractHandler {

    @Override
    public ParseState handle(String namespace, String tag, String qName,  Attributes atts, ParseState current) throws SAXException {
        try {
            if (manager.getCurrentList() != null && manager.getCurrentList().size() > 0) {
            	String stringList = joinStringsToCsv(manager.getCurrentList());
            	//System.out.println("Setting " + manager.currentAttributeID + " = " + stringList + " for current element");
            	manager.setElementProperty(manager.getCurrentElementId(), manager.currentAttributeID, stringList, "list");
                manager.listAttrHolder = null;
            }
        } catch (Exception e) {
            String err = "XGMML list handling error for attribute '" + manager.currentAttributeID +
                         "' and network '" + manager.getCurrentNetwork() + "': " + e.getMessage();
            throw new SAXException(err);
        }
        
        return current;
    }
    
	private String joinStringsToCsv(Collection<String> strings) {
		
		String resultString = "";
		if (strings == null || strings.size() == 0) return resultString;
		for (final String string : strings) {
			resultString += "'" + string + "',";
		}
		resultString = resultString.substring(0, resultString.length() - 1);
		return resultString;

	}
}
