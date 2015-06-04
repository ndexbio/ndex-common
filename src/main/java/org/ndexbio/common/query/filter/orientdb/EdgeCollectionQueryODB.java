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
