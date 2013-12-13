package org.ndexbio.commonexceptions.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.ndexbio.common.exceptions.ValidationException;

public class ValidationExceptionMapper implements ExceptionMapper<ValidationException>
{
    public Response toResponse(ValidationException exception)
    {
        return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).build();
    }
}
