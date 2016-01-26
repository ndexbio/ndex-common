package org.ndexbio.common.solr;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.task.Configuration;

public class NetworkGlobalIndexManager {

	private String solrUrl ;
	
	private static final String coreName = 
		//	"26fe0261-baeb-11e5-915b-c231b72caca9";
			"ndex-networks" ; 
	private HttpSolrClient client;
	
	private SolrInputDocument doc ;
	
	public static final String UUID = "uuid";
	private static final String NAME = "name";
	private static final String DESC = "description";
	private static final String VERSION = "version";
	private static final String USER_READ= "userRead";
	private static final String USER_EDIT = "userEdit";
	private static final String USER_ADMIN = "userAdmin";
	private static final String GRP_READ = "grpRead";
	private static final String GRP_EDIT = "grpEdit";
	private static final String GRP_ADMIN = "grpAdmin";
	
	private static final String VISIBILITY = "visibility";
	
	private static final String EDGE_COUNT = "EdgeCount";
	private static final String NODE_COUNT = "NodeCount";
	private static final String CREATION_TIME = "CreationTime";
	private static final String MODIFICATION_TIME = "ModificationTime";
	
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
	
	public SolrDocumentList searchForNetworks (String searchTerms, String userAccount, int limit, int offset, String adminedBy, Permissions permission, boolean canReadOnly,
			   List<String> groupNames) 
			throws SolrServerException, IOException {
		client.setBaseURL(solrUrl+ "/" + coreName);

		SolrQuery solrQuery = new SolrQuery();
		
		//create the result filter
		String visibilityFilter = canReadOnly? 
				(VISIBILITY + ":PUBLIC") :( "(NOT " + VISIBILITY + ":PRIVATE)");
		
		String adminFilter = "";		
		if ( adminedBy !=null) {
			adminFilter = " AND (" + USER_ADMIN + ":" + adminedBy + " OR " + GRP_ADMIN + ":" + adminedBy + ")";
		}
		
		String resultFilter = "";
		if ( userAccount !=null) {     // has a signed in user.
			if ( permission == Permissions.ADMIN)  {
				resultFilter =  USER_ADMIN + ":" + userAccount;
			    if ( groupNames!=null ) {
			    	for ( String grpName : groupNames)
			    	  resultFilter  +=  " OR " + GRP_ADMIN +":" + grpName ;
			    	resultFilter = "(" + resultFilter + ")";
			    }	
			    resultFilter = resultFilter + adminFilter;
			} else if ( permission == Permissions.READ) {
				resultFilter =  USER_ADMIN + ":" + userAccount + " OR " +
						USER_EDIT + ":" + userAccount + " OR "+ USER_READ + ":" + userAccount;
				if ( groupNames!=null) {
					for (String groupName : groupNames) {
					  resultFilter +=  " OR " + GRP_ADMIN + ":" + groupName + " OR " +
							  GRP_EDIT + ":" + groupName + " OR "+ GRP_READ + ":" + groupName;
					}
				}
				resultFilter = "(" + visibilityFilter + " OR " + resultFilter + ")" + adminFilter;
			} else if ( permission == Permissions.WRITE) {
				resultFilter =  USER_ADMIN + ":" + userAccount + " OR " +
						USER_EDIT + ":" + userAccount ;
				if ( groupNames !=null) {
					for ( String groupName : groupNames ) 
						resultFilter += " OR " + GRP_ADMIN + ":" + groupName + " OR " +
							GRP_EDIT + ":" + groupName ;
				} 
				resultFilter = "(" + resultFilter + ")" + adminFilter;
			}
		}  else {
			resultFilter = visibilityFilter + adminFilter;
		}
			
			
		solrQuery.setQuery(searchTerms).setFields(UUID);
		if ( offset >=0)
		  solrQuery.setStart(offset);
		if ( limit >0 )
			solrQuery.setRows(limit);
		
		solrQuery.setFilterQueries(resultFilter) ;
		
		QueryResponse rsp = client.query(solrQuery);		
			
		SolrDocumentList  dds = rsp.getResults();
		
		return dds;	
		
	}
	
	
	
	public void createIndexDocFromSummary(NetworkSummary summary) throws SolrServerException, IOException, NdexException {
		client.setBaseURL(solrUrl + "/" + coreName);
		doc = new SolrInputDocument();
	
		doc.addField(UUID,  summary.getExternalId().toString() );
		doc.addField(EDGE_COUNT, summary.getEdgeCount());
		doc.addField(NODE_COUNT, summary.getNodeCount());
		doc.addField(VISIBILITY, summary.getVisibility().toString());
		
		doc.addField(CREATION_TIME, summary.getCreationTime());
		doc.addField(MODIFICATION_TIME, summary.getModificationTime());

		if ( summary.getName() != null ) 
			doc.addField(NAME, summary.getName());
		if ( summary.getDescription() !=null)
			doc.addField(DESC, summary.getDescription());
		if ( summary.getVersion() !=null)
			doc.addField(VERSION, summary.getVersion());
		
		
		
		// dynamic fields from property table.
		
		try (NetworkDAO dao = new NetworkDAO()) {
			Map<String,Map<Permissions, Set<String>>> members = dao.getAllMembershipsOnNetwork(summary.getExternalId().toString());
			doc.addField(USER_READ, members.get(NdexClasses.User).get(Permissions.READ));
			doc.addField(USER_EDIT, members.get(NdexClasses.User).get(Permissions.WRITE));
			doc.addField(USER_ADMIN, members.get(NdexClasses.User).get(Permissions.ADMIN));
			doc.addField(GRP_ADMIN, members.get(NdexClasses.Group).get(Permissions.ADMIN));
			doc.addField(GRP_READ, members.get(NdexClasses.Group).get(Permissions.READ));
			doc.addField(GRP_EDIT, members.get(NdexClasses.Group).get(Permissions.WRITE));
		}

	}

	
	
