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

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.xgmml.parser.ObjectType;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleEdgeAttribute extends AbstractHandler {

	private static final String NAME = "name";
	private static final String VALUE = "value";

	@Override
	public ParseState handle(String namespace, String tag, String qName,  Attributes atts, ParseState current)
			throws SAXException, ExecutionException {
		if (atts == null)
			return current;

		manager.attState = current;

		// Is this a graphics override?
		String name = atts.getValue(NAME);
		final String value = atts.getValue(VALUE);
    	String type = atts.getValue("type");

    	// Check for blank attribute
		if (name == null && value == null)
			return current;
      
		if ( name.equals("interaction")) {
			manager.getCurrentXGMMLEdge().setPredicate(value);
			return current;
		}
		

		if (manager.getDocumentVersion() < 3.0) {
			// Writing locked visual properties as regular <att> tags is
			// deprecated!
			if (name.startsWith("edge.")) {
				// It is a bypass attribute...
				name = name.replace(".", "").toLowerCase();
				manager.getCurrentXGMMLEdge().getPresentationProps().add(
						new SimplePropertyValuePair(name,value));
				return current;
			}
		}

		ObjectType objType = typeMap.getType(type);
		if (objType.equals(ObjectType.LIST)){
			manager.currentAttributeID = name;
			manager.setCurrentList(new ArrayList<String>());			
			return ParseState.LIST_ATT;
		}

		
		NdexPropertyValuePair prop = new NdexPropertyValuePair(name,value);
		prop.setDataType(type);

		manager.getCurrentXGMMLEdge().getProps().add(prop);

		return current;
	}
}
