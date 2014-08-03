package org.ndexbio.common.persistence.orientdb;

import java.util.List;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.object.Status;
import org.ndexbio.model.object.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Vertex;

/*
 * Represents a collection of methods for interacting with Tasks in the orientdb database
 * Retained in the common ndex-common project to facilitate availability to multiple ndex
 * projects using Tasks
 * 
 * mod 13Jan2014 - use domain objects instead of model objects 
 * mod 01Apr2014 - add public method to delete task entities
 */

public class NdexTaskService 
{
    private static final Logger logger = LoggerFactory.getLogger(NdexTaskService.class);
    private OrientDBNoTxConnectionService ndexService;
    
    public NdexTaskService() throws NdexException
    {
    	ndexService = new OrientDBNoTxConnectionService();  
    }
    
    
    /*
     * public method to delete Task entities that have a status of
     * QUEUED_FOR_DELETION
     */
    public void deleteTasksQueuedForDeletion() throws NdexException {
    	String query = "select from task "
	            + " where status = '" +Status.QUEUED_FOR_DELETION.toString() +"'";
    	
    	try {
    		
			this.ndexService.setupDatabase();
			
/*			final List<ODocument> taskDocumentList = this.ndexService._orientDbGraph.getBaseGraph().
					 getRawGraph().query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument document : taskDocumentList) {
				this.ndexService._orientDbGraph.getVertex(document, ITask.class).asVertex().remove();
			}
	*/		
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
            throw new NdexException("Failed to search tasks.");
			
		}finally {
			this.ndexService.teardownDatabase();
		}
    	
    }

    
    /*
     * get a Collection of ITasks based on status value
     */
    
/*    private List<ITask> getITasksByStatus(Status aStatus) throws NdexException {
    	String query = "select from task "
	            + " where status = '" +aStatus +"'";
    	final List<ITask> foundITasks = Lists.newArrayList();
    	try {
    		if (!this.ndexService.isSetup()) {
				this.ndexService.setupDatabase();
			}
			final List<ODocument> taskDocumentList = this.ndexService._orientDbGraph.getBaseGraph().
					 getRawGraph().query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument document : taskDocumentList) {
				foundITasks.add(this.ndexService._orientDbGraph.getVertex(document, ITask.class));
			}
			
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
            throw new NdexException("Failed to search tasks.");
			
		}finally {
			this.ndexService.teardownDatabase();
		}
    	return foundITasks;
    }
  */  
    /*
     * get a Collection of Tasks based on Status value
     */
  /*  private List<Task> getTasksByStatus(Status aStatus) throws NdexException {
    	 String query = "select from task "
    	            + " where status = '" +aStatus +"'";
    	 final List<Task> foundTasks = Lists.newArrayList();
    	 try {
    		 if (!this.ndexService.isSetup()) {
 				this.ndexService.setupDatabase();
 			}

			 final List<ODocument> taskDocumentList = this.ndexService._orientDbGraph.getBaseGraph().
					 getRawGraph().query(new OSQLSynchQuery<ODocument>(query));
			 for (final ODocument document : taskDocumentList)
	                foundTasks.add(new Task());//this.ndexService._orientDbGraph.getVertex(document, ITask.class)));

	            return foundTasks;
		} catch (Exception e) {
			logger.error("Failed to search tasks", e);
	            throw new NdexException("Failed to search tasks.");
		}finally {
			this.ndexService.teardownDatabase();
		}

    }
   */
    
   /* public List<ITask> getInProgressITasks() throws NdexException{
   	 return getITasksByStatus(Status.PROCESSING);
    }
    */

}
