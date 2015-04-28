package org.ndexbio.common.query.filter.orientdb;

import java.util.ArrayList;
import java.util.List;

public class PropertyFilterODB {

	
	private List<String> _propertyList;
	
	public PropertyFilterODB () {
		_propertyList = new ArrayList<>(10);
	}
	
	public List<String> getPropertySpecList() {
		return _propertyList;
	}
	public void setPropertySpecList(List<String> propertyList) {
		this._propertyList = propertyList;
	}

}
