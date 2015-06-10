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


public class EdgeCollectionQueryODB {
	private EdgeByNodePropertyFilterODB nodeFilter;
	private EdgeByEdgePropertyFilterODB edgeFilter;
	private String queryName;
	
	private int edgeLimit; // -1 means no limit;

	public EdgeByNodePropertyFilterODB getNodeFilter() {
		return nodeFilter;
	}

	public void setNodeFilter(EdgeByNodePropertyFilterODB nodeFilter) {
		this.nodeFilter = nodeFilter;
	}

	public EdgeByEdgePropertyFilterODB getEdgeFilter() {
		return edgeFilter;
	}

	public void setEdgeFilter(EdgeByEdgePropertyFilterODB edgeFilter) {
		this.edgeFilter = edgeFilter;
	}

	public String getQueryName() {
		return queryName;
	}

	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	public int getEdgeLimit() {
		return edgeLimit;
	}

	public void setEdgeLimit(int edgeLimit) {
		this.edgeLimit = edgeLimit;
	}


	
	
}
