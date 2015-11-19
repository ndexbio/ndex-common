package org.ndexbio.common.solr;

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
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.ndexbio.model.exceptions.NdexException;

public class SingleNetworkSolrIdxManager {

	private String solrUrl = "http://localhost:8983/solr";
	
	private String coreName; 
	private HttpSolrClient client;
	
	static private final  int batchSize = 2000;
	private int counter ; 
	private Collection<SolrInputDocument> docs ;
	
	public static final String ID = "id";
	private static final String NAME = "name";
	private static final String REPRESENTS = "represents";
	private static final String ALIAS= "alias";
	private static final String RELATEDTO = "relatedTo";
		
	public SingleNetworkSolrIdxManager(String networkUUID) {
		coreName = networkUUID;
		client = new HttpSolrClient(solrUrl);
	}
	
	public SolrDocumentList getNodeIdsByQuery(String query, int limit) throws SolrServerException, IOException {
		client.setBaseURL(solrUrl+ "/" + coreName);

		SolrQuery solrQuery = new SolrQuery();
		
		solrQuery.setQuery(query).setFields(ID);
		solrQuery.setStart(0);
		solrQuery.setRows(limit);
		QueryResponse rsp = client.query(solrQuery);
		
		SolrDocumentList  dds = rsp.getResults();
		
		return dds;
		
	}
	
	public void createIndex() throws SolrServerException, IOException, NdexException {
		CoreAdminRequest.Create creator = new CoreAdminRequest.Create(); 
		creator.setCoreName(coreName);
		creator.setConfigSet("data_driven_schema_configs");
		CoreAdminResponse foo = creator.process(client);	
			
		if ( foo.getStatus() != 0 ) {
			throw new NdexException ("Failed to create solrIndex for network " + coreName + ". Error: " + foo.getResponseHeader().toString());
		}
		counter = 0;
		docs = new ArrayList<>(batchSize);
		
		client.setBaseURL(solrUrl + "/" + coreName);
	}
	
	public void dropIndex() throws SolrServerException, IOException {
		client.setBaseURL(solrUrl);
		CoreAdminRequest.unloadCore(coreName, true, true, client);
	}
	
	public void addNodeIndex(long id, String name, List<String> represents, List<String> alias, List<String> relatedTerms) throws SolrServerException, IOException {
		
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField(ID,  id );
		
		if ( name != null ) 
			doc.addField(NAME, name);
		if ( represents !=null && !represents.isEmpty())
			doc.addField(REPRESENTS, represents);
		if ( alias !=null && !alias.isEmpty())
			doc.addField(ALIAS, alias);
		if ( relatedTerms !=null && ! relatedTerms.isEmpty() ) 
			doc.addField(RELATEDTO, relatedTerms);
		
		docs.add(doc);
		
		counter ++;
		if ( counter == batchSize) {
			client.add(docs);
			client.commit();
			docs.clear();
			counter = 0;
		}

	}

	public void commit() throws SolrServerException, IOException {
		client.add(docs);
		client.commit();
	}
}
