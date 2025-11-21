package com.coolcoder.security;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    // Load from environment variables (Railway)
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expiration;

    // Convert secret to HMAC key
    private Key getKey() {
        // Use raw string -> simpler, safer, no Base64 needed
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Extract username
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract specific claim
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return resolver.apply(claims);
    }

    // Generate token without extra claims
    public String generateToken(UserDetails user) {
        return generateToken(Map.of(), user);
    }

    // Generate JWT token
    public String generateToken(Map<String, Object> claims, UserDetails user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate JWT
    public boolean isValid(String token, UserDetails user) {
        String username = extractUsername(token);
        Date expiry = extractClaim(token, Claims::getExpiration);

        return username.equals(user.getUsername()) && expiry.after(new Date());
    }
}
