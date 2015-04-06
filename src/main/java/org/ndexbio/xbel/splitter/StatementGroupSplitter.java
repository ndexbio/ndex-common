package org.ndexbio.xbel.splitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.task.parsingengines.XbelParser;
import org.ndexbio.xbel.model.Annotation;
import org.ndexbio.xbel.model.AnnotationGroup;
import org.ndexbio.xbel.model.Citation;
import org.ndexbio.xbel.model.CitationType;
import org.ndexbio.xbel.model.Parameter;
import org.ndexbio.xbel.model.Relationship;
import org.ndexbio.xbel.model.Statement;
import org.ndexbio.xbel.model.StatementGroup;
import org.ndexbio.xbel.model.Subject;
import org.ndexbio.xbel.model.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class StatementGroupSplitter extends XBelSplitter {
	private static final String xmlElement = "statementGroup";
	
	public static final String nameAttr = "name";
	public static final String commentAttr = "comment";


	private static final Logger logger = LoggerFactory
			.getLogger(StatementGroupSplitter.class);
	
	private NdexPersistenceService networkService;
	
	private int counter;

	public StatementGroupSplitter(JAXBContext jaxbContext,
			NdexPersistenceService networkService) {
		super(jaxbContext, xmlElement);
		this.networkService = networkService;
		counter=0;
	}

	@Override
	protected void process() throws JAXBException, ExecutionException,
			NdexException {
		// instantiate outer level StatementGroup
		StatementGroup sg = (StatementGroup) unmarshallerHandler.getResult();
		this.processStatementGroup(sg);

	}

	private void processStatementGroup(StatementGroup sg)
			throws ExecutionException, NdexException {
		processStatementGroup(sg, null, null, null, null,null);
	}

	// In an XBEL document, only one Citation and one Support are in scope for
	// any Statement
	// Therefore, as we recursively process StatementGroups, if the
	// AnnotationGroup for the inner StatementGroup
	// contains a Citation, it overrides any Citation set in the outer
	// StatementGroup. And the same
	// is true for Supports.
	private void processStatementGroup(StatementGroup sg,
			Long outerSupportId, Long outerCitationId,
			Map<String, String> outerAnnotations, String outerName, String outerComment) throws ExecutionException,
			NdexException {

		// process the Annotation group for this Statement Group
		AnnotationGroup annotationGroup = sg.getAnnotationGroup();
		
		String name = sg.getName(); 
		if ( name == null) name = outerName;
		String comment = sg.getComment(); 
		if ( comment == null) comment = outerComment;

		Map<String, String> annotations = annotationsFromAnnotationGroup(annotationGroup);
		if (null != outerAnnotations){
			// if outerAnnotations is not null, then need to deal with them
			if (null != annotations) {
				for (String key : outerAnnotations.keySet()) {
					if (!annotations.containsKey(key)) {
						// The annotations from the outer group are carried forward
						// unless the inner group overrides them.
						annotations.put(key, outerAnnotations.get(key));
					}
				}
			} else {
				// since annotations was null, just use the outer annotations 
				annotations = outerAnnotations;
			}
		}

		Long citationId = citationFromAnnotationGroup(annotationGroup);
		if (citationId != null) {
			// The AnnotationGroup had a Citation. This overrides the
			// outerCitation.
			// Furthermore, this means that the outerSupport does NOT apply to
			// the inner StatementGroup
			// The Support will either be null or will be specified in the
			// AnnotationGroup
			outerSupportId = null;
			
			//add the name and comment as properties to citation and then clear the context 
			if ( name != null ) {
				this.networkService.addElementProperty(citationId, nameAttr, name, "string");
				name = null;
			}
			if ( comment !=null ) {
				this.networkService.addElementProperty(citationId, commentAttr, comment, "string");
				comment = null;
			}
		} else {
			// There was no Citation in the AnnotationGroup, so use the
			// outerCitation
			citationId = outerCitationId;
		}

		// The ICitation is passed to the supportFromAnnotationGroup method
		// because
		// any ISupport created will be in the context of the ICitation and
		// should be linked to it.
		Long supportId = supportFromAnnotationGroup(annotationGroup, citationId);
		if (supportId == null) {
			// The AnnotationGroup had no Support, therefore use the
			// outerSupport
			supportId = outerSupportId;
		} else {
			//add the name and comment as properties to support and then clear the context 
			if ( name != null ) {
				this.networkService.addElementProperty(supportId, nameAttr, name, "string");
				name = null;
			}
			if ( comment !=null ) {
				this.networkService.addElementProperty(supportId, commentAttr, comment, "string");
				comment = null;
			}
		}

		// process the Statements belonging to this Statement Group
		this.processStatements(sg, supportId, citationId, annotations,name,comment);
		// process any embedded StatementGroup(s)
		for (StatementGroup isg : sg.getStatementGroup()) {
			this.processStatementGroup(isg, supportId, citationId, annotations, name, comment);
		}
	}

	private Map<String, String> annotationsFromAnnotationGroup(
			AnnotationGroup annotationGroup) {
		if (null == annotationGroup)
			return null;
		Map<String,String> annotationMap = new HashMap<>();
		for (Object object : annotationGroup
				.getAnnotationOrEvidenceOrCitation()) {
			if (object instanceof Annotation) {
				Annotation annotation = (Annotation)object; 
	/*			System.out.println("AnnoGroup: " +
						annotation.getRefID() + "=> " + 
						annotation.getValue()); */
				annotationMap.put(annotation.getRefID(), annotation.getValue());
			}
		}
		return annotationMap;
	}

	private Long supportFromAnnotationGroup(
			AnnotationGroup annotationGroup, Long citationId)
			throws ExecutionException {
		if (null == annotationGroup)
			return null;
		for (Object object : annotationGroup
				.getAnnotationOrEvidenceOrCitation()) {
			if (object instanceof String) {
				
				// No explicit type for Evidence, therefore if it is a string,
				// its an Evidence and we find/create an ISupport

				Long supportId = this.networkService.getSupportId((String) object, citationId);
				
				return supportId;
			}
		}
		return null;
	}

	private Long citationFromAnnotationGroup(
			AnnotationGroup annotationGroup) throws NdexException, ExecutionException {
		if (null == annotationGroup)
			return null;
		for (Object object : annotationGroup
				.getAnnotationOrEvidenceOrCitation()) {
			if (object instanceof Citation) {
				
				Citation c = (Citation)object;
				
				CitationType citationType = c.getType();
		//		String idType = c.getType().value();
				if ( citationType == CitationType.PUB_MED) {
					return this.networkService.getCitationId
							(c.getName(), NdexPersistenceService.defaultCitationType, 
									NdexPersistenceService.pmidPrefix+c.getReference(), 
								(c.getAuthorGroup() == null ? null : c.getAuthorGroup().getAuthor())	
						        );
				}
				
				return this.networkService.getCitationId
						(c.getName(), citationType.value(), c.getReference(), 
							(c.getAuthorGroup() == null ? null : c.getAuthorGroup().getAuthor())	
					        );
			}
		}
		return null;
	}

	/*
	 * process statement group
	 */
	private void processStatements(StatementGroup sg, Long supportId,
			Long citationId, Map<String,String> annotations, String name,String comment) 
					throws ExecutionException, NdexException {
		List<Statement> statementList = sg.getStatement();
		for (Statement statement : statementList) {
			processStatement(statement, supportId, citationId, annotations, 0, name, comment);
			counter ++;
			if ( counter %2000 == 0 ) {
				logger.info("processed " + counter + " edges so far. commit this batch.");
				this.networkService.commit();
			}
		}
	}

	private Long processStatement(Statement statement, Long supportId,
			Long citationId, Map<String,String> outerAnnotations, int level, String name, String outerComment) 
					throws ExecutionException, NdexException {
		if (level > 1) throw new NdexException("Attempt to process XBEL nested statement at level greater than 1");

		String comment = statement.getComment();
		if ( comment == null) comment = outerComment;
		
		// process the Annotation group for this Statement
		AnnotationGroup annotationGroup = statement.getAnnotationGroup();

		Map<String, String> annotations = annotationsFromAnnotationGroup(annotationGroup);
		if (null != outerAnnotations){
			// if outerAnnotations is not null, then need to deal with them
			if (null != annotations) {
				for (String key : outerAnnotations.keySet()) {
					if (!annotations.containsKey(key)) {
						// The annotations from the outer group are carried forward
						// unless the inner group overrides them.
						annotations.put(key, outerAnnotations.get(key));
					}
				}
			} else {
				// since annotations was null, just use the outer annotations 
				annotations = outerAnnotations;
			}
		}
		
		Long localCitationId = citationFromAnnotationGroup(annotationGroup);
		if ( localCitationId == null)
			localCitationId = citationId;
		else {
			//add the name and comment as properties to citation and then clear the context 
			if ( name != null ) {
				this.networkService.addElementProperty(citationId, nameAttr, name, "string");
				name = null;
			}
			if ( comment !=null ) {
				this.networkService.addElementProperty(citationId, commentAttr, comment, "string");
				comment = null;
			}
			
		}
		Long localSupportId  = supportFromAnnotationGroup (annotationGroup, localCitationId);
		if ( localSupportId == null)
			localSupportId = supportId;
		else {
			//add the name and comment as properties to citation and then clear the context 
			if ( name != null ) {
				this.networkService.addElementProperty(supportId, nameAttr, name, "string");
				name = null;
			}
			if ( comment !=null ) {
				this.networkService.addElementProperty(supportId, commentAttr, comment, "string");
				comment = null;
			}
			
		}
		
		
		if (null != statement.getSubject()) {

			// All statements are expected to have a subject.
			// It is valid to have a statement with just a subject
			// It creates a node - the biological meaning is "this exists"
			
			Relationship r = statement.getRelationship(); 
			Long subjectNodeId = this.processStatementSubject(statement
					.getSubject(), r==null);
			// A typical statement, however, has a relationship, and object
			// as well as a subject
			// In that case, we can create an edge

			if (null != r) {
				Long predicateId = this.networkService.getBaseTermId(
						XbelParser.belPrefix + ":"+statement.getRelationship().name());

				Long objectNodeId = this.processStatementObject(statement
						.getObject(), localSupportId, localCitationId, annotations, level);

				Long edgeId = this.networkService.createEdge(subjectNodeId, objectNodeId,
						predicateId, localSupportId, localCitationId, annotations);
				
/*				if( name != null ) {
					this.networkService.addElementProperty(edgeId, nameAttr,name,"string");
				} */
				if( comment != null ) {
					this.networkService.addElementProperty(edgeId, commentAttr,comment,"string");
				}
				return edgeId;
			} 
			
/*			if( name != null ) {
				this.networkService.addElementProperty(subjectNodeId, nameAttr,name,"string");
			} */
			if( comment != null ) {
				this.networkService.addElementProperty(subjectNodeId, commentAttr,comment,"string");
			}
			//System.out.println("Handling subject-only statement for node: " + subjectNode.getJdexId() );
			this.networkService.addMetaDataToNode(subjectNodeId, localSupportId, localCitationId, annotations);
			return null;

		} 

		throw new NdexException("No subject for XBEL statement in " + statement.toString());
	}
	

	private Long processStatementSubject(Subject sub, boolean isOrphanNode)
			throws ExecutionException, NdexException {
		if (null == sub) {
			return null;
		}
		try {
			Long representedTermId = this.processFunctionTerm(sub
					.getTerm());
			
			Long subjectNodeId ;
			if ( isOrphanNode ) {
				subjectNodeId =	this.networkService.createNodeFromFunctionTermId(representedTermId);
				
			} else {		
				subjectNodeId =	this.networkService.getNodeIdByFunctionTermId(representedTermId);
			}
			return subjectNodeId;
		} catch (ExecutionException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw (e);
		}

	}

	private Long processStatementObject(org.ndexbio.xbel.model.Object obj, 
			  Long supportId,
			  Long citationId, 
			  Map<String,String> annotations, int level)
			throws ExecutionException, NdexException {
		if (null == obj) {
			//TODO: Is this allowed? throws an exceptions? --cj
			return null;
		}
		try {
			if (null != obj.getStatement()) {
				// Case: object is another statement.
				// 
				// handled by processing the statement in the same context.
				// creates:
				//  1. the edge corresponding to the statement
				//  2. a term of type "reifiedEdgeTerm" which references the edge
				//  3. a object node which the term represents
				//
				Long reifiedEdgeId = this.processStatement(obj.getStatement(), supportId,
						              citationId, annotations, level + 1,null,null);
				Long representedTermId = this.networkService.getReifiedEdgeTermIdFromEdgeId(reifiedEdgeId);
				
				Long objectNodeId = this.networkService.getNodeIdByReifiedEdgeTermId(representedTermId);
						
				return objectNodeId;
			} 
			Long representedTermId = this.processFunctionTerm(obj.getTerm());
			Long objectNodeId = this.networkService.getNodeIdByFunctionTermId(representedTermId);
					
			return objectNodeId;
		} catch (ExecutionException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw (e);
		}
	}

	private Long processFunctionTerm(Term term)
			throws ExecutionException, NdexException {
		// XBEL "Term" corresponds to NDEx FunctionTerm
		
		Long functionId = this.networkService.getBaseTermId(XbelParser.belPrefix +":"+term.getFunction());

		List<Long> argumentList = this.processInnerTerms(term);

		return this.networkService.getFunctionTermId(functionId, argumentList);
	}

	/*
	 * A XBel Term model object implements a quasi-composite pattern in that it
	 * contains a List<Object> that my contain either Term or Pattern model
	 * objects. A XBel Term object is equivalent to a NDEx FunctionTerm object
	 * while a XBel Parameter object is equivalent to a NDEx BaseTerm object.
	 * 
	 * This method works through the parent/child hierarchy of the outermost
	 * Term found in a found in a XBel Subject or (XBel) Object object. It
	 * utilizes the XBelCacheService to distinguish novel from existing
	 * FunctionTerms and BaseTerms. It maintains an identifier for each
	 * FunctionTerm and BaseTerm. For FunctionTerms this is a concatenated
	 * String of the JDex IDs of its children. For BaseTerms, it is a String
	 * containing the namespace and term value.
	 */
	private List<Long> processInnerTerms(Term term) throws ExecutionException,
			NdexException {

		List<Long> argumentList = new ArrayList<Long> ();

		for (Object item : term.getParameterOrTerm()) {
			if (item instanceof Term) {

				Long functionTermId = processFunctionTerm((Term) item);
				argumentList.add(functionTermId);

			} else if (item instanceof Parameter) {
				Parameter parameter = (Parameter)item;
				String termString = 
				 ((parameter.getNs() == null) ? XbelParser.belPrefix : parameter.getNs()) +
				  		":" + parameter.getValue();
				Long baseTermId = this.networkService.getBaseTermId(termString);
				argumentList.add(baseTermId);
			} else {
				Preconditions.checkArgument(true,
						"unknown argument to function term " + item);
			}
		}
		return argumentList;
	}

}
