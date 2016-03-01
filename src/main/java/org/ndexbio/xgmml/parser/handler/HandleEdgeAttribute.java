/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
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

import org.cxio.aspects.datamodels.ATTRIBUTE_DATA_TYPE;
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
		if ( type != null && type.equalsIgnoreCase("real"))
			type = ATTRIBUTE_DATA_TYPE.toCxLabel(ATTRIBUTE_DATA_TYPE.DOUBLE);	
		else if (type!=null && type.equalsIgnoreCase("list"))
			type = ATTRIBUTE_DATA_TYPE.toCxLabel(ATTRIBUTE_DATA_TYPE.STRING);	

		prop.setDataType(type);

		manager.getCurrentXGMMLEdge().getProps().add(prop);

		return current;
	}
}
