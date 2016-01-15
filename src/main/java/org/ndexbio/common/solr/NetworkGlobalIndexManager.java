package org.ndexbio.common.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.task.Configuration;

public class NetworkGlobalIndexManager {

	private String solrUrl ;
	
	private static final String coreName = 
		//	"26fe0261-baeb-11e5-915b-c231b72caca9";
			"ndex-networks" ; 
	private HttpSolrClient client;
	
	private SolrInputDocument doc ;
	
	private static final String UUID = "uuid";
	private static final String NAME = "name";
	private static final String DESC = "description";
	private static final String VERSION = "version";
	private static final String USER_READ= "userRead";
	private static final String USER_EDIT = "userEdit";
	private static final String EDGE_COUNT = "EdgeCount";
	private static final String CREATION_TIME = "CreationTime";
	
	
	
	public NetworkGlobalIndexManager() throws NdexException {
		// TODO Auto-generated constructor stub
		solrUrl = Configuration.getInstance().getSolrURL();
		client = new HttpSolrClient(solrUrl);
		doc = null;
	}
	
	public void createCoreIfNotExists() throws SolrServerException, IOException, NdexException {
			
		CoreAdminResponse foo = CoreAdminRequest.getStatus(coreName,client);	
		if (foo.getStatus() != 0 ) {
			throw new NdexException ("Failed to get status of solrIndex for " + coreName + ". Error: " + foo.getResponseHeader().toString());
		}
		NamedList<Object> bar = foo.getResponse();
		
		NamedList<Object> st = (NamedList<Object>)bar.get("status");
		
		NamedList<Object> core = (NamedList<Object>)st.get(coreName);
		if ( core.size() == 0 ) {
			System.out.println("Solr core " + coreName + " doesn't exist. Creating it now ....");

			CoreAdminRequest.Create creator = new CoreAdminRequest.Create(); 
			creator.setCoreName(coreName);
			creator.setConfigSet( coreName); 
			foo = creator.process(client);				
			if ( foo.getStatus() != 0 ) {
				throw new NdexException ("Failed to create solrIndex for network " + coreName + ". Error: " + foo.getResponseHeader().toString());
			}
			System.out.println("Done.");		
		}
		else {
			System.out.println("Found core "+ coreName + " in Solr.");	
		}
		
	}
	
	
	public void createIndexDocFromSummary(NetworkSummary summary) throws SolrServerException, IOException, NdexException {
		client.setBaseURL(solrUrl + "/" + coreName);
		doc = new SolrInputDocument();
	
		doc.addField(UUID,  summary.getExternalId().toString() );
		doc.addField(EDGE_COUNT, summary.getEdgeCount());

		if ( summary.getName() != null ) 
			doc.addField(NAME, summary.getName());
		if ( summary.getDescription() !=null)
			doc.addField(DESC, summary.getDescription());
		if ( summary.getVersion() !=null)
			doc.addField(VERSION, summary.getVersion());

		
		// dynamic fields from property table.

	}

	public void commit () throws SolrServerException, IOException {
		Collection<SolrInputDocument> docs = new ArrayList<>(1);
		docs.add(doc);
		client.add(docs);
		client.commit();
		docs.clear();
		doc = null;

	}
}
