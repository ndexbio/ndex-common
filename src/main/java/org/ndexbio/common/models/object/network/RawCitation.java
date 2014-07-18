package org.ndexbio.common.models.object.network;

import java.util.List;

import org.ndexbio.common.exceptions.NdexException;

public class RawCitation implements Comparable <RawCitation>{
	
	private String title;
	private List<String> contributors;
	private String idType;   // pubmed or DOI etc...
	private String identifier;
	
	public RawCitation (String title, String idType, String identifier, List<String> contributors) throws NdexException {
		if ( title == null)
			throw new NdexException ("Citation title can't be null.");
		this.title = title;
		this.setContributors(contributors);
		this.setIdType(idType);
		this.setIdentifier(identifier);
	}
	
	 

	@Override
	public int compareTo(RawCitation o) {
		// TODO: check nulls in some cases
		int c = title.compareTo(o.getTitle());
		if ( c!=0) return c;
	
		c =identifier.compareTo(o.getIdentifier());
		if ( c != 0 ) return c;
		
		c = idType.compareTo(o.getIdType());
		return c;
	}


	@Override
	public int hashCode() {
		return title.hashCode();
	}
	
	@Override
	public boolean equals (Object o) {
		if (o instanceof RawCitation)
			return compareTo((RawCitation)o) == 0;
		return false;
	}

	public String getTitle() {
		return title;
	}


/*
	public void setTitle(String title) {
		this.title = title;
	}

*/

	public List<String> getContributors() {
		return contributors;
	}



	public void setContributors(List<String> contributors) {
		this.contributors = contributors;
	}



	public String getIdType() {
		return idType;
	}



	public void setIdType(String idType) {
		this.idType = idType;
	}



	public String getIdentifier() {
		return identifier;
	}



	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

}
