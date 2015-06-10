/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
