package com.nassim.crm_api.auth;

import com.nassim.crm_api.Employee.EmployeeResponse;
import com.nassim.crm_api.Employee.EmployeeRole;
import com.nassim.crm_api.config.TestSecurityConfig;
import com.nassim.crm_api.exception.EmailAlreadyExistsException;
import com.nassim.crm_api.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private AuthService authService;

    private AuthResponse authResponse() {
        return new AuthResponse("jwt-token",
                new EmployeeResponse(1L, "Grace Hopper", "grace@crm.com", EmployeeRole.AGENT));
    }

    @Test
    void register_returns201WithToken() {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse());

        MvcTestResultAssert result = mockMvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Grace Hopper\",\"email\":\"grace@crm.com\",\"password\":\"secret123\",\"role\":\"AGENT\"}")
                .exchange().assertThat();

        result.hasStatus(201);
        result.bodyJson().extractingPath("$.token").asString().isEqualTo("jwt-token");
        result.bodyJson().extractingPath("$.employee.email").asString().isEqualTo("grace@crm.com");
    }

    @Test
    void register_returns400WhenInvalid() {
        MvcTestResultAssert result = mockMvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"email\":\"nope\",\"password\":\"short\"}")
                .exchange().assertThat();

        result.hasStatus(400);
        result.bodyJson().extractingPath("$.errors.password").asString().isEqualTo("password must be at least 8 characters");
        result.bodyJson().extractingPath("$.errors.email").asString().isEqualTo("email must be a valid email address");
    }

    @Test
    void register_returns409WhenEmailExists() {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("grace@crm.com"));

        MvcTestResultAssert result = mockMvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Grace Hopper\",\"email\":\"grace@crm.com\",\"password\":\"secret123\"}")
                .exchange().assertThat();

        result.hasStatus(409);
        result.bodyJson().extractingPath("$.message").asString().isEqualTo("Email already registered: grace@crm.com");
    }

    @Test
    void login_returns200WithToken() {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse());

        MvcTestResultAssert result = mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"grace@crm.com\",\"password\":\"secret123\"}")
                .exchange().assertThat();

        result.hasStatusOk();
        result.bodyJson().extractingPath("$.token").asString().isEqualTo("jwt-token");
        result.bodyJson().extractingPath("$.employee.role").asString().isEqualTo("AGENT");
    }

    @Test
    void login_returns401OnBadCredentials() {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        MvcTestResultAssert result = mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"grace@crm.com\",\"password\":\"wrong123\"}")
                .exchange().assertThat();

        result.hasStatus(401);
        result.bodyJson().extractingPath("$.message").asString().isEqualTo("Invalid email or password");
    }
}
