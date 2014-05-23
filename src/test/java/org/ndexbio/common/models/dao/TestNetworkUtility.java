package org.ndexbio.common.models.dao;

import java.util.Arrays;
import java.util.List;

import org.ndexbio.common.models.dao.orientdb.helper.NetworkUtility;

public class TestNetworkUtility {
	
	public TestNetworkUtility() {}
	
	private void performTests() {
		this.testCsvGenerater();
	}
	
	private void testCsvGenerater() {
		final List<String> testList = Arrays.asList("ABC","DEF","123","XYZ");
		
		
		System.out.println(NetworkUtility.joinStringsToCsv(testList) );
	}

	public static void main(String[] args) {
		TestNetworkUtility test = new TestNetworkUtility();
		test.performTests();

	}

}
