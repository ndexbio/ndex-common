package org.ndexbio.common.models.object.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.ndexbio.common.models.data.*;
import org.ndexbio.common.models.object.Request;
import org.ndexbio.common.models.object.privilege.Membership;

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * @deprecated  As of release 1.0, replaced by {@link org.ndexbio.model.object.network.Network}
 */
@Deprecated
public class Network extends MetadataObject
{
    private Map<String, Citation> _citations;
    private String _description;
    private int _edgeCount;
    private Map<String, Edge> _edges;
    private boolean _isComplete;
    private boolean _isLocked;
    private boolean _isPublic;
    private List<Membership> _members;
    private String _name;
    private Map<String, RawNamespace> _namespaces;
    private int _nodeCount;
    private Map<String, Node> _nodes;
    private List<Request> _requests;
    private Map<String, Support> _supports;
    private Map<String, Term> _terms;



    /**************************************************************************
    * Default constructor.
    **************************************************************************/
    public Network()
    {
        super();

        _isComplete = false;
        _isLocked = false;
        _isPublic = false;
        _edgeCount = 0;
        _nodeCount = 0;
        
        initCollections();
    }

    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * Doesn't load Edges, Nodes, or Terms.
    * 
    * @param network The Network with source data.
    **************************************************************************/
    public Network(INetwork network)
    {
        this(network, false);
    }

    /**************************************************************************
    * Populates the class (from the database) and removes circular references.
    * 
    * @param network        The Network with source data.
    * @param loadEverything True to load Edges, Nodes, and Terms, false to
    *                       exclude them.
    **************************************************************************/
    public Network(INetwork network, boolean loadEverything)
    {
        super(network);

        this.initCollections();

        _description = network.getDescription();
        _edgeCount = network.getNdexEdgeCount();
        _isComplete = network.getIsComplete();
        _isLocked = network.getIsLocked();
        _isPublic = network.getIsPublic();
        _nodeCount = network.getNdexNodeCount();
        _name = network.getName();
        
        for (final INetworkMembership member : network.getMembers())
        {
            if (member.getMember() instanceof IUser)
                _members.add(new Membership((IUser)member.getMember(), member.getPermissions()));
            else if (member.getMember() instanceof IGroup)
                _members.add(new Membership((IGroup)member.getMember(), member.getPermissions()));
        }
        
        for (final IRequest request : network.getRequests())
            _requests.add(new Request(request));

        if (loadEverything)
        {
            for (final IEdge edge : network.getNdexEdges())
                _edges.put(edge.getJdexId(), new Edge(edge));

            for (final INode node : network.getNdexNodes())
                _nodes.put(node.getJdexId(), new Node(node));

            for (final ITerm term : network.getTerms())
            {
                if (term instanceof IBaseTerm)
                    _terms.put(term.getJdexId(), new BaseTerm((IBaseTerm)term));
                else if (term instanceof IFunctionTerm)
                    _terms.put(term.getJdexId(), new FunctionTerm((IFunctionTerm)term));
                else if (term instanceof IReifiedEdgeTerm)
                    _terms.put(term.getJdexId(), new ReifiedEdgeTerm((IReifiedEdgeTerm)term));
            }

            for (final ICitation citation : network.getCitations())
                _citations.put(citation.getJdexId(), new Citation(citation));

            for (final INamespace namespace : network.getNamespaces())
                _namespaces.put(namespace.getJdexId(), new RawNamespace(namespace));

            for (final ISupport support : network.getSupports())
                _supports.put(support.getJdexId(), new Support(support));
        }
    }

    

    public Map<String, Citation> getCitations()
    {
        return _citations;
    }

    public void setCitations(Map<String, Citation> citations)
    {
        _citations = citations;
    }

    public String getDescription()
    {
        return _description;
    }
    
    public void setDescription(String description)
    {
        _description = description;
    }
    
    public int getEdgeCount()
    {
        return _edgeCount;
    }

    public void setEdgeCount(int edgeCount)
    {
        _edgeCount = edgeCount;
    }

    public Map<String, Edge> getEdges()
    {
        return _edges;
    }

    public void setEdges(Map<String, Edge> edges)
    {
        _edges = edges;
    }

    public boolean getIsComplete()
    {
        return _isComplete;
    }
    
    public void setIsComplete(boolean isComplete)
    {
        _isComplete = isComplete;
    }

    public boolean getIsLocked()
    {
        return _isLocked;
    }
    
    public void setIsLocked(boolean isLocked)
    {
        _isLocked = isLocked;
    }
    
    public boolean getIsPublic()
    {
        return _isPublic;
    }
    
    public void setIsPublic(boolean isPublic)
    {
        _isPublic = isPublic;
    }
    
    public List<Membership> getMembers()
    {
        return _members;
    }
    
    public void setMembers(List<Membership> members)
    {
        _members = members;
    }
    
    public String getName()
    {
        return _name;
    }
    
    public void setName(String name)
    {
        _name = name;
    }

    public Map<String, RawNamespace> getNamespaces()
    {
        return _namespaces;
    }

    public void setNamespaces(Map<String, RawNamespace> namespaces)
    {
        _namespaces = namespaces;
    }

    public int getNodeCount()
    {
        return _nodeCount;
    }

    public void setNodeCount(int nodeCount)
    {
        _nodeCount = nodeCount;
    }

    public Map<String, Node> getNodes()
    {
        return _nodes;
    }

    public void setNodes(Map<String, Node> nodes)
    {
        _nodes = nodes;
    }
    
    public List<Request> getRequests()
    {
        return _requests;
    }
    
    public void setRequests(List<Request> requests)
    {
        _requests = requests;
    }

    public Map<String, Support> getSupports()
    {
        return _supports;
    }

    public void setSupports(Map<String, Support> supports)
    {
        _supports = supports;
    }

    public Map<String, Term> getTerms()
    {
        return _terms;
    }

    public void setTerms(Map<String, Term> terms)
    {
        _terms = terms;
    }

    

    /**************************************************************************
    * Initializes the collections. 
    **************************************************************************/
    private void initCollections()
    {
        _citations = new HashMap<String, Citation>();
        _edges = new HashMap<String, Edge>();
        _members = new ArrayList<Membership>();
        _namespaces = new HashMap<String, RawNamespace>();
        _nodes = new HashMap<String, Node>();
        _requests = new ArrayList<Request>();
        _supports = new HashMap<String, Support>();
        _terms = new HashMap<String, Term>();
    }
}
