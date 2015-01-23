package org.ndexbio.xgmml.parser.handler;

/*
 * #%L
 * Cytoscape IO Impl (io-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.util.concurrent.ExecutionException;

import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleGraphGraphics extends AbstractHandler {

	@Override
	public ParseState handle(String namespace, String tag, String qName,  Attributes atts, ParseState current) 
			throws SAXException, ExecutionException {
		if (atts == null)
			return current;

		
		manager.attState = current;
		ParseState nextState = current;
/*	
 *    CJ: commented out for now until we decide how to handle presentation properties in gxmml.
		if (tag.equals("graphics")) {
        	manager.addNetworkGraphicsAttributes( atts);
        } else if (tag.equals("att")) {
			String name = atts.getValue(AttributeValueUtil.ATTR_NAME);
            String value = atts.getValue(AttributeValueUtil.ATTR_VALUE);
            
            if (name != null && value != null)
            	manager.addNetworkGraphicsAttribute(name, value);
        }
*/
		if (nextState != ParseState.NONE)
			return nextState;

		return current;
	}
}
