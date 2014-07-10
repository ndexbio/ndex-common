package org.ndexbio.common.persistence.orientdb;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Namespace;


public class NDExNoTxMemoryPersistenceTest {

	private static NDExNoTxMemoryPersistence service;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		service = new NDExNoTxMemoryPersistence(new NdexDatabase());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		//service.commit();
	}

	@Test
	public void test() throws Exception {
		service.createNewNetwork("Support", "cjtest network", "0.1");
		
		RawNamespace rns1 = new RawNamespace("ns1", "http://foo.bar.com/"); 
		Namespace ns1 = service.getNamespace(rns1);
		
		RawNamespace rns2 = new RawNamespace("ns2", "http://something.com/");
		Namespace ns2 = service.getNamespace(rns2);

		RawNamespace rns3 = new RawNamespace("ns1", "http://foo.bar.com/"); 
		Namespace ns3 = service.getNamespace(rns3);

		String s3 = "http://foo.newdomain.com/P001";
		BaseTerm t3 = service.getBaseTerm(s3);

		String s1 = "Y0002";
		BaseTerm t1 = service.getBaseTerm(s1);
		
		String s2 = "ns1:y003";
		BaseTerm t2 = service.getBaseTerm(s2);
		
		
		String s4 = "http://foo.bar.com/XYC002";
		BaseTerm t4 = service.getBaseTerm(s4);
		
		
	    service.persistNetwork();
	}

/*	@Test
	public void testBaseTermInsert() throws NdexException {
		
		
	} */
}
