package org.ndexbio.xgmml.parser.handler;

import java.util.concurrent.ExecutionException;

import org.ndexbio.xgmml.parser.HandlerFactory;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


public class HandleNodeGraphicsDone extends AbstractHandler {

	
	@Override
	public ParseState handle(final String namespace, final String tag, final String qName,  Attributes atts, ParseState current) throws SAXException, ExecutionException {
/*
		this.manager.appendCurrentGraphicsString("</");
		this.manager.appendCurrentGraphicsString(qName);
		this.manager.appendCurrentGraphicsString(">\n");
		
		String presentationPropStr = this.manager.getCurrentGraphicsString();
		Long nodeId = this.manager.getCurrentNodeId();
				this.manager.addGraphicsAttribute(nodeId, HandlerFactory.graphics, presentationPropStr);
				
		this.manager.resetCurrentGraphicsString();
	*/	
		return current;
	}
}
