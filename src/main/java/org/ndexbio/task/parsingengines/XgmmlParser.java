package org.ndexbio.task.parsingengines;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.tools.ProvenanceHelpers;
import org.ndexbio.xgmml.parser.HandlerFactory;
import org.ndexbio.xgmml.parser.XGMMLParser;
import org.ndexbio.xgmml.parser.handler.ReadDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.ParserAdapter;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class XgmmlParser implements IParsingEngine {
    private final File xgmmlFile;
    private String ownerName;
    private String networkTitle;
    private NdexPersistenceService networkService;
    private static final Logger logger = LoggerFactory.getLogger(XgmmlParser.class);

	public XgmmlParser(String fn, String ownerAccountName, NdexDatabase db, String defaultNetworkName) throws Exception {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(fn),
				"A filename is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(fn),
				"A network owner name is required");
		this.ownerName = ownerAccountName;
		this.xgmmlFile = new File(fn);
		this.networkService = new NdexPersistenceService(db);
		this.networkTitle = defaultNetworkName;
		
	}  
	
	private static void log (String string){
		System.out.println(string);
	}
    
    
	private void setNetwork() throws Exception {
	//	String title = Files.getNameWithoutExtension(this.xgmmlFile.getName());
		this.networkService.createNewNetwork(this.getOwnerName(), networkTitle, null);
	}

	@Override
	public void parseFile() throws NdexException {
        
		try (FileInputStream xgmmlFileStream = new FileInputStream(this.getXgmmlFile())) { 

			try
			{
        	
				setNetwork();
				readXGMML(xgmmlFileStream);

				this.networkService.setNetworkSourceFormat(NetworkSourceFormat.XGMML);

				//add provenance to network
				NetworkSummary currentNetwork = this.networkService.getCurrentNetwork();
			
				String uri = NdexDatabase.getURIPrefix();

				// set the source format
			
				ProvenanceEntity provEntity = ProvenanceHelpers.createProvenanceHistory(currentNetwork,
					uri, "FILE_UPLOAD", currentNetwork.getCreationTime(), (ProvenanceEntity)null);
				provEntity.getCreationEvent().setEndedAtTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
			
				List<SimplePropertyValuePair> l = provEntity.getCreationEvent().getProperties();
				l.add(	new SimplePropertyValuePair ( "filename",this.xgmmlFile.getName()) );
			
				this.networkService.setNetworkProvenance(provEntity);

            
				// close database connection
				this.networkService.persistNetwork();
			}
			catch (Exception e)
			{
				// rollback current transaction and close the database connection
				this.networkService.abortTransaction();
				e.printStackTrace();
				throw new NdexException("Error occurred when loading "
            		+ this.xgmmlFile.getName() + ". " + e.getMessage());
			} 
		}	catch (IOException e1) {
        
            e1.printStackTrace();
            log("Could not read " + this.getXgmmlFile() + ": " + e1.getMessage());
            this.networkService.abortTransaction();  //TODO: close connection to database
            throw new NdexException("File not found: " + this.xgmmlFile.getName());
        }
		
	}
	
	/**
	 * Actual method to read XGMML documents.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	private void readXGMML(FileInputStream xgmmlFileStream) throws SAXException, IOException {
		final SAXParserFactory spf = SAXParserFactory.newInstance();

		try {
			// Get our parser
			final SAXParser sp = spf.newSAXParser();
			// Ignore the DTD declaration
			final XMLReader reader = sp.getXMLReader();
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			reader.setFeature("http://xml.org/sax/features/validation", false);
			// Make the SAX1 Parser act as a SAX2 XMLReader
			final ParserAdapter pa = new ParserAdapter(sp.getParser());
		//	RecordingInputStream ris=new RecordingInputStream(xgmmlFileStream);
			ReadDataManager readDataManager = new ReadDataManager(networkService);
			HandlerFactory handlerFactory = new HandlerFactory(readDataManager);
			XGMMLParser parser = new XGMMLParser(handlerFactory, readDataManager);
			pa.setContentHandler(parser);
			pa.setErrorHandler(parser);
			
			// Parse the XGMML input
			pa.parse(new InputSource(xgmmlFileStream));
		} catch (OutOfMemoryError oe) {
			// It's not generally a good idea to catch OutOfMemoryErrors, but in
			// this case, where we know the culprit (a file that is too large),
			// we can at least try to degrade gracefully.
			System.gc();
			throw new RuntimeException("Out of memory error caught. The network being loaded is too large for the current memory allocation.  Use the -Xmx flag for the java virtual machine to increase the amount of memory available, e.g. java -Xmx1G cytoscape.jar -p apps ....");
		} catch (ParserConfigurationException e) {
			logger.error("XGMMLParser: " + e.getMessage());
		} catch (SAXParseException e) {
			logger.error("XGMMLParser: fatal parsing error on line " + e.getLineNumber() + " -- '" + e.getMessage()
					+ "'");
			throw e;
		} finally {
			if (xgmmlFileStream != null) {
				try {
					xgmmlFileStream.close();
				} catch (Exception e) {
					logger.warn("Cannot close XGMML input stream", e);
				}
			}
		}
	}

	public String getOwnerName() {
		return ownerName;
	}


	public File getXgmmlFile() {
		return xgmmlFile;
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
