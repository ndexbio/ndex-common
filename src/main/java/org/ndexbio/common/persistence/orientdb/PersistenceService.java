package org.ndexbio.common.persistence.orientdb;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.util.TermUtilities;
import org.ndexbio.model.exceptions.NdexException;
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
    
	// prefix to namespace mapping
	private Map<String, Namespace> prefixMap;
	private Map<RawNamespace, Namespace> namespaceMap;
    private Map<String, Namespace> URINamespaceMap;

	private Map<String, Long> baseTermStrMap;

	protected OrientVertex networkVertex;

	protected NetworkDAO  networkDAO;

	protected ODocument networkDoc;

	protected NetworkSummary network;
	
    protected Logger logger ;

    protected OrientGraph graph;
	protected ODatabaseDocumentTx  localConnection;  //all DML will be in this connection, in one transaction.
	
	 // prefix to URI mapping
	 private static final Map<String, String> defaultNSMap;

	 // URI to prefix mapping
	 private static final Map<String, String> reverseNSMap;

	 static {
	        Map<String, String> aMap = new TreeMap<>();
	        
	        // fore the repetitive prefix entries, the first record will be used as the default namespace in 
	        // the reverse lookup.

			aMap.put("biogrid", 	"http://identifiers.org/biogrid/");
		 	
			aMap.put("CAS",			"http://identifiers.org/cas/");

			aMap.put("CAZY",		"http://identifiers.org/cazy/");

			aMap.put("CHEBI",		"http://identifiers.org/chebi/");
			aMap.put("ChEBI",		"http://identifiers.org/chebi/");
			
			aMap.put("CHEMBL",		"http://identifiers.org/chembl.compound/");
			
			aMap.put("DRUGBANK",	"http://www.drugbank.ca/drugs/");
			
			aMap.put("ENA",			"http://identifiers.org/ena.embl/");
			
			aMap.put("Ensembl", 	"http://identifiers.org/ensembl/");
			aMap.put("ENSEMBL", 	"http://identifiers.org/ensembl/");
			aMap.put("ensembl", 	"http://identifiers.org/ensembl/");

			aMap.put("GENE ONTOLOGY",		"http://identifiers.org/go/");
			aMap.put("Gene Ontology",		"http://identifiers.org/go/");

			aMap.put("GENPEPT",				"http://www.ncbi.nlm.nih.gov/protein/");

			aMap.put("HGNC",		"http://identifiers.org/hgnc/");
			aMap.put("hgnc",		"http://identifiers.org/hgnc/");

			aMap.put("HGNC Symbol",	"http://identifiers.org/hgnc.symbol/");
			aMap.put("HGNC SYMBOL",	"http://identifiers.org/hgnc.symbol/");

			aMap.put("hprd",		"http://identifiers.org/hprd/");
			
			
			aMap.put("InChIKey",	"http://identifiers.org/inchikey/");

			aMap.put("intact",		"http://identifiers.org/intact/");
			
			aMap.put("interpro",	"http://identifiers.org/interpro/");
			aMap.put("InterPro",	"http://identifiers.org/interpro/");

			aMap.put("KEGG COMPOUND",	"http://identifiers.org/kegg.compound/");
			aMap.put("KEGG GLYCAN",		"http://identifiers.org/kegg.glycan/");
			aMap.put("KEGG DRUG",		"http://identifiers.org/kegg.drug/");
			
			aMap.put("MOLECULAR MODELING DATABASE",		"http://identifiers.org/mmdb/");
			
			aMap.put("NCBI Gene","http://identifiers.org/ncbigene/");
			aMap.put("NCBI GENE","http://identifiers.org/ncbigene/");
			
			aMap.put("Pubmed",	"http://www.ncbi.nlm.nih.gov/pubmed/");
			aMap.put("Reactome",	"http://identifiers.org/reactome/");
			aMap.put("reactome",	"http://identifiers.org/reactome/");
			
			aMap.put("RefSeq",	"http://identifiers.org/refseq/");
			aMap.put("REFSEQ",	"http://identifiers.org/refseq/");
			aMap.put("refseq",	"http://identifiers.org/refseq/");
			
			aMap.put("pubchem-substance","http://identifiers.org/pubchem.substance/");
			
			aMap.put("pubchem",				"http://identifiers.org/pubchem.compound/");
			aMap.put("PUBCHEM-COMPOUND",	"http://identifiers.org/pubchem.compound/");

			aMap.put("omim",		"http://identifiers.org/omim/");

			aMap.put("PROTEIN DATA BANK", "http://identifiers.org/pdb/");
			aMap.put("Protein Data Bank", "http://identifiers.org/pdb/");
			
			aMap.put("Protein Modification Ontology", "http://identifiers.org/psimod/");
			
			
			aMap.put("Panther Family", 		"http://identifiers.org/panther.family/");
			aMap.put("PANTHER Family", 		"http://identifiers.org/panther.family/");
			aMap.put("Panther", 			"http://identifiers.org/panther.family/");

			aMap.put("Pfam", 		"http://identifiers.org/pfam/");
			
			aMap.put("PHARMGKB DRUG", 		"http://identifiers.org/pharmgkb.drug/");
			
			aMap.put("RAT GENOME DATABASE", 		"http://identifiers.org/rgd/");
			
			aMap.put("Smart", 		"http://identifiers.org/smart/");
			
			aMap.put("Taxonomy", 		"http://identifiers.org/taxonomy/");

			aMap.put("UniProt", 			"http://identifiers.org/uniprot/");
			aMap.put("UNIPROT", 			"http://identifiers.org/uniprot/");
			aMap.put("UniProt Isoform",		"http://identifiers.org/uniprot.isoform/");
			
			aMap.put("UniProt Knowledgebase",		"http://identifiers.org/uniprot/");
			aMap.put("UNIPROT KNOWLEDGEBASE",		"http://identifiers.org/uniprot/");
			
			aMap.put("WIKIPEDIA",		"http://identifiers.org/wikipedia.en/");
			
			aMap.put("nucleotide genbank identifier",	"http://www.ncbi.nlm.nih.gov/nuccore/");
			
			defaultNSMap = Collections.unmodifiableMap(aMap);

			// construct the reverse map.
			Map<String, String> bMap = new TreeMap<>();
			for ( Map.Entry<String, String> e: aMap.entrySet()) {
				if (!bMap.containsKey(e.getValue())) {
					bMap.put(e.getValue(), e.getKey());
				}
			}
			
			reverseNSMap = Collections.unmodifiableMap(bMap);

	    }

    public PersistenceService(NdexDatabase db) throws NdexException {
		this.database = db;
		this.localConnection = this.database.getAConnection();
		this.graph = new OrientGraph(this.localConnection,false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);
		
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
//            this.network.getProperties().addAll(properties);
		
		}

		if ( presentationProperties !=null ) {
			for (SimplePropertyValuePair e : presentationProperties) {
				ODocument pDoc = this.createSimplePropertyDoc(e.getName(),e.getValue());
               OrientVertex pV = graph.getVertex(pDoc);
               vertex.addEdge(NdexClasses.E_ndexPresentationProps, pV);
			}
//			this.network.getPresentationProperties().addAll(presentationProperties);
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
/*		
	private Long createNamespace ( String prefix, String URI) throws NdexException {
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
*/		
	private Namespace findOrCreateNamespace(RawNamespace key) throws NdexException {
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
			  
			networkVertex.getRecord().reload();
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

			OrientVertex supportV = graph.getVertex(supportDoc);

			networkVertex.addEdge(NdexClasses.Network_E_Supports, supportV);

			if ( citationId != null && citationId >= 0 ) {
				ODocument citationDoc = elementIdCache.get(citationId);
	        
				OrientVertex citationV = graph.getVertex(citationDoc);
				supportV.addEdge(NdexClasses.Support_E_citation, citationV);
				elementIdCache.put(citationId, citationV.getRecord());
			}

			supportDoc = supportV.getRecord();
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
					
				    String uriPrefix;
				    if ( fragment == null ) {
						    String path = termStringURI.getPath();
						    if (path != null && path.indexOf("/") != -1) {
							   fragment = path.substring(path.lastIndexOf('/') + 1);
							   uriPrefix = termStringRaw.substring(0,
									termStringRaw.lastIndexOf('/') + 1);
						    } else
						       throw new NdexException ("Unsupported URI format in term: " + termStringRaw);
				    } else {
						    uriPrefix = termStringURI.getScheme()+":"+termStringURI.getSchemeSpecificPart()+"#";
				    }
		                 
				    Namespace ns = this.URINamespaceMap.get(uriPrefix);
				    if ( ns != null ) {
				    	if (ns.getPrefix() != null ) 
				    		termString =  ns.getPrefix() + ":" + fragment;
				    } else { // check if it is a uri we know
				    	String prefix = reverseNSMap.get(uriPrefix);
				    	if ( prefix !=null ) {
				    		// create the namespace and the term.
				    		return createBaseTerm(termString);
				    	}
				    	
				    }
				  } catch (URISyntaxException e) {
					// ignore and move on to next case
				  }
			}		
			
			Long termId = this.baseTermStrMap.get(termString);
			if ( termId != null) {
				return termId;
			}
			
			//check its canonical form
			String[] termStringComponents = TermUtilities.getNdexQName(termString);
			if (termStringComponents != null && termStringComponents.length == 2) {
				String identifier = termStringComponents[1];
				String orgPrefix = termStringComponents[0];
				
				Namespace ns2 = this.prefixMap.get(orgPrefix);  // exists in prefix mapping & map with a different prefix.
				if ( ns2 !=null ) {
					if ( ! orgPrefix.equals(ns2.getPrefix())) {   
						termId = getBaseTermId(ns2.getPrefix() +":"+ identifier); // use the canonical form
				        this.baseTermStrMap.put(termString, termId);
				        return termId;
					} 
				} else {
				
					// consult the default ns mapping
					String newURI = defaultNSMap.get(orgPrefix);
					
				    if ( newURI !=null) {
				    	Namespace ns = this.URINamespaceMap.get(newURI);
				    	if (ns != null && ns.getPrefix() != null) {
				    		this.prefixMap.put(orgPrefix, ns); // add this prefix to mapping
				    		termId = getBaseTermId(ns.getPrefix() +":"+ identifier);
				    		this.baseTermStrMap.put(termString, termId);
				    		return termId;
				    	} 
				    } 
				}
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
	                 
				    RawNamespace rns = new RawNamespace(
				    		(reverseNSMap.containsKey(prefix)? reverseNSMap.get(prefix):null),
				    		prefix);  //prefix value is actually the URI of the namespace.
				    Namespace namespace = getNamespace(rns);
				
				    // create baseTerm in db
				    Long id = createBaseTerm(fragment, namespace.getId());
				    if ( namespace.getPrefix() == null)
				    	this.baseTermStrMap.put(termString, id);
				    else 
				    	this.baseTermStrMap.put(namespace.getPrefix()+ ":"+fragment, id);
			        return id;
			  } catch (URISyntaxException e) {
				// ignore and move on to next case
			  }
			}
			// case 2: termString is of the form (NamespacePrefix:)*Identifier
			// find or create the namespace based on the prefix
			// when creating, set the URI based on the PREFIX-URI table for known
			// namespaces, otherwise do not set.
			//
			
			String[] termStringComponents = TermUtilities.getNdexQName(termString);
			if (termStringComponents != null && termStringComponents.length == 2) {
				String identifier = termStringComponents[1];
				String prefix = termStringComponents[0];
				Namespace namespace = prefixMap.get(prefix);
				
				if ( namespace == null) {
					// check the Ndex namepace lookup table to see of we understand it
					String uri = defaultNSMap.get(prefix);
					
					if ( uri != null) {
						namespace = getNamespace( new RawNamespace(prefix, uri));
					} else {
						namespace = createLocalNamespaceforPrefix(prefix);
						logger.warning("Prefix '" + prefix + "' is not defined in the network. URI "+
								namespace.getUri()	+ " has been created for it by Ndex." );
					}
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
		
		
	  public void setNetworkSourceFormat(NetworkSourceFormat fmt) {
		  this.networkDoc.field(NdexClasses.Network_P_source_format, fmt.toString());
		  NdexPropertyValuePair p = new NdexPropertyValuePair (NdexClasses.Network_P_source_format, fmt.toString());
		  this.network.getProperties().add(p);
	  }
		
	  public void close () {
		  this.localConnection.close();
//		  this.database.close();
	  }
}
