package org.ndexbio.common.persistence;
import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.IFunctionTerm;
import org.ndexbio.common.models.data.IGroup;
import org.ndexbio.common.models.data.IGroupInvitationRequest;
import org.ndexbio.common.models.data.IGroupMembership;
import org.ndexbio.common.models.data.IJoinGroupRequest;
import org.ndexbio.common.models.data.INetworkAccessRequest;
import org.ndexbio.common.models.data.INetworkMembership;
import org.ndexbio.common.models.data.IReifiedEdgeTerm;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.orientdb.NdexSchemaManager;

import com.google.common.base.Strings;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;
import com.tinkerpop.frames.modules.gremlingroovy.GremlinGroovyModule;
import com.tinkerpop.frames.modules.typedgraph.TypedGraphModuleBuilder;

public class NdexOrientdbConnection {
	private FramedGraphFactory graphFactory = null;
    private ODatabaseDocumentTx ndexDatabase = null;
    private FramedGraph<OrientBaseGraph> orientDbGraph = null;
    
    public NdexOrientdbConnection() {
    	 this.graphFactory = new FramedGraphFactory(new GremlinGroovyModule(),
                 new TypedGraphModuleBuilder()
                     .withClass(IGroup.class)
                     .withClass(IUser.class)
                     .withClass(IGroupMembership.class)
                     .withClass(INetworkMembership.class)
                     .withClass(IGroupInvitationRequest.class)
                     .withClass(IJoinGroupRequest.class)
                     .withClass(INetworkAccessRequest.class)
                     .withClass(IBaseTerm.class)
                     .withClass(IReifiedEdgeTerm.class)
                     .withClass(IFunctionTerm.class).build());
    	 this.ndexDatabase = ODatabaseDocumentPool.global().acquire(
                 Configuration.getInstance().getProperty("OrientDB-URL"),
                 Configuration.getInstance().getProperty("OrientDB-Username"),
                 Configuration.getInstance().getProperty("OrientDB-Password"));
             
             if (Boolean.parseBoolean(Configuration.getInstance().getProperty("OrientDB-Use-Transactions")))
                 this.orientDbGraph = this.graphFactory.create((OrientBaseGraph)new OrientGraph(this.ndexDatabase));
             else
                this.orientDbGraph = this.graphFactory.create((OrientBaseGraph) 
                		new OrientGraphNoTx(this.ndexDatabase));

             /*
              * only initialize the ORM once
              */
             if (!NdexSchemaManager.INSTANCE.isInitialized()) {
    			NdexSchemaManager.INSTANCE.init(this.orientDbGraph.getBaseGraph());
    		}
    }
    
    public void teardownDatabase()
    {
        
        
        if (this.ndexDatabase != null)
        {
            this.ndexDatabase.close();
            this.ndexDatabase = null;
        }
        
        if (this.orientDbGraph != null)
        {
            this.orientDbGraph.shutdown();
        }
    }
    
	public ODatabaseDocumentTx getNdexDatabase() {
		return ndexDatabase;
	}
	public void set_ndexDatabase(ODatabaseDocumentTx ndexDatabase) {
		this.ndexDatabase = ndexDatabase;
	}
	public FramedGraph<OrientBaseGraph> getOrientDbGraph() {
		return orientDbGraph;
	}
	public void set_orientDbGraph(FramedGraph<OrientBaseGraph> orientDbGraph) {
		this.orientDbGraph = orientDbGraph;
	}
	public FramedGraphFactory getGraphFactory() {
		return graphFactory;
	}
	public void set_graphFactory(FramedGraphFactory graphFactory) {
		this.graphFactory =  graphFactory;
	}
	
	

}
