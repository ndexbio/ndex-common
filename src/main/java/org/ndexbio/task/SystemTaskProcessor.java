package org.ndexbio.task;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.logging.Logger;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.TaskDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;

public class SystemTaskProcessor extends NdexTaskProcessor {

    private Logger logger = Logger.getLogger("com.darwinsys");
	
	public SystemTaskProcessor () {
		super();
	}
	
	@Override
	public void run() {
		while ( !shutdown) {
			Task task = null;
			try {
				task = NdexServerQueue.INSTANCE.takeNextSystemTask();
			} catch (InterruptedException e) {
				break;
			}

			TaskType type = task.getTaskType();
			if ( type == TaskType.SYSTEM_DELETE_NETWORK) {
				try {
				    cleanupDeletedNetwork(task);
				} catch (NdexException e) {
					logger.severe("Error when executing system task: " + e);
					e.printStackTrace();
				} 
			} else {
					logger.severe("Unsupported system task type " + type + ". Task ignored.");
			}
		}
	}
	
	
	private void cleanupDeletedNetwork (Task task) throws NdexException  {
		logger.info( "Cleanup deleted network " + task.getResource());

		task.setStartTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
		try (NetworkDAO networkDao = new NetworkDAO(NdexDatabase.getInstance().getAConnection()); ) {
			int cnt = networkDao.deleteNetwork(task.getResource());
			networkDao.commit();
			networkDao.close();
			logger.info("Network " + task.getResource() + " cleanup finished.");
			task.setFinishTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
			task.setStatus(Status.COMPLETED);
			task.setMessage(cnt + " vertex deleted.");
			try (TaskDAO taskdao = new TaskDAO (NdexDatabase.getInstance().getAConnection())) {
				taskdao.createTask(null, task);
				taskdao.commit();
				taskdao.close();
			}
		}	
	}
}
