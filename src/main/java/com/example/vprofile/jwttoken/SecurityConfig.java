package com.example.vprofile.jwttoken;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Using BCrypt for password hashing
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults()) // Enable CORS configuration
            .csrf(csrf -> csrf.disable())  // Disable CSRF using new API
            .httpBasic(httpBasic -> httpBasic.disable())  // Disable HTTP Basic Authentication using new API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/users/signup/user", "/api/login","/api/users/check-Recruteremail","/users/check-email","/api/users/check-phone","/api/users/update-password"
                ,"/api/verify-email","/api/verify/placement/{token}","/api/auth/signup/placement","/api/users/share","/api/videos/video/{videoId}").permitAll()  // Allow signup and login without authentication
                .anyRequest().authenticated()  // Protect other routes
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);  // Add JWT filter

        return http.build();
    }
}
