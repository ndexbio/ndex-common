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
package org.ndexbio.task.parsingengines;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Strings;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.task.Configuration;
import org.openbel.framework.common.xbel.parser.XBELValidator;
import org.xml.sax.SAXException;

public class XbelFileValidator {

	private String xmlFileName;
//	private final static String XBEL_ROOT = ""; //"xbel/";
	private  String XBEL_XSD; //= XBEL_ROOT + "xbel.xsd";
	private  String ANNO_XSD ; //= XBEL_ROOT + "xbel-annotations.xsd";

	private XBELValidator xv;
	private final ValidationState validationState;

	public XbelFileValidator(String fileName) throws NdexException {
		if (Strings.isNullOrEmpty(fileName)) {
			this.validationState = new ValidationState(false,
					"Null or empty filename parameter");
			return;
		}
		this.xmlFileName = fileName;
		
		XBEL_XSD = Configuration.getInstance().getNdexRoot() + "/resource/xbel.xsd";
		ANNO_XSD = Configuration.getInstance().getNdexRoot() + "/resource/xbel-annotations.xsd";
		if (!this.xsdCheck() || !this.initValidator()) {
			this.validationState = new ValidationState(false,
					"Valid BEL XSD file(s) unavailable");
			return;
		}

		this.validationState = this.run();

	}

	public boolean xsdCheck() {
		if (!new File(XBEL_XSD).canRead()) {
			System.err.println("can't read " + XBEL_XSD);
			return false;
		}
		if (!new File(ANNO_XSD).canRead()) {
			System.err.println("can't read " + ANNO_XSD);
			return false;
		}

		return true;
	}

	private boolean initValidator() {
		try {
			this.xv = new XBELValidator(XBEL_XSD, ANNO_XSD);
			return true;
		} catch (SAXException e) {
			String err = "SAX exception validating XSDs";
			err += ", exception message follows:\n\t";
			err += e.getMessage();
			System.err.println(err);
		}
		return false;
	}

	private ValidationState run() {

		try {

			xv.validate(new File( this.xmlFileName));
					//getClass().getClassLoader().getResource(this.xmlFileName).toURI()));
			String message = "File " + this.xmlFileName
					+ " is a valid xbel file";
			return new ValidationState(true, message);
		} catch (SAXException e) {
			String err = "SAX exception validating " + this.xmlFileName;
			err += ", exception message follows:\n\t";
			err += e.getMessage();
			return new ValidationState(false, err);
		} catch (IOException e) {
			String err = "IO exception validating " + this.xmlFileName;
			err += ", exception message follows:\n\t";
			err += e.getMessage();
			return new ValidationState(false, err);
		}
	}

	public ValidationState getValidationState() {
		return validationState;
	}

	public class ValidationState {
		private final boolean valid;
		private final String validationMessage;

		ValidationState(boolean v, String m) {
			this.valid = v;
			this.validationMessage = m;
		}

		public boolean isValid() {
			return valid;
		}

		public String getValidationMessage() {
			return validationMessage;
		}
	}

}
