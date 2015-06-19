/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.task.audit.network;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.google.common.base.Objects;

/*
 * Represents a set of attributes used to track the creation and
 * evolution of a NDEx network
 */
public class NetworkProvenanceRecord {
	private final NetworkIdentifier networkId;
	
	private final Timestamp createdTime;
	private final Timestamp lastModifiedTime;
	private final Boolean original;
	private final UUID sourceUUID;
	private final Long validityMetric;
	
	public NetworkProvenanceRecord( NetworkIdentifier anId,
			 Timestamp cDate, Timestamp mDate, Boolean orig, UUID aSource,
			  Long aCount ){
		this.networkId = anId;
		
		this.createdTime = Objects.firstNonNull(cDate, new Timestamp(Calendar.getInstance().getTimeInMillis()));
		this.lastModifiedTime = Objects.firstNonNull(mDate, new Timestamp(Calendar.getInstance().getTimeInMillis()));
		this.original = Objects.firstNonNull(orig, Boolean.FALSE);
		this.sourceUUID = aSource;
		this.validityMetric = aCount;		
	}

	public NetworkIdentifier getNetworkId() {
		return networkId;
	}


	public Date getCreatedDate() {
		return createdTime;
	}

	public Date getLastModifiedDate() {
		return lastModifiedTime;
	}

	public Boolean getOriginal() {
		return original;
	}

	public UUID getSourceUUID() {
		return sourceUUID;
	}

	public Long getEdgeCount() {
		return validityMetric;
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
