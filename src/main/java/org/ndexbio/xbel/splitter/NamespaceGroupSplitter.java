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
