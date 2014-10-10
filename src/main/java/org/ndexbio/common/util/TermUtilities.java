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
		
		for ( String str : termStringComponents) {
			
				if (!str.matches("^[a-zA-Z_]([0-9a-zA-Z._-])*$"))
					return null;
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
