package org.ndexbio.common.models.dao.orientdb;

/*
 * Represents a set of literals used throughout dao related classes
 */

public interface CommonDAOValues {

	public static final String SEARCH_MATCH_EXACT = "exact-match";
	public static final String SEARCH_MATCH_CONTAINS = "contains";
	public static final String SEARCH_MATCH_STARTS_WITH = "starts-with";
	
	public static final String DUPLICATED_KEY_FLAG = "duplicated key";

	public static final String DUPLICATED_ACCOUNT_FLAG = "duplicated account name";
	public static final String DUPLICATED_EMAIL_FLAG = "duplicated email";
	
	public static final String ORIENTDB_DAO_TYPE = "orientdb";
}
