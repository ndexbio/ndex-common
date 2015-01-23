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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.common.util.TermStringType;
import org.ndexbio.common.util.TermUtilities;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.PropertiedObject;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.xgmml.parser.ParseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ReadDataManager {
	
	private NdexPersistenceService networkService;
	
	// table that stores all the prefixes defined in the current network.
	private Map<String, Namespace> prefixMap;
	private String ownerName;
	private String networkTitle;
	private String networkDesc;

	protected final static String XLINK = "http://www.w3.org/1999/xlink";
	
/*	private static final String PATTERN2X = "type|fill|w|h|size|width|outline|"
			 + "(cy:)?((node|edge)Transparency|(node|edge)LabelFont|(border|edge)LineType)|"
			 + "(cy:)?(source|target)Arrow(Color)?";
	private static final String BG_COLOR_PATTERN = "backgroundColor";
*/	
//	private static final Pattern P2X = Pattern.compile(PATTERN2X);
//	private static final Pattern PBG_COLOR = Pattern.compile(BG_COLOR_PATTERN);

	private StringBuilder currentCData; 
	private StringBuilder currentGraphicsStr;

	private boolean saved;
	/* Stack of original network IDs */
	private Stack<Object> networkStack;

	/* Attribute values */
	protected ParseState attState = ParseState.NONE;
	protected String currentAttributeID;

	/** Edge handle list */
	protected List<String> handleList;
	/** X handle */
	protected String edgeBendX;
	/** Y handle */
	protected String edgeBendY;

	protected List<Object> listAttrHolder;
	
	/** The graph-global directedness, which will be used as default directedness of edges. */
	protected boolean currentNetworkIsDirected = true;

	
//	private Map<Long/*network suid*/, Map<String/*att name*/, String/*att value*/>> networkGraphics;
//	private Map<Long/*node suid*/, Map<String/*att name*/, String/*att value*/>> nodeGraphics;
//	private Map<Long/*edge suid*/, Map<String/*att name*/, String/*att value*/>> edgeGraphics;
	

	//private Map<CyRow, Map<String/*column name*/, String/*equation*/>> equations;
	
	
	protected int graphCount;
	protected int graphDoneCount;
	
	private boolean viewFormat;
	private double documentVersion;
	
	
	private List<String> currentList;
	
	
//	private List<NdexPropertyValuePair> currentProperties;
	private Long currentNodeId;
//	private Long currentEdgeId;
	private XGMMLEdge currentXGMMLEdge;

	
//	private Network currentNetwork;
	
	// Network view format properties
	private Object networkViewId;
	private Object networkId;
	private String visualStyleName;
	private String rendererName;
	private Long currentElementId; // node/edge/network old id
	
	//private RecordingInputStream ris;

	
	private static final Logger logger = LoggerFactory.getLogger(ReadDataManager.class);

	public ReadDataManager(NdexPersistenceService networkService) {

		this.networkService = networkService;
	//	this.ris = ris;
		
		init();
	}

	public void init() {
		
		viewFormat = false;
		graphCount = 0;
		graphDoneCount = 0;
		documentVersion = 1;

//		currentProperties = null;
		currentNodeId = null;
		currentXGMMLEdge = null;
		//parentNetwork = null;
		currentNetworkIsDirected = true;
		//currentRow = null;

	//	ParseState attState = ParseState.NONE;
		currentAttributeID = null;
		currentCData = new StringBuilder();
		currentGraphicsStr = new StringBuilder();
		
		/* Edge handle list */
		handleList = null;

		edgeBendX = null;
		edgeBendY = null;
		
		saved = false;
		networkStack = new Stack<>();
		
		// TODO: determine how these are used 
/*
		networkGraphics = new LinkedHashMap<>();
		nodeGraphics = new LinkedHashMap<>();
		edgeGraphics = new LinkedHashMap<>();
*/		
		prefixMap = new TreeMap<> ();
		
		networkViewId = null;
		networkId = null;
		visualStyleName = null;
		rendererName = null;
		this.networkTitle = null;
		this.networkDesc = null;

	}
	
	public void dispose() {

		currentNodeId = null;
		currentXGMMLEdge = null;
//		currentProperties = null;

	}
	
	public void handleSpecialNetworkAttributes() {
		List<NdexPropertyValuePair> properties = getCurrentNetwork().getProperties();
		String title = null;
		String name = null;
		String identifier = null;
		String networkName = "unknown";
		for (NdexPropertyValuePair prop : properties){
			if (prop.getPredicateString().equalsIgnoreCase("dc:description")){
				getCurrentNetwork().setDescription(prop.getValue());
			} else if (prop.getPredicateString().equalsIgnoreCase("dc:title")){
				title = prop.getValue();
			} else if (prop.getPredicateString().equalsIgnoreCase("name")){
				name = prop.getValue();
			} else if (prop.getPredicateString().equalsIgnoreCase("dc:identifier")){
				identifier = prop.getValue();
			}
		}
		if (title != null){
			networkName = title;
		} else if (name != null){
			networkName = name;
		} else if (identifier != null){
			networkName = identifier;
		}
		
		getCurrentNetwork().setName(networkName);	

	}	
	
	public double getDocumentVersion() {
		return documentVersion;
	}
	
	public void setDocumentVersion(String documentVersion) {
		this.documentVersion = XGMMLParseUtil.parseDocumentVersion(documentVersion);
	}

	
	public boolean isViewFormat() {
		return viewFormat;
	}

	public void setViewFormat(boolean viewFormat) {
		this.viewFormat = viewFormat;
	}

	/**
	 * @param element an Node or Edge
	 * @param attName The name of the attribute
	 * @param attValue The value of the attribute
	 * @throws ExecutionException 
	 */
	protected void addGraphicsAttribute(Long elementId, String attName, String attValue) throws ExecutionException {
		this.networkService.addElementPresentationProperty(elementId, attName, attValue);
	}
	
	public List<SimplePropertyValuePair> getGraphicsAttributes(PropertiedObject element) {
		return element.getPresentationProperties();
	}
	
/*	protected void addGraphicsAttributes(Long elementId, Attributes atts) throws ExecutionException, SAXException {
		if (elementId != null) {
			final int attrLength = atts.getLength();

			for (int i = 0; i < attrLength; i++) {
				System.out.println("Adding graphics " + atts.getLocalName(i) + " = " + atts.getValue(i) + " to " + elementId.toString());
				try {
					this.setElementProperty(elementId, atts.getLocalName(i), 
							atts.getValue(i));
				} catch ( NdexException e) {
					throw new SAXException("Ndex error:" + e.getMessage());
				}
			}
		}
	} */

	protected void addNetworkGraphicsAttributes( Attributes atts) throws ExecutionException, SAXException {
		final int attrLength = atts.getLength();

		ArrayList<SimplePropertyValuePair> plist = new ArrayList<> ();
		for (int i = 0; i < attrLength; i++) {
			SimplePropertyValuePair p = new SimplePropertyValuePair( atts.getLocalName(i), atts.getValue(i));
			plist.add(p);
		}
		try {
			this.networkService.setNetworkProperties(null, plist);
		}catch (NdexException e) {
			throw new SAXException ("Ndex error: " + e.getMessage());
		}
	}
	
	protected void appendCurrentGraphicsString(String s) {
		this.currentGraphicsStr.append(s);
	}

	protected String getCurrentGraphicsString() {
		return this.currentGraphicsStr.toString();
	}

	protected void resetCurrentGraphicsString() {
		this.currentGraphicsStr.setLength(0);
	}
	
	protected void addNetworkGraphicsAttribute( String key, String value) throws SAXException {

		ArrayList<SimplePropertyValuePair> plist = new ArrayList<> ();
		SimplePropertyValuePair p = new SimplePropertyValuePair( key, value);
		plist.add(p);
		try {
			this.networkService.setNetworkProperties(null, plist);
		} catch ( Exception e) {
			String message = "Error accours when adding graphic attribute in XGMML parser. " 
					+e.getMessage(); 
			logger.error(message);
			throw new SAXException(message);
			
		}
	}
	
	protected void addNetworkAttribute( String key, String value, String type) throws SAXException {

		ArrayList<NdexPropertyValuePair> plist = new ArrayList<> ();
		NdexPropertyValuePair p = new NdexPropertyValuePair( key, value);
		p.setDataType(type);
		plist.add(p);
		try {
			this.networkService.setNetworkProperties(plist, null);
		} catch ( Exception e) {
			String message = "Error accours when adding attribute in XGMML parser. " 
					+e.getMessage(); 
			logger.error(message);
			throw new SAXException(message);
			
		}
	}
	
	

	protected NetworkSummary getCurrentNetwork() {
		return this.networkService.getCurrentNetwork();
	}
	
	/**
	 * @return Stack of network IDs (XGMML IDs).
	 */
	protected Stack<Object> getNetworkIDStack() {
		return networkStack;
	}
	
	// This is code from the original Cytoscape XGMML parser 
	// It handles the case of subnetworks expressed as compound nodes 
	// Not supported at this time for NDEx parsing of XGMML

	/*
	public Stack<CyNode> getCompoundNodeStack() {
		return compoundNodeStack;
	}

	protected CyRootNetwork createRootNetwork() {
		if (this.rootNetwork != null)
			return this.rootNetwork;
		
		final CyNetwork baseNet = networkFactory.createNetwork();
		final CyRootNetwork rootNetwork = rootNetworkManager.getRootNetwork(baseNet);
		
		return rootNetwork;
	}
	
	protected CyRootNetwork getRootNetwork() {
		return (currentNetwork != null) ? rootNetworkManager.getRootNetwork(currentNetwork) : null;
	}
	*/
	
	public Long findOrCreateNodeId(String id, String name) throws ExecutionException, NdexException {
		TermStringType stype = TermUtilities.getTermType(name);
		if ( stype == TermStringType.NAME) {
			return this.networkService.findOrCreateNodeIdByExternalId(id, name);
		} 
		
		Long baseTermId = this.networkService.getBaseTermId(name);
		
		Long nodeId = this.networkService.findOrCreateNodeIdByExternalId(id, null);
        
		this.networkService.setNodeRepresentTerm(nodeId, baseTermId);
		
		return null;
	}

	public void addEdge(String subjectId, String predicateStr, String objectId) {
		this.currentXGMMLEdge = new XGMMLEdge(subjectId, predicateStr, objectId);
		

	}
	
	public void insertCurrentEdge () throws NdexException, ExecutionException {
		Long subjectNodeId = this.networkService.findOrCreateNodeIdByExternalId(
				                 this.currentXGMMLEdge.getSubjectId(), null);
		Long objectNodeId  = this.networkService.findOrCreateNodeIdByExternalId(
				this.currentXGMMLEdge.getObjectId(), null);
		Long predicateTermId = this.networkService.getBaseTermId(this.currentXGMMLEdge.getPredicate());
		this.networkService.createEdge(subjectNodeId, objectNodeId, predicateTermId, 
				 null, null,this.currentXGMMLEdge.getProps(),this.currentXGMMLEdge.getPresentationProps() );
	}

	

	public Long findOrCreateBaseTerm(String name, Namespace namespace) 
			throws ExecutionException, NdexException{
		return this.networkService.getBaseTermId(namespace,name );
	}
	
	public Namespace findOrCreateNamespace(String uri, String prefix) throws NdexException{
		Namespace ns = this.prefixMap.get(prefix);
		if ( ns !=null) return ns;
		
		RawNamespace rns = new RawNamespace (prefix, uri);
		
		ns = this.networkService.getNamespace(rns);
		this.prefixMap.put(prefix, ns);
		return ns;
	}

	public XGMMLEdge getCurrentXGMMLEdge () { return this.currentXGMMLEdge;}
	
	public Object getNetworkViewId() {
		return networkViewId;
	}

	protected void setNetworkViewId(Object networkViewId) {
		this.networkViewId = networkViewId;
	}

	public Object getNetworkId() {
		return networkId;
	}

	protected void setNetworkId(Object networkId) {
		this.networkId = networkId;
	}

	public String getVisualStyleName() {
		return visualStyleName;
	}

	protected void setVisualStyleName(String visualStyleName) {
		this.visualStyleName = visualStyleName;
	}

	public String getRendererName() {
		return rendererName;
	}

	protected void setRendererName(String rendererName) {
		this.rendererName = rendererName;
	}
	
	protected void setCurrentElementId(Long currentElementId) {
		this.currentElementId = currentElementId;
	}

	public Long getCurrentNodeId() {
		return this.currentNodeId;
	}

	public void setCurrentNodeId(Long currentNode) {
		this.currentNodeId = currentNode;
	}

/*	
	public Long getCurrentEdgeId() {
		return this.currentEdgeId;
	} */
	
	protected void setElementProperty ( Long elementId, String key, String value,
			String type) throws ExecutionException, NdexException {
		this.networkService.addElementProperty(elementId, key, value,type);
	}

	protected Long getCurrentElementId() {
		return this.currentElementId;
	}
	
	public List<String> getCurrentList() {
		return currentList;
	}

	public void setCurrentList(List<String> currentList) {
		this.currentList = currentList;
	}
	
	public String getCurrentCData() {
		return currentCData.toString();
	}

	public void addCurrentCData(char ch[], int start, int length) {
		this.currentCData.append(ch,start, length);
	}
	
	public void resetCurrentCData() {
		this.currentCData.setLength(0);
	}
	
	public void setNetworkTitle (String title) {
		this.networkTitle = title;
	}
	
	public void setNetworkDesc (String description) {
		this.networkDesc = description;
	}
	
	public void saveNetworkSummary() throws ObjectNotFoundException, NdexException, ExecutionException {
	  if ( !saved) {
		  this.networkService.getCurrentNetwork().setName(this.networkTitle);
		this.networkService.getCurrentNetwork().setDescription(this.networkDesc);
		this.networkService.updateNetworkSummary();
		saved = true;
	  }
	}
/*
	public RecordingInputStream getRis() {
		return ris;
	}
*/



}
