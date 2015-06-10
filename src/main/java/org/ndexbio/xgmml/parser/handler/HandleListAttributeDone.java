/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
