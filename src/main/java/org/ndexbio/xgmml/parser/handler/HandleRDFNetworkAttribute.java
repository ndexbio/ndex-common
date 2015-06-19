/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
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