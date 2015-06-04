package org.ndexbio.xgmml.parser.handler;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.xgmml.parser.HandlerFactory;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleEdgeDone extends AbstractHandler {

	@Override
	public ParseState handle(String namespace, String tag, String qName,
			Attributes atts, ParseState current) throws SAXException,
			NdexException, Exception {
		this.manager.insertCurrentEdge();
		return current;
	}

}
