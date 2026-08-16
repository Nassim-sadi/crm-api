package com.nassim.crm_api.auth;

import com.nassim.crm_api.Employee.EmployeeResponse;

public record AuthResponse(String token, EmployeeResponse employee) {
}
