package ru.mephi.identity.common.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    List<FieldErrorResponse> fields
) {
}
