package com.nassim.crm_api.auth;

import com.nassim.crm_api.Employee.Employee;
import com.nassim.crm_api.Employee.EmployeeRepository;
import com.nassim.crm_api.Employee.EmployeeRole;
import com.nassim.crm_api.exception.EmailAlreadyExistsException;
import com.nassim.crm_api.exception.InvalidCredentialsException;
import com.nassim.crm_api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_encodesPasswordAndReturnsToken() {
        when(employeeRepository.existsByEmail("grace@crm.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-hash");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(Employee.class))).thenReturn("jwt-token");

        AuthResponse result = authService.register(
                new RegisterRequest("Grace Hopper", "grace@crm.com", "secret123", EmployeeRole.AGENT));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(captor.getValue().getRole()).isEqualTo(EmployeeRole.AGENT);
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.employee().email()).isEqualTo("grace@crm.com");
    }

    @Test
    void register_defaultsRoleToAgent() {
        when(employeeRepository.existsByEmail("grace@crm.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(Employee.class))).thenReturn("jwt-token");

        authService.register(new RegisterRequest("Grace Hopper", "grace@crm.com", "secret123", null));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(EmployeeRole.AGENT);
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        when(employeeRepository.existsByEmail("grace@crm.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Grace Hopper", "grace@crm.com", "secret123", EmployeeRole.AGENT)))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void login_returnsTokenWhenCredentialsValid() {
        Employee employee = new Employee("Grace Hopper", "grace@crm.com", EmployeeRole.AGENT);
        employee.setId(1L);
        employee.setPasswordHash("encoded-hash");
        when(employeeRepository.findByEmail("grace@crm.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("secret123", "encoded-hash")).thenReturn(true);
        when(jwtService.generateToken(employee)).thenReturn("jwt-token");

        AuthResponse result = authService.login(new LoginRequest("grace@crm.com", "secret123"));

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.employee().id()).isEqualTo(1L);
    }

    @Test
    void login_throwsWhenPasswordWrong() {
        Employee employee = new Employee("Grace Hopper", "grace@crm.com", EmployeeRole.AGENT);
        employee.setPasswordHash("encoded-hash");
        when(employeeRepository.findByEmail("grace@crm.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("wrong", "encoded-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("grace@crm.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_throwsWhenEmailUnknown() {
        when(employeeRepository.findByEmail("nobody@crm.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@crm.com", "secret123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsWhenEmployeeHasNoPassword() {
        Employee employee = new Employee("Legacy", "legacy@crm.com", EmployeeRole.SUPPORT);
        when(employeeRepository.findByEmail("legacy@crm.com")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> authService.login(new LoginRequest("legacy@crm.com", "secret123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
