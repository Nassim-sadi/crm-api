package com.nassim.crm_api.Employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 150, message = "email must be at most 150 characters")
        String email,

        @NotNull(message = "role is required")
        EmployeeRole role,

        @Size(min = 8, message = "password must be at least 8 characters")
        String password
) {
}
