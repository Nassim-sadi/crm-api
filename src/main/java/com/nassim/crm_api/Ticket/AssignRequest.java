package com.nassim.crm_api.Ticket;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(
        @NotNull(message = "employeeId is required")
        Long employeeId
) {
}
