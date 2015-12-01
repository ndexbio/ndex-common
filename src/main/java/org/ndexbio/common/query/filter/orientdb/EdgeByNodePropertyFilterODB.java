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
package org.ndexbio.common.query.filter.orientdb;

import java.util.Set;
import java.util.TreeSet;

import org.ndexbio.model.network.query.SpecMatchMode;

public class EdgeByNodePropertyFilterODB extends PropertyFilterODB {
	
	
	private SpecMatchMode mode;
	private Set<String>   nodeNames;
	private Set<Long>   representTermIDs;  // term elementIds
	private Set<String>  functionTermNames;  //Orient rids
	
	public EdgeByNodePropertyFilterODB () { super();
		representTermIDs = new TreeSet<> ();
		nodeNames = new TreeSet<> ();
		functionTermNames = new TreeSet<>();
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
	public void addRepresentTermID( Long termID) {
		this.representTermIDs.add(termID);
	}

	public boolean containsRepresentTermId(Long representTermId) {
		return representTermIDs.contains(representTermId);
	}
	
	public boolean conatinsNodeName(String name) {
		return nodeNames.contains(name.toLowerCase());
	}
	
	public Set<String> getFunctionTermNames() {
		return functionTermNames;
	}

	public void addFunctionTermName(String functionTerm) {
		this.functionTermNames.add( functionTerm);
	} 
	
	
   
}
