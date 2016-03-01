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
package org.ndexbio.common.access;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.ndexbio.common.models.object.NetworkQueryParameters;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.SimplePathQuery;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.PropertyGraphNetwork;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class TestNetworkAOrientDBDAO {
	
	private static final NetworkAOrientDBDAO noi = new NetworkAOrientDBDAO();
	
	private final ObjectMapper jsonMapper = new ObjectMapper();
/*	
	@Test
	public void getNetworkBaseTerms(){
		String networkId = "C25R45";
		int skipBlocks = 0;
		int blockSize = 50;
		List<BaseTerm> baseTerms = new ArrayList();
		try {
			baseTerms = noi.getTerms(networkId, skipBlocks, blockSize);
			Assert.assertEquals(blockSize, baseTerms.size());
		} catch (IllegalArgumentException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		} catch (NdexException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	private String[] testSymbols = {"ATP5B", "RIMS1", "SIT1", "LCK", "EBF1", "FGFR1", "NOMO1", "SGTA", "CAV1", "TLE4", "GAD1", "CABP1", "CACNA1A", "FZD3", "LOR", "GIPC1", "PARP1", "GYPB", "GAS2L1", "CD82", "CD69", "CD7", "CD5", "AANAT", "STOML1", "MYBPC3", "ANKRD2", "MYOM2", "TTN", "ANK1", "SLC4A1", "ICAM4", "RHAG", "FCER2", "CD47", "DFFB", "ANKRD1", "CIDEB", "DFFA", "ANKRD23", "MYPN", "NEB", "S100B", "MRPS15", "SEC14L2", "EIF3J", "SMURF1"};
	

	 
	@Test
	public void sourceTargetQuery(){
		noi.testSTQuery();
	}
*/	
	
	@Test 
	public void StringToRIDTest() {
	}
/*	
	@Test 
	public void queryForEdges(){

		String s = "abc(#20:2)";
		ORID id = noi.stringToRid(s);
		
		System.out.println(id);
		
		SimplePathQuery parameters = new SimplePathQuery();
		parameters.setSearchDepth(1);
//		parameters.setSearchString("Q16566");
		parameters.setSearchString("YGR218W");
		int skipBlocks = 0;
		int blockSize = 80;
		try {
			
			PropertyGraphNetwork n = noi.queryForSubPropertyGraphNetwork("d750c790-199e-11e4-86bd-90b11c72aefa",parameters);
			System.out.println (n);
			List<Edge> edges = noi.queryForEdges(
					//"Support",
					"d750c790-199e-11e4-86bd-90b11c72aefa",
					parameters, 
					skipBlocks,
					blockSize);
			for (Edge edge : edges){
				System.out.println("subject = " + edge.getSubjectId() + " predicate = " + edge.getPredicateId() 
						+ " object = " + edge.getObjectId());
			}
		} catch (IllegalArgumentException | NdexException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		} 
	}
*/	
/*	
	@Test
	public void mytest() throws NdexException {
	   	
		NetworkAOrientDBDAO dao = NetworkAOrientDBDAO.getInstance();
		
		List<ODocument> ds = dao.getUserFromName("biologist1");
		
		for (ODocument d : ds) {
			String email = d.field("emailAddress");
			System.out.println (email);
			ODocument grp = d.field("out_userGroups");
			System.out.println(grp);
			String mtype = ((ODocument)(d.field("out_userGroups"))).field("membershipType");
			System.out.println("use memtype:" + mtype);
			System.out.println (d);
		}
		
		
	}
*/	
	
/*	@Test 
 *   comment out for now. --cj
	public void networkByInterconnect(){
		
		NetworkQueryParameters parameters = new NetworkQueryParameters();
		int skipBlocks = 0;
		int blockSize = 50;
		//parameters.setSearchType("INTERCONNECT");
		parameters.setSearchType("NEIGHBORHOOD");
		parameters.setSearchDepth(2);
		List<String> termNames = new ArrayList<String>();
		
		//termNames.add("YOL058W");
		//termNames.add("YKR099W");
		
		termNames.add("MAF");
		termNames.add("JUN");
		parameters.setStartingTermStrings(termNames);
		try {
			
			Network network = noi.queryForSubnetwork(new User(),
					"C25R18",
					parameters, 
					skipBlocks,
					blockSize);
			System.out.println("network edges = " + network.getEdges().values().size());
			System.out.println(jsonMapper.writeValueAsString(network));

		} catch (IllegalArgumentException | NdexException | JsonProcessingException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		} 
	} */
}
