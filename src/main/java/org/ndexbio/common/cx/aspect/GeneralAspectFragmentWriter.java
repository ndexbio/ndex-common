package org.ndexbio.common.cx.aspect;

import org.cxio.aspects.writers.AbstractFragmentWriter;


public class GeneralAspectFragmentWriter extends AbstractFragmentWriter {

	private String aspectName ;
	
	
	public GeneralAspectFragmentWriter(String aspectName) {
		this.aspectName = aspectName;
	}

	@Override
	public String getAspectName() {
		return aspectName;
	}

}
