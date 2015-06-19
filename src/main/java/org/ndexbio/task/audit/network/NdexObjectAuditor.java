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
package org.ndexbio.task.audit.network;



import java.util.Map;
import java.util.Set;

import org.ndexbio.model.object.network.NetworkElement;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;



/*
 * Represents a generic class that supports basic tracking of NdexObject collections.
 * It creates a Set of a particular subclass of NdexObjects (e.g. Edge, Node, Support , etc) 
 * and supports a method that allows individual set entries to be removed.
 * It also provides a method that will list the JdexId of entries remaining in the set.
 * 
 */
public class NdexObjectAuditor<T> {

	private Set<Long> idSet;
	private final Class<T> ndexClass;

	public NdexObjectAuditor(Class<T> aClass) {
		Preconditions.checkArgument(null != aClass,
				"An subclass of NdexObject is required");
		this.ndexClass = aClass;
		this.idSet = Sets.newConcurrentHashSet();
	}

	public void registerJdexIds(Map<Long, T> ndexMap) {
		idSet.addAll(ndexMap.keySet());
	}

	public void removeProcessedNdexObject(T obj) {
		Preconditions.checkArgument(null != obj, "An NdexObject  is required");

		if ( -1 != ((NetworkElement) obj).getId() && this.idSet.contains(((NetworkElement) obj).getId())) {
			this.idSet.remove(((NetworkElement) obj).getId());
			
		}
	}

	public String displayUnprocessedNdexObjects() {
		if (this.idSet.isEmpty()) {
			return "\nAll " + this.ndexClass.getSimpleName()
					+ " entries were processed";
		}
		StringBuffer sb = new StringBuffer("\nUnprocessed "
				+ this.ndexClass.getSimpleName() + " objects\n");
		for (Long id : Lists.newArrayList(this.idSet)) {
			sb.append(id + " ");
		}
		return sb.toString();
	}
}