package org.ndexbio.task.audit.network;

import java.net.URI;
import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/*
 * Represents a set of attributes to identify and access a NDEx network
 */
public class NetworkIdentifier {
	private final UUID networkId;
	private final String networkName;
	private final URI networkURI;
	
	public NetworkIdentifier(UUID aUUID, String aName, URI aURI){
		this.networkId = aUUID;
		this.networkName = aName;
		this.networkURI = aURI;
	}
	
	public UUID getNetworkId() {
		return networkId;
	}

	public String getNetworkName() {
		return networkName;
	}

	public URI getNetworkURI() {
		return networkURI;
	}

	@Override
	public String toString() {
	     return ReflectionToStringBuilder.toString(this);
	 }
	
	@Override
	public boolean equals(Object obj) {
		   return EqualsBuilder.reflectionEquals(this, obj);
		 }
	@Override
	public int hashCode() {
		   return HashCodeBuilder.reflectionHashCode(this);
		 }

}
