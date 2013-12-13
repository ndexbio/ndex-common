package org.ndexbio.common.exceptions;

public class JdexParsingException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    
    
    public JdexParsingException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public JdexParsingException(String message)
    {
        super(message);
    }
}
