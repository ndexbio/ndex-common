package org.ndexbio.common.models.data;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface INetwork extends IMetadataObject
{
    @Adjacency(label = "networkCitations")
    public void addCitation(ICitation citation);

    @Adjacency(label = "networkCitations")
    public Iterable<ICitation> getCitations();
    
    @Adjacency(label = "networkCitations")
    public void removeCitation(ICitation citation);

    @Property("description")
    public String getDescription();

    @Property("description")
    public void setDescription(String description);

    @Property("isLocked")
    public boolean getIsLocked();
    
    @Property("isLocked")
    public void setIsLocked(boolean isLocked);

    @Property("isPublic")
    public boolean getIsPublic();
    
    @Property("isPublic")
    public void setIsPublic(boolean isPublic);
    
    @Adjacency(label = "networkMemberships")
    public void addMember(INetworkMembership newMember);
    
    @Adjacency(label = "networkMemberships")
    public Iterable<INetworkMembership> getMembers();
    
    @Adjacency(label = "networkMemberships")
    public void removeMember(INetworkMembership member);

    @Property("name")
    public String getName();

    @Property("name")
    public void setName(String name);

    @Adjacency(label = "networkNamespaces")
    public void addNamespace(INamespace namespace);

    @Adjacency(label = "networkNamespaces")
    public Iterable<INamespace> getNamespaces();
    
    @Adjacency(label = "networkNamespaces")
    public void removeNamespace(INamespace namespace);

    @Property("ndexEdgeCount")
    public int getNdexEdgeCount();

    @Property("ndexEdgeCount")
    public void setNdexEdgeCount(int edgesCount);

    @Adjacency(label = "networkEdges")
    public void addNdexEdge(IEdge edge);

    @Adjacency(label = "networkEdges")
    public Iterable<IEdge> getNdexEdges();
    
    @Adjacency(label = "networkEdges")
    public void removeNdexEdge(IEdge edge);

    @Property("ndexNodeCount")
    public int getNdexNodeCount();

    @Property("ndexNodeCount")
    public void setNdexNodeCount(int nodesCount);

    @Adjacency(label = "networkNodes")
    public void addNdexNode(INode node);

    @Adjacency(label = "networkNodes")
    public Iterable<INode> getNdexNodes();
    
    @Adjacency(label = "networkNodes")
    public void removeNdexNode(INode node);
    
    @Adjacency(label = "networkRequests")
    public void addRequest(IRequest request);
    
    @Adjacency(label = "networkRequests")
    public Iterable<IRequest> getRequests();
    
    @Adjacency(label = "networkRequests")
    public void removeRequest(IRequest request);

    @Adjacency(label = "networkSupports")
    public void addSupport(ISupport support);

    @Adjacency(label = "networkSupports")
    public Iterable<ISupport> getSupports();
    
    @Adjacency(label = "networkSupports")
    public void removeSupport(ISupport support);

    @Adjacency(label = "networkTerms")
    public void addTerm(ITerm term);

    @Adjacency(label = "networkTerms")
    public Iterable<ITerm> getTerms();

    @Adjacency(label = "networkTerms")
    public void removeTerm(ITerm term);
}
