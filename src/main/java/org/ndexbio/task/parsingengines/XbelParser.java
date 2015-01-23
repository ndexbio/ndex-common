package org.ndexbio.task.parsingengines;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.xbel.model.Header;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.tools.ProvenanceHelpers;
import org.ndexbio.task.parsingengines.XbelFileValidator.ValidationState;
import org.ndexbio.xbel.splitter.AnnotationDefinitionGroupSplitter;
import org.ndexbio.xbel.splitter.HeaderSplitter;
import org.ndexbio.xbel.splitter.NamespaceGroupSplitter;
import org.ndexbio.xbel.splitter.StatementGroupSplitter;
import org.ndexbio.xbel.splitter.XBelSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/*
 * represents a parser that can map an file conforming to the XBEL schema to
 * one or more object graphs using model classes from JAXB processing of the 
 * XBEL XSD files
 * 
 * The class requires a filename for the XML file used as input
 * The specified file is tested for validity against the XBEL schemas
 * 
 */
public class XbelParser implements IParsingEngine
{
    private final String xmlFile;
    private final ValidationState validationState;
    private JAXBContext context;
    private XMLReader reader;
    private NamespaceGroupSplitter nsSplitter;
    private AnnotationDefinitionGroupSplitter adSplitter;
    private StatementGroupSplitter sgSplitter;
    private HeaderSplitter headerSplitter;
//    private INetwork network;
    private String ownerName;
    private NdexPersistenceService networkService;
    private static final Logger logger = LoggerFactory.getLogger(XbelParser.class);

    public static final String belPrefix = "bel";
    public static final String elementContactInfo = belPrefix + ":contactInfo";
    public static final String elementCopyright   = belPrefix + ":copyright";
    public static final String elementDisclaimer  = belPrefix + ":disclaimer";
    public static final String elementAuthor      = belPrefix + ":author";
    public static final String elementLicense     = belPrefix + ":license";
    
