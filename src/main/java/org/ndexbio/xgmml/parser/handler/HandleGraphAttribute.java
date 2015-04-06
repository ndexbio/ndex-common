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

import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.task.utility.XGMMLNetworkExporter;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleGraphAttribute extends AbstractHandler {

	@Override
	public ParseState handle(String namespace, String tag, String qName,  Attributes atts, ParseState current) throws SAXException, ExecutionException {
		if (atts == null)
			return current;
		
		manager.attState = current;
		ParseState nextState = current;

		String attName = atts.getValue(AttributeValueUtil.ATTR_NAME);
		String value = atts.getValue("value");
		
		if (attName == null && value == null)
			return current;

		// Look for "special" network attributes
		if (attName.equals("documentVersion")) {
			// Old format only!
			manager.setDocumentVersion(attributeValueUtil.getAttributeValue(atts, attName));
		} else if ( attName.equals("name")) { 
			manager.setNetworkTitle(value);	
		} else if ( attName.equals(XGMMLNetworkExporter.descAttr)) {
			manager.setNetworkDesc(value);
		} else if (attName.matches("backgroundColor|GRAPH_VIEW_ZOOM|GRAPH_VIEW_CENTER_[XY]|NODE_SIZE_LOCKED|__layoutAlgorithm")) {
			
		//	String attValue = attributeValueUtil.getAttributeValue(atts, attName);
		
		/*  CJ: commented out for now until we decide how to handle presentation property.	
			StringBuilder sb = new StringBuilder ();
			sb.append("<"+qName );
			for ( int i = 0 ; i < atts.getLength(); i++) {
				sb.append(" " + atts.getQName(i) + "=\"" +atts.getValue(i) + "\""); 
			}
			sb.append("/>");
			manager.addNetworkGraphicsAttribute( attName, sb.toString());
		*/	
			
		} else if ( attName.equals(PropertyGraphNetwork.uuid)) {
			// do nothing.
			
		} else {
			if ( ! (attName.equals("networkMetadata") && namespace.equals("http://www.cs.rpi.edu/XGMML"))) 	
				AttributeValueUtil.setAttribute(manager.getCurrentNetwork(), attName, value, atts.getValue("type"));
				
			nextState = ParseState.NONE; // attributeValueUtil.handleAttribute(atts, true);
		}

		if (nextState != ParseState.NONE)
			return nextState;

		return current;
	}
}
