package com.nassim.crm_api.Employee;

import com.nassim.crm_api.exception.EmployeeNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public EmployeeResponse getEmployeeById(Long id) {
        return toResponse(getEntityById(id));
    }

    public EmployeeResponse createEmployee(EmployeeRequest request) {
        Employee employee = new Employee(
                request.name(),
                request.email(),
                request.role()
        );
        applyPassword(request.password(), employee);
        return toResponse(employeeRepository.save(employee));
    }

    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = getEntityById(id);
        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setRole(request.role());
        applyPassword(request.password(), employee);
        return toResponse(employeeRepository.save(employee));
    }

    public void deleteEmployee(Long id) {
        getEntityById(id);
        employeeRepository.deleteById(id);
    }

    private Employee getEntityById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private void applyPassword(String password, Employee employee) {
        if (password != null && !password.isBlank()) {
            employee.setPasswordHash(passwordEncoder.encode(password));
        }
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole()
        );
    }
}
