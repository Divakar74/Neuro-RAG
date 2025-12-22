package com.skillmap.config;

import com.skillmap.config.JwtAuthFilter;
import com.skillmap.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public endpoints
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/resume/parse").permitAll() // Allow test parsing endpoint
                // Protected endpoints - require authentication
                .requestMatchers("/api/auth/validate", "/api/auth/logout").authenticated()
                .requestMatchers("/api/assessment/**").authenticated()
                .requestMatchers("/api/questions/**").authenticated()
                .requestMatchers("/api/responses/**").authenticated()
                .requestMatchers("/api/resume/**").authenticated()
                .requestMatchers("/api/roadmap/**").authenticated()
                .requestMatchers("/api/feedback/**").authenticated()
                .requestMatchers("/api/ai/**").authenticated()
                .anyRequest().permitAll()
            )
            .authenticationProvider(authenticationProvider());

        // JWT filter before username/password auth
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
