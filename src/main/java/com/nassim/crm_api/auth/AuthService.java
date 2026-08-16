package com.nassim.crm_api.auth;

import com.nassim.crm_api.Employee.Employee;
import com.nassim.crm_api.Employee.EmployeeRepository;
import com.nassim.crm_api.Employee.EmployeeResponse;
import com.nassim.crm_api.Employee.EmployeeRole;
import com.nassim.crm_api.exception.EmailAlreadyExistsException;
import com.nassim.crm_api.exception.InvalidCredentialsException;
import com.nassim.crm_api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(EmployeeRepository employeeRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        EmployeeRole role = request.role() != null ? request.role() : EmployeeRole.AGENT;
        Employee employee = new Employee(request.name(), request.email(), role);
        employee.setPasswordHash(passwordEncoder.encode(request.password()));
        employeeRepository.save(employee);
        return new AuthResponse(jwtService.generateToken(employee), toResponse(employee));
    }

    public AuthResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByEmail(request.email())
                .filter(e -> e.getPasswordHash() != null)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return new AuthResponse(jwtService.generateToken(employee), toResponse(employee));
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
