package org.ndexbio.xgmml.parser.handler;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleNodeDone extends AbstractHandler {

	@Override
	public ParseState handle(String namespace, String tag, String qName,
			Attributes atts, ParseState current) throws SAXException,
			NdexException, Exception {
		return current;
	}

}
