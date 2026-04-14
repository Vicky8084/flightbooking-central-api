package central_api.central_api.controller;

import central_api.central_api.dto.response.BookingDetailsDTO;
import central_api.central_api.dto.response.BookingSummaryDTO;
import central_api.central_api.dto.response.UserProfileDTO;
import central_api.central_api.security.JwtUtil;
import central_api.central_api.service.UserDashboardService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserDashboardController {

    private final UserDashboardService userDashboardService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // Java regex pattern for password validation
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{6,}$");

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return "redirect:/signin";
        }
        Long userId = jwtUtil.extractUserId(token);
        model.addAttribute("userId", userId);
        return "user-dashboard";
    }

    @GetMapping("/api/profile")
    @ResponseBody
    public ResponseEntity<UserProfileDTO> getProfile(HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        System.out.println("🔐 TOKEN from cookie: " + (token != null ? token.substring(0, Math.min(50, token.length())) + "..." : "NULL"));

        if (token == null) {
            System.out.println("❌ No token found!");
            return ResponseEntity.status(401).build();
        }

        Long userId = jwtUtil.extractUserId(token);
        System.out.println("📌 Extracted userId from token: " + userId);

        if (userId == null) {
            System.out.println("❌ userId is NULL from token!");
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(userDashboardService.getUserProfile(userId));
    }

    @GetMapping("/api/bookings")
    @ResponseBody
    public ResponseEntity<List<BookingSummaryDTO>> getAllBookings(HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(userDashboardService.getUserBookingsSummary(userId));
    }

    @GetMapping("/api/bookings/upcoming")
    @ResponseBody
    public ResponseEntity<List<BookingSummaryDTO>> getUpcomingBookings(HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(userDashboardService.getUpcomingBookings(userId));
    }

    @GetMapping("/api/bookings/past")
    @ResponseBody
    public ResponseEntity<List<BookingSummaryDTO>> getPastBookings(HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(userDashboardService.getPastBookings(userId));
    }

    @GetMapping("/api/bookings/cancelled")
    @ResponseBody
    public ResponseEntity<List<BookingSummaryDTO>> getCancelledBookings(HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(userDashboardService.getCancelledBookings(userId));
    }

    @GetMapping("/api/bookings/{bookingId}")
    @ResponseBody
    public ResponseEntity<BookingDetailsDTO> getBookingDetails(
            @PathVariable Long bookingId,
            HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(userDashboardService.getBookingDetails(bookingId, userId));
    }

    @PostMapping("/api/bookings/{bookingId}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {

        System.out.println("🔍 Cancel booking request received for bookingId: " + bookingId);

        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            System.out.println("❌ Invalid token");
            return ResponseEntity.status(401).build();
        }

        Long userId = jwtUtil.extractUserId(token);
        System.out.println("📌 User ID from token: " + userId);

        String reason = requestBody.getOrDefault("reason", "");
        System.out.println("📌 Cancellation reason: " + reason);

        try {
            Map<String, Object> result = userDashboardService.cancelBooking(bookingId, userId, reason);
            System.out.println("✅ Cancel result: " + result);

            // Check if email was sent
            if (result.containsKey("emailSent") && (Boolean) result.get("emailSent")) {
                System.out.println("📧 Cancellation email sent successfully");
            } else if (result.containsKey("emailSent")) {
                System.out.println("⚠️ Email sending failed: " + result.get("emailError"));
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.out.println("❌ Cancel error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/api/profile")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, Object> updates,
            HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(userDashboardService.updateUserProfile(userId, updates));
    }

    /**
     * Change password endpoint
     */
    @PostMapping("/api/change-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, String> passwordData,
            HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = jwtUtil.extractUserId(token);
        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Current password and new password are required");
            return ResponseEntity.badRequest().body(error);
        }

        if (newPassword.length() < 6) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "New password must be at least 6 characters");
            return ResponseEntity.badRequest().body(error);
        }

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Password must contain at least one letter and one number");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            String encryptedPassword = passwordEncoder.encode(newPassword);
            Map<String, Object> response = userDashboardService.updateUserPassword(userId, encryptedPassword);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private String getTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}