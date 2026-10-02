package com.smartlib.config;

import com.smartlib.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    // ============================================================
    // PASSWORD ENCODER
    // ============================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // ============================================================
    // CORS CONFIGURATION
    // ============================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        List<String> allowedOrigins = new java.util.ArrayList<>(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174",
                "https://smartlib-frontend-nmml.onrender.com"
        ));
        String envFrontend = System.getenv("FRONTEND_URL");
        if (envFrontend != null && !envFrontend.isBlank() && !allowedOrigins.contains(envFrontend.trim())) {
            allowedOrigins.add(envFrontend.trim());
        }
        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }


    // ============================================================
    // SECURITY FILTER CHAIN
    // ============================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

                // ------------------------------------------------
                // Disable CSRF
                // ------------------------------------------------

                .csrf(csrf ->
                        csrf.disable()
                )


                // ------------------------------------------------
                // Enable CORS
                // ------------------------------------------------

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )


                // ------------------------------------------------
                // JWT = STATELESS
                // ------------------------------------------------

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                // ------------------------------------------------
                // AUTHORIZATION RULES
                // ------------------------------------------------

                .authorizeHttpRequests(auth -> {

                    // =================================================
                    // PUBLIC AUTH
                    // =================================================

                    auth.requestMatchers(
                            "/api/auth/register",
                            "/api/auth/login",
                            "/api/auth/forgot-password",
                            "/api/auth/reset-password"
                    ).permitAll();


                    // =================================================
                    // PUBLIC BOOK CATALOG
                    // =================================================

                    auth.requestMatchers(
                            "/api/books/**",
                            "/api/categories/**"
                    ).permitAll();

                    auth.requestMatchers(
        HttpMethod.GET,
        "/api/book-copies/**"
).permitAll();


                    // =================================================
                    // PUBLIC BOOK REVIEWS
                    // Anyone can view reviews
                    // =================================================

                    auth.requestMatchers(
                            "/api/reviews/book/**"
                    ).permitAll();


                    // =================================================
                    // ADMIN ENDPOINTS
                    // =================================================

                    auth.requestMatchers(
                            "/api/admin/**"
                    ).hasRole("ADMIN");


                    // =================================================
                    // MEMBER BORROWING + RESERVATION
                    // =================================================

                    auth.requestMatchers(
        "/api/borrowings/**",
        "/api/reservations/**"
).hasAnyRole("MEMBER", "ADMIN");


                    // =================================================
                    // REVIEW MANAGEMENT
                    // MEMBER + ADMIN
                    // =================================================

                    auth.requestMatchers(
        "/api/reviews/**"
).hasAnyRole("MEMBER", "ADMIN");
auth.requestMatchers(
        "/api/book-copies/qr/**"
).hasAnyRole("MEMBER", "ADMIN");

                    // =================================================
                    // AI ASSISTANT
                    // MEMBER + ADMIN
                    // =================================================

                    auth.requestMatchers(
                            "/api/ai/**"
                    ).hasAnyRole("MEMBER", "ADMIN");


                    // =================================================
                    // OBSERVABILITY / ACTUATOR
                    // Health/info public for cloud orchestration; metrics admin only
                    // =================================================

                    auth.requestMatchers(
                            "/actuator/health",
                            "/actuator/info"
                    ).permitAll();

                    auth.requestMatchers(
                            "/actuator/metrics",
                            "/actuator/metrics/**"
                    ).hasRole("ADMIN");


                    // =================================================
                    // EVERYTHING ELSE
                    // =================================================

                    auth.anyRequest().authenticated();
                })


                // ------------------------------------------------
                // JWT FILTER
                // ------------------------------------------------

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }
}