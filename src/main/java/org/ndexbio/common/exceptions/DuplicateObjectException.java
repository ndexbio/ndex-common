package org.ndexbio.common.exceptions;

import org.ndexbio.model.exceptions.NdexException;

public class DuplicateObjectException extends NdexException
{
    private static final long serialVersionUID = 1L;

    
    
    public DuplicateObjectException(String message)
    {
        super(message);
    }

    public DuplicateObjectException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
