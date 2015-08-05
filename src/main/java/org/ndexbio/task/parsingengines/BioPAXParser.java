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
package org.ndexbio.task.parsingengines;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipTypeVocabulary;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.UserDocDAO;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.NdexProvenanceEventType;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.model.tools.PropertyHelpers;
import org.ndexbio.model.tools.ProvenanceHelpers;
import org.ndexbio.task.parsingengines.IParsingEngine;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class BioPAXParser implements IParsingEngine {
	private final File bioPAXFile;
	private final String bioPAXURI;
	private List<String> msgBuffer;
	public String networkUUID;
	public int entityCount;
	public int pubXrefCount;
	public int uXrefCount;
	public int rXrefCount;
	public int literalPropertyCount;
	public int referencePropertyCount;

	
	EditorMap editorMap ;

	private static Logger logger = Logger.getLogger("BioPAXParser");
	
	private Map<String, Long> rdfIdToElementIdMap;

	private NdexPersistenceService persistenceService;

    private String description;
    private User loggedInUser;
    
    private String bioPaxPrefix;  // prefix for namespace http://www.biopax.org/release/biopax-level3.owl#

	public BioPAXParser(String fn, String ownerName, NdexDatabase db, String networkName, String description)
			throws Exception {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(fn),
				"A filename is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(ownerName),
				"A network owner name is required");
		this.msgBuffer = Lists.newArrayList();
		if (fn.startsWith("/") || fn.matches("^[a-zA-Z]:.*"))
			this.bioPAXFile = new File(fn);
		else
			this.bioPAXFile = new File(getClass().getClassLoader()
					.getResource(fn).toURI());
		this.bioPAXURI = bioPAXFile.toURI().toString();
		this.persistenceService = new NdexPersistenceService(db);
		this.rdfIdToElementIdMap = new HashMap<>();

        //String title = Files.getNameWithoutExtension(this.bioPAXFile.getName());

		editorMap =  SimpleEditorMap.L3;
		
		persistenceService.createNewNetwork(ownerName, networkName, null);
        this.description = description;

        try (UserDocDAO userDocDAO = new UserDocDAO(db.getAConnection())) {
           loggedInUser = userDocDAO.getUserByAccountName(ownerName);
        } 
	}

	public List<String> getMsgBuffer() {
		return this.msgBuffer;
	}

	public String getBioPAXURI() {
		return bioPAXURI;
	}

	public File getBioPAXFile() {
		return bioPAXFile;
	}

	@Override
	public void parseFile() throws NdexException {
		this.entityCount = 0;
		this.pubXrefCount = 0;
		this.uXrefCount = 0;
		this.rXrefCount = 0;
		this.literalPropertyCount = 0;
		this.referencePropertyCount = 0;
		
		try  {

			this.getMsgBuffer()
					.add("Parsing lines from " + this.getBioPAXURI());

			this.processBioPAX(this.getBioPAXFile());

			// add provenance to network
			NetworkSummary currentNetwork = this.persistenceService
					.getCurrentNetwork();

			// set the source format
			this.persistenceService.setNetworkSourceFormat(NetworkSourceFormat.BIOPAX);

			String uri = NdexDatabase.getURIPrefix();
			this.persistenceService.setNetworkVisibility(VisibilityType.PRIVATE);

			// close database connection
			this.persistenceService.persistNetwork();

            ProvenanceEntity provEntity = ProvenanceHelpers
                    .createProvenanceHistory(currentNetwork, uri, 
                    		NdexProvenanceEventType.FILE_UPLOAD, currentNetwork.getCreationTime(),
                            (ProvenanceEntity) null);
            Helper.populateProvenanceEntity(provEntity, currentNetwork);
            provEntity.getCreationEvent().setEndedAtTime(
                    currentNetwork.getModificationTime());

            List<SimplePropertyValuePair> l = provEntity.getCreationEvent()
                    .getProperties();

            Helper.addUserInfoToProvenanceEventProperties( l, loggedInUser);

            l.add(new SimplePropertyValuePair("filename", this.description));

            this.persistenceService.setNetworkProvenance(provEntity);

            persistenceService.commit();
			
			System.out.println("Network UUID: " + currentNetwork.getExternalId());
			this.networkUUID = currentNetwork.getExternalId().toString();

		} catch (Exception e) {
			// delete network and close the database connection
			e.printStackTrace();
			this.persistenceService.abortTransaction();
			throw new NdexException("Error occurred when loading file "
					+ this.bioPAXFile.getName() + ". " + e.getMessage());
		} 
        finally
        {
            persistenceService.close();
        }
	}

	private void processBioPAX(File f) throws IOException, NdexException, ExecutionException  {
		try ( FileInputStream fin = new FileInputStream(f) ) 
		{
			SimpleIOHandler handler = new SimpleIOHandler(BioPAXLevel.L3);
			handler.mergeDuplicates(false);
			Model model = handler.convertFromOWL(fin);
			
			loadBioPAXModel(model);
		} catch ( FileNotFoundException e) {
			throw new NdexException ("File not found " + f.getAbsolutePath());
		}
	}

	private void loadBioPAXModel(Model model) throws NdexException, ExecutionException  {
		

		String xmlBase = model.getXmlBase();
		NdexPropertyValuePair xmlBaseProp = new NdexPropertyValuePair("xmlBase", xmlBase);
		List<NdexPropertyValuePair> networkProperties = new ArrayList<>();
		networkProperties.add(xmlBaseProp);	
		this.persistenceService.setNetworkProperties(networkProperties, null);
		addBioPAXNamespaces(model);
		
		Set<BioPAXElement> elementSet = model.getObjects();
		//
		// Iterate over all elements to create Node, Citation and BaseTerm
		// objects
		//
		for (BioPAXElement bpe : elementSet) {
			this.entityCount++;
			if (bpe instanceof Xref) {
				// Process Xrefs to create BaseTerm and Citation objects
				this.processXREFElement((Xref)bpe);
			} else {
				// Process all Other Elements to create Node objects
				this.processElementToNode(bpe);
			}
			if ( entityCount % 10000 == 0 ) {
				logger.info("Commiting " + entityCount + " entities in BioPAX loader.");
				this.persistenceService.commit();
			}
		}
		//
		// Iterate over all BioPAX elements to
		// process all Properties in each Element
		// to create NDExPropertyValuePair and Edge objects
		//
		int counter = 0; 
		for (BioPAXElement bpe : elementSet) {
			if (! (bpe instanceof Xref)) {
				// Process all other Elements
				this.processElementProperties(bpe);
				counter ++;
				if ( counter % 5000 == 0 ) {
					logger.info("Commiting " + counter + " properities in BioPAX loader.");
					this.persistenceService.commit();
				}
			}
		}
		
	}

	
	private void processElementToNode(BioPAXElement bpe) throws NdexException, ExecutionException {
		String rdfId = bpe.getRDFId();
//		String className = bpe.getClass().getName();
//		String simpleName = bpe.getModelInterface().getSimpleName();
		//System.out.println("Element To Node: " + rdfId + ": " + simpleName);
		// this.persistenceService.
		// create the node, map the id to the rdfId
		// add a property to the node, setting bp:nodeType to the simpleName
		Long nodeId = this.persistenceService.getNodeIdByBaseTerm(rdfId);

		//Long nodeId = this.persistenceService.getNodeId();
		this.mapRdfIdToElementId(rdfId, nodeId);
	}

	private void processElementProperties(BioPAXElement bpe) throws ExecutionException, NdexException {
		String rdfId = bpe.getRDFId();
		// Get the elementId for the Node corresponding to this rdfId
		Long nodeId = this.persistenceService.getNodeIdByBaseTerm(rdfId);
		
		List<NdexPropertyValuePair> literalProperties = new ArrayList<>();

		String simpleName = bpe.getModelInterface().getSimpleName();
		literalProperties.add(new NdexPropertyValuePair("ndex:bioPAXType", simpleName));
		
//		EditorMap editorMap =  SimpleEditorMap.L2;
		Set<PropertyEditor> editors = editorMap.getEditorsOf(bpe);
		
		String nodeName = null;
		String standardName = null;
		String displayName = null;
		//
		// iterate over the property editors
		//
		List<Long> aliasList = new ArrayList<>();
		List<Long> relateToList = new ArrayList<> ();
		List<Long> citationList = new ArrayList<> (); 
		
		for (PropertyEditor editor : editors) {
			//
			// iterate over the values for each editor:
			//
			// For each property that has a value or values, we want to see if
			// whether each value is a literal or a resource
			//
			// If the value is a Xref resource, handle specially:
			// - link the current Node to a BaseTerm or Citation
			//

			// If the value is a Resource of any other type:
			// - create an Edge from the current Node to the Node for that
			// Resource
			//
			// Else, the value is a literal:
			// - create an NdexPropertyValuePair and add it to the current Node
			// - (note that Edges do not have properties in BioPAX3, only Nodes)
			//
			String propertyName = editor.getProperty();
			Long predicateId = this.persistenceService.getBaseTermId(this.bioPaxPrefix,propertyName);
			
			for (Object val : editor.getValueFromBean(bpe)) {
				// System.out.println("       Property: " + editor.getProperty()
				// + " : (" + val.getClass().getName() + ") " + val.toString());
				if (val instanceof PublicationXref) {
			        Long objectId = this.persistenceService.getNodeIdByBaseTerm(((PublicationXref)val).getRDFId());
					
					this.persistenceService.createEdge(nodeId, objectId, predicateId, null,null,(List<NdexPropertyValuePair>)null);
					
					Long citationId = getElementIdByRdfId(((PublicationXref)val).getRDFId());
					
					citationList.add(citationId);
//					processPublicationXrefProperty(predicateId,(PublicationXref) val, nodeId);
				} else if (val instanceof UnificationXref) {
				     Long objectId = this.persistenceService.getNodeIdByBaseTerm(((UnificationXref)val).getRDFId());
						
					 this.persistenceService.createEdge(nodeId, objectId, predicateId, null,null,(List<NdexPropertyValuePair>)null);
						
					 Long termId = getElementIdByRdfId(((UnificationXref)val).getRDFId());
					 
					 aliasList.add(termId);
					 	
//					processUnificationXrefProperty(predicateId, (UnificationXref) val,nodeId);
				} else if (val instanceof RelationshipXref) {
			        Long objectId = this.persistenceService.getNodeIdByBaseTerm(((RelationshipXref)val).getRDFId());
					
					this.persistenceService.createEdge(nodeId, objectId, predicateId, null,null,(List<NdexPropertyValuePair>)null);
					
					Long termId = getElementIdByRdfId(((RelationshipXref)val).getRDFId());
					
					relateToList.add(termId);
				} else if (val instanceof BioPAXElement){
					// create the edge
					processEdge(predicateId, (BioPAXElement) val, nodeId);
					this.referencePropertyCount++;
				} else if (null != val){
					// queue up a property to be in the set to add
					String valueString = val.toString();

					NdexPropertyValuePair pvp = new NdexPropertyValuePair(propertyName, valueString);
					literalProperties.add(pvp);
					this.literalPropertyCount++;
					
					// populate the node name if possible.
					if ( nodeName ==null && propertyName.equals("name")) {
						nodeName = valueString;
					} 
					if ( propertyName.equals("displayName")) {
						displayName = valueString;
					} else if ( propertyName.equals("standardName")) {
						standardName = valueString;
					}
				}
			}

		}
		
		this.persistenceService.setReferencesOnNode(nodeId, citationList, relateToList, aliasList);
		
		literalProperties.add(new NdexPropertyValuePair("ndex:bioPAXType", bpe.getModelInterface().getSimpleName()));
		this.persistenceService.setNodeProperties(nodeId, literalProperties, null);

		// set the node name if possible.
		if ( displayName != null) { 
			this.persistenceService.setNodeName(nodeId, displayName);
		} else if ( standardName !=null) {
			this.persistenceService.setNodeName(nodeId, standardName);
		} else if (nodeName !=null) {
			this.persistenceService.setNodeName(nodeId, nodeName);
		}
	}

	private void processEdge(
			Long predicateId,
			BioPAXElement bpe, 
			Long subjectNodeId) throws NdexException, ExecutionException {
		Long objectNodeId = getElementIdByRdfId(bpe.getRDFId());
		this.persistenceService.createEdge(subjectNodeId, objectNodeId, predicateId, null, null, (List<NdexPropertyValuePair>)null);		
	}
	

	private void processXREFElement(Xref xref) throws NdexException,
			ExecutionException {
		if (xref instanceof PublicationXref) {
			processPublicationXref((PublicationXref)xref);
		} else if (xref instanceof UnificationXref) {
			processUnificationXref((UnificationXref)xref);
			this.uXrefCount++;
		} else if (xref instanceof RelationshipXref) {
			processRelationshipXref((RelationshipXref)xref);
			this.rXrefCount++;
		} else {
			
			processXref(xref);
			this.rXrefCount++;
			System.out.println("Unexpected xref of type: " + xref.getClass().getSimpleName());
		}
	}

	private void processUnificationXref(UnificationXref xref) throws NdexException, ExecutionException {
		String rdfId = xref.getRDFId();
		// Create a node to hold the mapping of the rdfId to a biopax type
		Long nodeId = this.persistenceService.getNodeIdByBaseTerm(rdfId);

		List<NdexPropertyValuePair> literalProperties = getXRefProperties( xref,true);
		
		String simpleName = xref.getModelInterface().getSimpleName();
		literalProperties.add(new NdexPropertyValuePair("ndex:bioPAXType", simpleName));
			
		this.persistenceService.setNodeProperties(nodeId, literalProperties, null);
	}
	
	private void processRelationshipXref(RelationshipXref xref) throws NdexException, ExecutionException {
		
		String rdfId = xref.getRDFId();
		
		// Create a node to hold the mapping of the rdfId to a biopax type
		Long nodeId = this.persistenceService.getNodeIdByBaseTerm(rdfId);

		List<NdexPropertyValuePair> literalProperties = getXRefProperties( xref,true);
		String simpleName = xref.getModelInterface().getSimpleName();
		literalProperties.add(new NdexPropertyValuePair("ndex:bioPAXType", simpleName));
			
		RelationshipTypeVocabulary vocabObj=xref.getRelationshipType();
		
		if ( vocabObj != null) {   // add an edge to the vocab entity.
			Long vocabNodeId = this.persistenceService.getNodeIdByBaseTerm(vocabObj.getRDFId()); 
			Long predicateId = this.persistenceService.getBaseTermId(this.bioPaxPrefix, "relationshipType");
			this.persistenceService.createEdge(nodeId, vocabNodeId, predicateId, null, null, (List<NdexPropertyValuePair>)null);
		}
		
		this.persistenceService.setNodeProperties(nodeId, literalProperties, null);
		
	}

	
	/**
	 * Get all the properties in the Xref and put them in a property list. This function also create a baseTerm for the object
	 * it referenced, and add the the baseTerm id to the RDFID => elementId mapping table.
	 * @param xref
	 * @return
	 * @throws NdexException
	 * @throws ExecutionException
	 */
	private List<NdexPropertyValuePair> getXRefProperties(Xref xref, boolean createBaseterm) throws NdexException, ExecutionException {
		List<NdexPropertyValuePair> literalProperties = new ArrayList<>();
			
		Set<String> comments = xref.getComment();
		String xrefDb = xref.getDb();
		String xrefDbVersion = xref.getDbVersion();
		String xrefId = xref.getId();
		String xrefIdVersion = xref.getIdVersion();
		
		addBPPropertyToList("db", xrefDb, literalProperties);
		addBPPropertyToList("dbVersion", xrefDbVersion, literalProperties);
		addBPPropertyToList("id", xrefId, literalProperties);
		addBPPropertyToList("idVersion", xrefIdVersion, literalProperties);
      
		if ( comments != null) {
			for (String comment : comments) {
				addBPPropertyToList("comment", comment, literalProperties);
			}
		}

		if ( createBaseterm) {
			Long termId = null;
			if (null != xrefId && null != xrefDb) {
				// We have both an identifier string for a BaseTerm
				// AND a prefix string for a Namespace
				termId = this.persistenceService.getBaseTermId(xrefDb + ":" + xrefId);
			} else if (null != xrefId) {
				// We have an identifier string for a BaseTerm but no Namespace prefix
				termId = this.persistenceService.getBaseTermId(xrefId);
			} else {
				// bad xref with no id!
				throw new NdexException("no id for xref " + xref.getRDFId());
			}

			this.mapRdfIdToElementId(xref.getRDFId(), termId); 
		}
		return literalProperties;
	}

	
	/**
	 * Add property to the given. property list. This function will add the "bp:" prefix to the property name automatically 
	 * if the default bioPAX namespace prefix is "bp:". If vaule is null, no property will be added to the list.
	 * 
	 * @param propertyName
	 * @param value
	 * @param propertyList
	 */
	private static void addBPPropertyToList(String propertyName, String value, List<NdexPropertyValuePair> propertyList) {
		if (value != null) {
			PropertyHelpers.addNdexProperty(/*this.bioPaxPrefix + ":"+*/ propertyName, value, propertyList);
		}
	}
	
	
	private void processXref(Xref xref) throws NdexException, ExecutionException {

		String rdfId = xref.getRDFId();
		// Create a node to hold the mapping of the rdfId to a biopax type
		Long nodeId = this.persistenceService.getNodeIdByBaseTerm(rdfId);

		List<NdexPropertyValuePair> literalProperties = getXRefProperties( xref,true);
		
		String simpleName = xref.getModelInterface().getSimpleName();
		literalProperties.add(new NdexPropertyValuePair("ndex:bioPAXType", simpleName));
			
		this.persistenceService.setNodeProperties(nodeId, literalProperties, null);

	}

	private void processPublicationXref(PublicationXref xref)
			throws NdexException, ExecutionException {

		/*
		 * 
		 * An xref that defines a reference to a publication such as a journal
		 * article, book, web page, or software manual. The reference may or may
		 * not be in a database, although references to PubMed are preferred
		 * when possible. The publication should make a direct reference to the
		 * instance it is attached to.
		 * 
		 * Comment: Publication xrefs should make use of PubMed IDs wherever
		 * possible. The db property of an xref to an entry in PubMed should use
		 * the string "PubMed" and not "MEDLINE". Examples: PubMed:10234245
		 * 
		 * therefore, if both xref.db and xref.id are available,
		 * Citation.identifier = xref.id and Citation.idType = xref.db
		 * 
		 * The following properties may be used when the db and id fields cannot
		 * be used, such as when referencing a publication that is not in
		 * PubMed. The url property should not be used to reference publications
		 * that can be uniquely referenced using a db, id pair.
		 * 
		 * therefore, if xref.url is available, the second choices is:
		 * Citation.identifier = xref.url and Citation.idType = "url"
		 * 
		 * author - The authors of this publication, one per property value.
		 * 
		 *         stored as Citation.contributors
		 * 
		 * title - The title of the publication: stored as Citation.title
		 * 
		 * Store as pv pairs:
		 * 
		 * db (redundant, except in possible case where db is non-null, )
		 * dbVersion 
		 * id (redundant)
		 * idVersion 
		 * source - each source is a string indicating a source in which the reference was
		 * published, such as: a book title, or a journal title and volume and
		 * pages. url - The URL at which the publication can be found, if it is
		 * available through the Web. 
		 * */
		this.pubXrefCount++;
		

		String rdfId = xref.getRDFId();
		Long nodeId = this.persistenceService.getNodeIdByBaseTerm(rdfId);

		List<NdexPropertyValuePair> nodeProperties = getXRefProperties( xref,false);  // properties for node 
		List<NdexPropertyValuePair> citationProperties = new ArrayList<>();           // properties for citation.

		
		for ( NdexPropertyValuePair prop : nodeProperties) {
			if ( !prop.getPredicateString().equals(/*this.bioPaxPrefix+":"+*/"db") &&
					!prop.getPredicateString().equals(/*this.bioPaxPrefix+":"+*/"id")) {
				citationProperties.add(prop);
			}
		}
		
		String simpleName = xref.getModelInterface().getSimpleName();
		nodeProperties.add(new NdexPropertyValuePair("ndex:bioPAXType", simpleName));
		
		Set<String> authors = xref.getAuthor();
		Set<String> sources = xref.getSource();
		String xrefTitle = xref.getTitle();
		Set<String> urls = xref.getUrl();
		int year = xref.getYear();

		for (String source : sources){
			addBPPropertyToList("source", source, citationProperties);
			addBPPropertyToList("source", source, nodeProperties);
		}
		for (String url : urls){
			addBPPropertyToList("url", url, nodeProperties);
		}

		List<String> contributors = new ArrayList<>();
		if (null != authors) {
			for (String author : authors) {
				contributors.add(author);
				addBPPropertyToList("author",author, nodeProperties);
			}
		}
		
		addBPPropertyToList("title", xrefTitle, nodeProperties);
		addBPPropertyToList("year", Integer.toString(year), nodeProperties);
		this.persistenceService.setNodeProperties(nodeId, nodeProperties, null);


		// create extra citation node to help searches in the future.
		String identifier = "unspecified";
		Long citationId = null;
		if ( xref.getDb()!=null && xref.getId()!=null) {
			identifier = xref.getDb() + ":"+ xref.getId();
			citationId = this.persistenceService.getCitationId(xrefTitle,"URI", identifier, contributors);
		} else if ( xref.getId() !=null ) {
			citationId = this.persistenceService.getCitationId(xrefTitle,"Unknown", xref.getId(), contributors);
		} else 
			citationId = this.persistenceService.getCitationId(xrefTitle,"Unknown", identifier, contributors);
		
		this.persistenceService.setCitationProperties(citationId, citationProperties, null);		
		this.mapRdfIdToElementId(rdfId, citationId);
	}

	private void addBioPAXNamespaces(Model model) throws NdexException {
		Map<String,String> prefixMap = model.getNameSpacePrefixMap();
		for (Entry<String, String> pair : prefixMap.entrySet()){
			String prefix = pair.getKey();
			String uri = pair.getValue();
			this.persistenceService.createNamespace2(prefix, uri);
			if ( uri.equals("http://www.biopax.org/release/biopax-level3.owl#"))
				this.bioPaxPrefix = prefix;
		}
	}
	
	private Long getElementIdByRdfId(String rdfId) {
		return this.rdfIdToElementIdMap.get(rdfId);
	}
	
	private void mapRdfIdToElementId(String rdfId, Long elementId) throws NdexException {
		Long previousId = rdfIdToElementIdMap.put(rdfId, elementId);
		if ( previousId != null && !previousId.equals(elementId)){
			throw new NdexException(
					"Attempted to map rdfId = " + rdfId + 
					" to elementId = " + elementId + 
					" but it is already mapped to " + previousId);
		}
	}

	public String getNetworkUUID() {
		return networkUUID;
	}

	public int getEntityCount() {
		return entityCount;
	}

	public int getPubXrefCount() {
		return pubXrefCount;
	}

	public int getuXrefCount() {
		return uXrefCount;
	}

	public int getrXrefCount() {
		return rXrefCount;
	}

	public int getLiteralPropertyCount() {
		return literalPropertyCount;
	}

	public int getReferencePropertyCount() {
		return referencePropertyCount;
	}
	
	@Override
	public UUID getUUIDOfUploadedNetwork() {
		try { 
			return persistenceService.getCurrentNetwork().getExternalId();
		} catch ( Exception e) {
			e.printStackTrace();
			return null;
		}
	}


}
