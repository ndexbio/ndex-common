package org.ndexbio.common.models.dao.orientdb;

import java.util.Date;
import java.util.UUID;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.object.NdexExternalObject;

import com.orientechnologies.orient.core.record.impl.ODocument;

public class Helper {
	
	public static NdexExternalObject populateExternalObjectFromDoc(NdexExternalObject obj, ODocument doc) {
		obj.setCreationDate((Date)doc.field(NdexClasses.ExternalObj_cDate));
		obj.setExternalId(UUID.fromString((String)doc.field(NdexClasses.Network_P_UUID)));
		obj.setModificationDate((Date)doc.field(NdexClasses.ExternalObj_mDate));
		
	    return obj;
	}

}
