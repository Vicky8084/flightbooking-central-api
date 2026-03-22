package central_api.central_api.security;

import central_api.central_api.client.AuthApiClient;
import central_api.central_api.dto.response.ValidateTokenResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthApiClient authApiClient;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        log.debug("Request path: {}", requestPath);

        // ✅ Public paths - skip authentication
        if (isPublicPath(requestPath)) {
            log.debug("Public path, skipping authentication: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Get token from cookie
        String token = getTokenFromCookies(request);
        log.debug("Token from cookie: {}", token != null ? "present" : "null");

        if (token == null) {
            log.warn("No token found for protected path: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Authentication required\"}");
            return;
        }

        // ✅ Validate token with Auth API
        try {
            ValidateTokenResponse validation = authApiClient.validateToken("Bearer " + token);
            log.debug("Token validation result: valid={}, role={}", validation.isValid(), validation.getRole());

            if (validation.isValid()) {
                // ✅ Create authentication with roles
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + validation.getRole())
                );

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        validation.getEmail(),
                        null,
                        authorities
                );
                authToken.setDetails(validation);
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authentication set for user: {}, role: {}", validation.getEmail(), validation.getRole());
            } else {
                log.warn("Invalid token for path: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"" + validation.getMessage() + "\"}");
                return;
            }
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token validation failed\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.debug("No cookies found");
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> "token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isPublicPath(String path) {
        return path.equals("/") ||
                path.equals("/home") ||
                path.equals("/signin") ||
                path.equals("/signup") ||
                path.equals("/forgot-password") ||
                path.equals("/system-admin-login") ||
                path.equals("/flights/search") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register-airline") ||
                path.startsWith("/api/auth/forgot-password") ||
                path.startsWith("/api/auth/reset-password") ||
                path.startsWith("/api/auth/logout") ||
                path.startsWith("/api/auth/me");
    }
}