package org.ndexbio.commonexceptions.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.ndexbio.common.exceptions.NdexException;

public class NdexExceptionMapper implements ExceptionMapper<NdexException>
{
    public Response toResponse(NdexException exception)
    {
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
    }
}
