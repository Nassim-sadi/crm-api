package com.nassim.crm_api.Customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "firstName is required")
        @Size(max = 100, message = "firstName must be at most 100 characters")
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 100, message = "lastName must be at most 100 characters")
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 150, message = "email must be at most 150 characters")
        String email
) {
}
