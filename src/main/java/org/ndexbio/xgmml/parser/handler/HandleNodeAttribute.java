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

public class HandleNodeAttribute extends AbstractHandler {

	@Override
	public ParseState handle(final String namespace, final String tag, final String qName,  Attributes atts, ParseState current)
			throws SAXException, ExecutionException {
		if (atts == null)
			return current;
		manager.attState = current;

		// Is this a graphics override?
		String name = atts.getValue("name");

		// Check for blank attribute (e.g. surrounding a group)
		if (name == null && atts.getValue("value") == null)
			return current;

		ParseState nextState = attributeValueUtil.handleAttribute(atts, false);

		if (nextState != ParseState.NONE)
			return nextState;

		return current;
	}
}
