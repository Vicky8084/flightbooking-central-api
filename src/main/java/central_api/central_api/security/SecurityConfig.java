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
                                "/flights/results",
                                "/price-comparison",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // ========== PUBLIC API ENDPOINTS ==========
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
                                "/api/db/flights/*/price-breakdown",
                                "/api/config/razorpay-key"
                        ).permitAll()

                        // ========== PAYMENT ENDPOINTS ==========
                        .requestMatchers(
                                "/api/payments/create-order",
                                "/api/payments/verify",
                                "/api/payments/status/**"
                        ).permitAll()

                        // ========== USER DASHBOARD & BOOKING ==========
                        .requestMatchers(
                                "/user-dashboard",
                                "/my-bookings",
                                "/booking-details",
                                "/user/api/**",
                                "/booking",
                                "/passenger-details"
                        ).authenticated()

                        // ========== DB API PROXY ==========
                        .requestMatchers("/api/db/**").authenticated()

                        // ========== AIRLINE ADMIN ==========
                        .requestMatchers(
                                "/airline-dashboard",
                                "/airline-flights",
                                "/airline-aircraft",
                                "/airline-bookings"
                        ).hasRole("AIRLINE_ADMIN")

                        // ========== SYSTEM ADMIN ==========
                        .requestMatchers(
                                "/system-dashboard",
                                "/admin/airlines/**"
                        ).hasRole("SYSTEM_ADMIN")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}