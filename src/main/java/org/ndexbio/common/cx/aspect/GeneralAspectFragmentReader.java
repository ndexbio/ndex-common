package org.ndexbio.common.cx.aspect;

import java.io.IOException;

import org.cxio.aspects.readers.AbstractFragmentReader;
import org.cxio.core.interfaces.AspectElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GeneralAspectFragmentReader extends AbstractFragmentReader {

	private String aspectName;
	private Class cls;
	
	public GeneralAspectFragmentReader(String aspectName, Class cls) {
		this.aspectName = aspectName;
		this.cls = cls;
	}

	@Override
	public String getAspectName() {
		return aspectName;
	}

	@Override
	public AspectElement readElement(ObjectNode o) throws IOException {
		ObjectMapper jsonObjectMapper = new ObjectMapper();
		return (AspectElement)jsonObjectMapper.treeToValue(o, cls);
	//	return o;
	}

	
	
}
