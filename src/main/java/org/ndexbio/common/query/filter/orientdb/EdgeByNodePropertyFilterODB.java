package org.ndexbio.common.query.filter.orientdb;

import java.util.ArrayList;
import java.util.Collection;

import org.ndexbio.model.network.query.SpecMatchMode;

public class EdgeByNodePropertyFilterODB extends PropertyFilterODB {
	
	
	private SpecMatchMode mode;
	private Collection<String>   nodeNames;
	private Collection<String>   representTermIDs;  // Orient rids
//	private String[]   functionTermIDs;  //Orient rids
	
	public EdgeByNodePropertyFilterODB () { super();
		representTermIDs = new ArrayList<> ();
		nodeNames = new ArrayList<> ();
	}

	public SpecMatchMode getMode() {
		return mode;
	}

	public void setMode(SpecMatchMode mode) {
		this.mode = mode;
	}

	public Collection<String> getNodeNames() {
		return nodeNames;
	}

	public void addNodeName(String nodeName) {
		this.nodeNames.add(nodeName);
	}

	public Collection<String> getRepresentTermIDs() {
		return representTermIDs;
	}

	public void addRepresentTermID( String termID) {
		this.representTermIDs.add(termID);
	}

	/*
	public String[] getFunctionTermIDs() {
		return functionTermIDs;
	}

	public void setFunctionTermIDs(String[] functionTermIDs) {
		this.functionTermIDs = functionTermIDs;
	}
	
	*/
   
}
