package com.nassim.crm_api.security;

import com.nassim.crm_api.Employee.Employee;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${crm.jwt.secret}") String secret,
                      @Value("${crm.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Employee employee) {
        Date now = new Date();
        return Jwts.builder()
                .subject(employee.getEmail())
                .claim("employeeId", employee.getId())
                .claim("role", employee.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractSubject(String token) {
        return parse(token).getSubject();
    }

    public String extractRole(String token) {
        return parse(token).get("role", String.class);
    }

    public boolean isTokenValid(String token, Employee employee) {
        Claims claims = parse(token);
        return claims.getSubject().equals(employee.getEmail())
                && claims.getExpiration().after(new Date());
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
