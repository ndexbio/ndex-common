package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.task.Configuration;
import org.ndexbio.task.utility.BulkFileUploadUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class SifParserTest {

	private static ODatabaseDocumentTx db;
	private static String user = "Support";
	
	private static final Logger logger = LoggerFactory.getLogger(BulkFileUploadUtility.class);

	
/*	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		db = NdexAOrientDBConnectionPool.getInstance().acquire();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
//		db.close();
	}
*/
/*	
	@Test 
	public void URITest () throws URISyntaxException {
		URI termStringURI = new URI("http://www.foo.bar.org/testpath/something#NW223");
		String scheme = termStringURI.getScheme();
		System.out.println(scheme);
		String p = termStringURI.getSchemeSpecificPart();
		System.out.println(p);
		String f = termStringURI.getFragment();
		System.out.println(f);
		
	}
*/
	@Test
	public void test() throws Exception {
    	// read configuration
    	Configuration configuration = Configuration.getInstance();
    	
    	//and initialize the db connections
    	NdexAOrientDBConnectionPool.createOrientDBConnectionPool(
    			configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd(),1);
    	
    	
		NdexDatabase db = new NdexDatabase(configuration.getHostURI());
		
		String userAccount = "reactomeadmin";
		SifParser parser = new SifParser("ca-calmodulin-dependent_protein_kinase_activation.SIF", userAccount,
				db,"ca-calmodulin-dependent_protein_kinase_activation");
		parser.parseFile();
		parser = new SifParser("gal-filtered.sif", userAccount,db,"gal-filtered");
		parser.parseFile();
		
		parser = new SifParser("Calcineurin-regulated_NFAT-dependent_transcription_in_lymphocytes.SIF",userAccount,
				db,"Calcineurin-regulated_NFAT-dependent_transcription_in_lymphocytes");
		parser.parseFile();

//		SifParser parser = new SifParser("/home/chenjing/working/ndex/networks/reactome46_human/Meiosis.SIF","Support");
	//	SifParser parser = new SifParser("/home/chenjing/working/ndex/networks/reactome46_human/Metabolism_of_RNA.SIF","Support");
		
//		SifParser parser = new SifParser("/home/chenjing/working/ndex/networks/reactome46_human/Cell_Cycle.SIF","Support");
//		SifParser parser = new SifParser("/home/chenjing/working/ndex/networks/reactome46_human/Signaling_Pathways.SIF","Support");
//		SifParser parser = new SifParser("/home/chenjing/working/ndex/networks/reactome46_human/Circadian_Clock.SIF","Support");
//		parser.parseFile();
		
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				Paths.get("/home/chenjing/working/ndex/networks/reactome46_human"))) 
		{
            for (Path path : directoryStream) {
              logger.info("Processing file " +path.toString());
              SifParser parser2 = new SifParser(path.toString(),userAccount,db, path.toString());
         		parser2.parseFile();
      		
  			 logger.info("file upload for  " + path.toString() +" finished.");
            }
        } catch (IOException | IllegalArgumentException e) {
        	logger.error(e.getMessage());
        	throw e;
        }
		
		db.close();
		
		NdexAOrientDBConnectionPool.close();
	} 

}
