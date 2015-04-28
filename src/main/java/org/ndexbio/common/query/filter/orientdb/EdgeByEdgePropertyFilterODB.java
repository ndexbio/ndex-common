package org.ndexbio.common.query.filter.orientdb;

import java.util.ArrayList;
import java.util.Collection;

public class EdgeByEdgePropertyFilterODB extends PropertyFilterODB {
	
	private Collection<String> predicateIds;  //rids of predicates
	
	public EdgeByEdgePropertyFilterODB () {
		super();
		predicateIds = new ArrayList<String>();
	}

	public Collection<String> getPredicateIds() {
		return predicateIds;
	}

	public void setPredicateIds(Collection<String> predicateIds) {
		this.predicateIds = predicateIds;
	}

}
