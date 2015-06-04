package org.ndexbio.common.query.filter.orientdb;

import java.util.Set;
import java.util.TreeSet;

public class PropertyFilterODB {

	
	private Set<String> _propertyIdSet;
	
	public PropertyFilterODB () {
		_propertyIdSet = new TreeSet<>();
	}
	
/*	public List<String> getPropertySpecList() {
		return _propertyList;
	}
*/	
	public void addPropertyId(String propertyId) {
		this._propertyIdSet.add( propertyId);
	}

	public boolean containsPropertyId(String propertyRid) {
		return _propertyIdSet.contains(propertyRid);
	}
}
