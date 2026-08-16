package com.nassim.crm_api.Employee;

import com.nassim.crm_api.exception.EmployeeNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    private EmployeeRequest request() {
        return new EmployeeRequest("Grace Hopper", "grace@crm.com", EmployeeRole.AGENT, null);
    }

    @Test
    void getAllEmployees_returnsAll() {
        when(employeeRepository.findAll()).thenReturn(List.of(new Employee("Grace", "grace@crm.com", EmployeeRole.AGENT)));

        List<EmployeeResponse> employees = employeeService.getAllEmployees();

        assertThat(employees).hasSize(1);
        assertThat(employees.get(0).email()).isEqualTo("grace@crm.com");
    }

    @Test
    void getEmployeeById_returnsWhenFound() {
        Employee employee = new Employee("Grace", "grace@crm.com", EmployeeRole.AGENT);
        employee.setId(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponse result = employeeService.getEmployeeById(1L);

        assertThat(result.role()).isEqualTo(EmployeeRole.AGENT);
    }

    @Test
    void getEmployeeById_throwsWhenNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createEmployee_mapsRequestAndSaves() {
        Employee saved = new Employee("Grace Hopper", "grace@crm.com", EmployeeRole.AGENT);
        saved.setId(3L);
        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        EmployeeResponse result = employeeService.createEmployee(request());

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Grace Hopper");
        assertThat(captor.getValue().getRole()).isEqualTo(EmployeeRole.AGENT);
        assertThat(result.id()).isEqualTo(3L);
    }

    @Test
    void createEmployee_encodesPasswordWhenProvided() {
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-hash");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse result = employeeService.createEmployee(
                new EmployeeRequest("Grace Hopper", "grace@crm.com", EmployeeRole.AGENT, "secret123"));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(result.email()).isEqualTo("grace@crm.com");
    }

    @Test
    void updateEmployee_updatesAndSaves() {
        Employee existing = new Employee("Old", "old@crm.com", EmployeeRole.SUPPORT);
        existing.setId(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenReturn(existing);

        EmployeeResponse result = employeeService.updateEmployee(1L, request());

        assertThat(result.name()).isEqualTo("Grace Hopper");
        assertThat(result.email()).isEqualTo("grace@crm.com");
        assertThat(result.role()).isEqualTo(EmployeeRole.AGENT);
    }

    @Test
    void updateEmployee_throwsWhenNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployee(99L, request()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void deleteEmployee_deletesWhenFound() {
        Employee existing = new Employee("Grace", "grace@crm.com", EmployeeRole.AGENT);
        existing.setId(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));

        employeeService.deleteEmployee(1L);

        verify(employeeRepository).deleteById(1L);
    }

    @Test
    void deleteEmployee_throwsWhenNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.deleteEmployee(99L))
                .isInstanceOf(EmployeeNotFoundException.class);
        verify(employeeRepository, never()).deleteById(99L);
    }
}
