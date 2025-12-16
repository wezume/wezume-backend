package com.example.vprofile.jwttoken;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;
    public String generateToken(String name, String email) {
        System.out.println("Generating JWT token for username: " + name);

        String token = Jwts.builder()
                .setSubject(email)
                .claim("name", name)
                .setIssuedAt(new Date())
                // .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();

        System.out.println("JWT token generated: " + token);
        return token;
    }
    @SuppressWarnings("UseSpecificCatch")
    public boolean validateToken(String token) {
        System.out.println("Validating JWT token: " + token);
        try {
            Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token);
            System.out.println("Token is valid.");
            return true;
        } catch (Exception e) {
            System.out.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }
    @SuppressWarnings("UseSpecificCatch")
    public String extractUsername(String token) {
        try {
            System.out.println("Parsing token: " + token);
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            System.out.println("Decoded claims: " + claims);
            String username = claims.get("name", String.class);
            System.out.println("Extracted username: " + username);
            return username;
        } catch (Exception e) {
            System.out.println("Failed to extract username: " + e.getMessage());
        }
        return null;
    }
    @SuppressWarnings("UseSpecificCatch")
    public String extractEmail(String token) {
        try {
            System.out.println("Parsing token: " + token);
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            System.out.println("Decoded claims: " + claims);
            String email = claims.getSubject();
            System.out.println("Extracted email: " + email);
            return email;
        } catch (Exception e) {
            System.out.println("Failed to extract email: " + e.getMessage());
        }
        return null;
    }

}
