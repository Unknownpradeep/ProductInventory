package com.hepl.product.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

import com.hepl.product.Filter.JwtAuthenticationFilter;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/impersonate/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/v1/auth/**",
                                "/api/v1/invoices/download/order/**",
                                "/api/v1/orders/code/**",
                                "/api/v1/orders/server-ip",
                                "/api/stock-requests",
                                "/api/v1/email/**",
                                "/uploads/**")
                        .permitAll()
                        // Admin only endpoints
                        .requestMatchers("/api/v1/users/**", "/api/v1/roles/**", "/api/v1/permissions/**")
                        .hasRole("ADMIN")

                        // Allow read-only (GET) requests for ALL authenticated users (including VIEWER)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/**")
                        .hasAnyRole("ADMIN", "MANAGER", "SALES", "STORES", "DISPATCH", "DELIVERY", "VIEWER")

                        // Order Creation & Updation
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER", "SALES")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/orders/*/status")
                        .hasAnyRole("ADMIN", "MANAGER", "STORES", "DISPATCH", "DELIVERY")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/orders/*/payment")
                        .hasAnyRole("ADMIN", "MANAGER", "SALES")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER", "SALES")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // Customers modifications
                        .requestMatchers("/api/v1/customers/**").hasAnyRole("ADMIN", "MANAGER", "SALES")

                        // Stocks & Stock Requests modifications
                        .requestMatchers("/api/v1/stocks/**").hasAnyRole("ADMIN", "MANAGER", "STORES")
                        .requestMatchers("/api/v1/stock-requests/**")
                        .hasAnyRole("ADMIN", "MANAGER", "STORES", "VIEWER", "SALES")

                        // Product & other structural changes
                        .requestMatchers("/api/v1/product/**", "/api/v1/divisions/**", "/api/v1/invoices/**",
                                "/api/v1/price-lists/**", "/api/v1/item-groups/**", "/api/v1/composite/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .anyRequest().authenticated())

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                    .getContext().getAuthentication();
                            String roleInfo = "";
                            if (auth != null) {
                                roleInfo = " for role(s) " + auth.getAuthorities();
                            }
                            response.setStatus(org.springframework.http.HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write(String.format(
                                    "{\"status\": 403, \"error\": \"Forbidden\", \"message\": \"Access Denied: You do not have the required role to access this resource%s.\", \"data\": null}",
                                    roleInfo));
                        }))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

