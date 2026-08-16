package com.nassim.crm_api.config;

import com.nassim.crm_api.Employee.Employee;
import com.nassim.crm_api.Employee.EmployeeRepository;
import com.nassim.crm_api.Employee.EmployeeRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (employeeRepository.count() == 0) {
            Employee admin = new Employee("System Admin", "admin@crm.com", EmployeeRole.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            employeeRepository.save(admin);
            log.info("Seeded admin user: admin@crm.com / admin123");
        }
    }
}
