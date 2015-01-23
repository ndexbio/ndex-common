package org.ndexbio.xgmml.parser;

import java.util.HashMap;
import java.util.Map;

import static org.ndexbio.xgmml.parser.ParseState.EDGE;
import static org.ndexbio.xgmml.parser.ParseState.EDGE_ATT;
import static org.ndexbio.xgmml.parser.ParseState.EDGE_BEND;
import static org.ndexbio.xgmml.parser.ParseState.EDGE_GRAPHICS;
import static org.ndexbio.xgmml.parser.ParseState.EDGE_HANDLE;
import static org.ndexbio.xgmml.parser.ParseState.GRAPH;
import static org.ndexbio.xgmml.parser.ParseState.LIST_ATT;
import static org.ndexbio.xgmml.parser.ParseState.LIST_ELEMENT;
import static org.ndexbio.xgmml.parser.ParseState.LOCKED_VISUAL_PROP_ATT;
import static org.ndexbio.xgmml.parser.ParseState.NET_ATT;
import static org.ndexbio.xgmml.parser.ParseState.NET_GRAPHICS;
import static org.ndexbio.xgmml.parser.ParseState.NODE;
import static org.ndexbio.xgmml.parser.ParseState.NODE_ATT;
import static org.ndexbio.xgmml.parser.ParseState.NODE_GRAPH;
import static org.ndexbio.xgmml.parser.ParseState.NODE_GRAPHICS;
import static org.ndexbio.xgmml.parser.ParseState.NONE;
import static org.ndexbio.xgmml.parser.ParseState.RDF;
import static org.ndexbio.xgmml.parser.ParseState.RDF_DESC;

import org.ndexbio.xgmml.parser.handler.*;

//import org.ndexbio.xgmml.parser.ParseState.*;
//import org.ndexbio.xgmml.parser.RDFTags.*;

public class HandlerFactory {

	public final static String graphics = "graphics";
	
	private Map<ParseState, Map<String, SAXState>> startParseMap;
	private Map<ParseState, Map<String, SAXState>> endParseMap;

	// Should be injected through DI
	private ReadDataManager manager;
	private AttributeValueUtil attributeValueUtil;

	public HandlerFactory(ReadDataManager manager) {
		this.manager = manager;
		this.attributeValueUtil = new AttributeValueUtil(manager);
	}
	
	public void init() {
		startParseMap = new HashMap<>();
		endParseMap = new HashMap<>();
		
		final Object[][] startParseTable = createStartParseTable();
		final Object[][] endParseTable = createEndParseTable();
		
		buildMap(startParseTable, startParseMap);
		buildMap(endParseTable, endParseMap);
	}

	/**
	 * Create the main parse table. This table controls the state machine, and follows the
	 * standard format for a state machine: StartState, Tag, EndState, Method
	 */
	private Object[][] createStartParseTable() {
		if (manager.isViewFormat()) {
			// Cy3 network view format
			final Object[][] tbl = {
					// Initial state. It's all noise until we see our <graph> tag
					{ NONE, "graph", GRAPH, new HandleViewGraph() },
					{ GRAPH, graphics, NET_GRAPHICS, new HandleViewGraphGraphics() },
					{ NET_GRAPHICS, "att", NET_GRAPHICS, new HandleViewGraphGraphics() },
					// Handle nodes
					{ GRAPH, "node", NODE, new HandleViewNode() },
					{ NODE, graphics, NODE_GRAPHICS, new HandleViewNodeGraphics() },
					{ NODE_GRAPHICS, "att", NODE_GRAPHICS, new HandleViewNodeGraphics() },
					// TODO: att-list for bypass
					// Handle edges
					{ GRAPH, "edge", EDGE, new HandleViewEdge() },
					{ EDGE, "graphics", EDGE_GRAPHICS, new HandleViewEdgeGraphics() },
					{ EDGE_GRAPHICS, "att", EDGE_GRAPHICS, new HandleViewEdgeGraphics() },
					// Vizmap Bypass attributes
					{ LOCKED_VISUAL_PROP_ATT, "att", LOCKED_VISUAL_PROP_ATT, new HandleViewLockedVisualPropAttribute() }
			};
			return tbl;
		} else {
			// Cy3 network, Cy2 network+view or regular XGMML formats
			final Object[][] tbl = {
					// Initial state. It's all noise until we see our <graph> tag
					{ NONE, "graph", GRAPH, new HandleGraph() },
					{ GRAPH, graphics, NET_GRAPHICS, new HandleGraphGraphics() },
					{ NET_GRAPHICS, "att", NET_GRAPHICS, new HandleGraphGraphics() },
					{ GRAPH, "att", NET_ATT, new HandleGraphAttribute() },
					// RDF
					{ NET_ATT, "RDF", RDF, null },
					// RDF tags -- most of the data for the RDF tags comes from the CData
					{ RDF, "Description", RDF_DESC, new HandleRDF() },
					{ RDF_DESC, "type", RDF_DESC, null },
					{ RDF_DESC, "description", RDF_DESC, null },
					{ RDF_DESC, "identifier", RDF_DESC, null },
					{ RDF_DESC, "date", RDF_DESC, null },
					{ RDF_DESC, "title", RDF_DESC, null },
					{ RDF_DESC, "source", RDF_DESC, null },
					{ RDF_DESC, "format", RDF_DESC, null },
					// Sub-graphs
					{ NET_ATT, "graph", GRAPH, new HandleGraph() },
					// Nodes
					{ GRAPH, "node", NODE, new HandleNode() },
					{ NODE_GRAPH, "node", NODE, new HandleNode() },
					{ NODE, graphics, NODE_GRAPHICS, new HandleNodeGraphics() },
					{ NODE, "att", NODE_ATT, new HandleNodeAttribute() },
					{ NODE_ATT, "graph", NODE_GRAPH, new HandleNodeGraph() },
					{ NODE_GRAPH, "att", NET_ATT, new HandleGraphAttribute() },
					{ NODE_GRAPHICS, "att", NODE_GRAPHICS, new HandleNodeGraphics() },
					// Edges
					{ GRAPH, "edge", EDGE, new HandleEdge() },
					{ NODE_GRAPH, "edge", EDGE, new HandleEdge() },
					{ EDGE, "att", EDGE_ATT, new HandleEdgeAttribute() },
					{ EDGE, graphics, EDGE_GRAPHICS, new HandleEdgeGraphics() },
					{ EDGE_GRAPHICS, "att", EDGE_GRAPHICS, new HandleEdgeGraphics() },
					{ EDGE_BEND, "att", EDGE_HANDLE, new HandleEdgeHandle() },
					{ EDGE_HANDLE, "att", EDGE_HANDLE, new HandleEdgeHandle() },
					{ LIST_ATT, "att", LIST_ELEMENT, new HandleListAttribute() },
					{ LIST_ELEMENT, "att", LIST_ELEMENT, new HandleListAttribute() } };
			return tbl;
		}
	}
	
