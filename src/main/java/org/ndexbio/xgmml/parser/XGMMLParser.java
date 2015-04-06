package org.ndexbio.xgmml.parser;

/*
 * #%L
 * Cytoscape IO Impl (io-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.util.Stack;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.xgmml.parser.handler.ReadDataManager;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.uuid.Logger;

public class XGMMLParser extends DefaultHandler {

	private Locator locator;
	//private String currentCData;

	private ParseState parseState;
	private Stack<ParseState> stateStack;

	private final HandlerFactory handlerFactory;

	private final ReadDataManager readDataManager;

	/**
	 * Main constructor for our parser. Initialize any local arrays. Note that
	 * this parser is designed to be as memory efficient as possible. As a
	 * result, a minimum number of local data structures are created.
	 */
	public XGMMLParser(HandlerFactory handlerFactory, ReadDataManager readDataManager) {
		this.handlerFactory = handlerFactory;
		this.readDataManager = readDataManager;
	}

	/********************************************************************
	 * Handler routines. The following routines are called directly from the SAX
	 * parser.
	 *******************************************************************/

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {
		stateStack = new Stack<ParseState>();
		parseState = ParseState.NONE;
		handlerFactory.init();
		super.startDocument();
	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	@Override
	public void endDocument() throws SAXException {
		// The current NDEx data model includes fields for 
		// name and description and also expects a "Format" metadata attribute
		// rather than dc:format
		// This replicates attribute values as necessary.
		readDataManager.handleSpecialNetworkAttributes();
		super.endDocument();
	}
	
	/**
	 * startElement is called whenever the SAX parser sees a start tag. We use
	 * this as the way to fire our state table.
	 * 
	 * @param namespace
	 *            the URL of the namespace (full spec)
	 * @param localName
	 *            the tag itself, stripped of all namespace stuff
	 * @param qName
	 *            the tag with the namespace prefix
	 * @param atts
	 *            the Attributes list from the tag
	 */
	@Override
	public void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException {
		ParseState nextState;
		try {
			//System.out.println("starting state = [" + parseState.toString() + "] tag = " + localName );
/*			  byte[] bs=readDataManager.getRis().getBytes();
	          System.out.print(new String(bs));
	          readDataManager.getRis().resetSink(); */
			nextState = handleStartState(parseState, namespace, localName, qName, atts);
			stateStack.push(parseState);
			parseState = nextState;
		} catch (NdexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * endElement is called whenever the SAX parser sees an end tag. We use this
	 * as the way to fire our state table.
	 * 
	 * @param uri
	 *            the URL of the namespace (full spec)
	 * @param localName
	 *            the tag itself, stripped of all namespace stuff
	 * @param qName
	 *            the tag with the namespace prefix
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			//System.out.println("ending " + localName + " uri = [" + uri + "] qname = " + qName + " ");
			handleEndState(parseState, uri, localName, qName, null);
		} catch (NdexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		parseState = stateStack.pop();
	}
	
	@Override
	public void startPrefixMapping(String prefix, String uri)         throws SAXException  {
		
		try {
			if ( prefix == null || prefix.equals(""))
				prefix = "xmlns";
			this.readDataManager.findOrCreateNamespace(uri, prefix);
		} catch (NdexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			throw new SAXException ("Failed to create namespces. Error from Ndex: " + e.getMessage());
		}
	}

/*	@Override
	public void endPrefixMapping(String prefix)         throws SAXException  {
		
		System.out.println("end: " + prefix );
	} */
	
	/**
	 * characters is called to handle CData
	 * 
	 * @param ch
	 *            the character data
	 * @param start
	 *            the start of the data for this tag
	 * @param length
	 *            the number of bytes for this tag
	 */
	@Override
	public void characters(char[] ch, int start, int length) {
		readDataManager.addCurrentCData(ch,start,length);
		// set a var in readDataManager so this can be found by handlers...
	}

	/**
	 * fatalError -- handle a fatal parsing error
	 * 
	 * @param e
	 *            the exception that generated the error
	 */
	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		String err = "Fatal parsing error on line " + e.getLineNumber() + " -- '" + e.getMessage() + "'";
		throw new SAXException(err);
	}

	/**
	 * error -- handle a parsing error
	 * 
	 * @param e
	 *            the exception that generated the error
	 */
	@Override
	public void error(SAXParseException e) {

	}

	/**
	 * Set the document locator to help us construct our own exceptions
	 * 
	 * @param locator
	 *            the document locator to set
	 */
	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	/********************************************************************
	 * Private parser routines. The following routines are used to manage the
	 * state data.
	 *******************************************************************/

	/**
	 * handleState takes as input a state table, the current state, and the tag.
	 * It then looks for a match in the state table of current state and tag,
	 * then it calls the appropriate handler.
	 * 
	 * @param table
	 *            the state table to use
	 * @param currentState
	 *            the current state
	 * @param tag
	 *            the element tag
	 * @param qName 
	 * @param tag 
	 * @param atts
	 *            the Attributes associated with this tag. These will be passed
	 *            to the handler
	 * @return the new state
	 * @throws Exception 
	 * @throws NdexException 
	 */
	private ParseState handleStartState(ParseState currentState, String namespace, String tag, String qName, Attributes atts) throws NdexException, Exception {
		return handleState(currentState, namespace, tag, qName, atts, handlerFactory.getStartHandler(currentState, tag));
	}

	private ParseState handleEndState(ParseState currentState, String namespace, String tag, String qName, Attributes atts) throws NdexException, Exception {
		return handleState(currentState, namespace, tag, qName, atts, handlerFactory.getEndHandler(currentState, tag));
	}

	private ParseState handleState(ParseState currentState, String namespace, String tag, String qName, Attributes atts, SAXState state)
			throws NdexException, Exception {
		if (state != null) {
			final Handler handler = state.getHandler();

			if (handler != null){
				//System.out.println("sax state = " + state.getTag() + "  handler = " + handler.toString());
				return handler.handle(namespace, tag, qName, atts, state.getEndState());
			}
			return state.getEndState();
		}
		return currentState;
	}
}
