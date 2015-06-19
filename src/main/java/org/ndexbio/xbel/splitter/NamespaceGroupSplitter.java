/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.xbel.splitter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.xbel.model.Namespace;
import org.ndexbio.xbel.model.NamespaceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceGroupSplitter extends XBelSplitter {

	private static final String xmlElement = "namespaceGroup";
	private static final Logger logger = LoggerFactory
			.getLogger(NamespaceGroupSplitter.class);
	
	private NdexPersistenceService networkService;

	/*
	 * Extension of XBelSplitter to parse NamespaceGroup data from an XBEL
	 * document
	 */
	public NamespaceGroupSplitter(JAXBContext context,
			NdexPersistenceService networkService) {
		super(context,xmlElement);
		this.networkService = networkService;
	}

	@Override
	/*
	 * method to process unmarshaled XBEL namespace elements from XBEL document
	 * responsible for registering novel namespace prefixes in the identifier
	 * cache, for determining the new or existing jdex id for the namespace and
	 * for persisting new namespaces into the orientdb databases
	 * 
	 * @see org.ndexbio.xbel.splitter.XBelSplitter#process()
	 */
	protected void process() throws JAXBException {
		NamespaceGroup ng = (NamespaceGroup) unmarshallerHandler.getResult();
		logger.info("The XBEL document has " + ng.getNamespace().size()
				+ " namespaces");

		for (Namespace ns : ng.getNamespace()) {

			try {
				this.networkService.getNamespace(new RawNamespace(ns.getPrefix(),ns.getResourceLocation()));

			} catch (NdexException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}

		}
		logger.info("done with namespaces");

	}

}
