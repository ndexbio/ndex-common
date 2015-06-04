package org.ndexbio.task.parsingengines;

import java.util.UUID;

import org.ndexbio.model.exceptions.NdexException;

public interface IParsingEngine
{
    /**************************************************************************
    * Parses the specified file. 
    **************************************************************************/
    public void parseFile() throws  NdexException;
	public UUID getUUIDOfUploadedNetwork()  ;

}
