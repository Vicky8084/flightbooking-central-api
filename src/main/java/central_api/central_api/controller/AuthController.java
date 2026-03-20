package central_api.central_api.controller;

import central_api.central_api.dto.request.LoginRequest;
import central_api.central_api.dto.request.RegisterAirlineRequest;
import central_api.central_api.dto.request.RegisterRequest;
import central_api.central_api.dto.response.AuthResponse;
import central_api.central_api.service.AuthService;
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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/register-airline")
    public ResponseEntity<AuthResponse> registerAirline(@Valid @RequestBody RegisterAirlineRequest request) {
        return ResponseEntity.ok(authService.registerAirline(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ✅ ADD THIS ENDPOINT
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        authService.forgotPassword(email);

        Map<String, String> response = new HashMap<>();
        response.put("message", "If your email is registered, you will receive an OTP shortly.");
        return ResponseEntity.ok(response);
    }

    // ✅ ADD THIS ENDPOINT
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");

        authService.resetPassword(email, newPassword);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successfully!");
        return ResponseEntity.ok(response);
    }
}