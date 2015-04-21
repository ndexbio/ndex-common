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

public class TestXBELParsingEngine {
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

/*			SearchResult<IUser> result = (new SIFNetworkService())
					.findUsers(searchParameters);
			_testUser = (IUser) result.getResults().iterator().next(); */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	/*
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

	@Test
	public void parseLargeXbelFile() throws Exception {
		final URL url = getClass().getResource("/resources/small-corpus.xbel");
/*		final XbelParser xbelParser = new XbelParser(url.toURI().getPath(),
				_testUserName);

		if (!xbelParser.getValidationState().isValid())
			Assert.fail("small-corpus.xbel is invalid.");

		xbelParser.parseFile(); */
	}

	@Test
	public void parseThreeCitationXbelFile() throws Exception {
		final URL url = getClass().getResource("/resources/three_citation_corpus.xbel");
/*		final XbelParser xbelParser = new XbelParser(url.toURI().getPath(),
				_testUserName);

		if (!xbelParser.getValidationState().isValid())
			Assert.fail("three_citation_corpus.xbel is invalid.");

		xbelParser.parseFile(); */
	}
}