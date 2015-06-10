/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.common.models.dao;

import java.util.Date;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;
import org.ndexbio.model.object.Priority;

import com.orientechnologies.orient.core.id.ORID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTaskDAO
{
   

  /*  private final TaskDAO taskDao = DAOFactorySupplier.INSTANCE.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE)
			.get().getTaskDAO();
    
    @Test
    public void createTask()
    {
        Assert.assertTrue(createNewTask());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createTaskInvalid() throws IllegalArgumentException, NdexException
    {
        taskDao.createTask(null,null);
    }

    @Test
    public void deleteTask() throws IllegalArgumentException, NdexException
    {
        Assert.assertTrue(createNewTask());

        final ORID testTaskRid = getRid("This is a test task.");
        Assert.assertTrue(deleteTargetTask(IdConverter.toJid(testTaskRid)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteTaskInvalid() throws IllegalArgumentException, NdexException
    {
        taskDao.deleteTask("","");
    }

    @Test
    public void getTask()
    {
        try
        {
            Assert.assertTrue(createNewTask());
            
            final ORID testTaskRid = getRid("This is a test task.");
            final Task testTask = taskDao.getTask(IdConverter.toJid(testTaskRid), testUserId);
            Assert.assertNotNull(testTask);

            Assert.assertTrue(deleteTargetTask(testTask.getId()));
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTaskInvalid() throws IllegalArgumentException, NdexException
    {
        taskDao.getTask("","");
    }

    @Test
    public void updateTask()
    {
        try
        {
            Assert.assertTrue(createNewTask());
            
            final ORID testTaskRid = getRid("This is a test task.");
            final Task testTask = taskDao.getTask(IdConverter.toJid(testTaskRid), testUserId);

            testTask.setDescription("This is an updated test task.");
            taskDao.updateTask(testTask, testUserId);
            Assert.assertEquals(taskDao.getTask(testTask.getId(),testUserId).getDescription(), testTask.getDescription());
            
            Assert.assertTrue(deleteTargetTask(testTask.getId()));
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateTaskInvalid() throws IllegalArgumentException, NdexException
    {
        taskDao.updateTask(null,null);
    }
    
    
    
    private boolean createNewTask()
    {
        final Task newTask = new Task();
       
        newTask.setDescription("This is a test task.");
        newTask.setCreatedDate(new Date());
		newTask.setId("");  
		newTask.setPriority(Priority.LOW);
		newTask.setProgress(0);
		newTask.setResource("small corpus");
		newTask.setStatus(Status.COMPLETED);
		newTask.setType(TaskType.EXPORT_NETWORK_TO_FILE);
        
        try
        {
            final Task createdTask = taskDao.createTask(newTask,testUserId);
            Assert.assertNotNull(createdTask);
            
            return true;
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    private boolean deleteTargetTask(String taskId)
    {
        try
        {
            taskDao.deleteTask(taskId,testUserId);
            Assert.assertNull(taskDao.getTask(taskId, testUserId));
            
            return true;
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    } */
}
