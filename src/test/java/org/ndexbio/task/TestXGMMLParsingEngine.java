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