	public void deleteNetwork(String networkId) throws SolrServerException, IOException {
		client.setBaseURL(solrUrl + "/" + coreName);
		client.deleteById(networkId);
		client.commit();
	}
	
	public void commit () throws SolrServerException, IOException {
		Collection<SolrInputDocument> docs = new ArrayList<>(1);
		docs.add(doc);
		client.add(docs);
		client.commit();
		docs.clear();
		doc = null;

	}
	
	
	public void updateNetworkProfile(String networkId, Map<String,Object> table) throws SolrServerException, IOException {
		client.setBaseURL(solrUrl + "/" + coreName);
		SolrInputDocument tmpdoc = new SolrInputDocument();
		tmpdoc.addField(UUID, networkId);
		 
		String newTitle = (String)table.get(NdexClasses.Network_P_name); 
		if ( newTitle !=null) {
			Map<String,String> cmd = new HashMap<>();
			cmd.put("set", newTitle);
			tmpdoc.addField(NAME, cmd);
		}
		
		String newDesc =(String) table.get(NdexClasses.Network_P_desc);
		if ( newDesc != null) {
			Map<String,String> cmd = new HashMap<>();
			cmd.put("set", newDesc);
			tmpdoc.addField(DESC, cmd);
		}
		
		String newVersion = (String)table.get(NdexClasses.Network_P_version);
		if ( newVersion !=null) {
			Map<String,String> cmd = new HashMap<>();
			cmd.put("set", newVersion);
			tmpdoc.addField(VERSION, cmd);
		}
		
		if ( table.get(NetworkDAO.RESET_MOD_TIME)!=null) {
			Map<String,Timestamp> cmd = new HashMap<>();
			java.util.Date now = Calendar.getInstance().getTime();
			cmd.put("set",  new java.sql.Timestamp(now.getTime()));
			tmpdoc.addField(MODIFICATION_TIME, cmd);
		}
		
		VisibilityType  vt = (VisibilityType)table.get(NdexClasses.Network_P_visibility);		
		if ( vt!=null) {
			Map<String,String> cmd = new HashMap<>();
			cmd.put("set",  vt.toString());
			tmpdoc.addField(VISIBILITY, cmd);
		}
		
		Collection<SolrInputDocument> docs = new ArrayList<>(1);
		docs.add(tmpdoc);
		client.add(docs);
		client.commit();

	}
	
	public void revokeNetworkPermission(String networkId, String accountName, Permissions p, boolean isUser) 
			throws NdexException, SolrServerException, IOException {
		client.setBaseURL(solrUrl + "/" + coreName);
		SolrInputDocument tmpdoc = new SolrInputDocument();
		tmpdoc.addField(UUID, networkId);
		 
		Map<String,String> cmd = new HashMap<>();
		cmd.put("remove", accountName);

		switch ( p) {
		case ADMIN : 
			tmpdoc.addField( isUser? USER_ADMIN: GRP_ADMIN, cmd);
			break;
		case WRITE:
			tmpdoc.addField( isUser? USER_EDIT: GRP_EDIT, cmd);
			break;
		case READ:
			tmpdoc.addField( isUser? USER_READ: GRP_READ, cmd);
			break;
			
		default: 
			throw new NdexException ("Invalid permission type " + p + " received in network previlege revoke.");
		}
		
		Collection<SolrInputDocument> docs = new ArrayList<>(1);
		docs.add(tmpdoc);
		client.add(docs);
		client.commit();

	}
	
	public void grantNetworkPermission(String networkId, String accountName, Permissions newPermission, 
			 Permissions oldPermission, boolean isUser) 
			throws NdexException, SolrServerException, IOException {
		client.setBaseURL(solrUrl + "/" + coreName);
		SolrInputDocument tmpdoc = new SolrInputDocument();
		tmpdoc.addField(UUID, networkId);
		 
		Map<String,String> cmd = new HashMap<>();
		cmd.put("add", accountName);

		switch ( newPermission) {
		case ADMIN : 
			tmpdoc.addField( isUser? USER_ADMIN: GRP_ADMIN, cmd);
			break;
		case WRITE:
			tmpdoc.addField( isUser? USER_EDIT: GRP_EDIT, cmd);
			break;
		case READ:
			tmpdoc.addField( isUser? USER_READ: GRP_READ, cmd);
			break;		
		default: 
			throw new NdexException ("Invalid permission type " + newPermission
					+ " received in network previlege revoke.");
		}
		
		if ( oldPermission !=null ) {
			Map<String,String> rmCmd = new HashMap<>();
			rmCmd.put("remove", accountName);

			switch ( oldPermission) {
			case ADMIN : 
				tmpdoc.addField( isUser? USER_ADMIN: GRP_ADMIN, rmCmd);
				break;
			case WRITE:
				tmpdoc.addField( isUser? USER_EDIT: GRP_EDIT, rmCmd);
				break;
			case READ:
				tmpdoc.addField( isUser? USER_READ: GRP_READ, rmCmd);
				break;
				
			default: 
				throw new NdexException ("Invalid permission type " + oldPermission + " received in network previlege revoke.");
			}
		}
		
		Collection<SolrInputDocument> docs = new ArrayList<>(1);
		docs.add(tmpdoc);
		client.add(docs);
		client.commit();

	}
}
