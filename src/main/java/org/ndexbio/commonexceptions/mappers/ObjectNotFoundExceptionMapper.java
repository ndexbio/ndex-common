package org.ndexbio.commonexceptions.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.ndexbio.common.exceptions.ObjectNotFoundException;

public class ObjectNotFoundExceptionMapper implements ExceptionMapper<ObjectNotFoundException>
{
    public Response toResponse(ObjectNotFoundException exception)
    {
        return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).build();
    }
}
