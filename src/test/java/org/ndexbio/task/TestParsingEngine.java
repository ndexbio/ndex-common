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

public class TestParsingEngine {
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
			_testUser = (User) result.getResults().iterator().next(); */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * @Test public void parseExcelFile() throws Exception { final URL
	 * smallExcelNetworkUrl =s
	 * getClass().getResource("/resources/small-excel-network.xls"); final
	 * ExcelParser excelParser = new
	 * ExcelParser(smallExcelNetworkUrl.toURI().getPath(), _testUserName);
	 * 
	 * System.out.println("Parsing Excel file : " +
	 * smallExcelNetworkUrl.toURI().getPath()); excelParser.parseFile(); }
	 */

	/*
	 * @Test public void parseLargeExcelFile() throws Exception { final URL
	 * smallExcelNetworkUrl =
	 * getClass().getResource("/resources/large-excel-network.xls"); final
	 * ExcelParser excelParser = new
	 * ExcelParser(smallExcelNetworkUrl.toURI().getPath()); //
	 * excelParser.setNetwork
	 * (SIFNetworkService.getInstance().createNewNetwork()); //
	 * INetworkMembership membership =
	 * SIFNetworkService.getInstance().createNewMember(); //
	 * membership.setMember(_testUser); //
	 * membership.setPermissions(Permissions.ADMIN); // INetwork network =
	 * excelParser.getNetwork(); // membership.setNetwork(network); //
	 * network.setIsPublic(true); // // _testUser.addNetwork(membership); //
	 * network.addMember(membership);
	 * 
	 * excelParser.parseFile(); }
	 * 
	 * @Test public void parseSifFile() throws Exception { final URL
	 * galNetworkUrl = getClass().getResource("/resources/gal-filtered.sif");
	 * final SifParser sifParser = new
	 * SifParser(galNetworkUrl.toURI().getPath());
	 * sifParser.setNetwork(SIFNetworkService.getInstance().createNewNetwork());
	 * INetworkMembership membership =
	 * SIFNetworkService.getInstance().createNewMember();
	 * membership.setMember(_testUser);
	 * membership.setPermissions(Permissions.ADMIN); INetwork network =
	 * sifParser.getNetwork(); membership.setNetwork(network);
	 * network.setIsPublic(true);
	 * 
	 * _testUser.addNetwork(membership); network.addMember(membership);
	 * 
	 * sifParser.parseFile(); }
	 */

/*	@Test
	public void parseLargeSifFile() throws Exception {
		final URL url = getClass().getResource(
				"/resources/Glucocorticoid_receptor_regulatory_network.SIF");
		final SifParser sifParser = new SifParser(url.toURI().getPath(),
				_testUserName);
		System.out.println("Parsing SIF : " + url.toURI().getPath());
		sifParser.parseFile();
	}

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

	} */


}