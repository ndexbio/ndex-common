package org.ndexbio.task;

import java.io.File;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.Status;
import org.ndexbio.task.parsingengines.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

/*
 * This class represents a NdexTask subclass that is responsible
 * for uploading a specified data file into a new NDEx network in
 * orientdb. A particular file parser is selected based on the file type.
 * Since this class is invoked based on a Task registered in the orientdb 
 * database, no user authentication is required.
 * 
 */

public class FileUploadTask extends NdexTask {

	private final String filename;
	private static final Logger logger = LoggerFactory
			.getLogger(FileUploadTask.class);

	private Status taskStatus;
    private NdexDatabase db;
	
	
	public FileUploadTask(Task itask, NdexDatabase db) throws IllegalArgumentException,
			SecurityException, NdexException {
		super(itask);
		this.filename = this.getTask().getResource();
		// this.filename = this.getTask().getResource();
		if (!(new File(this.filename).isFile())) {
			throw new NdexException("File " + this.filename + " does not exist");
		}
		this.db = db;
	}

	@Override
	public Task call() throws Exception {
		
		try {
			this.processFile();
			return this.getTask();
		} catch (InterruptedException e) {
			logger.info("FileUploadTask interupted");
			return null;
		}
	}

	protected String getFilename() {
		return this.filename;

	}

	private void processFile() throws Exception {
		logger.info("Processing file: " + this.getFilename());
		this.taskStatus = Status.PROCESSING;
		this.startTask();
		File file = new File(this.getFilename());
		String fileExtension = com.google.common.io.Files
				.getFileExtension(this.getFilename()).toUpperCase().trim();
		logger.info("File extension = " + fileExtension);
		String networkName = Files.getNameWithoutExtension(this.getTask().getDescription());
		switch (fileExtension) {
		case ("SIF"):
			try {
				final SifParser sifParser = new SifParser(
						file.getAbsolutePath(), this.getTaskOwnerAccount(),db, networkName);
				sifParser.parseFile();
				this.taskStatus = Status.COMPLETED;
			} catch (Exception e) {
				this.taskStatus = Status.COMPLETED_WITH_ERRORS;
				logger.error(e.getMessage());
			}
			break;
		case ("XGMML"):
			try {
				final XgmmlParser xgmmlParser = new XgmmlParser(
						file.getAbsolutePath(), this.getTaskOwnerAccount(),db, networkName);
				xgmmlParser.parseFile();
				this.taskStatus = Status.COMPLETED;
			} catch (Exception e) {
				this.taskStatus = Status.COMPLETED_WITH_ERRORS;
				logger.error(e.getMessage());
			}
			break;
		case ("XBEL"):
			try {
				logger.info("Processing xbel file " + file.getAbsolutePath()
						+ " for " + this.getTaskOwnerAccount());
				final XbelParser xbelParser = new XbelParser(
						file.getAbsolutePath(), this.getTaskOwnerAccount(),db);

				if (!xbelParser.getValidationState().isValid()) {
					logger.info("XBel validation failed");
					this.taskStatus = Status.COMPLETED_WITH_ERRORS;
					throw new NdexException(
							"XBEL file fails XML schema validation - one or more elements do not meet XBEL specification.");
				}
				xbelParser.parseFile();
				this.taskStatus = Status.COMPLETED;
			} catch (Exception e) {
				this.taskStatus = Status.COMPLETED_WITH_ERRORS;
				logger.error(e.getMessage());
			}
			break;
		case ("XLSX"):
		case ("XLS"):
			try {
				final ExcelParser excelParser = new ExcelParser(
						file.getAbsolutePath(), this.getTaskOwnerAccount(),db);
				excelParser.parseFile();
				this.taskStatus = Status.COMPLETED;
			} catch (Exception e) {
				this.taskStatus = Status.COMPLETED_WITH_ERRORS;
				logger.error(e.getMessage());
			}
			break;
		default:		
			logger.error("The uploaded file type is not supported; must be SIF, XGMML, XBEL, XLS or XLSX.");
			this.taskStatus = Status.COMPLETED_WITH_ERRORS;

		}
		logger.info("Network upload file: " + file.getName() +" deleted from staging area");			
		file.delete(); // delete the file from the staging area
		this.updateTaskStatus(this.taskStatus);
	}

}
