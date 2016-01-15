package org.ndexbio.common.solr;

public class NetworkIndexField {

	private String name;
	private boolean isMultiValued;
	private String type;
	
	public NetworkIndexField(String fieldName, boolean multiValued, String dataType) {
		this.setName(fieldName);
		this.setMultiValued(multiValued);
		this.setType(dataType);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isMultiValued() {
		return isMultiValued;
	}

	public void setMultiValued(boolean isMultiValued) {
		this.isMultiValued = isMultiValued;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	
}
