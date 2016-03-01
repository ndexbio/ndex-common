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
package org.ndexbio.xgmml.parser.handler;

import java.util.LinkedList;
import java.util.List;

import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.SimplePropertyValuePair;

public class XGMMLEdge {
	
	private String subjectId;
	private String predicate;
	private String objectId;
	
	private List<NdexPropertyValuePair> props;
	
	private List<SimplePropertyValuePair> presentationProps;

	public XGMMLEdge(String subjectIdStr, String predicateStr, String objectIdStr) {
		this.subjectId = subjectIdStr;
		this.objectId  = objectIdStr;
		this.predicate = predicateStr;
		props = new LinkedList<> ();
		this.presentationProps = new LinkedList<>();
	}
	
	public String getSubjectId() {
		return subjectId;
	}

/*	public void setSubjectId(String subjectId) {
		this.subjectId = subjectId;
	} */

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicateStr) {
		this.predicate = predicateStr;
	}

	public String getObjectId() {
		return objectId;
	}

/*	public void setObjectId(String objectId) {
		this.objectId = objectId;
	} */

	public List<NdexPropertyValuePair> getProps() {
		return props;
	}

	public void setProps(List<NdexPropertyValuePair> properties) {
		this.props = properties;
	}

	public List<SimplePropertyValuePair> getPresentationProps() {
		return presentationProps;
	}

	public void setPresentationProps(List<SimplePropertyValuePair> presentationProperties) {
		this.presentationProps = presentationProperties;
	}
	
	
	
	
}
