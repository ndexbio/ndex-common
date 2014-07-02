package org.ndexbio.common.access;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.object.NetworkQueryParameters;
import org.ndexbio.common.models.object.network.BaseTerm;
import org.ndexbio.common.models.object.network.Edge;
import org.ndexbio.common.models.object.network.Network;
import org.ndexbio.common.models.object.privilege.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class TestNetworkAOrientDBDAO {
	
	private static final NetworkADAO noi = new NetworkAOrientDBDAO();
	
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
	
	@Test 
	public void queryForEdges(){
		
		NetworkQueryParameters parameters = new NetworkQueryParameters();
		int skipBlocks = 0;
		int blockSize = 50;
		parameters.setSearchType("INTERCONNECT");
		parameters.setSearchDepth(2);
		List<String> termNames = new ArrayList<String>();
		
		//termNames.add("YOL058W");
		//termNames.add("YKR099W");
		
		termNames.add("MAF");
		termNames.add("JUN");
		parameters.setStartingTermStrings(termNames);
		try {
			
			List<Edge> edges = noi.queryForEdges(
					"C25R18",
					parameters, 
					skipBlocks,
					blockSize);
			for (Edge edge : edges){
				System.out.println("subject = " + edge.getS() + " predicate = " + edge.getP() + " object = " + edge.getO());
			}
		} catch (IllegalArgumentException | NdexException e) {
			Assert.fail(e.getMessage());
			e.printStackTrace();
		} 
	}
	*/
	
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
