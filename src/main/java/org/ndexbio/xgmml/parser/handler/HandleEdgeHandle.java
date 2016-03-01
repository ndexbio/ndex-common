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

import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Routines to handle edge bends. There are two different formats for edge
 * bends. The original XGMML code encoded edge bends as: <att
 * name="edgeBend"> <att name="handle"> <att value="15277.6748046875"
 * name="x"/> <att value="17113.919921875" name="y"/> </att> <att
 * name="handle"> <att value="15277.6748046875" name="x"/> <att
 * value="17113.919921875" name="y"/> </att> </att>
 * 
 * In version 1.1, which was simplified to: <att name="edgeBend"> <att
 * name="handle" x="15277.6748046875" y="17113.919921875" /> <att
 * name="handle" x="15277.6748046875" y="17113.919921875" /> </att>
 */

/**
 * Handle the "handle" attribute. If this is an original format XGMML file (1.0)
 * we just punt to the next level down. If this is a newer format file, we
 * handle the attributes directly.
 */
public class HandleEdgeHandle extends AbstractHandler {
	
	private static final String HANDLE = "handle";

	
	@Override
	public ParseState handle(String namespace, String tag, String qName,  Attributes atts, ParseState current) throws SAXException {

		final String name = attributeValueUtil.getAttribute(atts, "name");
		
		if (manager.getDocumentVersion() == 1.0) {
			// This is the outer "handle" attribute
			if (!name.equals(HANDLE)) {
				// OK, this is one of our "data" attributes
				if (attributeValueUtil.getAttributeValue(atts, "x") != null) {
					manager.edgeBendX = attributeValueUtil.getAttributeValue(atts, "x");
				} else if (attributeValueUtil.getAttributeValue(atts, "y") != null) {
					manager.edgeBendY = attributeValueUtil.getAttributeValue(atts, "y");
				} else {
					throw new SAXException("expected x or y value for edgeBend handle - got " + atts.getValue("name"));
				}
			}
		} else {
			// New format -- get the x and y values directly
			if (attributeValueUtil.getAttribute(atts, "x") != null)
				manager.edgeBendX = attributeValueUtil.getAttribute(atts, "x");
			
			if (attributeValueUtil.getAttribute(atts, "y") != null)
				manager.edgeBendY = attributeValueUtil.getAttribute(atts, "y");
			
			if (manager.edgeBendX != null && manager.edgeBendY != null) {
				if (manager.handleList == null)
					manager.handleList = new ArrayList<>();
				
				manager.handleList.add(manager.edgeBendX + "," + manager.edgeBendY);
				manager.edgeBendX = null;
				manager.edgeBendY = null;
			}
		}
		
		return current;
	}
}
