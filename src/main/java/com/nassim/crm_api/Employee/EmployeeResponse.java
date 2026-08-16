package com.nassim.crm_api.Employee;

public record EmployeeResponse(Long id, String name, String email, EmployeeRole role) {
}
