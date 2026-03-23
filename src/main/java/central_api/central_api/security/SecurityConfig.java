package central_api.central_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ========== PUBLIC PAGES (No Auth Required) ==========
                        .requestMatchers(
                                "/",
                                "/home",
                                "/signin",
                                "/signup",
                                "/forgot-password",
                                "/system-admin-login",
                                "/flights/search",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // ========== PUBLIC API ENDPOINTS (No Auth Required) ==========
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/register-airline",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/logout",
                                "/api/auth/me",
                                "/api/db/airports",
                                "/api/db/flights/search",
                                "/api/db/fare-classes",
                                "/api/db/fare-classes/**",
                                "/api/db/flights/*/price-breakdown"
                        ).permitAll()

                        // ========== USER DASHBOARD PAGES (Authenticated Only) ==========
                        .requestMatchers(
                                "/user-dashboard",
                                "/my-bookings",
                                "/booking-details",
                                "/booking-details/**"
                        ).authenticated()

                        // ========== USER API ENDPOINTS (Authenticated Only) ==========
                        .requestMatchers(
                                "/user/api/profile",
                                "/user/api/bookings",
                                "/user/api/bookings/**",
                                "/user/api/bookings/*/cancel"
                        ).authenticated()

                        // ========== DB API PROXY ENDPOINTS (Mix) ==========
                        // Public DB endpoints
                        .requestMatchers(
                                "/api/db/airports",
                                "/api/db/flights/search",
                                "/api/db/fare-classes",
                                "/api/db/fare-classes/**"
                        ).permitAll()
                        // Protected DB endpoints
                        .requestMatchers("/api/db/**").authenticated()

                        // ========== AIRLINE DASHBOARD ==========
                        .requestMatchers(
                                "/airline-dashboard",
                                "/airline-flights",
                                "/airline-aircraft",
                                "/airline-bookings"
                        ).hasRole("AIRLINE_ADMIN")

                        // ========== SYSTEM ADMIN DASHBOARD ==========
                        .requestMatchers(
                                "/system-dashboard",
                                "/admin/airlines/**"
                        ).hasRole("SYSTEM_ADMIN")

                        // ========== DEFAULT: All other requests need authentication ==========
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}