	/**
	 * Create the end tag parse table. This table handles calling methods on end tags under
	 * those circumstances where the CData is used, or when it is important to
	 * take some sort of post-action (e.g. associating nodes to groups)
	 */
	private Object[][] createEndParseTable() {
		if (manager.isViewFormat()) {
			// Cy3 network view format
			final Object[][] tbl = {
					{ LOCKED_VISUAL_PROP_ATT, "att", NONE, null },
					{ GRAPH, "graph", NONE, null } };
			return tbl;
		} 
		
			// Cy3 network, Cy2 network+view or regular XGMML formats
		final Object[][] tbl = {
					{ RDF_DESC, "type", RDF_DESC, new HandleRDFNetworkAttribute() },
					{ RDF_DESC, "description", RDF_DESC, new HandleRDFNetworkAttribute()},
					{ RDF_DESC, "identifier", RDF_DESC, new HandleRDFNetworkAttribute() },
					{ RDF_DESC, "date", RDF_DESC, new HandleRDFNetworkAttribute() },
					{ RDF_DESC, "title", RDF_DESC, new HandleRDFNetworkAttribute() },
					{ RDF_DESC, "source", RDF_DESC, new HandleRDFNetworkAttribute()},
					{ RDF_DESC, "format", RDF_DESC, new HandleRDFNetworkAttribute() },
					{ NODE_GRAPHICS, "att", NODE_GRAPHICS, new HandleGraphicsAttributeDone() },
					{ NODE_GRAPHICS, graphics, NODE, new HandleNodeGraphicsDone() },
					{ NODE,   "node", GRAPH, new HandleNodeDone() },
					{ EDGE_GRAPHICS, "att", EDGE_GRAPHICS, new HandleGraphicsAttributeDone() },
					{ EDGE_GRAPHICS, graphics, EDGE_GRAPHICS, new HandleEdgeGraphicsDone() },
					{ EDGE, "edge", GRAPH, new HandleEdgeDone() },
					{ EDGE_HANDLE, "att", EDGE_BEND, new HandleEdgeHandleDone() },
					{ EDGE_BEND, "att", EDGE_BEND, new HandleEdgeHandleList() },
					{ NODE_GRAPH, "graph", NODE, new HandleNodeGraphDone() },
					{ GRAPH, "graph", NONE, new HandleGraphDone() },
					{ LIST_ATT, "att", NONE, new HandleListAttributeDone() } };
		return tbl;
	}

	/**
	 * Build hash
	 * 
	 * @param table
	 * @param map
	 */
	private void buildMap(Object[][] table, Map<ParseState, Map<String, SAXState>> map) {
		int size = table.length;
		Map<String, SAXState> internalMap = null;
		
		for (int i = 0; i < size; i++) {
			SAXState st = new SAXState((ParseState) table[i][0],
					(String) table[i][1], (ParseState) table[i][2],
					(Handler) table[i][3]);
			
			if (st.getHandler() != null) {
				st.getHandler().setManager(manager);
				st.getHandler().setAttributeValueUtil(attributeValueUtil);
			}
			
			internalMap = map.get(st.getStartState());
			if (internalMap == null) {
				internalMap = new HashMap<>();
			}
			
			internalMap.put(st.getTag(), st);
			map.put(st.getStartState(), internalMap);
		}
	}
	
	public SAXState getStartHandler(ParseState currentState, String tag) {
		if (startParseMap.get(currentState) != null)
			return startParseMap.get(currentState).get(tag);

		return null;
	}

	public SAXState getEndHandler(ParseState currentState, String tag) {
		if (endParseMap.get(currentState) != null)
			return endParseMap.get(currentState).get(tag);

		return null;
	}
}
