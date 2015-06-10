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
