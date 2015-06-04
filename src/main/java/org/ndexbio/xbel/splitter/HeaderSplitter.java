package org.ndexbio.xbel.splitter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.ndexbio.xbel.model.Header;
import org.xml.sax.helpers.NamespaceSupport;

public class HeaderSplitter extends XBelSplitter {

	private Header header;
	private static final String xmlElement = "header";
	public HeaderSplitter(JAXBContext context) {
		super(context,  xmlElement);
	}

	@Override
	protected void process() throws JAXBException {
		this.header = (Header) unmarshallerHandler
				.getResult();
	}
	
	public Header getHeader() { return this.header;}
	

}
