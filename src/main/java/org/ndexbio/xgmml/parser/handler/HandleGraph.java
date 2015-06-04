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


import org.ndexbio.xgmml.parser.ObjectTypeMap;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;

public class HandleGraph extends AbstractHandler {
	
	public static final String label = "label";
	private static final String docVersionStr = "cy:documentVersion";
	
	@Override
	public ParseState handle(String namespace, String tag, String qName,  Attributes atts, ParseState current) throws Exception {
		manager.graphCount++;
		
		if (manager.graphCount == 1) {
			// Root <graph>...
			final String docVersion = atts.getValue(docVersionStr);
			
			if (docVersion != null) 
				manager.setDocumentVersion(docVersion); // version 3.0+
		
			
		}
		/*
		if (manager.isSessionFormat()) {	
			if (manager.getDocumentVersion() >= 3.0) {
				handleCy3Model(tag, atts, current);
			} else {
				handleCy2ModelAndView(tag, atts, current);
			}
		} else {
			handleGenericXGMMLGraph(tag, atts, current);
		}
		*/
		handleGenericXGMMLGraph(tag, atts, current);
		
		return current;
	}
	
	protected boolean isRegistered(Attributes atts) {
		String s = atts.getValue("cy:registered");

		return s == null || ObjectTypeMap.fromXGMMLBoolean(s);
	}
	

	

	/**
	 * Handles standalone XGMML graphs, not associated with a session file.
	 * @param tag
	 * @param atts
	 * @param current
	 * @return
	 * @throws Exception 
	 */
	private ParseState handleGenericXGMMLGraph(String tag, Attributes atts, ParseState current) throws Exception {
		//final Network currentNet;

		for (int i = 0 ; i < atts.getLength() ; i++ ) {
	//		String name = atts.getLocalName(i);
			String qname = atts.getQName(i);
/*			String type = atts.getType(i);
			String uri = atts.getURI(i); */
			String v = atts.getValue(i);
			
		//	System.out.println(name + ","+qname+","+type+","+uri+","+v);
			if ( qname.equals(label))
				manager.setNetworkTitle(v);

			else if ( !qname.equals(docVersionStr))
				AttributeValueUtil.setAttribute(manager.getCurrentNetwork(), qname, v, null);
	
		}
		
		return current;
	}
	
	/**
	 * @param oldId The original Id of the graph element. If null, one will be created.
	 * @param net Can be null if just adding an XLink to an existing network
	 * @param atts The attributes of the graph tag
	 * @param register Should be true for networks that must be registered.
	 * @return The string identifier of the network
	 */
	/*
	protected Object addCurrentNetwork(Object oldId, INetwork net, Attributes atts, boolean register) {
		if (oldId == null)
			oldId = String.format("_graph%s_%s", manager.graphCount, net.getSUID());
		
		manager.setCurrentElement(net);
		manager.getNetworkIDStack().push(oldId);
		
		if (net != null) {
			manager.addNetwork(oldId, net, register);
			
			if (!manager.isSessionFormat() || manager.getDocumentVersion() < 3.0)
				setNetworkName(net, atts);
		} else {
			manager.setCurrentNetwork(null);
		}
		
		return oldId;
	}
	*/
	/**
	 * Should be used when handling 2.x format only or importing the network from a standalone XGMML file.
	 */
	/*
	protected void setNetworkName(CyNetwork net, Attributes atts) {
		String name = getLabel(atts);
		
		if (name == null || name.trim().isEmpty()) {
			if (net instanceof CyRootNetwork) {
				name = "Root-Network " + net.getSUID();
			} else if (manager.graphCount == 1) {
				name = "Network " + net.getSUID();
			} else {
				CyRootNetwork root = manager.getRootNetwork();
				
				if (root != null) {
					name = root.getBaseNetwork().getRow(root.getBaseNetwork()).get(CyNetwork.NAME, String.class);
					
					if (name == null || name.trim().isEmpty())
						name = root.getRow(root).get(CyNetwork.NAME, String.class);
				}
			}
			
			if (name == null || name.trim().isEmpty())
				name = "Network " + net.getSUID();
			
			name += " - " + manager.graphCount;
		}
		
		if (net != null && name != null) {
			CyRow netRow = net.getRow(net);
			netRow.set(CyNetwork.NAME, name);
		}
	}
	*/
}
