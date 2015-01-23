package org.ndexbio.task.event;

/*
 * A singleton implemented as an enum to provide global access to the 
 * current network name and id being processed.
 */
public enum NdexNetworkState {
	INSTANCE;
	
	private String networkId;
	private String networkName;
	
	public String getNetworkId() {
		return networkId;
	}
	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}
	public String getNetworkName() {
		return networkName;
	}
	public void setNetworkName(String networkName) {
		this.networkName = networkName;
	}
	
	
	
}
