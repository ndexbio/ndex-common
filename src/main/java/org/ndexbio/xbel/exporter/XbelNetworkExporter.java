package org.ndexbio.xbel.exporter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.PropertiedNetworkElement;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.model.object.network.Term;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.task.audit.NdexAuditService;
import org.ndexbio.task.audit.NdexAuditServiceFactory;
import org.ndexbio.task.audit.NdexAuditUtils;
import org.ndexbio.task.audit.network.NdexObjectAuditor;
import org.ndexbio.task.parsingengines.XbelParser;
import org.ndexbio.task.service.NdexTaskModelService;
import org.ndexbio.xbel.model.AnnotationDefinitionGroup;
import org.ndexbio.xbel.model.AnnotationGroup;
import org.ndexbio.xbel.model.Annotation;
import org.ndexbio.xbel.model.AuthorGroup;
import org.ndexbio.xbel.model.Citation;
import org.ndexbio.xbel.model.Document;
import org.ndexbio.xbel.model.ExternalAnnotationDefinition;
import org.ndexbio.xbel.model.Header;
import org.ndexbio.xbel.model.InternalAnnotationDefinition;
import org.ndexbio.xbel.model.LicenseGroup;
import org.ndexbio.xbel.model.NamespaceGroup;
import org.ndexbio.xbel.model.ObjectFactory;
import org.ndexbio.xbel.model.Relationship;
import org.ndexbio.xbel.model.Statement;
import org.ndexbio.xbel.model.StatementGroup;
import org.ndexbio.xbel.model.Subject;
import org.ndexbio.xbel.model.Function;
import org.ndexbio.xbel.model.Parameter;
import org.ndexbio.xbel.model.CitationType;
import org.ndexbio.xbel.splitter.AnnotationDefinitionGroupSplitter;
import org.ndexbio.xbel.splitter.StatementGroupSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class XbelNetworkExporter {
	private final NdexTaskModelService modelService;
	private final String networkId;
	private final String userId;
	private final Network network;
	private Network subNetwork;
	private XbelStack<org.ndexbio.xbel.model.Term> xbelTermStack;
	
	private XbelMarshaller xm;
	private final ObjectFactory xbelFactory = new ObjectFactory();
	// incorporate operation auditing
	private NdexAuditService auditService;

	private final NdexObjectAuditor<Edge> edgeAuditor = new NdexObjectAuditor<>(
			Edge.class);
	private final NdexObjectAuditor<Node> nodeAuditor = new NdexObjectAuditor<>(
			Node.class);
	private final NdexObjectAuditor<BaseTerm> termAuditor = new NdexObjectAuditor<>(
			BaseTerm.class);
	private final NdexObjectAuditor<ReifiedEdgeTerm> reifiedTermAuditor = new NdexObjectAuditor<>(
			ReifiedEdgeTerm.class);
	private final NdexObjectAuditor<FunctionTerm> functionTermAuditor = new NdexObjectAuditor<>(
			FunctionTerm.class);
	private final NdexObjectAuditor<Support> supportAuditor = new NdexObjectAuditor<>(
			Support.class);

	private static final Logger logger = LoggerFactory
			.getLogger(XbelNetworkExporter.class);

	public XbelNetworkExporter(String userId, String networkId, NdexTaskModelService service,
			String exportFilename) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A userId id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
				"A network id is required");
		Preconditions.checkArgument(null != service,
				"A NdexDataModelService object is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(exportFilename),"A filename is required");
		this.userId = userId;
		this.networkId = networkId;
		this.modelService = service;
		xm = new XbelMarshaller(exportFilename);
		this.network = this.modelService.getNetworkById(this.networkId);

		this.xbelTermStack = new XbelStack<>(
				"XBEL Term", Boolean.FALSE);
		
		this.initiateAuditService(network.getName());
		System.out.println("XBEL network id " +this.networkId 
				+" will be exported to " +exportFilename
				+" for user id " +this.userId
				);

	}
	

	private void initiateAuditService(String networkName) {
		Optional<NdexAuditService> optService = NdexAuditServiceFactory.INSTANCE
				.getAuditServiceByOperation(networkName, NdexAuditUtils.AuditOperation.NETWORK_EXPORT);
		if (optService.isPresent()) {
			this.auditService = optService.get();
			logger.info("NdexAuditServiceFactory returned an instance of "
					+ this.auditService.getClass().getName());
		} else {
			logger.error("NdexAuditServiceFactory failed to return a NdexAuditService subclass");
		}
	}

	private void setAuditExpectedMetrics(Network subNetwork) {
		// edge count

		this.auditService.increaseExpectedMetricValue("edge count",
				(long) subNetwork.getEdgeCount());

		this.auditService.increaseExpectedMetricValue(
				"function term count", (long)subNetwork.getFunctionTerms().size());
		this.auditService.increaseExpectedMetricValue(
				"base term count", (long)subNetwork.getBaseTerms().size());
		this.auditService.increaseExpectedMetricValue(
				"reified edge count", (long)subNetwork.getReifiedEdgeTerms().size());

		this.auditService.increaseExpectedMetricValue("citation count",
				(long) subNetwork.getCitations().size());
		this.auditService.increaseExpectedMetricValue("node count",
				(long) subNetwork.getNodeCount());
		this.auditService.increaseExpectedMetricValue("support count",
				(long) subNetwork.getSupports().size());
		logger.info("Expected values set in Audit Service");

	}

	/*
	 * public method to initiate export of the specified network uses an
	 * instance of an inner class to control marshalling of JAXB objects to an
	 * XML file
	 */

	public void exportNetwork() throws NdexException{

		
		xm.open();

		// export the header
		this.createHeader();
		// export the namespaces
		Iterable<Namespace> namespaces = this.modelService
				.getNamespacesByNetworkId(networkId);
		this.addNamespaceGroup(namespaces);
		// process the annotation definition group
				this.processAnnotationDefinitionGroup();
		/*
		 * process the network in segments to manage memory requirements each
		 * citation within the network is treated as a subnetwork and represnts
		 * an outer level statement group
		 */
		this.processCitationSubnetworks();
		
		// output the observed metrics
		this.auditService.registerComment(this.edgeAuditor
				.displayUnprocessedNdexObjects());
		this.auditService.registerComment(this.nodeAuditor
				.displayUnprocessedNdexObjects());
		this.auditService.registerComment(this.termAuditor
				.displayUnprocessedNdexObjects());
		this.auditService.registerComment(this.supportAuditor
				.displayUnprocessedNdexObjects());
		System.out.println(this.auditService.displayObservedValues());
		System.out.println(this.auditService.displayExpectedValues());
		System.out.println(this.auditService.displayDeltaValues());
		// close the XML document
//		System.out.println("Closing export file.");
		xm.close();
//		System.out.println("Export finished.");
	}
	
	/*
	 * private method to map internal and external annotations to the document's
	 * AnnotationDefinitionGroup
	 * The entity is marshalled to to document by a dedicated method
	 */
	private void processAnnotationDefinitionGroup() {
		AnnotationDefinitionGroup adg = this.xbelFactory.createAnnotationDefinitionGroup();
		
		processInternalAnnotations(adg);
		processExternalAnnotations(adg);
		try {
			xm.writeAnnotationDefinitionGroup(adg);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * 		private method to add external annotation definitions to the annotation definition group
	 */
	private void processExternalAnnotations(AnnotationDefinitionGroup adg) {
		for (Namespace ns : this.modelService.getExternalAnnotationsByNetworkId(networkId)){
			ExternalAnnotationDefinition ead = this.xbelFactory.createExternalAnnotationDefinition();
			ead.setId(ns.getPrefix());
			ead.setUrl(ns.getUri());
			adg.getExternalAnnotationDefinition().add(ead);
		}
	}

	/*
	 * 		private method to add internal annotation definitions to the annotation definition group
	 */
	private void processInternalAnnotations(AnnotationDefinitionGroup adg) {
		for (Namespace ns : this.modelService.getInternalAnnotationsByNetworkId(networkId)){
			InternalAnnotationDefinition iad = this.xbelFactory.createInternalAnnotationDefinition();
			adg.getInternalAnnotationDefinition().add(iad);
			iad.setId(ns.getPrefix());
			String desc = ns.getPropertyAsString(AnnotationDefinitionGroupSplitter.desc);
			if ( desc != null ) { 
  			    iad.setDescription(desc);
			    iad.setUsage(desc);
			}
			String annotationPattern = ns.getPropertyAsString(AnnotationDefinitionGroupSplitter.patternAnnotation);
			if ( annotationPattern != null ) { 
  			    iad.setPatternAnnotation(annotationPattern);
			}
			List<String> listAnno = new LinkedList<>();
			
			for ( NdexPropertyValuePair p : ns.getProperties()) {
				if (p.getPredicateString().equals(AnnotationDefinitionGroupSplitter.list_annotation)) {
					listAnno.add(p.getValue());
				}
			}
			
			if ( !listAnno.isEmpty() ) {
				iad.setListAnnotation(this.xbelFactory.createListAnnotation());
				for ( String s : listAnno)
					iad.getListAnnotation().getListValue().add(s);
			}	
		}
	}

	/*
	 * Each citation within the network is used as a marker for a new
	 * outer-level statement group. A subnetwork is obtained from the database
	 * for the NDEx objects that belong to that Citation
	 */
	private void processCitationSubnetworks() throws NdexException{
		Collection<org.ndexbio.model.object.network.Citation> modelCitations = this
				.getCitationsByNetworkId();

	  try {	
		for (org.ndexbio.model.object.network.Citation citation : modelCitations) {

			this.subNetwork = this.modelService.getSubnetworkByCitationId(
					this.networkId, citation.getId());
			if (null == subNetwork) {
				continue;
			}
			
			System.out.println(" Citation " + citation.getTitle()
					+ " has a subnetwork with  " + subNetwork.getEdgeCount()
					+ " edges and " + subNetwork.getCitations().size()
					+ " citations");
			// register expected values in audit service
			this.setAuditExpectedMetrics(subNetwork);
			// add the edges to an audited Set
			this.edgeAuditor.registerJdexIds(subNetwork.getEdges());
			this.nodeAuditor.registerJdexIds(subNetwork.getNodes());
			this.termAuditor.registerJdexIds(subNetwork.getBaseTerms());
			this.functionTermAuditor.registerJdexIds(subNetwork.getFunctionTerms());
			this.reifiedTermAuditor.registerJdexIds(subNetwork.getReifiedEdgeTerms());
			this.supportAuditor.registerJdexIds(subNetwork.getSupports());
			StatementGroup sg = this.processCitationStatementGroup(citation);
	//		this.processCitationSupports(citation);

			xm.writeStatementGroup(sg);

		}
		
		// process orphan supports
		this.subNetwork = this.modelService.getOrphanSupportNetwork(this.networkId);
		StatementGroup stmtGrp = this.processOrphanSupportsStatementGroup();
		xm.writeStatementGroup(stmtGrp);
		
		
		// process the remainder ( statements that are not under any citation)
		this.subNetwork = this.modelService.getNoCitationSubnetwork(this.networkId);
		if ( this.subNetwork.getNodeCount()>0 && this.subNetwork.getEdgeCount()>0 ) {
			StatementGroup sg = this.processUnCitedStatementGroup();
	  	    xm.writeStatementGroup(sg);
		}
	  } catch (JAXBException e) {
			e.printStackTrace();
			String msg = "Error occured when writing statement group: " + e.getMessage();
			logger.error(msg);
			throw new NdexException(msg);
	  }	
		
	}

	/*
	 * Initiate a new outer level statement group and create the outer-level
	 * annotation group to hold the citation data
	 */

	private StatementGroup processCitationStatementGroup(org.ndexbio.model.object.network.Citation modelCitation) {
		// clear the statement group stack
		StatementGroup sg = new StatementGroup();
		AnnotationGroup ag = new AnnotationGroup();

		processNameNCommentOnStatementGroup(sg, modelCitation);
		
		
 		this.createXbelCitation(ag, modelCitation);
		sg.setAnnotationGroup(ag);
		
		// Setup tracker so that we can tell which nodes are orphan nodes
		TreeSet<Long> processedNodeIds = new TreeSet<>();
		
		// a collection of edge ids that referenced by ReifiedEdgesTerm
		TreeSet<Long> reifiedEdgeIds = new TreeSet<>();
		for ( ReifiedEdgeTerm rt: this.subNetwork.getReifiedEdgeTerms().values()) {
			reifiedEdgeIds.add(rt.getEdgeId());
		}
		
		for ( Edge e : this.subNetwork.getEdges().values()) {
			if(  e.getSupportIds().size() == 0 && (! reifiedEdgeIds.contains(e.getId())) ) {
				this.processSupportEdge(sg, e, processedNodeIds);
				this.edgeAuditor.removeProcessedNdexObject(e);
			}
		}
		
		processCitationSupports(sg, modelCitation, reifiedEdgeIds,processedNodeIds);
		
		// process orphan nodes in this Citation
		for (Map.Entry<Long, Node> entry : this.subNetwork.getNodes()
					.entrySet()) {
			Node node = entry.getValue();
			if (node.getCitationIds().contains(modelCitation.getId()) && !processedNodeIds.contains(entry.getKey())) {
					// we've identified a node that belongs to this support
				this.processSupportNode(sg, node);
					
				this.nodeAuditor.removeProcessedNdexObject(node);
			}
		}

		// increment the audit citation count
		this.auditService.incrementObservedMetricValue("citation count");
		
		return sg;
	}

	
	
	private StatementGroup processOrphanSupportsStatementGroup() {
		// clear the statement group stack
		StatementGroup sg = new StatementGroup();
		AnnotationGroup ag = new AnnotationGroup();
	//	sg.setName(this.createXbelCitation(ag, modelCitation));
		sg.setAnnotationGroup(ag);
		
		// Setup tracker so that we can tell which nodes are orphan nodes
		TreeSet<Long> processedNodeIds = new TreeSet<>();
		// a collection of edge ids that referenced by ReifiedEdgesTerm

		TreeSet<Long> reifiedEdgeIds = new TreeSet<>();
		for ( ReifiedEdgeTerm rt: this.subNetwork.getReifiedEdgeTerms().values()) {
			reifiedEdgeIds.add(rt.getEdgeId());
		}
		
		for ( Support support : this.subNetwork.getSupports().values() ) {
		   StatementGroup isg = new StatementGroup();
		   AnnotationGroup iag = new AnnotationGroup();
		   iag.getAnnotationOrEvidenceOrCitation().add( support.getText());

		   processNameNCommentOnStatementGroup(isg, support);

		   // increment audit support count
		   this.auditService.incrementObservedMetricValue("support count");
			
		   // add support annotations
		   this.processSupportAnnotations(iag, support);

		   isg.setAnnotationGroup(iag);
		   sg.getStatementGroup().add(isg);
		   this.auditService.incrementObservedMetricValue("support count");
		   this.supportAuditor.removeProcessedNdexObject(support);
		   
		   processSupportStatementGroup(isg, support.getId(), reifiedEdgeIds, processedNodeIds);
		}
		
		return sg;
	}
	
	private static void processNameNCommentOnStatementGroup(StatementGroup sg, PropertiedNetworkElement element) {
		   String name = element.getPropertyAsString(StatementGroupSplitter.nameAttr);
		   if ( name != null) 
			   sg.setName(name);
		   
		   String comment = element.getPropertyAsString(StatementGroupSplitter.commentAttr);
		   if ( comment != null) 
			   sg.setComment(comment);
	}
	
	
	private StatementGroup processUnCitedStatementGroup() {
		// clear the statement group stack
//		StatementGroup sg = new StatementGroup();
//		AnnotationGroup ag = new AnnotationGroup();
//		sg.setName(this.createXbelCitation(ag, modelCitation));
//		sg.setAnnotationGroup(ag);
		
		// a collection of edge ids that referenced by ReifiedEdgesTerm
		TreeSet<Long> reifiedEdgeIds = new TreeSet<>();
		for ( ReifiedEdgeTerm rt: this.subNetwork.getReifiedEdgeTerms().values()) {
			reifiedEdgeIds.add(rt.getEdgeId());
		}
		
		return processUncitedStatements(reifiedEdgeIds);
		// increment the audit citation count
//		this.auditService.incrementObservedMetricValue("citation count");
		
//		return sg;
	}
	
	/*
	 * Process the supports for a given citation Each support represents an
	 * inner level statement group and contains a collection of edges
	 */
	private void processCitationSupports(StatementGroup outerSG,
			org.ndexbio.model.object.network.Citation modelCitation, Set<Long> reifiedEdgeIds, Set<Long> processedNodeIds) {
		
		for ( Support support : this.subNetwork.getSupports().values()) {
			if ( support.getCitationId() == modelCitation.getId()) {
				StatementGroup supportStatementGroup = new StatementGroup();
				AnnotationGroup ag = new AnnotationGroup();
				String evidence = Objects.firstNonNull(support.getText(), " ");
				ag.getAnnotationOrEvidenceOrCitation().add(evidence);
				// increment audit support count
				this.auditService.incrementObservedMetricValue("support count");
				// add support annotations
				this.processSupportAnnotations(ag, support);
				supportStatementGroup.setAnnotationGroup(ag);
				outerSG.getStatementGroup().add(supportStatementGroup);
//				sgStack.peek().getStatementGroup().add(supportStatementGroup);
				// increment audit support count
				this.auditService.incrementObservedMetricValue("support count");
				this.supportAuditor.removeProcessedNdexObject(support);

				this.processSupportStatementGroup(supportStatementGroup, support.getId(), reifiedEdgeIds, processedNodeIds);
			}
		}

	}

	private StatementGroup processUncitedStatements( Set<Long> reifiedEdgeIds) {
		
		StatementGroup statementGroup = new StatementGroup();
		AnnotationGroup ag = new AnnotationGroup();
		
		// increment audit support count
		statementGroup.setAnnotationGroup(ag);

//		processUnCitedStatementGroupInner(supportStatementGroup, reifiedEdgeIds);
				
		TreeSet<Long> processedNodes = new TreeSet<>();
				
		//edges
		for (Map.Entry<Long, Edge> entry : this.subNetwork.getEdges()
						.entrySet()) {
			Edge edge = entry.getValue();
			if ( (!reifiedEdgeIds.contains(edge.getId()))) {
				// we've identified an Edge that belongs to this support
				this.processSupportEdge(statementGroup, edge,processedNodes);
				this.edgeAuditor.removeProcessedNdexObject(edge);
			}
		}

		// process orphan nodes
		for (Map.Entry<Long, Node> entry : this.subNetwork.getNodes()
						.entrySet()) {
			Node node = entry.getValue();
				if ( !processedNodes.contains(entry.getKey())) {
						// we've identified a node that belongs to this support
					this.processSupportNode(statementGroup, node);
						
					this.nodeAuditor.removeProcessedNdexObject(node);
				}
			}
		
		return statementGroup;

	}
	
	
	private void processSupportAnnotations(AnnotationGroup ag, Support support) {
		//TODO: commented out by cj. need to review it.
		// because we dont have annotations on support now. so we commented this function out. --cj
		/*
		Annotation annotation = new Annotation();
		annotation.setRefID("evidence");
		annotation.setValue(support.getText());
		System.out.println("Support Annotation " + support.getText());
		ag.getAnnotationOrEvidenceOrCitation().add(annotation);
		 */
	}

	/*
	 * The collection of nodes and edges which reference the same support (i.e. evidence)
	 * represent an inner level statement group wrt to the outer level citation
	 * statement group
	 */
	private void processSupportStatementGroup(StatementGroup sg, Long supportId, Set<Long> reifiedEdgeIds, Set<Long> processedNodeIds) {

		//edges
		for (Map.Entry<Long, Edge> entry : this.subNetwork.getEdges()
				.entrySet()) {
			Edge edge = entry.getValue();
			if ( (!reifiedEdgeIds.contains(edge.getId())) && edge.getSupportIds().contains(supportId)) {
				// we've identified an Edge that belongs to this support
				this.processSupportEdge(sg, edge, processedNodeIds);
				this.edgeAuditor.removeProcessedNdexObject(edge);
			}
		}

		// process orphan nodes
		for (Map.Entry<Long, Node> entry : this.subNetwork.getNodes()
				.entrySet()) {
			Node node = entry.getValue();
			if (node.getSupportIds().contains(supportId) && !processedNodeIds.contains(entry.getKey())) {
				// we've identified a node that belongs to this support
				this.processSupportNode(sg, node);
				processedNodeIds.add(node.getId());
				this.nodeAuditor.removeProcessedNdexObject(node);
			}
		}

	}


	
	/*
	 * An NDEx Edge object is equivalent to an XBEL Statement object we need to
	 * construct a new Statement, and complete its Subject, Predicate, and
	 * Object properties by transversing the Terms associated with the Edge
	 * Since we are starting processing for a new Support we can clear the
	 * Statement stack
	 */
	private void processSupportEdge(StatementGroup sg, Edge edge, Set<Long> processedNodeIds) {

		StatementGroup outerGrp = sg;
/*		String name = edge.getPropertyAsString(StatementGroupSplitter.nameAttr);
		if ( name !=null)  {
		  StatementGroup innerGrp = new StatementGroup();	
		  innerGrp.setName(name);	
		  outerGrp = innerGrp;
		} */
		
		Statement stmt = new Statement();
		outerGrp.getStatement().add(stmt);
		
		this.processStatement(stmt, edge);
		processedNodeIds.add(edge.getObjectId());
		processedNodeIds.add(edge.getSubjectId());
	}

	private void processSupportNode(StatementGroup sg, Node node) {
		// we're at the outer level so clear the Statement stack
//		this.stmtStack.clear();
		Statement stmt = new Statement();
		sg.getStatement().add(stmt);
//		this.sgStack.peek().getStatement().add(stmt);
		
		this.processNodeStatement(stmt, node);
	}

	private void processNodeStatement(Statement stmt, Node node) {

		// increment the audit edge count
		this.auditService.incrementObservedMetricValue("node count");
	
		this.processStatementAnnotations(stmt, node);

		this.processTermSubject(stmt, node.getId());
	}
	
	private void processStatement(Statement stmt, Edge edge) {

		// increment the audit edge count
		this.auditService.incrementObservedMetricValue("edge count");
		// put this new Statement into the Statement stack
//		this.stmtStack.push(stmt);
		// process statement annotations
		
		
        String comment = edge.getPropertyAsString(StatementGroupSplitter.commentAttr);
		if ( comment !=null)
			stmt.setComment(comment);
        
		processStatementAnnotations(stmt, edge);
		
		this.processTermPredicate(stmt, edge.getPredicateId());

		this.processTermSubject(stmt, edge.getSubjectId());
		this.processTermObject(stmt, edge.getObjectId());
		// we're done with the current Statement remove it from the stack
//		this.stmtStack.pop();
	}

	/*
	 * private method to map NDEx Edge metadata to an XBEL AnnotationGroup
	 */
	private static void processStatementAnnotations(Statement stmt, 
			PropertiedNetworkElement edge) {
    
		if (null == edge.getProperties() || edge.getProperties().isEmpty()) {
			return;
		}
		AnnotationGroup ag = new AnnotationGroup();
		stmt.setAnnotationGroup(ag);
		for (NdexPropertyValuePair entry : edge.getProperties()) {
			String refid = entry.getPredicateString();
		    if ( !refid.equals(StatementGroupSplitter.commentAttr) && 
		    		!refid.equals(StatementGroupSplitter.nameAttr) ) {	
		    	String value = entry.getValue();
		    	Annotation annotation = new Annotation();
		    	annotation.setRefID(refid);
		    	annotation.setValue(value);
		    	ag.getAnnotationOrEvidenceOrCitation().add(annotation);
		    }
		} 
	}

	/*
	 * An NDEx Edge subject maps to an XBEL subject element with 1 or more
	 * terms. The terms may have nested terms as well as parameters. The input
	 * parameter is the JdexId for the top level object for an edge
	 */
	private void processTermSubject(Statement stmt, Long edgeSubjectId) {
		Node node = this.subNetwork.getNodes().get(edgeSubjectId);
		Subject subject = new Subject();
		if (null != node && node.getRepresents() != null) {
			// increment audit node count
			this.auditService.incrementObservedMetricValue("node count");
			// get initial function term
			Term term = this.getSubNetworkTerm(node.getRepresents());
			if (null != term && term instanceof FunctionTerm) {
				// clear the term statck
				this.xbelTermStack.clear();
				subject.setTerm(this.processFunctionTerm((FunctionTerm) term));
			}
			stmt.setSubject(subject);
			this.nodeAuditor.removeProcessedNdexObject(node);
		}
	}

	private Term getSubNetworkTerm(Long termId) {
		Term t = this.subNetwork.getBaseTerms().get(termId); 
		if (t != null ) return t;
		t = this.subNetwork.getFunctionTerms().get(termId);
		if ( t !=null ) return t;
		t = this.subNetwork.getReifiedEdgeTerms().get(termId);
		return t;
	}
	
	/*
	 * An NDEx Edge maps to an XBEL object element with 1 or more terms. These
	 * terms may have nested terms as well as parameters The input parameter is
	 * the JdexId for the top level object for an edge
	 * 
	 * mod 14Mar2014 - add support for inner statements: ReifiedEdgeTerm
	 */
	private void processTermObject(Statement outerStmt, Long edgeObjectId) {
		Node node = this.subNetwork.getNodes().get(edgeObjectId);
		org.ndexbio.xbel.model.Object object = new org.ndexbio.xbel.model.Object();
		if (null != node && node.getRepresents() != null) {
			// get initial function term
			Term term = this.getSubNetworkTerm(node.getRepresents());
			if ( null != term && term instanceof ReifiedEdgeTerm){
				// this represents an inner statement - add to Object
				ReifiedEdgeTerm rt = (ReifiedEdgeTerm) term;
				Edge innerEdge = this.network.getEdges().
						get(rt.getEdgeId());
				if(null != innerEdge){
					Statement stmt = new Statement();
					object.setStatement(stmt);
					this.processStatement(stmt, innerEdge);
				} else {
					System.out.println("ReifiedEdgeTerm edge " +rt.getEdgeId()
							+" not found in subnetwork edges");
				}
			}
			else if (null != term && term instanceof FunctionTerm) {
				// clear the term statck
				this.xbelTermStack.clear();
				object.setTerm(this.processFunctionTerm((FunctionTerm) term));
			}
			outerStmt.setObject(object);
			this.nodeAuditor.removeProcessedNdexObject(node);
		}
	}

	/*
	 * private method to map a hierarchy of NDEx function term model objects to
	 * an equivalent hierarchy of XBEL term objects this method can be invoked
	 * recursively
	 */
	private org.ndexbio.xbel.model.Term processFunctionTerm(FunctionTerm ft) {
		org.ndexbio.xbel.model.Term xbelTerm = new org.ndexbio.xbel.model.Term();

		// push new term onto the stack as the current term being constructed
		this.xbelTermStack.push(xbelTerm);
		// set the function attribute for the current term
		BaseTerm bt = this.subNetwork.getBaseTerms().get(
				ft.getFunctionTermId());
		this.xbelTermStack.peek().setFunction(Function.valueOf(bt.getName()));
		// increment audit base term count
		this.auditService.incrementObservedMetricValue("base term count");
		// remove base term from list of unprocess terms
		this.termAuditor.removeProcessedNdexObject(bt);
		// now process any parameters (either base terms or inner function
		// terms)
		// the function terms parameters map to XBEL parameter elements
		//
		
		// the parameter map key now represents an ordering value
		// process map sorted by keys
		for (Long tId : ft.getParameterIds()) {
			Term parameter = this.getSubNetworkTerm(tId);
			if (parameter instanceof FunctionTerm) {
				// register the generated XBEL term in the hierarchy
				this.xbelTermStack
						.peek()
						.getParameterOrTerm()
						.add(this.processFunctionTerm((FunctionTerm) parameter));
				// increment audit function term count
				this.auditService
						.incrementObservedMetricValue("function term count");
				// remove function term from unprocessed Term collection
				this.functionTermAuditor.removeProcessedNdexObject(ft);
			} else if (parameter instanceof BaseTerm) {
				BaseTerm parameterBt = (BaseTerm) parameter;
				Namespace ns = this.subNetwork.getNamespaces().get(
						parameterBt.getNamespaceId());
				Parameter xbelParameter = new Parameter();
				/*
				 * don't export the parameter namespace if it is BEL
				 */
			//	if ( ns == null)
			//	   System.out.println("Namespace is null");
				if ( ns != null && ns.getId() >0 && !ns.getPrefix().equals(XbelParser.belPrefix)) {
					// this.xbelTermStack.peek().getParameterOrTerm().add(xbelParameter);
					xbelParameter.setNs(ns.getPrefix());
				}
				xbelParameter.setValue(parameterBt.getName());
				this.xbelTermStack.peek().getParameterOrTerm()
				.add(xbelParameter);
				this.auditService.incrementObservedMetricValue("base term count");
			}

		} // end of parameter processing
			
		return this.xbelTermStack.pop();
	}

	/*
	 * An NDEx Edge predicate maps to an XBEL Statement relationship attribute
	 */
	private void processTermPredicate(Statement stmt, Long jdexId) {
		BaseTerm bt = this.subNetwork.getBaseTerms().get(jdexId);
		if (null != bt) {
			// add the base term to the current Statement as a relationship
			// attribute
			stmt.setRelationship(
					Relationship.valueOf(bt.getName()));
			// increment the audit base term count
			this.auditService.incrementObservedMetricValue("base term count");
		}

	}

	private String createXbelCitation(AnnotationGroup annotGroup,
			org.ndexbio.model.object.network.Citation modelCitation) {
		Citation xbelCitation = new Citation();

		xbelCitation.setName(modelCitation.getType());
		String idString = modelCitation.getIdentifier();
		if ( idString.startsWith(NdexPersistenceService.pmidPrefix))
			xbelCitation.setReference(idString.substring(5));
		else
			xbelCitation.setReference(idString);
		xbelCitation.setName(modelCitation.getTitle());

		if ( modelCitation.getIdType().equals("URI") && 
					modelCitation.getIdentifier().startsWith(NdexPersistenceService.pmidPrefix)) {
			xbelCitation.setType(CitationType.PUB_MED);
		} else {
			xbelCitation.setType(CitationType.fromValue(modelCitation.getIdType()));
		}
		if (null != modelCitation.getContributors() && !modelCitation.getContributors().isEmpty()) {
			org.ndexbio.xbel.model.Citation.AuthorGroup authors = new org.ndexbio.xbel.model.Citation.AuthorGroup();
			for (String contributor : modelCitation.getContributors()) {
				authors.getAuthor().add(contributor);
			}
			xbelCitation.setAuthorGroup(authors);
		}

		annotGroup.getAnnotationOrEvidenceOrCitation().add(xbelCitation);

		return xbelCitation.getType().value() + " " + xbelCitation.getReference();
	}

	/*
	 * Create the bel document header section
	 */
	private void createHeader() {
		// Document document = new Document();
		Header header = new Header();
		header.setName(this.network.getName());
		String description = Objects.firstNonNull(
				this.network.getDescription(), "XBEL network");
		header.setDescription(description);
		header.setVersion(this.network.getVersion() == null? "N/A" : this.network.getVersion());
		
		AuthorGroup ag = new AuthorGroup();
		LicenseGroup lg = new LicenseGroup();
		
		for (NdexPropertyValuePair p : this.network.getProperties()) {
			String predicateStr = p.getPredicateString();
			if ( predicateStr.equals(XbelParser.elementAuthor)) {
				ag.getAuthor().add(p.getValue());
			} else if ( predicateStr.equals(XbelParser.elementLicense)) {
				lg.getLicense().add(p.getValue());
			} else if ( predicateStr.equals(XbelParser.elementContactInfo)) {
				header.setContactInfo(p.getValue());
			} else if ( predicateStr.equals(XbelParser.elementCopyright)) {
				header.setCopyright(p.getValue());
			} else if ( predicateStr.equals(XbelParser.elementDisclaimer)) {
				header.setDisclaimer(p.getValue());
			}
		}

		if ( ag.getAuthor().size() > 0 )
			header.setAuthorGroup(ag);
		if ( lg.getLicense().size() > 0 )
			header.setLicenseGroup(lg);
		// document.setHeader(header);
		try {
			xm.writeHeader(header);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			header = null;
		}

	}
	

	private void addNamespaceGroup(Iterable<Namespace> namespaces) {
		NamespaceGroup nsg = new NamespaceGroup();
		for (Namespace modelNamespace : namespaces) {
		  if ( !modelNamespace.getPrefix().equals(XbelParser.belPrefix)) {  //ignore xbel	
			org.ndexbio.xbel.model.Namespace xbelNamespace = this.xbelFactory.createNamespace();
			xbelNamespace.setPrefix(modelNamespace.getPrefix());
			xbelNamespace.setResourceLocation(modelNamespace.getUri());
			if (Strings.isNullOrEmpty(modelNamespace.getUri())) {
				System.out.println("++++ empty namespace uri prefix = "
						+ modelNamespace.getPrefix());
			}
			//System.out.println("Namespace: "+modelNamespace.getPrefix() +" " +modelNamespace.getUri());
			nsg.getNamespace().add(xbelNamespace);
		  }
		}
		try {
			xm.writeNamespaceGroup(nsg);
		} catch (JAXBException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} finally {
			nsg = null;
		}

	}

	private Collection<org.ndexbio.model.object.network.Citation> getCitationsByNetworkId() {
		Collection<org.ndexbio.model.object.network.Citation> modelCitations = this.modelService
						.getCitationsByNetworkId(networkId);
		System.out.println("Network " + networkId + " has "
				+ modelCitations.size() + " citations");
		return modelCitations;

	}

	/*
	 * An inner class to keep track of processed edges
	 */

	/*
	 * An inner class responsible marshalling (i.e. outputting) an JAXB object
	 * graph to an XML document
	 */

	public class XbelMarshaller {

		private final String exportedFilename;
		
		private JAXBContext context;
		private XMLStreamWriter writer;
		private Class type;
		private Marshaller marshaller;

		public XbelMarshaller(String nn) {
			Preconditions.checkArgument(!Strings.isNullOrEmpty(nn),
					"A export file name is required");
			this.exportedFilename = nn;

			try {
				
				this.context = JAXBContext.newInstance(Document.class);
				this.marshaller = context.createMarshaller();
				this.marshaller.setProperty(Marshaller.JAXB_FRAGMENT,
						Boolean.TRUE);
				this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
						Boolean.TRUE);
				NamespacePrefixMapper npm = new XbelPrefixMapper();
				this.marshaller.setProperty(
						"com.sun.xml.bind.namespacePrefixMapper", npm);

			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void open() {
			try {
				XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
				xmlFactory.setProperty(
						"javax.xml.stream.isRepairingNamespaces", Boolean.TRUE);
				
				this.writer = xmlFactory
						.createXMLStreamWriter(new FileOutputStream(
								this.exportedFilename));
				 this.writer.setDefaultNamespace("http://belframework.org/schema/1.0/xbel");
				this.writer.setPrefix("bel",
						"http://belframework.org/schema/1.0/xbel");
				this.writer.writeStartDocument("UTF-8", "1.0");
				this.writer.writeStartElement("bel:document");
				this.writer.writeNamespace("xsi",
						"http://www.w3.org/2001/XMLSchema-instance");
				this.writer
						.writeAttribute(
								"http://www.w3.org/2001/XMLSchema-instance",
								"schemaLocation",
								"http://belframework.org/schema/1.0/xbel http://resource.belframework.org/belframework/1.0/schema/xbel.xsd");

			} catch (FileNotFoundException | XMLStreamException
					| FactoryConfigurationError e) {

				e.printStackTrace();
			} 
		}

		public void writeDocument(Document d) throws JAXBException {
			JAXBElement<Document> element = new JAXBElement<>(
					QName.valueOf("bel:document"),
					(Class<Document>) d.getClass(), d);
			this.marshaller.marshal(element, this.writer);
		}

		public void writeHeader(Header h) throws JAXBException {
			JAXBElement<Header> element = new JAXBElement<>(
					QName.valueOf("bel:header"), (Class<Header>) h.getClass(),
					h);

			this.marshaller.marshal(element, this.writer);
		}

		public void writeNamespaceGroup(NamespaceGroup nsg)
				throws JAXBException {
			JAXBElement<NamespaceGroup> element = new JAXBElement<>(
					QName.valueOf("bel:namespaceGroup"),
					(Class<NamespaceGroup>) nsg.getClass(), nsg);
			this.marshaller.marshal(element, this.writer);
		}
		
		public void writeAnnotationDefinitionGroup(AnnotationDefinitionGroup adg) throws JAXBException{
			JAXBElement<AnnotationDefinitionGroup> element = new JAXBElement<>(
					QName.valueOf("bel:annotationDefinitionGroup"),
					(Class<AnnotationDefinitionGroup>) adg.getClass(), adg);
			this.marshaller.marshal(element, this.writer);
		}

		public void writeStatementGroup(StatementGroup sg) throws JAXBException {
			JAXBElement<StatementGroup> element = new JAXBElement<>(
					QName.valueOf("bel:statementGroup"),
					(Class<StatementGroup>) sg.getClass(), sg);
			this.marshaller.marshal(element, this.writer);
		}

		public void close() {
			try {
				this.writer.writeEndElement();
				this.writer.writeEndDocument();
			} catch (XMLStreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					this.writer.close();
				} catch (XMLStreamException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	public class XbelPrefixMapper extends NamespacePrefixMapper {
		private static final String BEL_NS = "bel";
		private static final String BEL_URI = "http://belframework.org/schema/1.0/xbel";
		private static final String XSI_NS = "xsi";
		private static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

		@Override
		public String getPreferredPrefix(String nsUri, String suggestion,
				boolean requiredPrefix) {
			if (nsUri.equals(XSI_URI)) {
				return XSI_NS;
			}

			return BEL_NS;

		}

	}

	/*
	 * an inner class that encapsulates a regular stack supports a VERBOSE
	 * option that logs stack operations
	 */

	public class XbelStack<T> {
		private final Stack<T> stack;
		private final String stackName;
		private Boolean VERBOSE;

		public XbelStack(String aName) {
			this.stackName = Objects.firstNonNull(aName, "unspecified");
			this.stack = new Stack<T>();
			this.VERBOSE = false;
		}

		public XbelStack(String aName, Boolean verbose) {
			this(aName);
			this.VERBOSE = verbose;
		}

		T pop() {

			try {
				T obj = this.stack.pop();
				if (this.VERBOSE) {
					System.out.println("The current " + this.stackName
							+ " stack level is " + this.stack.size());
				}
				return obj;
			} catch (java.util.EmptyStackException e) {
				System.out.println("ERROR pop " + this.stackName
						+ " stack is empty");
				e.printStackTrace();
			}
			return null;

		}

		T peek() {
			try {
				return this.stack.peek();
			} catch (java.util.EmptyStackException e) {
				System.out.println("ERROR peek " + this.stackName
						+ " stack is empty");
				e.printStackTrace();
			}
			return null;
		}

		void push(T obj) {
			Preconditions.checkArgument(null != obj,
					"invalid push operation with null object");
			this.stack.push(obj);
			if (this.VERBOSE) {
				System.out.println("The current " + this.stackName
						+ " stack level is " + this.stack.size());
			}

		}

		boolean empty() {
			return this.stack.empty();
		}

		void clear() {
			this.stack.clear();
			if (this.VERBOSE) {
				System.out.println("The  " + this.stackName
						+ " stack has been cleared ");
			}
		}
	} // end of XbelStack inner class
}
