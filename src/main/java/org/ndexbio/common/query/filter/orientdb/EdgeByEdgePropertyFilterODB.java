package org.ndexbio.common.query.filter.orientdb;

import java.util.Set;
import java.util.TreeSet;

public class EdgeByEdgePropertyFilterODB extends PropertyFilterODB {
	
	private Set<String> predicateIds;  //rids of predicates
	
	public EdgeByEdgePropertyFilterODB () {
		super();
		predicateIds = new TreeSet<String>();
	}

/*	public Set<String> getPredicateIds() {
		return predicateIds;
	} */

	public void addPredicateId(String predicateId) {
		this.predicateIds.add(predicateId);
	}
	
	public boolean containsPredicateId(String id) {
		return predicateIds.contains(id);
	}

}
