package org.ndexbio.common.models.dao;

import java.util.Date;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.object.Priority;
import org.ndexbio.common.models.object.Status;
import org.ndexbio.common.models.object.Task;
import org.ndexbio.common.models.object.TaskType;
import org.ndexbio.common.helpers.IdConverter;

import com.orientechnologies.orient.core.id.ORID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTaskDAO extends TestDAO
{
   

    private final TaskDAO taskDao = DAOFactorySupplier.INSTANCE.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE)
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
    public void deleteTask()
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
    }
}
