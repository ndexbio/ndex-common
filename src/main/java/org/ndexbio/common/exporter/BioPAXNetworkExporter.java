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
package org.ndexbio.common.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.tools.PropertyHelpers;
import org.xml.sax.SAXException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class BioPAXNetworkExporter {

	private NetworkDocDAO dao;
	private BioPAXFactory bioPAXFactory;
	private BioPAXIOHandler bioPAXIOHandler;
	private Map<Long, BioPAXElement> elementIdToBioPAXElementMap;
	private Map<Long, UnificationXref> termIdToUnificationXrefMap;
	private Map<Long, RelationshipXref> termIdToRelationshipXrefMap;
	
	private Network network;

	public BioPAXNetworkExporter (ODatabaseDocumentTx db) {
		dao = new NetworkDocDAO (db);
		bioPAXFactory = BioPAXLevel.L3.getDefaultFactory();
		bioPAXIOHandler = new SimpleIOHandler();

	}

	public void exportNetwork(UUID networkId, OutputStream output) throws NdexException, ClassCastException {
		
//		System.out.println("Finding network to export by " + networkId);
		
		network = dao.getNetworkById(networkId);
		if (null == network){
			throw new NdexException("No Network found by: " + networkId);
		}
		Model bioPAXModel = bioPAXFactory.createModel();
		elementIdToBioPAXElementMap = new HashMap<>();
		termIdToUnificationXrefMap = new HashMap<>();
		termIdToRelationshipXrefMap = new HashMap<>();
		setUpModel(bioPAXModel);
//		processCitations(bioPAXModel, network);
		processXREFNodes(bioPAXModel);
		processNodes(bioPAXModel);
		processEdges(bioPAXModel);
		bioPAXIOHandler.convertToOWL(bioPAXModel, output);

	}

	private void setUpModel(Model bioPAXModel){
		String xmlBase = PropertyHelpers.getNetworkPropertyValueString(network, "xmlBase");
		if (null == xmlBase){
			xmlBase = "http://www.ndexbio.org/biopax/";
		}
		bioPAXModel.setXmlBase(xmlBase);
	}

/*	
	private void processCitations(Model bioPAXModel, Network network) {
		// Each Citation object becomes a PublicationXref
		// Create the PublicationXref in the model and add it to the elementIdToBioPAXObjectMap
		for (Entry<Long, Citation> entry : network.getCitations().entrySet()){
			Long elementId = entry.getKey();
			Citation citation = entry.getValue();
			String rdfId = PropertyHelpers.getCitationPropertyValueString(network, citation, "rdfId");
//			System.out.println("Citation: " + elementId + " rdfId: " + rdfId);
			PublicationXref px = bioPAXModel.addNew(PublicationXref.class, rdfId);

			String idType = citation.getIdType();
			if (idType.equalsIgnoreCase("unspecified")) idType = null;
			String identifier = citation.getIdentifier();
			if (identifier.equalsIgnoreCase("unspecified")) identifier = null;

			String id = null;
			String db = null;
			if (null !=  idType && idType.equalsIgnoreCase("url")){
				// Special case where the NDEx citation was stored with idType "url"
				// because the pubXref db and id were not both set but there was a url
				// The url will be recovered later from the citation properties.
				// Here, the db or id, if either is available, will be recovered from citation properties
				db = PropertyHelpers.getCitationPropertyValueString(network, citation, "db");
				id = PropertyHelpers.getCitationPropertyValueString(network, citation, "id");
			} else if (null != identifier && null != idType){
				// Standard case where the citation was stored mapping 
				// db -> idType
				// id -> identifier
				id = identifier;
				db = idType;
			} else if (null != identifier){
				// Case where citation was stored only mapping
				// id -> identifier
				id = identifier;
			}

			if (null != db) px.setDb(db);
			if (null != id) px.setId(id);


			//			String xrefTitle = pubXref.getTitle();
			if (null != citation.getTitle()){
				px.setTitle(citation.getTitle());
			}

			//
			// TBD: handle annotations, both for storing in NDEx and re-exporting
			//			Map<String, Object> annotations = pubXref.getAnnotations();
			//	
			// TBD: handle getXrefOf
			//			Set<XReferrable> refersTo = pubXref.getXrefOf();
			//

			//			Set<String> authors = pubXref.getAuthor();
			if (null != citation.getContributors()){
				for (String author : citation.getContributors()){
					px.addAuthor(author);
				}			
			}
			//			Set<String> comments = pubXref.getComment();
			for (String comment : PropertyHelpers.getCitationPropertyValueStrings(network, citation, "comment")){
				px.addComment(comment);
			}
			//			Set<String> sources = pubXref.getSource();	
			for (String source : PropertyHelpers.getCitationPropertyValueStrings(network, citation, "source")){
				px.addSource(source);
			}
			//			String xrefDbVersion = pubXref.getDbVersion();
			String dbVersion = PropertyHelpers.getCitationPropertyValueString(network, citation, "dbVersion");
			if (null != dbVersion) px.setDbVersion(dbVersion);

			//			String xrefIdVersion = pubXref.getIdVersion();
			String idVersion = PropertyHelpers.getCitationPropertyValueString(network, citation, "idVersion");
			if (null != idVersion) px.setDbVersion(idVersion);

			//			int year = pubXref.getYear();
			String yearString = PropertyHelpers.getCitationPropertyValueString(network, citation, "year");
			if (null != yearString){
				// TODO: catch error. 
				int year = Integer.parseInt(yearString);
				px.setYear(year);
			}


			this.elementIdToBioPAXElementMap.put(elementId, px);
		}

	}
*/
	private void processXREFNodes(Model bioPAXModel) throws NdexException {
		for (Entry<Long, Node> e : network.getNodes().entrySet()){
			Node node = e.getValue();
			Long nodeId = e.getKey();

			String bioPAXType = PropertyHelpers.getNodePropertyValueString(network, node, "ndex:bioPAXType");
			if (bioPAXType != null){
				if (bioPAXType.equals("UnificationXref")){
					processUnificationXREFNode(bioPAXModel, node);
				} else if(bioPAXType.equals("RelationshipXref")){
					processRelationshipXREFNode(bioPAXModel, node);
				} else if (bioPAXType.equals("PublicationXref")) {
					processPublicationXrefNode(bioPAXModel,  node);
				}
			}
		}
	}

	private void processUnificationXREFNode(Model bioPAXModel, 	Node node) throws NdexException {
		String rdfId = getNodeRDFID(node);
		UnificationXref bpe = bioPAXModel.addNew(UnificationXref.class, rdfId);
		processProperties(bpe, node.getProperties(), UnificationXref.class);
		this.elementIdToBioPAXElementMap.put(node.getId(), bpe);
//		Long termId = node.getRepresents();
//		this.termIdToUnificationXrefMap.put(termId, bpe);
	}

	private void processRelationshipXREFNode(Model bioPAXModel, Node node) throws NdexException {
		String rdfId = getNodeRDFID( node);
		RelationshipXref bpe = bioPAXModel.addNew(RelationshipXref.class, rdfId);
		processProperties(bpe, node.getProperties(),RelationshipXref.class);
		this.elementIdToBioPAXElementMap.put(node.getId(), bpe);
//		Long termId = node.getRepresents();
//		this.termIdToRelationshipXrefMap.put(termId, bpe);
	}
	
	
	private void processPublicationXrefNode(Model bioPAXModel, Node node) throws NdexException {
		
		String rdfId = getNodeRDFID( node);
		PublicationXref bpe = bioPAXModel.addNew(PublicationXref.class, rdfId);
		processProperties(bpe, node.getProperties(),PublicationXref.class);
		this.elementIdToBioPAXElementMap.put(node.getId(), bpe);
/*		Long termId = node.getRepresents();
//		System.out.println("RelationshipXref: " + termId + " rdfId: " + rdfId);
		this.termIdToRelationshipXrefMap.put(termId, bpe); */
		
	}

	private void processProperties(BioPAXElement bpe,
			List<NdexPropertyValuePair> properties, 
			Class<? extends BioPAXElement> bioPAXClass
			) throws NdexException {
//		System.out.println("Properties for " + bpe.getRDFId());
		EditorMap editorMap = SimpleEditorMap.L3;
		for (NdexPropertyValuePair pvp : properties){
			if ( ! pvp.getPredicateString().equals("ndex:bioPAXType")){
				String propertyString = pvp.getPredicateString();
				String value = pvp.getValue();
				PropertyEditor editor = editorMap.getEditorForProperty(propertyString, bioPAXClass);
				if (editor != null){
//					System.out.println("    Property: " + propertyString + " value: " + value);
					editor.setValueToBean(value, bpe);
				} else {
					throw new NdexException("Can't export property " + propertyString + " to element class " + bioPAXClass);
				}
			}			
		}
	}

/*	
	private String getBioPaxProperty(NdexPropertyValuePair prop) {
		return prop.getPredicateString();
//		return bTerm.getName();
	}
	
*/
	// Process all Nodes that are *not* XREF nodes
	// None of these will have a "represents" property
	private void processNodes(Model bioPAXModel) throws NdexException {
		for (Entry<Long, Node> e : network.getNodes().entrySet()){
			Node node = e.getValue();
			Long nodeId = e.getKey();

			String bioPAXType = PropertyHelpers.getNodePropertyValueString(network, node, "ndex:bioPAXType");
			if (bioPAXType != null){
				if (!bioPAXType.equals("UnificationXref") && !bioPAXType.equals("RelationshipXref")
						&& !bioPAXType.equals("PublicationXref") ){


					Class<? extends BioPAXElement> bioPAXClass;
					try {
						bioPAXClass = (Class<? extends BioPAXElement>) Class.forName("org.biopax.paxtools.model.level3." + bioPAXType);
					} catch (ClassNotFoundException e1) {
						e1.printStackTrace();
						throw new NdexException("Unknown BioPAX class " + bioPAXType);
					}
					BioPAXElement bpe;
					String rdfId = getNodeRDFID(node);
					if (bioPAXClass != null && rdfId != null){
						bpe = bioPAXModel.addNew(bioPAXClass, rdfId);
					} else {
						throw new NdexException("Processing a node to a BioPAX element needs both: bioPAXClass: " + bioPAXClass + " rdfId: " + rdfId);
					}

					EditorMap editorMap = SimpleEditorMap.L3;

					processProperties(bpe, node.getProperties(),bioPAXClass);

					this.elementIdToBioPAXElementMap.put(nodeId, bpe);

				}
			}
		}
	}

	private RelationshipXref getRelationshipXREF(Long termId) {
		return this.termIdToRelationshipXrefMap.get(termId);
	}

	private UnificationXref getUnificationXREF(Long termId) {
		return this.termIdToUnificationXrefMap.get(termId);
	}

	private void processEdges(Model bioPAXModel) throws NdexException {
		EditorMap editorMap = SimpleEditorMap.L3;

		for (Edge edge : network.getEdges().values()){
			Long subjectId = edge.getSubjectId();
			if (subjectId == null || subjectId == 0) throw new NdexException("Malformed BioPAX Edge with subjectId = " + subjectId);
			BioPAXElement subjectBPE = this.elementIdToBioPAXElementMap.get(subjectId);
			if (subjectBPE == null) throw new NdexException("Malformed BioPAX Edge, no BioPAX Element found for subjectId = " + subjectId);

			Long objectId = edge.getObjectId();
			if (objectId == null || objectId == 0) throw new NdexException("Malformed BioPAX Edge with objectId = " + objectId);
			BioPAXElement objectBPE = this.elementIdToBioPAXElementMap.get(objectId);
			if (objectBPE == null) 
				throw new NdexException("Malformed BioPAX Edge, no BioPAX Element found for objectId = " + objectId);

			Long predicateId = edge.getPredicateId();
			BaseTerm predicateTerm = network.getBaseTerms().get(predicateId);
			if (predicateTerm == null) throw new NdexException("Malformed BioPAX Edge, no BaseTerm found for predicateId = " + predicateId);

			String predicate = predicateTerm.getName();
			Class<? extends BioPAXElement> subjectClass = subjectBPE.getModelInterface();
			PropertyEditor editor = editorMap.getEditorForProperty(predicate, subjectClass);
			
			if (null == editor){
				throw new NdexException("Malformed BioPAX Edge, no PropertyEditor found for predicate = " + predicate);
			}
			editor.setValueToBean(objectBPE, subjectBPE);

		}

	}

	
	private String getNodeRDFID( Node node) {
		BaseTerm bTerm = network.getBaseTerms().get(node.getRepresents());
		
		Namespace ns = network.getNamespaces().get(bTerm.getNamespaceId());
		
		if ( ns ==null || ns.getUri() == null)
			return bTerm.getName();
		
		return ns.getUri() + bTerm.getName();
		
	}
	

	public static  void main (String[] args) throws NdexException, ClassCastException {
		ODatabaseDocumentTx db = NdexDatabase.getInstance().getAConnection();

		BioPAXNetworkExporter exporter = new BioPAXNetworkExporter(db);
		/*
		FileOutputStream fo = new FileOutputStream("C:/tmp/galout.xgmml");
		exporter.exportNetwork(UUID.fromString("4b91eadb-5c84-11e4-9ec0-040ccee25000"),
				fo);
		fo.close();
		 */
		exporter.exportNetwork(UUID.fromString("4b91eadb-5c84-11e4-9ec0-040ccee25000"), System.out);

		db.close();

	}






}
