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
package org.ndexbio.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.SearchParameters;
import org.ndexbio.model.object.SearchResult;
import org.ndexbio.task.parsingengines.*;

public class TestXGMMLParsingEngine {
	private static String _testUserName = "dexterpratt";
	private static User _testUser = null;
	private static final String NETWORK_UPLOAD_PATH = "/opt/ndex/uploaded-networks/";

	@BeforeClass
	public static void setupUser() throws Exception {
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setSearchString(_testUserName);
		searchParameters.setSkip(0);
		searchParameters.setTop(1);

		try {

/*			SearchResult<User> result = (new SIFNetworkService())
					.findUsers(searchParameters);
			_testUser = (IUser) result.getResults().iterator().next(); */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/*
	@Test
	public void parseLargeXGMMLFile() throws Exception {
		final URL url = getClass().getResource("/resources/galFiltered.xgmml");
		final XgmmlParser xgmmlParser = new XgmmlParser(url.toURI().getPath(),
				_testUserName);
		System.out.println("Parsing XGMML : " + url.toURI().getPath());
		xgmmlParser.parseFile();
	}
*/	
	@Test
	public void parseExternalXGMMLFile() throws Exception {
		
		// Full_qMax_0.95_Severe-Healthy_net_gml.xgmml
		// IBD_subnetwork_go.xgmml
		//final String path = "/Users/dextergraphics/Documents/NDEx_xgmml_test/Full_qMax_0.95_Severe-Healthy_net_gml.xgmml";
		final String path = "/Users/dextergraphics/Documents/NDEx_xgmml_test/IBD_subnetwork_go.xgmml";
//		final XgmmlParser xgmmlParser = new XgmmlParser(path,
//				_testUserName);
		System.out.println("Parsing XGMML : " + path);
//		xgmmlParser.parseFile();
	}

	/*
	@Test
	public void parseSeveralSifFiles() throws Exception {
		List<String> files = new ArrayList<String>();
		files.add("/resources/ca-calmodulin-dependent_protein_kinase_activation.SIF");
		files.add("/resources/cadmium_induces_dna_synthesis_and_proliferation_in_macrophages.SIF");
		files.add("/resources/Calcineurin-regulated_NFAT-dependent_transcription_in_lymphocytes.SIF");
		files.add("/resources/Calcitonin-like_ligand_receptors.SIF");
		files.add("/resources/calcium_signaling_by_hbx_of_hepatitis_b_virus.SIF");
		files.add("/resources/Calcium_signaling_in_the_CD4+_TCR_pathway.SIF");
		files.add("/resources/Calmodulin_induced_events.SIF");
		for (String file : files) {
			URL url = getClass().getResource(file);
			SifParser sifParser = new SifParser(url.toURI().getPath(),
					_testUserName);
			System.out.println("Parsing SIF : " + url.toURI().getPath());
			sifParser.parseFile();
		}

	}


	 * @Test public void parseXbelFile() throws Exception { final URL
	 * xbelNetworkURL = getClass().getResource("/resources/tiny-corpus.xbel");
	 * String fn = xbelNetworkURL.getPath(); //String fn = NETWORK_UPLOAD_PATH +
	 * "small-corpus.xbel"; final XbelParser xbelParser = new XbelParser(fn,
	 * _testUserName);
	 * 
	 * if (!xbelParser.getValidationState().isValid())
	 * Assert.fail("tiny-corpus.xbel is invalid.");
	 * System.out.println("Parsing XBEL : " + fn); xbelParser.parseFile(); }
	 */
/*
	@Test
	public void parseLargeXbelFile() throws Exception {
		final URL url = getClass().getResource("/resources/small-corpus.xbel");
		final XbelParser xbelParser = new XbelParser(url.toURI().getPath(),
				_testUserName);

		if (!xbelParser.getValidationState().isValid())
			Assert.fail("small-corpus.xbel is invalid.");

		xbelParser.parseFile();
	}
*/
}