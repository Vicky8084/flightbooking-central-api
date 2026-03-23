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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserDashboardController {

    private final UserDashboardService userDashboardService;
    private final JwtUtil jwtUtil;

    /**
     * Serve user dashboard page
     */
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

    /**
     * API: Get user profile
     */
    @GetMapping("/api/profile")
    @ResponseBody
    public ResponseEntity<UserProfileDTO> getProfile(HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = jwtUtil.extractUserId(token);
        return ResponseEntity.ok(userDashboardService.getUserProfile(userId));
    }

    /**
     * API: Get all user bookings
     */
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

    /**
     * API: Get upcoming bookings
     */
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

    /**
     * API: Get past bookings
     */
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

    /**
     * API: Get cancelled bookings
     */
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

    /**
     * API: Get booking details by ID
     */
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

    /**
     * API: Cancel booking
     */
    @PostMapping("/api/bookings/{bookingId}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        String token = getTokenFromCookies(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = jwtUtil.extractUserId(token);
        String reason = requestBody.getOrDefault("reason", "");

        Map<String, Object> result = userDashboardService.cancelBooking(bookingId, userId, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * API: Update user profile
     */
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
     * Extract token from cookies
     */
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