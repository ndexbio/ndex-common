package org.ndexbio.common.persistence.orientdb;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.NetworkSummary;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public abstract class PersistenceService {

	//TODO: turn this into configuration property
	private static final long CACHE_SIZE = 200000L;

	protected NdexDatabase database;

	protected LoadingCache<Long, ODocument>  elementIdCache;
    
	private Map<String, Namespace> prefixMap;
	private Map<RawNamespace, Namespace> namespaceMap;
    private Map<String, Namespace> URINamespaceMap;

	private Map<String, Long> baseTermStrMap;

	protected OrientVertex networkVertex;

	protected NetworkDAO  networkDAO;
	
	protected NetworkSummary network;
	
    protected Logger logger ;

    protected OrientGraph graph;
	protected ODatabaseDocumentTx  localConnection;  //all DML will be in this connection, in one transaction.

    public PersistenceService(NdexDatabase db) {
		this.database = db;
		this.localConnection = this.database.getAConnection();
		this.graph = new OrientGraph(this.localConnection,false);
		
		this.networkDAO = new NetworkDAO(localConnection,true);

		this.baseTermStrMap = new TreeMap <>();
		prefixMap = new HashMap<>();
		this.namespaceMap   = new TreeMap <>();
		URINamespaceMap = new HashMap<>();

		this.elementIdCache = CacheBuilder
				.newBuilder().maximumSize(CACHE_SIZE*5)
				.expireAfterAccess(240L, TimeUnit.MINUTES)
				.build(new CacheLoader<Long, ODocument>() {
				   @Override
				   public ODocument load(Long key) throws NdexException, ExecutionException {
//					   logger.info("Element Id loading cache loading element " + key + " from db .");
					   ODocument o = networkDAO.getDocumentByElementId(key);
                    if ( o == null )
                    	throw new NdexException ("Document is not found for element id: " + key);
					return o;
				   }
			    });

    }
    

	protected void addPropertiesToVertex (OrientVertex vertex, Collection<NdexPropertyValuePair> properties, 
			Collection<SimplePropertyValuePair> presentationProperties) throws NdexException, ExecutionException {

		if ( properties != null) {
			for (NdexPropertyValuePair e : properties) {
				OrientVertex pV = this.createNdexPropertyVertex(e);
               vertex.addEdge(NdexClasses.E_ndexProperties, pV);
			}
		
		}

		if ( presentationProperties !=null ) {
			for (SimplePropertyValuePair e : presentationProperties) {
				ODocument pDoc = this.createSimplePropertyDoc(e.getName(),e.getValue());
               OrientVertex pV = graph.getVertex(pDoc);
               vertex.addEdge(NdexClasses.E_ndexPresentationProps, pV);
			}
		}
	}


	 protected OrientVertex createNdexPropertyVertex(NdexPropertyValuePair e) throws NdexException, ExecutionException {
		 Long baseTermId = this.getBaseTermId(e.getPredicateString());
		 ODocument btDoc = this.elementIdCache.get(baseTermId);
		 OrientVertex btV = graph.getVertex(btDoc);
		 
 		 ODocument pDoc = new ODocument(NdexClasses.NdexProperty)
				.fields(//NdexClasses.ndexProp_P_predicateStr,key,
						NdexClasses.ndexProp_P_value, e.getValue(),
						NdexClasses.ndexProp_P_datatype, e.getDataType())
			   .save();
 		
 		 OrientVertex pV = graph.getVertex(pDoc);
 		 pV.addEdge(NdexClasses.ndexProp_E_predicate, btV);
 		 e.setPredicateId(baseTermId);
 		 this.elementIdCache.put(baseTermId, btV.getRecord());
 		 return pV;
		}

	 protected ODocument createSimplePropertyDoc(String key, String value) {
			ODocument pDoc = new ODocument(NdexClasses.SimpleProperty)
				.fields(NdexClasses.SimpleProp_P_name,key,
						NdexClasses.SimpleProp_P_value, value)
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
		
	public Long createNamespace ( String prefix, String URI) throws NdexException {
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
		
	public Namespace findOrCreateNamespace(RawNamespace key) throws NdexException {
		Namespace ns = namespaceMap.get(key);

		if ( ns != null ) {
	        // check if namespace definitions are consistent
			if (key.getPrefix() !=null && key.getURI() !=null && 
	          		 !ns.getUri().equals(key.getURI()))
	          	   throw new NdexException("Namespace conflict: prefix " 
	          		       + key.getPrefix() + " maps to  " + 
	          			   ns.getUri() + " and " + key.getURI());

	        return ns;
		}
		
		if ( key.getPrefix() !=null && key.getURI() == null )
			throw new NdexException ("Prefix " + key.getPrefix() + " is not defined." );
		
		// persist the Namespace in db.
		ns = new Namespace();
		ns.setPrefix(key.getPrefix());
		ns.setUri(key.getURI());
		ns.setId(database.getNextId());
		

		ODocument nsDoc = new ODocument(NdexClasses.Namespace);
		nsDoc = nsDoc.field("prefix", key.getPrefix())
		  .field("uri", ns.getUri())
		  .field("id", ns.getId())
		  .save();
		
        
		OrientVertex nsV = graph.getVertex(nsDoc);
		networkVertex.addEdge(NdexClasses.Network_E_Namespace, nsV);
		
		if (ns.getPrefix() != null) 
			prefixMap.put(ns.getPrefix(), ns);
		
		if ( ns.getUri() != null) 
			URINamespaceMap.put(ns.getUri(), ns);
		
		elementIdCache.put(ns.getId(),nsDoc);
		namespaceMap.put(key, ns);
		return ns; 
		
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
	        elementIdCache.put(termId, basetermV.getRecord());
			return termId;
	 }

	 
	 protected Long createCitation(String title, String idType, String identifier, 
				List<String> contributors, 
				Collection<NdexPropertyValuePair> properties, Collection<SimplePropertyValuePair> presentationProperties) throws NdexException, ExecutionException {
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
			elementIdCache.put(citationId, citationV.getRecord());
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

			elementIdCache.put(supportId, supportV.getRecord());
			elementIdCache.put(citationId, citationV.getRecord());
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
		    
	        //add link to the network vertex
	        this.networkVertex.addEdge(NdexClasses.Network_E_FunctionTerms, fTermV);
	        
	        elementIdCache.put(functionTermId, fTerm);
	        return functionTermId;
		}

		/**
		 * Find or create a base term from a string, and return its identifier.
		 * @param termString
		 * @return
		 * @throws NdexException
		 * @throws ExecutionException
		 */
		public Long getBaseTermId(String termStringRaw) throws NdexException, ExecutionException {
			
	        String termString = termStringRaw;		
			if ( termStringRaw.length() > 8 && termStringRaw.substring(0, 7).equalsIgnoreCase("http://") ) {
		  		  try {
					URI termStringURI = new URI(termStringRaw);
					String fragment = termStringURI.getFragment();
					
				    String prefix;
				    if ( fragment == null ) {
						    String path = termStringURI.getPath();
						    if (path != null && path.indexOf("/") != -1) {
							   fragment = path.substring(path.lastIndexOf('/') + 1);
							   prefix = termStringRaw.substring(0,
									termStringRaw.lastIndexOf('/') + 1);
						    } else
						       throw new NdexException ("Unsupported URI format in term: " + termStringRaw);
				    } else {
						    prefix = termStringURI.getScheme()+":"+termStringURI.getSchemeSpecificPart()+"#";
				    }
		                 
				    Namespace ns = this.URINamespaceMap.get(prefix);
				    if ( ns != null && ns.getPrefix() != null) {
				    	termString =  ns.getPrefix() + ":" + fragment;
				    }
				  } catch (URISyntaxException e) {
					// ignore and move on to next case
				  }
			}		
			
			Long termId = this.baseTermStrMap.get(termString);
			if ( termId != null) {
				return termId;
			}
		    return this.createBaseTerm(termString);	
		}
		
		public Long getBaseTermId (  String prefix, String localTerm) throws ExecutionException {
			Long termId = this.baseTermStrMap.get(prefix+":"+localTerm);
			if ( termId != null) {
				return termId;
			}
		    return this.createBaseTerm(prefix,localTerm);	
		}
			
		
		private Long createBaseTerm(String termString) throws NdexException, ExecutionException {
			// case 1 : termString is a URI
			// example: http://identifiers.org/uniprot/P19838
			// treat the last element in the URI as the identifier and the rest as
			// the namespace URI
			// find or create the namespace based on the URI
			// when creating, set the prefix based on the PREFIX-URI table for known
			// namespaces, otherwise do not set.
			//
			if ( termString.length() > 8 && termString.substring(0, 7).equalsIgnoreCase("http://") ) {
	  		  try {
				URI termStringURI = new URI(termString);
//				String scheme = termStringURI.getScheme();
					String fragment = termStringURI.getFragment();
				
				    String prefix;
				    if ( fragment == null ) {
					    String path = termStringURI.getPath();
					    if (path != null && path.indexOf("/") != -1) {
						   fragment = path.substring(path.lastIndexOf('/') + 1);
						   prefix = termString.substring(0,
								termString.lastIndexOf('/') + 1);
					    } else
					       throw new NdexException ("Unsupported URI format in term: " + termString);
				    } else {
					    prefix = termStringURI.getScheme()+":"+termStringURI.getSchemeSpecificPart()+"#";
				    }
	                 
				    RawNamespace rns = new RawNamespace(null, prefix);
				    Namespace namespace = getNamespace(rns);
				
				    // create baseTerm in db
				    Long id = createBaseTerm(fragment, namespace.getId());
			        this.baseTermStrMap.put(termString, id);
			        return id;
			  } catch (URISyntaxException e) {
				// ignore and move on to next case
			  }
			}
			// case 2: termString is of the form NamespacePrefix:Identifier
			// find or create the namespace based on the prefix
			// when creating, set the URI based on the PREFIX-URI table for known
			// namespaces, otherwise do not set.
			//
			String[] termStringComponents = termString.split(":");
			if (termStringComponents != null && termStringComponents.length == 2) {
				String identifier = termStringComponents[1];
				String prefix = termStringComponents[0];
				Namespace namespace = prefixMap.get(prefix);
				
				if ( namespace == null) {
					namespace = createLocalNamespaceforPrefix(prefix);
					logger.warning("Prefix '" + prefix + "' is not defined in the network. URI "+
					namespace.getUri()	+ " has been created for it by Ndex." );
				}
				
				// create baseTerm in db
				Long id= createBaseTerm(identifier, namespace.getId());
		        this.baseTermStrMap.put(termString, id);
		        return id;

			}

			// case 3: termString cannot be parsed, use it as the identifier.
			// find or create the namespace for prefix "LOCAL" and use that as the
			// namespace.

				// create baseTerm in db
	    	Long id = createBaseTerm(termString, -1);
	        this.baseTermStrMap.put(termString, id);
	        return id;
			
		}
	
		
		private Long createBaseTerm (String prefix, String localName) throws ExecutionException {
			Namespace namespace = this.prefixMap.get(prefix);
			Long id= createBaseTerm(localName, namespace.getId());
	        this.baseTermStrMap.put(prefix+":"+localName, id);
	        return id;
		}
		
		private Namespace createLocalNamespaceforPrefix (String prefix) throws NdexException {
			String urlprefix = prefix.replace(' ', '_');
			return findOrCreateNamespace(
					new RawNamespace(prefix, "http://uri.ndexbio.org/ns/"+this.network.getExternalId()
							+"/" + urlprefix + "/"));
		}

		/**
		 * Find or create a namespace object from database;
		 * @param rns
		 * @return
		 * @throws NdexException
		 */
		public Namespace getNamespace(RawNamespace rns) throws NdexException {
			if (rns.getPrefix() == null) {
				Namespace ns = URINamespaceMap.get(rns.getURI());
				if ( ns != null ) {
					return ns; 
				}
			}
			
			if (rns.getURI() == null) {
				Namespace ns = this.prefixMap.get(rns.getPrefix()); 
				if ( ns != null ) {
					return ns; 
				}
			}
			
			return findOrCreateNamespace(rns);
		}
		
		
	  public void close () {
		  this.localConnection.close();
		  this.database.close();
	  }
}
