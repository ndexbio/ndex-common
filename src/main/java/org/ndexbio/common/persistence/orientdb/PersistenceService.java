package org.ndexbio.common.persistence.orientdb;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.object.network.RawCitation;
import org.ndexbio.common.models.object.network.RawSupport;
import org.ndexbio.model.object.NdexProperty;
import org.ndexbio.model.object.network.FunctionTerm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public abstract class PersistenceService {

	private static final long CACHE_SIZE =  200000L;

	protected NdexDatabase database;

	protected LoadingCache<Long, ODocument>  elementIdCache;

	protected OrientVertex networkVertex;

	private NetworkDAO  networkDAO;

    protected OrientGraph graph;
	protected ODatabaseDocumentTx  localConnection;  //all DML will be in this connection, in one transaction.

    public PersistenceService(NdexDatabase db) {
		this.database = db;
		this.localConnection = this.database.getAConnection();
		this.graph = new OrientGraph(this.localConnection,false);

		this.networkDAO = new NetworkDAO(localConnection,true);

		elementIdCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE*5)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<Long, ODocument>() {
				   @Override
				   public ODocument load(Long key) throws NdexException, ExecutionException {
					ODocument o = networkDAO.getDocumentByElementId(key);
                    if ( o == null )
                    	throw new NdexException ("Document is not found for element id: " + key);
					return o;
				   }
			    });

    }
    

	protected void addPropertiesToVertex (OrientVertex vertex, Collection<NdexProperty> properties, 
			Collection<NdexProperty> presentationProperties) {

		if ( properties != null) {
			for (NdexProperty e : properties) {
				ODocument pDoc = this.createNdexPropertyDoc(e.getPredicateString(),e.getValue());
				pDoc.field(NdexClasses.ndexProp_P_datatype, e.getDataType())
				.save();
               OrientVertex pV = graph.getVertex(pDoc);
               vertex.addEdge(NdexClasses.E_ndexProperties, pV);
			}
		
		}

		if ( presentationProperties !=null ) {
			for (NdexProperty e : presentationProperties) {
				ODocument pDoc = this.createNdexPropertyDoc(e.getPredicateString(),e.getValue());
				pDoc.field(NdexClasses.ndexProp_P_datatype, e.getDataType())
				.save();
               OrientVertex pV = graph.getVertex(pDoc);
               vertex.addEdge(NdexClasses.E_ndexPresentationProps, pV);
			}
		}
	}


	 private ODocument createNdexPropertyDoc(String key, String value) {
			ODocument pDoc = new ODocument(NdexClasses.NdexProperty)
				.fields(NdexClasses.ndexProp_P_predicateStr,key,
						NdexClasses.ndexProp_P_value, value)
			   .save();
			return pDoc;
		}


	 public void commit () {
			//graph.commit();
			this.localConnection.commit();
	//		this.networkDoc.reload();
	//		this.networkVertex = graph.getVertex(networkDoc);
		//	logger.info("elementIdCachSize:" + elementIdCache.size());
		//	this.localConnection.begin();
		//	database.commit();
		}
		
	protected Long createNamespace ( String prefix, String URI) throws NdexException {
			if ( prefix !=null && URI == null )
			 throw new NdexException ("Prefix " + prefix + " is not defined." );
		
	    	Long nsId = database.getNextId();

	    	ODocument nsDoc = new ODocument(NdexClasses.Namespace)
		      .fields(NdexClasses.ns_P_prefix,prefix,
		    		  NdexClasses.ns_P_uri, URI,
		              NdexClasses.Element_ID, nsId)
		      .save();
		
	    
	    	OrientVertex nsV = graph.getVertex(nsDoc);
	    	networkVertex.addEdge(NdexClasses.Network_E_Namespace, nsV);
	    	
	    	elementIdCache.put(nsId, nsDoc);
		    return nsId;
		}
		

	 protected Long createBaseTerm(String localTerm, long nsId) throws ExecutionException {

			Long termId = database.getNextId();
			
			ODocument btDoc = new ODocument(NdexClasses.BaseTerm)
			  .fields(NdexClasses.BTerm_P_name, localTerm,
					  NdexClasses.Element_ID, termId)
			  .save();

			OrientVertex basetermV = graph.getVertex(btDoc);
			
			if ( nsId >= 0) {

	  		  ODocument nsDoc = elementIdCache.get(nsId); 
	  		  
	  		  OrientVertex nsV = graph.getVertex(nsDoc);
	  		
	  		  basetermV.addEdge(NdexClasses.BTerm_E_Namespace, nsV);
			}
			  
	        networkVertex.addEdge(NdexClasses.Network_E_BaseTerms, basetermV);
	        elementIdCache.put(termId, btDoc);
			return termId;
	 }

	 
	 protected Long createCitation(String title, String idType, String identifier, 
				List<String> contributors, 
				Collection<NdexProperty> properties, Collection<NdexProperty> presentationProperties) {
			Long citationId = database.getNextId();

			ODocument citationDoc = new ODocument(NdexClasses.Citation)
			  .fields(
					NdexClasses.Element_ID, citationId,
			        NdexClasses.Citation_P_title, title,
			        NdexClasses.Citation_p_idType, idType,
			        NdexClasses.Citation_P_identifier, identifier)
			  .field(NdexClasses.Citation_P_contributors, contributors, OType.EMBEDDEDLIST)
			  .save();
	        
			OrientVertex citationV = graph.getVertex(citationDoc);
			networkVertex.addEdge(NdexClasses.Network_E_Citations, citationV);
			this.addPropertiesToVertex(citationV, properties, presentationProperties);
			elementIdCache.put(citationId, citationDoc);
			return citationId; 
		}

	 
	  protected Long createSupport(String literal, Long citationId) throws ExecutionException {
			
			Long supportId =database.getNextId() ;

			ODocument supportDoc = new ODocument(NdexClasses.Support)
			   .fields(NdexClasses.Element_ID, supportId,
			           NdexClasses.Support_P_text, literal)	
			   .save();

			ODocument citationDoc = elementIdCache.get(citationId);
	        
			OrientVertex supportV = graph.getVertex(supportDoc);
			OrientVertex citationV = graph.getVertex(citationDoc);
			supportV.addEdge(NdexClasses.Support_E_citation, citationV);
			networkVertex.addEdge(NdexClasses.Network_E_Supports, supportV);

			elementIdCache.put(supportId, supportDoc);
			return supportId; 
			
		}

		protected Long createFunctionTerm(Long baseTermId, List<Long> termList) throws ExecutionException {
			
			Long functionTermId = database.getNextId(); 
			
		    ODocument fTerm = new ODocument(NdexClasses.FunctionTerm)
		       .field(NdexClasses.Element_ID, functionTermId)
		       .save();
		    
	        OrientVertex fTermV = graph.getVertex(fTerm);
	        
	        ODocument bTermDoc = elementIdCache.get(baseTermId); 
	        fTermV.addEdge(NdexClasses.FunctionTerm_E_baseTerm, graph.getVertex(bTermDoc));
	        
	        for (Long id : termList) {
	        	ODocument o = elementIdCache.get(id);
	        	fTermV.addEdge(NdexClasses.FunctionTerm_E_paramter, graph.getVertex(o));
	        }
		    
	        elementIdCache.put(functionTermId, fTerm);
	        return functionTermId;
		}

}
