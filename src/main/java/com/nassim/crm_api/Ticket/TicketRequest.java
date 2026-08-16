package com.nassim.crm_api.Ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @NotNull(message = "priority is required")
        Priority priority,

        @NotNull(message = "customerId is required")
        Long customerId,

        @NotNull(message = "assignedEmployeeId is required")
        Long assignedEmployeeId
) {
}
