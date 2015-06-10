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
package org.ndexbio.common.models.dao.orientdb;

/*
 * Represents a set of literals used throughout dao related classes
 */

public interface CommonDAOValues {

	public static final String SEARCH_MATCH_EXACT = "exact-match";
	public static final String SEARCH_MATCH_CONTAINS = "contains";
	public static final String SEARCH_MATCH_STARTS_WITH = "starts-with";
	
	public static final String DUPLICATED_KEY_FLAG = "duplicated key";

	public static final String DUPLICATED_ACCOUNT_FLAG = "A user with that Account Name has already registered.";
	public static final String DUPLICATED_EMAIL_FLAG = "A user with that Email has already registered.";
	
	public static final String ORIENTDB_DAO_TYPE = "orientdb";
}
