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


import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.xgmml.parser.Handler;
import org.ndexbio.xgmml.parser.ObjectTypeMap;
import org.ndexbio.xgmml.parser.ParseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public abstract class AbstractHandler implements Handler {

	protected ReadDataManager manager;
	protected AttributeValueUtil attributeValueUtil;
	
	protected static final String LABEL = "label";
	
	ObjectTypeMap typeMap;
	
	protected static final Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

	public AbstractHandler() {
	    typeMap = new ObjectTypeMap();
	}

	@Override
	abstract public ParseState handle(String namespace, String tag, String qName, Attributes atts, ParseState current) throws SAXException, NdexException, Exception;

	@Override
	public void setManager(ReadDataManager manager) {
		this.manager = manager;
	}

	@Override
	public void setAttributeValueUtil(AttributeValueUtil attributeValueUtil) {
		this.attributeValueUtil = attributeValueUtil;
	}
	
	protected static String getLabel(Attributes atts) {
		String label = atts.getValue(LABEL);
		
		if (label == null || label.isEmpty())
			label = atts.getValue("id");

		return label;
	}
/*	
	protected static String getId(Attributes atts) {
		Object id = atts.getValue("id");

		if (id != null) {
			final String str = id.toString().trim();

			if (!str.isEmpty()) {
				try {
					id = Long.valueOf(str);
				} catch (NumberFormatException nfe) {
					logger.debug("Graph id is not a number: " + id);
					id = str;
				}
			}
		}
		
		if (id == null || id.toString().isEmpty())
			id = atts.getValue(LABEL);
		
		return id;
	} */
}
