package com.nassim.crm_api.security;

import com.nassim.crm_api.Employee.Employee;
import com.nassim.crm_api.Employee.EmployeeRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;

    public JwtAuthenticationFilter(JwtService jwtService, EmployeeRepository employeeRepository) {
        this.jwtService = jwtService;
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        try {
            String email = jwtService.extractSubject(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                employeeRepository.findByEmail(email).ifPresent(employee -> authenticate(employee));
            }
        } catch (JwtException | IllegalArgumentException ignored) {
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(Employee employee) {
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()));
        var authentication = new UsernamePasswordAuthenticationToken(employee, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
