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
