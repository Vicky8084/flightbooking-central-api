package central_api.central_api.controller;

import central_api.central_api.client.AuthApiClient;
import central_api.central_api.dto.request.LoginRequest;
import central_api.central_api.dto.request.RegisterAirlineRequest;
import central_api.central_api.dto.request.RegisterRequest;
import central_api.central_api.dto.response.AuthResponse;
import central_api.central_api.dto.response.ValidateTokenResponse;
import central_api.central_api.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthApiClient authApiClient;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/register-airline")
    public ResponseEntity<AuthResponse> registerAirline(@Valid @RequestBody RegisterAirlineRequest request) {
        return ResponseEntity.ok(authService.registerAirline(request));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);

        Cookie tokenCookie = new Cookie("token", authResponse.getToken());
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(true);
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(24 * 60 * 60);
        tokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(tokenCookie);

        if (authResponse.getRefreshToken() != null) {
            Cookie refreshCookie = new Cookie("refreshToken", authResponse.getRefreshToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60);
            refreshCookie.setAttribute("SameSite", "Strict");
            response.addCookie(refreshCookie);
        }

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("id", authResponse.getId());
        responseBody.put("email", authResponse.getEmail());
        responseBody.put("fullName", authResponse.getFullName());
        responseBody.put("role", authResponse.getRole());
        responseBody.put("airlineId", authResponse.getAirlineId());
        responseBody.put("status", authResponse.getStatus());
        responseBody.put("message", "Login successful");

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        Cookie tokenCookie = new Cookie("token", null);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(true);
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(0);
        tokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(tokenCookie);

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "Logged out successfully");
        return ResponseEntity.ok(responseBody);
    }

    // ✅ NEW: Get current user from token
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@CookieValue(name = "token", required = false) String token) {
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            ValidateTokenResponse validation = authApiClient.validateToken("Bearer " + token);

            if (validation.isValid()) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", validation.getUserId());
                userInfo.put("email", validation.getEmail());
                userInfo.put("role", validation.getRole());
                userInfo.put("airlineId", validation.getAirlineId());
                userInfo.put("status", validation.getStatus());
                return ResponseEntity.ok(userInfo);
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
    }
}