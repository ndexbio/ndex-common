package org.ndexbio.xbel.splitter;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.xbel.model.AnnotationDefinitionGroup;
import org.ndexbio.xbel.model.ExternalAnnotationDefinition;
import org.ndexbio.xbel.model.InternalAnnotationDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationDefinitionGroupSplitter extends XBelSplitter {

	private static final String xmlElement = "annotationDefinitionGroup";
	
	public static final String property_Type = "type";
	public static final String internal_annotation_def = "InternalAnnotationDefinition"; 
	public static final String external_annotation_def = "ExternalAnnotationDefinition";
	public static final String desc = "description";
	public static final String list_annotation = "listAnnotation";
	public static final String patternAnnotation = "patternAnnotation";

	private static final String internalAnnotationDefPrefix = "http://belframework.org/schema/1.0/xbel/internalAnnotation/"; 

	private static final Logger logger = LoggerFactory
			.getLogger(AnnotationDefinitionGroupSplitter.class);

	private NdexPersistenceService networkService;
	
	/*
	 * Extension of XBelSplitter to parse NamespaceGroup data from an XBEL
	 * document
	 */
	public AnnotationDefinitionGroupSplitter(JAXBContext context,
			NdexPersistenceService networkService) {
		super(context, xmlElement);
		this.networkService = networkService;
		
	}

	@Override
	/*
	 * method to process unmarshaled XBEL Internal and External Annotation Definition elements from XBEL document
	 * 
	 * For each Annotation Definition ad, we add a namespace ns to the network.
	 * ns.prefix = ad.id
	 * 
	 * We distinguish this as a special kind of namespace by setting its metadata: isAnnotationDefinition = true
	 * 
	 * For an External Annotation Definition, a URL is specified:
	 * ns.uri = ead.url
	 * 
	 * metadata: isExternalAnnotationDefinition = true
	 * 
	 * For an Internal Annotation Definition iad,
	 * 
	 * metadata: type = InternalAnnotationDefinition
	 * metadata: description = iad.getDescription();
	 * 
	 * for each String in  iad.getListAnnotation()
	 * 
	 * create an iBaseTerm in the namespace
	 * 
	 * 
	 * responsible for registering novel namespace prefixes in the identifier
	 * cache, for determining the new or existing jdex id for the namespace and
	 * for persisting new namespaces into the orientdb databases
	 * 
	 * @see org.ndexbio.xbel.splitter.XBelSplitter#process()
	 */
	protected void process() throws JAXBException, ExecutionException {

		AnnotationDefinitionGroup annotationDefinitionGroup = (AnnotationDefinitionGroup) unmarshallerHandler
				.getResult();
		logger.info("The XBEL document has "
				+ annotationDefinitionGroup.getInternalAnnotationDefinition()
						.size() + " internal annotation definitions");

		for (InternalAnnotationDefinition internalAnnotationDefinition : annotationDefinitionGroup.getInternalAnnotationDefinition()) {

			try {
				Namespace internalAnnotationNamespace = this.networkService.getNamespace(
						new RawNamespace(internalAnnotationDefinition.getId(),
								  internalAnnotationDefPrefix + internalAnnotationDefinition.getId()));
			
				String attDataType = "String";
				this.networkService.addElementProperty(internalAnnotationNamespace.getId(),
						property_Type, internal_annotation_def, attDataType);
				
				if (null != internalAnnotationDefinition.getDescription()){
					this.networkService.addElementProperty(internalAnnotationNamespace.getId(),desc, 
										internalAnnotationDefinition.getDescription(), "");
				}
				if (null != internalAnnotationDefinition.getPatternAnnotation()){
					this.networkService.addElementProperty(internalAnnotationNamespace.getId(),
							patternAnnotation, internalAnnotationDefinition.getPatternAnnotation(), attDataType);
				}
				if (null != internalAnnotationDefinition.getListAnnotation()){
					for (String annotation : internalAnnotationDefinition.getListAnnotation().getListValue()){
						this.networkService.addElementProperty(internalAnnotationNamespace.getId(),
								list_annotation, annotation, attDataType); 
						// Create a term in the namespace
						this.networkService.getBaseTermId(internalAnnotationNamespace.getPrefix()+":"+ annotation);
					}
				}
         

			} catch (NdexException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}

		}
		logger.info("done with internal annotation definitions");
		
		logger.info("The XBEL document has "
				+ annotationDefinitionGroup.getExternalAnnotationDefinition()
						.size() + " external annotation definitions");

		for (ExternalAnnotationDefinition externalAnnotationDefinition : annotationDefinitionGroup.getExternalAnnotationDefinition()) {

			try {
				Namespace externalAnnotationNamespace = this.networkService.getNamespace(
						new RawNamespace(externalAnnotationDefinition.getId(), 
						externalAnnotationDefinition.getUrl()));
				this.networkService.addElementProperty(externalAnnotationNamespace.getId(),
						"type", "ExternalAnnotationDefinition", "String");

			} catch (NdexException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}

		}
		logger.info("done with internal annotation definitions");

	}

}
