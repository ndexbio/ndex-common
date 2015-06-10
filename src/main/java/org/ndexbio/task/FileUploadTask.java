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
	
	
	public FileUploadTask(Task itask, NdexDatabase ndexDb) throws IllegalArgumentException,
			SecurityException, NdexException {
		super(itask);
		this.filename = this.getTask().getResource();
		// this.filename = this.getTask().getResource();
		if (!(new File(this.filename).isFile())) {
			throw new NdexException("File " + this.filename + " does not exist");
		}
		this.db = ndexDb;
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
		IParsingEngine parser = null;

		switch (fileExtension) {
		case ("SIF"):
			parser = new SifParser(
						file.getAbsolutePath(), this.getTaskOwnerAccount(),db, networkName, getTask().getDescription());
			break;
		case ("XGMML"):
			parser = new XgmmlParser(
						file.getAbsolutePath(), this.getTaskOwnerAccount(),db, networkName, getTask().getDescription());
			break;
		case ("OWL"):
			parser = new BioPAXParser(
						file.getAbsolutePath(), this.getTaskOwnerAccount(),db, networkName, getTask().getDescription());
			break;
		case ("XBEL"):
			parser = new XbelParser(
						file.getAbsolutePath(), this.getTaskOwnerAccount(),db, getTask().getDescription());

			if (!((XbelParser)parser).getValidationState().isValid()) {
					logger.info("XBel validation failed");
					this.taskStatus = Status.COMPLETED_WITH_ERRORS;
					throw new NdexException(
							"XBEL file fails XML schema validation - one or more elements do not meet XBEL specification.");
			}
			break;
		case ("XLSX"):
		case ("XLS"):
			parser = new ExcelParser(
						file.getAbsolutePath(), this.getTaskOwnerAccount(),db);
			break;
		default:		
			String message = "The uploaded file type is not supported; must be SIF, XGMML, XBEL, XLS or XLSX."; 
			logger.error(message);
			throw new NdexException (message);

		}
		parser.parseFile();
		this.taskStatus = Status.COMPLETED;
		logger.info("Network upload file: " + file.getName() +" deleted from staging area");			
		file.delete(); // delete the file from the staging area
		this.addTaskAttribute("networkUUID", parser.getUUIDOfUploadedNetwork().toString());
		this.updateTaskStatus(this.taskStatus);
	}

}
