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
