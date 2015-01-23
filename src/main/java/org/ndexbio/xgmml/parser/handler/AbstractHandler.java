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
