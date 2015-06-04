package org.ndexbio.common.query.filter.orientdb;

public class PropertySpecificationODB {
	private String propertyId ;  //This will be the stringified Orientdb rid for a baseTerm.
	private String value;
	
	public PropertySpecificationODB(String propertyId, String value) {
	   this.setPropertyId(propertyId);
	   this.setValue(value);
	}

	public String getPropertyId() {
		return propertyId;
	}

	public void setPropertyId(String propertyId) {
		this.propertyId = propertyId;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	

}
