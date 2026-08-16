package com.nassim.crm_api.security;

import com.nassim.crm_api.Employee.Employee;
import com.nassim.crm_api.Employee.EmployeeRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString("jwt-test-secret-key-that-is-long-enough".getBytes());
        jwtService = new JwtService(secret, 3_600_000);
    }

    private Employee employee() {
        Employee employee = new Employee("Grace Hopper", "grace@crm.com", EmployeeRole.ADMIN);
        employee.setId(1L);
        return employee;
    }

    @Test
    void generateToken_roundTripsSubjectAndRole() {
        String token = jwtService.generateToken(employee());

        assertThat(jwtService.extractSubject(token)).isEqualTo("grace@crm.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isTokenValid_returnsTrueForMatchingEmployee() {
        String token = jwtService.generateToken(employee());

        assertThat(jwtService.isTokenValid(token, employee())).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForOtherEmployee() {
        String token = jwtService.generateToken(employee());

        Employee other = new Employee("Ada", "ada@crm.com", EmployeeRole.AGENT);
        other.setId(2L);
        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }
}
