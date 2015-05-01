package org.ndexbio.common.exporter;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Term;


public class SIFNetworkExporter {
	
	private Network network;
	
	private Set<String> uniqueNodeNames;
	private Map<Long, String> nodeNameMap;
 	
	
	public SIFNetworkExporter (Network network) {
		this.network = network;
		this.nodeNameMap = new TreeMap<>();
		uniqueNodeNames = new TreeSet<>();
	}

	public void exportNetwork(Writer writer) throws NdexException, IOException {
			for ( Edge edge: network.getEdges().values()) {
				writer.write(getNodeSIFId(edge.getSubjectId()).replace('\t', ' ').replace('\n', ' '));
				writer.write("\t");
				writer.write(getBaseTermName(edge.getPredicateId(),true));
				writer.write("\t");
				writer.write(getNodeSIFId(edge.getObjectId()));
				writer.write("\n");
			}
			writer.flush();
	}


	private String getNodeSIFId(long nodeId) throws NdexException {
	    Node node = network.getNodes().get(nodeId);
	    Long termId = node.getRepresents();
	    if ( termId != null ) {
	    	return getSIFIdFromTerm(termId);
	    }
	    
	    String name = node.getName();
	    if ( name != null) {
	    	return getUniqueNodeName(node);
	    }
	    
	    throw new NdexException ("Node " + nodeId + " has neither represent term nor node name.");
	}
	
	private String getUniqueNodeName (Node node) {
		String name = nodeNameMap.get(node.getId());
		if (name != null)
			return name;
		
		name = node.getName();
		if ( uniqueNodeNames.contains(name)) {   // create suffix to the name
			int i = 1 ;
			while ( uniqueNodeNames.contains(name + "("+i+")")) {
				i++;
			}
			// found the right suffix i.
			name = name + "("+i+")";
		} 
		
		name = name.replace('\t', ' ').replace('\n', ' ');
		//add name to the tables and return the name;
		uniqueNodeNames.add(name);
		nodeNameMap.put(node.getId(), name);
		return name;
	}
	
	
	private String getSIFIdFromTerm(Long termId) throws NdexException {
    	Term termObj = network.getBaseTerms().get(termId);
    	if ( termObj !=null) {  // is base term.
    		return  getBaseTermName(termObj.getId() , true) ;
    	}
    	
    	termObj = network.getFunctionTerms().get(termId);
    	if ( termObj !=null ) {
    		return generateFunctionTermSIFId ((FunctionTerm)termObj);
    	}
    	
    	termObj = network.getReifiedEdgeTerms().get(termId);
    	if ( termObj !=null) {
    		return generateReifiedEdgeSIFId((ReifiedEdgeTerm)termObj);
    	}
    	
    	throw new NdexException ("Term with id " + termId + " is not found in network.");
	}
	
	private String generateFunctionTermSIFId(FunctionTerm term) throws NdexException {
		StringBuffer sb = new StringBuffer();
		sb.append(getBaseTermName(term.getFunctionTermId(),false));
		sb.append("(");
		int i = 0;
		for ( Long termId: term.getParameterIds()) {
			if ( i >0 ) 
				sb.append(",");
			i++;
			sb.append(getSIFIdFromTerm(termId));
		}
		sb.append(")");
		return sb.toString();
	}
	
	private String generateReifiedEdgeSIFId(ReifiedEdgeTerm reTerm) throws NdexException {
		StringBuffer sb = new StringBuffer("EDGE:");
		
		Edge e = network.getEdges().get(reTerm.getEdgeId());
		sb.append("<");
		sb.append(getNodeSIFId(e.getSubjectId()));
		sb.append("><");
		sb.append(getBaseTermName(e.getPredicateId(), true));
		sb.append("><");
		sb.append(getNodeSIFId(e.getObjectId()));
		sb.append(">");
		
		return sb.toString();
	}
	
	private  String getBaseTermName(long bTermId, boolean includeNameSpace) {
		BaseTerm bterm = network.getBaseTerms().get(bTermId);
		if ( includeNameSpace && bterm.getNamespaceId()  > 0) {
			Namespace ns = network.getNamespaces().get(bterm.getNamespaceId());
			if (ns.getPrefix()!=null)
				return ns.getPrefix() + ":" + bterm.getName().replace('\t', ' ').replace('\n', ' ');
			return ns.getUri() + ":" + bterm.getName().replace('\t', ' ').replace('\n', ' ');
		}
			
		return bterm.getName();
	}
	
	
}
