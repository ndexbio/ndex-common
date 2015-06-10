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
