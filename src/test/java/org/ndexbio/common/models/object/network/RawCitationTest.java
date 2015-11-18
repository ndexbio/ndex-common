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
package org.ndexbio.common.models.object.network;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.ndexbio.model.exceptions.NdexException;

public class RawCitationTest {

	@Test
	public void test0() throws SolrServerException, IOException {
		String solrUrl = "http://localhost:8983/solr/";
		
		String coreName = "test12";
		HttpSolrClient client = new HttpSolrClient(solrUrl);
				
	//	ConcurrentUpdateSolrServer solrServer = new ConcurrentUpdateSolrServer(solrUrl, 1,2);
	//	CoreAdminResponse foo = CoreAdminRequest.createCore(coreName, "test1", client, ""
				
	//			);
		
		CoreAdminRequest.Create creator = new CoreAdminRequest.Create(); 
		creator.setCoreName(coreName);
		creator.setConfigSet("data_driven_schema_configs");
		creator.process(client);
	
		String baseURL = client.getBaseURL();
		System.out.println("base url:"+ baseURL);
		
		client.setBaseURL(solrUrl+ coreName);
		
		Collection<SolrInputDocument> docs = new ArrayList<>();
		SolrInputDocument doc1 = new SolrInputDocument();
		doc1.addField("id",  12 );
		doc1.addField("n", "XXBC");
		doc1.addField("r", "HGNC:MK1");
		doc1.addField("r", "MK1");
		doc1.addField("a", "MKX");
		doc1.addField("a", "MK2");
		
		docs.add(doc1);
		
		
		SolrInputDocument doc2 = new SolrInputDocument();
		doc2.addField("id",  22 );
		doc2.addField("n", "protein");
		doc2.addField("r", "UniProt:Q6FGS5");
		doc2.addField("r", "Q6FGS5");
		doc2.addField("a", "Ensembl:ENSP00000254661");
		doc2.addField("a", "ENSP00000254661");
		doc2.addField("a", "HGNC Symbol:RAMP1");
		doc2.addField("a", "RAMP1");
		
		docs.add(doc2);
		
		
		client.add(docs);
		
		client.commit();
		
		SolrQuery solrQuery = new SolrQuery();
		
		solrQuery.setQuery("protein").setFields("id");
		QueryResponse rsp = client.query(solrQuery);
		
		SolrDocumentList  dds = rsp.getResults();
		
		for ( SolrDocument d : dds) {
			System.out.println(d.get("id"));
		}
		
		client.setBaseURL(baseURL);
		
	//	foo = CoreAdminRequest.unloadCore(coreName, client);
//		System.out.println(foo);
	}
	
	
	@Test
	public void test() throws NdexException {
		
       RawCitation c1 = new RawCitation ("title1", "pubmed", "1001", null);
       RawCitation c2 = new RawCitation ("title1", "pubmed", "1001", null);
       RawCitation c3 = new RawCitation ("title2", "pubmed", "1001", null);
       RawCitation c4 = new RawCitation ("title2", "pubmed", "1002", null);
       

       RawCitation c5 = new RawCitation ("", null, "1001", null);

       
       assertEquals ( c1.compareTo(c2), 0);
       assertEquals ( c2.compareTo(c1), 0);
       
       assertEquals ( c2.compareTo(c3), 0);
       assertEquals ( c3.compareTo(c2), 0);
       assertEquals ( c3.compareTo(c4), -1);
       assertEquals ( c4.compareTo(c3), 1);
       assertEquals ( c1.compareTo(c5), 1);
       assertEquals ( c5.compareTo(c1), -1);
       
       assertEquals ( c1.equals(c2), true);
       assertEquals ( c2.equals(c1), true);
       assertEquals ( c1.equals(c3), true);
       assertEquals ( c3.equals(c1), true);
       
       assertEquals ( c2.equals(c4), false);
       assertEquals ( c4.equals(c1), false);
       
	}

}
