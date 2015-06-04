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



import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;

public class HandleRDFNetworkAttribute extends AbstractHandler {
	
	private final static String title = "dc:title";
	private final static String description  = "dc:description";

	
	@Override
	public ParseState handle(String namespace, String tag, String qName,  Attributes atts, ParseState current)
			throws Exception {
		
		// check that the currentCData is not null and not empty.
		if (null == manager.getCurrentCData()) return current;
		if ("" == manager.getCurrentCData()) return current;

		if ( qName.equals(title)) {
			manager.setNetworkTitle(manager.getCurrentCData().trim());
		} else if ( qName.equals(description)) {
			manager.setNetworkDesc(manager.getCurrentCData().trim());
		} else {
		
			// check that the qName has a prefix, otherwise error
			int colonIndex = qName.indexOf(':');
			if (colonIndex < 1) throw new Exception("no namespace prefix in network attribute qName");
			String prefix = qName.substring(0, colonIndex);
			// 	Find or create the namespace
		
			Namespace ns = manager.findOrCreateNamespace(namespace, prefix);
		//	System.out.println(namespace+":"+prefix);
		
			// Find or create the term for the attribute
			// In the case of a typical XGMML network, this will result in a dublin core namespace
			// with terms for the typical metadata employed by XGMML
				
			manager.findOrCreateBaseTerm(tag, ns);
		
			// set the network metadata with the qName as the property and the currentCData as the value
		
			AttributeValueUtil.setAttribute(manager.getCurrentNetwork(), qName, manager.getCurrentCData().trim(), null);
		}

		manager.resetCurrentCData();
		return current;
	}
}