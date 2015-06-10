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
package org.ndexbio.common.models.dao;


import org.ndexbio.model.exceptions.*;
import org.ndexbio.model.object.network.Network;

public class TestDOAOperations {

	private  String testUserId = "C31R0";
	private  String testUserName = "biologist1";
	private  String testPassword = "bio1";
	
	public TestDOAOperations(){
		
	}
	
	private void performTests() {
		try {
//			this.testChangePassword();
	//		this.testFindNetworks();
		//	String networkId = this.testCreateNetwork();
		//	this.deleteNetwork(networkId);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
/*	private void deleteNetwork(String networkId){
		System.out.println("++++++Test delete network " +networkId);
		final NetworkDAO dao = DAOFactorySupplier.INSTANCE
				.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE).get().getNetworkDAO();
		try {
			dao.deleteNetwork(testUserId, networkId);
		} catch (IllegalArgumentException
				| NdexException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	} */
	
	
	private String testCreateNetwork() throws IllegalArgumentException, DuplicateObjectException, NdexException {
		System.out.println("++++++Test create network ");
/*		final NetworkDAO dao = DAOFactorySupplier.INSTANCE
				.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE).get().getNetworkDAO(); */
		final Network newNetwork = new Network();
        newNetwork.setDescription("This is a test network.");
        newNetwork.setIsComplete(true);
     //   newNetwork.getMetadata().put("Copyright", "2013 Cytoscape Consortium");
      //  newNetwork.getMetadata().put("Format", "JDEX");
        newNetwork.setName("Test Network");
   //     final Network createdNetwork = dao.createNetwork(this.testUserId,newNetwork);
    //    System.out.println("Created network: " +createdNetwork.getName());
    //    return createdNetwork.getId();
          return "";
            
	}
	
/*	private void testFindNetworks() throws IllegalArgumentException, NdexException {
		System.out.println("++++++Test find newtworks ");
		final NetworkDAO networkDAO = DAOFactorySupplier.INSTANCE
				.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE).get().getNetworkDAO();
		
		final SearchParameters searchParameters = new SearchParameters();
		searchParameters.setSearchString("reactome");
		searchParameters.setSkip(0);
		searchParameters.setTop(25);

		final List<Network> networksFound = networkDAO.findNetworks(testUserId,
				searchParameters, "starts-with");
		System.out.println("Found " +networksFound.size() +" networks.");
	}
*/	
/*	private void testChangePassword() throws IllegalArgumentException, NdexException{
		System.out.println("++++++Test change password ");
	 UserDAO userDAO = DAOFactorySupplier.INSTANCE
				.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE).get()
				.getUserDAO();
	 userDAO.changePassword("newpassword", testUserId);
	 System.out.println("Changed biologist1's password to newpassword");

		userDAO.changePassword(testPassword, testUserId);
		System.out.println("Restored biologist1's original password");
	}

	public static void main(String[] args) {
		TestDOAOperations test = new TestDOAOperations();
		test.performTests();
	

	}
*/
}
