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
package org.ndexbio.common.util;

public class TermUtilities {

	public static TermStringType getTermType(String termString ) {
	   if(termString.length() > 8 && termString.substring(0, 7).equalsIgnoreCase("http://")) 
		   return TermStringType.URI;
	   
	   String[] termStringComponents = getNdexQName(termString);
	   if (termStringComponents != null && termStringComponents.length == 2) 
		   return TermStringType.CURIE;
       return TermStringType.NAME;
	} 
	
	
	/**
	 * Return the prefix and an identifier based on Ndex QName rule (which extends from QName
	 *  syntax to allow multiple prefixes, but only use the token before the first ":" as the 
	 *  real prefix.  
	 * @param termString
	 * @return If success, this function returns a 2 element array which contains the prefix and identifier.
	 *     If failed, return null;
	 */
	public static String[] getNdexQName (String termString) {
		String[] termStringComponents = termString.split(":");
		if ( termStringComponents.length<2) return null;
		
		for ( int i = 0 ; i < termStringComponents.length; i ++) {
			if ( i == 0 ) {
				if (!termStringComponents[i].matches("^[a-zA-Z_]([0-9a-zA-Z\\s._-])*$"))
					return null;
			} else {
				if (!termStringComponents[i].matches("^[0-9a-zA-Z_].*$"))
					return null;
			}
		}
		String rest ="";
		
		for ( int i = 1 ; i < termStringComponents.length; i++ ) {
			if ( i >1 ) {
				rest += ":";
			}
			rest +=termStringComponents[i];
		}
		return new String [] {termStringComponents[0], rest};
	}

}
