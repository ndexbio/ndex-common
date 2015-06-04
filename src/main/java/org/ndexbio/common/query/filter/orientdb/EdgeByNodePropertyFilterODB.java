package org.ndexbio.common.query.filter.orientdb;

import java.util.Set;
import java.util.TreeSet;

import org.ndexbio.model.network.query.SpecMatchMode;

public class EdgeByNodePropertyFilterODB extends PropertyFilterODB {
	
	
	private SpecMatchMode mode;
	private Set<String>   nodeNames;
	private Set<String>   representTermIDs;  // Orient rids
//	private String[]   functionTermIDs;  //Orient rids
	
	public EdgeByNodePropertyFilterODB () { super();
		representTermIDs = new TreeSet<> ();
		nodeNames = new TreeSet<> ();
	}

	public SpecMatchMode getMode() {
		return mode;
	}

	public void setMode(SpecMatchMode mode) {
		this.mode = mode;
	}
/*
	public Collection<String> getNodeNames() {
		return nodeNames;
	}
*/
	public void addNodeName(String nodeName) {
		this.nodeNames.add(nodeName.toLowerCase());
	}
/*
	public Collection<String> getRepresentTermIDs() {
		return representTermIDs;
	}
*/
	public void addRepresentTermID( String termID) {
		this.representTermIDs.add(termID);
	}

	public boolean containsRepresentTermId(String representTermId) {
		return representTermIDs.contains(representTermId);
	}
	
	public boolean conatinsNodeName(String name) {
		return nodeNames.contains(name.toLowerCase());
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
