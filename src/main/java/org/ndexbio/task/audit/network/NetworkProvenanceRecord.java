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
