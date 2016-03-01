/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
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
