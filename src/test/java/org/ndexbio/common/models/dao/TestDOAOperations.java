/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
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
