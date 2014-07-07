package org.ndexbio.common.models.dao.orientdb.helper;

import java.util.Map;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.IFunctionTerm;
import org.ndexbio.common.models.data.INamespace;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.common.models.data.IReifiedEdgeTerm;
import org.ndexbio.common.models.data.ISupport;
import org.ndexbio.common.models.object.network.BaseTerm;
import org.ndexbio.common.models.object.network.Citation;
import org.ndexbio.common.models.object.network.Edge;
import org.ndexbio.common.models.object.network.FunctionTerm;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.models.object.network.Node;
import org.ndexbio.common.models.object.network.ReifiedEdgeTerm;
import org.ndexbio.common.models.object.network.Support;

import com.tinkerpop.frames.VertexFrame;



public interface EquivalenceFinder {

    
    INetwork getTargetNetwork();
    
    Map<String, VertexFrame> getNetworkIndex();
    
    INamespace getNamespace(RawNamespace namespace, String jdexId) throws NdexException;
    
    IBaseTerm getBaseTerm(BaseTerm baseTerm, String jdexId) throws NdexException;
    
    IFunctionTerm getFunctionTerm(FunctionTerm functionTerm, String jdexId) throws NdexException;
    
    ICitation getCitation(Citation citation, String jdexId) throws NdexException;
    
    ISupport getSupport(Support support, String jdexId) throws NdexException;
    
    INode getNode(Node node, String jdexId) throws NdexException;
    
    IEdge getEdge(Edge edge, String jdexId) throws NdexException;

	IReifiedEdgeTerm getReifiedEdgeTerm(ReifiedEdgeTerm term, String jdexId) throws NdexException;


}


