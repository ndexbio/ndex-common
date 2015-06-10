/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.task.parsingengines;


import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.task.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SifParserTest {

	
	private static final Logger logger = LoggerFactory.getLogger(SifParserTest.class);

	
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
    	NdexDatabase db = NdexDatabase.createNdexDatabase(configuration.getHostURI(),
				configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd(), 1);
		
		String userAccount = "reactomeadmin";
		SifParser parser = new SifParser("ca-calmodulin-dependent_protein_kinase_activation.SIF", userAccount,
				db,"ca-calmodulin-dependent_protein_kinase_activation", "");
		parser.parseFile();
		parser = new SifParser("gal-filtered.sif", userAccount,db,"gal-filtered", "");
		parser.parseFile();
		
		parser = new SifParser("Calcineurin-regulated_NFAT-dependent_transcription_in_lymphocytes.SIF",userAccount,
				db,"Calcineurin-regulated_NFAT-dependent_transcription_in_lymphocytes", "");
		parser.parseFile();

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				Paths.get("/home/chenjing/working/ndex/networks/reactome46_human"))) 
		{
            for (Path path : directoryStream) {
              logger.info("Processing file " +path.toString());
              SifParser parser2 = new SifParser(path.toString(),userAccount,db, path.toString(), "");
         		parser2.parseFile();
      		
  			 logger.info("file upload for  " + path.toString() +" finished.");
            }
        } catch (IOException | IllegalArgumentException e) {
        	logger.error(e.getMessage());
        	throw e;
        }
		
		db.close();
		
	} 

}