    public XbelParser(String fn, String ownerName, NdexDatabase db) throws JAXBException, NdexException
    {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fn), "A filename is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(ownerName),
        		"A network owner name is required");
     
        File f = new File(fn);
        if ( !f.exists()) {
        	throw new NdexException("File not found: "+ f.getAbsolutePath());
        }
        this.xmlFile = f.getAbsolutePath(); 
        this.setOwnerName(ownerName);
        this.validationState = new XbelFileValidator(this.xmlFile).getValidationState();
        logger.info(this.validationState.getValidationMessage());
        this.context = JAXBContext.newInstance("org.ndexbio.xbel.model");
        this.networkService = new NdexPersistenceService(db);
        this.nsSplitter = new NamespaceGroupSplitter(context, this.networkService);
        this.adSplitter = new AnnotationDefinitionGroupSplitter(context, networkService);
        this.sgSplitter = new StatementGroupSplitter(context, this.networkService);
        this.headerSplitter = new HeaderSplitter(context);
        
        this.initReader();
    }
  
    @Override
	public void parseFile() throws NdexException
    {
        try
        {
            this.processHeaderAndCreateNetwork();
            this.processNamespaces();
            this.processAnnotationDefinitions();
            this.processStatementGroups();
            
			//add provenance to network
			NetworkSummary currentNetwork = this.networkService.getCurrentNetwork();
			
			String uri = NdexDatabase.getURIPrefix();

			ProvenanceEntity provEntity = ProvenanceHelpers.createProvenanceHistory(currentNetwork,
					uri, "FILE_UPLOAD", currentNetwork.getCreationTime(), (ProvenanceEntity)null);
			provEntity.getCreationEvent().setEndedAtTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
			
			File f = new File (this.xmlFile);
			List<SimplePropertyValuePair> l = provEntity.getCreationEvent().getProperties();
			l.add(	new SimplePropertyValuePair ( "filename",f.getName()) );
			
			this.networkService.setNetworkProvenance(provEntity);
            
            // set edge count and node count,
            // then close database connection
            this.networkService.persistNetwork();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            // rollback current transaction and close the database connection
            this.networkService.abortTransaction();
            throw new NdexException ("Error occurred when loading " +
              xmlFile + ". " + e.getMessage());
        }

    }

    private void processHeaderAndCreateNetwork() throws Exception
    {
        reader.setContentHandler(headerSplitter);
        try
        {
            reader.parse(this.getXmlFile());
        }
        catch (IOException | SAXException e)
        {
            logger.error(e.getMessage());
            throw new Exception(e);
        }
        Header header = this.headerSplitter.getHeader();
        
        String networkTitle = header.getName();
        this.networkService.createNewNetwork(
        		this.getOwnerName(), 
        		networkTitle,
        		this.headerSplitter.getHeader().getVersion());
        this.networkService.setNetworkTitleAndDescription(null, header.getDescription());
        this.networkService.setNetworkSourceFormat(NetworkSourceFormat.BEL);
		try {
				// create a few default name spaces. 
				// BEL namespace
				RawNamespace belNamespace = new RawNamespace(belPrefix,XBelSplitter.belURI);
				this.networkService.getNamespace(belNamespace);
				
		} catch (NdexException ex) {
				ex.printStackTrace();
				logger.error(ex.getMessage());
				throw new Exception ("Error occurred when creating default namespaces: " + ex.getMessage());
		}
		
        // insert others as network properties.
		List<NdexPropertyValuePair> propList = new ArrayList <> ();
		
        String contact = header.getContactInfo();
        if ( contact != null ) {
        	NdexPropertyValuePair p = new NdexPropertyValuePair(elementContactInfo, contact);
        	propList.add(p);
        }

        String copyright = header.getCopyright();
        if ( copyright != null ) {
        	NdexPropertyValuePair p = new NdexPropertyValuePair(elementCopyright, copyright);
        	propList.add(p);
        }

        String disclaimer = header.getDisclaimer();
        if ( disclaimer != null ) {
        	NdexPropertyValuePair p = new NdexPropertyValuePair(elementDisclaimer, disclaimer);
        	propList.add(p);
        }
        
        if ( header.getAuthorGroup() !=null ) {
        	for ( String author : header.getAuthorGroup().getAuthor()) {
        		NdexPropertyValuePair p = new NdexPropertyValuePair(elementAuthor, author);
            	propList.add(p);
        	}
        }

        if ( header.getLicenseGroup() !=null ) {
        	for ( String l : header.getLicenseGroup().getLicense()) {
        		NdexPropertyValuePair p = new NdexPropertyValuePair(elementLicense, l);
            	propList.add(p);
        	}
        }

        
        this.networkService.setNetworkProperties(propList, null);

    }

    private void processNamespaces() throws Exception
    {
        logger.info("Parsing namespaces from " + this.getXmlFile());
        reader.setContentHandler(nsSplitter);
        try
        {
            reader.parse(this.getXmlFile());
        }
        catch (IOException | SAXException e)
        {
            logger.error(e.getMessage());
            throw new Exception(e);
        }
    }
    
	private void processAnnotationDefinitions() throws Exception {
		logger.info("Parsing annotation definitions from " + this.getXmlFile());
		reader.setContentHandler(adSplitter);
		try {
			reader.parse(this.getXmlFile());
		} catch (IOException | SAXException e) {
			logger.error(e.getMessage());
			throw new Exception(e);
		}
	}

    private void processStatementGroups() throws Exception
    {
        logger.info("Parsing statement groups from " + this.getXmlFile());
        reader.setContentHandler(sgSplitter);
        try
        {
            reader.parse(this.getXmlFile());
        }
        catch (IOException | SAXException e)
        {
            logger.error(e.getMessage());
            throw new Exception(e);
        }
    }

    private void initReader()
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try
        {
            this.setReader(factory.newSAXParser().getXMLReader());
        }
        catch (SAXException | ParserConfigurationException e)
        {
            logger.error(e.getMessage());
        }
    }

    public ValidationState getValidationState()
    {
        return this.validationState;
    }

    public XMLReader getReader()
    {
        return reader;
    }

    public void setReader(XMLReader reader)
    {
        this.reader = reader;
    }

    public String getXmlFile()
    {
        return xmlFile;
    }

	public String getOwnerName() {
		return ownerName;
	}

	private void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	@Override
	public UUID getUUIDOfUploadedNetwork() {
		try { 
			return networkService.getCurrentNetwork().getExternalId();
		} catch ( Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
