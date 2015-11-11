package org.ndexbio.common.models.dao.orientdb;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.cxio.metadata.MetaDataCollection;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.cx.BELNamespaceElement;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.NetworkSourceFormat;

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class SingleNetworkDAO extends BasicNetworkDAO {
		
	public static final String CXsrcFormatAttrName="ndex:sourceFormat";
	protected ODocument networkDoc;
	
	private OrientVertex networkVertex;
	
    protected OrientGraph graph;
    protected String uuid;
    
    
	public SingleNetworkDAO ( String UUID) throws NdexException {
		super();
		uuid = UUID;
		networkDoc = getRecordByUUIDStr(UUID);
		
		graph =  new OrientGraph(db,false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);
		networkVertex = graph.getVertex(networkDoc);
		
		
	}

	
	protected long getVertexCount(String edgeName) {
		return networkVertex.countEdges(Direction.OUT,edgeName);
	}
	
	
    protected Iterable<ODocument> getNetworkElements(String elementEdgeString) {	
    	
    	Object f = networkDoc.field("out_"+ elementEdgeString);
    	
    	if ( f == null) return Helper.emptyDocs;
    	
    	if ( f instanceof ODocument)
    		 return new OrientDBIterableSingleLink((ODocument)f);
    	
    	Iterable<ODocument> iterable = (Iterable<ODocument>)f;
		return iterable;
    	     
    }
	
	public Iterator<Namespace> getNamespaces() {
		return new NamespaceIterator(getNetworkElements(NdexClasses.Network_E_Namespace));
	}
		
	/*
	public Iterable<CitationElement>  getCXCitations () {
		return new CXCitationCollection(getNetworkElements(NdexClasses.Network_E_Citations),db);
	}
	
	*/



    protected String getBaseTermStringById(long id) throws ObjectNotFoundException {
    	ODocument doc = getBasetermDocById(id);
    	return  getBaseTermStringFromDoc(doc);
    	
    }
    
    protected String getBaseTermStringFromDoc(ODocument doc) throws ObjectNotFoundException {
	    String name = doc.field(NdexClasses.BTerm_P_name);
    	
	    String prefix = doc.field(NdexClasses.BTerm_P_prefix);
	    if (prefix !=null)
	    	name = prefix + name;
	    
    	Long nsId = doc.field(NdexClasses.BTerm_NS_ID); 
    	if ( nsId == null || nsId.longValue() <= 0) 
    		return name;
    	
    	ODocument nsdoc = getNamespaceDocById(nsId);
        prefix = nsdoc.field(NdexClasses.ns_P_prefix)	;
    	return prefix + ":"+ name;
    	
    //	return nsdoc.field(NdexClasses.ns_P_uri) + name;
    }
    
	/**
	 * This function check if the given network contains all the give aspects. 
	 * @param aspectNames
	 * @return the aspect list that are not found in this network. if all aspects are found in the given network,
	 * an empty set will be returned.
	 *
	 */
	public Set<String> findMissingAspect ( Collection<String> aspectNames) {
		MetaDataCollection md = networkDoc.field(NdexClasses.Network_P_metadata);
		TreeSet<String> result = new TreeSet<>();
		if ( md !=null) {
			for (String aspect: aspectNames) {
				if ( md.getMetaDataElement(aspect) == null) 
					result.add(aspect);
			}
			return result;
		}
		
		for ( String aspect : aspectNames) {
			if (Arrays.binarySearch(NdexDatabase.NdexSupportedAspects, aspect) ==-1)
				result.add(aspect);
		}
		return result;
	}
	
	
   public NetworkSourceFormat getSourceFormat()    {
	   return NetworkSourceFormat.valueOf((String)networkDoc.field(NdexClasses.Network_P_source_format));
   }

   /**
    *  
    * @param task This function will update the status in the passed in task argument. populate the message attribute in it 
    * if error occurs.
    * @return the status of this task. complete, error etc.
 * @throws  
    */
   public Status attachNamespaceFiles(Task task) {
	   try { 
		   Map<String,String> namespaceFileMap = new TreeMap<>();
		   for (Iterator<Namespace> i = getNamespaces() ; i.hasNext(); ) {
			   Namespace ns = i.next();
		   
			   URL link = new URL(ns.getUri());
			   InputStream in = new BufferedInputStream(link.openStream());
			   String inputStreamString = new Scanner(in,"UTF-8").useDelimiter("\\A").next();
			   namespaceFileMap.put(ns.getPrefix(),inputStreamString)	;
			   in.close();
		   }
		   
		   for ( Map.Entry<String, String > entry : namespaceFileMap.entrySet()) {
			   ODocument doc = new ODocument ( NdexClasses.OpaqueElement)
					   .fields(NdexClasses.BELPrefix, entry.getKey(),
							   NdexClasses.BELNamespaceFileContent, entry.getValue()).save();
			   networkVertex.addEdge(BELNamespaceElement.ASPECT_NAME, graph.getVertex(doc));
		   }
			
		   task.setStatus(Status.COMPLETED);
	   } catch (MalformedURLException e) {
		   task.setMessage("Malformed URL found in namespace: " + e.getMessage());
		   task.setStatus(Status.FAILED);
		   
	   } catch (IOException e) {
		   task.setMessage("IOExeception when downloading namespace file. " + e.getMessage());
		   
		   task.setStatus(Status.FAILED);
		   
	   }
	return task.getStatus();
   }
   

   public void commit () {
	   this.graph.commit();
   }
}
