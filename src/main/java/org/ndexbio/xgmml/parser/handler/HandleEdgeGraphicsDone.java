package org.ndexbio.xgmml.parser.handler;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.xgmml.parser.HandlerFactory;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleEdgeGraphicsDone extends AbstractHandler {

	@Override
	public ParseState handle(String namespace, String tag, String qName,
			Attributes atts, ParseState current) throws SAXException,
			NdexException, Exception {
/*
		this.manager.appendCurrentGraphicsString("</");
		this.manager.appendCurrentGraphicsString(qName);
		this.manager.appendCurrentGraphicsString(">\n");
		
		String presentationPropStr = this.manager.getCurrentGraphicsString();
	//	Long edgeId = this.manager.getCurrentEdgeId();
		this.manager.getCurrentXGMMLEdge().getPresentationProps().add(new SimplePropertyValuePair(HandlerFactory.graphics, presentationPropStr));
				
		this.manager.resetCurrentGraphicsString();
*/
		return current;
	}

}
