package org.ndexbio.common.util;

public class TermUtilities {

	public static TermStringType getTermType(String termString ) {
	   if(termString.length() > 8 && termString.substring(0, 7).equalsIgnoreCase("http://")) 
		   return TermStringType.URI;
	   
	   String[] termStringComponents = termString.split(":");
	   if (termStringComponents != null && termStringComponents.length == 2) 
		   return TermStringType.CURIE;
       return TermStringType.NAME;
	} 
	
}
