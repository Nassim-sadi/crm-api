package com.nassim.crm_api.Ticket;

import com.nassim.crm_api.Customer.CustomerResponse;
import com.nassim.crm_api.Employee.EmployeeResponse;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String title,
        String description,
        Status status,
        Priority priority,
        CustomerResponse customer,
        EmployeeResponse assignedEmployee,
        Instant createdAt,
        Instant updatedAt
) {
}
