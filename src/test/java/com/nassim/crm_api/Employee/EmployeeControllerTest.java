package com.nassim.crm_api.Employee;

import com.nassim.crm_api.config.TestSecurityConfig;
import com.nassim.crm_api.exception.EmployeeNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@WebMvcTest(EmployeeController.class)
@Import(TestSecurityConfig.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    private static final String VALID_BODY = """
            {"name": "Grace Hopper", "email": "grace@crm.com", "role": "AGENT"}
            """;

    private EmployeeResponse response() {
        return new EmployeeResponse(1L, "Grace Hopper", "grace@crm.com", EmployeeRole.AGENT);
    }

    @Test
    void findAll_returnsEmployees() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(response()));

        MvcTestResultAssert result = mockMvc.get().uri("/api/employees").exchange().assertThat();

        result.hasStatusOk();
        result.bodyJson().extractingPath("$[0].name").asString().isEqualTo("Grace Hopper");
        result.bodyJson().extractingPath("$[0].role").asString().isEqualTo("AGENT");
    }

    @Test
    void findById_returnsEmployee() {
        when(employeeService.getEmployeeById(1L)).thenReturn(response());

        MvcTestResultAssert result = mockMvc.get().uri("/api/employees/1").exchange().assertThat();

        result.hasStatusOk();
        result.bodyJson().extractingPath("$.email").asString().isEqualTo("grace@crm.com");
    }

    @Test
    void findById_returns404WhenNotFound() {
        when(employeeService.getEmployeeById(99L)).thenThrow(new EmployeeNotFoundException(99L));

        MvcTestResultAssert result = mockMvc.get().uri("/api/employees/99").exchange().assertThat();

        result.hasStatus(404);
        result.bodyJson().extractingPath("$.message").asString().isEqualTo("Employee not found with id: 99");
    }

    @Test
    void create_returns201WithLocation() {
        when(employeeService.createEmployee(any(EmployeeRequest.class))).thenReturn(new EmployeeResponse(5L, "Grace Hopper", "grace@crm.com", EmployeeRole.AGENT));

        MvcTestResultAssert result = mockMvc.post().uri("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)
                .exchange().assertThat();

        result.hasStatus(201);
        result.headers().hasHeaderSatisfying("Location",
                values -> assertThat(values).anyMatch(v -> v.endsWith("/api/employees/5")));
    }

    @Test
    void create_returns400WhenInvalid() {
        String invalid = """
                {"name": "", "email": "nope", "role": "AGENT"}
                """;

        MvcTestResultAssert result = mockMvc.post().uri("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid)
                .exchange().assertThat();

        result.hasStatus(400);
        result.bodyJson().extractingPath("$.errors.email").asString().isEqualTo("email must be a valid email address");
    }

    @Test
    void create_returns400WhenPasswordTooShort() {
        String invalid = """
                {"name": "Grace Hopper", "email": "grace@crm.com", "role": "AGENT", "password": "short"}
                """;

        MvcTestResultAssert result = mockMvc.post().uri("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid)
                .exchange().assertThat();

        result.hasStatus(400);
        result.bodyJson().extractingPath("$.errors.password").asString().isEqualTo("password must be at least 8 characters");
    }

    @Test
    void update_returnsUpdatedEmployee() {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class))).thenReturn(new EmployeeResponse(1L, "Ada Lovelace", "ada@crm.com", EmployeeRole.ADMIN));

        MvcTestResultAssert result = mockMvc.put().uri("/api/employees/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)
                .exchange().assertThat();

        result.hasStatusOk();
        result.bodyJson().extractingPath("$.name").asString().isEqualTo("Ada Lovelace");
    }

    @Test
    void delete_returns204() {
        doNothing().when(employeeService).deleteEmployee(1L);

        MvcTestResultAssert result = mockMvc.delete().uri("/api/employees/1").exchange().assertThat();

        result.hasStatus(204);
    }
}
