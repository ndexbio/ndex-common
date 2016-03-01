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

import java.util.Map;
import java.util.Set;

import org.ndexbio.model.exceptions.*;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * handleGraphDone is called when we finish parsing the entire XGMML file. This
 * allows us to do deal with some cleanup line creating all of our groups, etc.
 */
public class HandleGraphDone extends AbstractHandler {

	@Override
	public ParseState handle(String namespace, String tag, String qName,  Attributes atts, ParseState current) throws SAXException {
		graphDone();
		try {
			manager.saveNetworkSummary();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SAXException("Error occurs when saving network summary. " + e.getMessage());
		}
		
		// End of document?
		if (manager.graphDoneCount != manager.graphCount)
			return current;
		
		/*
		// Resolve any unresolved node and edge references
		Map<CyNetwork, Set<Long>> nodeMap = manager.getCache().getNodeLinks();
		
		for (Map.Entry<CyNetwork, Set<Long>> entry : nodeMap.entrySet()) {
			CyNetwork net = entry.getKey();
			Set<Long> ids = entry.getValue();
			
			if (net != null && ids != null && !ids.isEmpty()) {
				if (net instanceof CySubNetwork) {
					final CySubNetwork sn = (CySubNetwork) net;
					
					for (Long id : ids) {
						CyNode n = manager.getCache().getNode(id);
						
						if (n != null)
							sn.addNode(n);
						else
							logger.error("Cannot find XLink node with id \"" + id + "\".");
					}
				} else {
					logger.error("Cannot add existing nodes \"" + ids.toArray()
							+ "\" to a network which is not a CySubNetwork");
				}
			}
		}
		
		// TODO: refactor
		Map<CyNetwork, Set<Long>> edgeMap = manager.getCache().getEdgeLinks();
		
		for (Map.Entry<CyNetwork, Set<Long>> entry : edgeMap.entrySet()) {
			CyNetwork net = entry.getKey();
			Set<Long> ids = entry.getValue();
			
			if (net != null && ids != null && !ids.isEmpty()) {
				if (net instanceof CySubNetwork) {
					final CySubNetwork sn = (CySubNetwork) net;
					
					for (Long id : ids) {
						final CyEdge e = manager.getCache().getEdge(id);
						
						if (e != null)
							sn.addEdge(e);
						else
							logger.error("Cannot find XLink edge with id \"" + id + "\".");
					}
				} else {
					logger.error("Cannot add existing edges \"" + ids.toArray()
							+ "\" to a network which is not a CySubNetwork");
				}
			}
		}
		
		manager.getCache().deleteUnresolvedNodes();
		resolveEquations();
		resolveNetworkPointers();
		updateSUIDAttributes();
		createGroups(); // TODO: we should not create (and specially register) groups here!
		*/
		
		return ParseState.NONE;
	}
	
	protected void graphDone() {
		++manager.graphDoneCount;
		
		// In order to handle sub-graphs correctly
		if (!manager.getNetworkIDStack().isEmpty())
			manager.getNetworkIDStack().pop();
		
		Network currentNet = null;
		final Object netId = manager.getNetworkIDStack().isEmpty() ? null : manager.getNetworkIDStack().peek();
		
		/*
		if (netId != null)
			currentNet = manager.getCache().getNetwork(netId);
		*/
		//TODO: this line is removed by cj. Need to review it.
	//	manager.setCurrentElement(currentNet);
	}
	
	private void resolveEquations() {
		/*
		if (!manager.isSessionFormat() || manager.getDocumentVersion() < 3.0) {
			// 3.0+ session files should not contain raw attributes or equations!
			manager.parseAllEquations();
		}
		*/
	}
	
	private void resolveNetworkPointers() {
		/*if (!manager.isSessionFormat()) {
			// If reading a session file, network pointers should be processed after all XGMML files are parsed,
			// because some network references might not have been created yet.
			manager.getCache().createNetworkPointers();
		}*/
	}
	
	private void updateSUIDAttributes() {
		/*if (!manager.isSessionFormat()) {
			final SUIDUpdater updater = manager.getSUIDUpdater();
			updater.addTables(manager.getCache().getNetworkTables());
			updater.updateSUIDColumns();
		}*/
	}
	
	private void createGroups() {
/*		if (!manager.isSessionFormat())
			manager.createGroups();*/
	}
}
