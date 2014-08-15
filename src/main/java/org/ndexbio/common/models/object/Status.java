package org.ndexbio.common.models.object;

@Deprecated
public enum Status
{
    QUEUED,
    STAGED,
    PROCESSING,
    COMPLETED,
    COMPLETED_WITH_WARNINGS,
    COMPLETED_WITH_ERRORS,
    FAILED,
    QUEUED_FOR_DELETION
}
