package central_api.central_api.controller;

import central_api.central_api.dto.request.BookingRequest;
import central_api.central_api.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(bookingService.createBooking(request, token));
    }

    @GetMapping("/pnr/{pnr}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AIRLINE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getBookingByPNR(@PathVariable String pnr) {
        return ResponseEntity.ok(bookingService.getBookingByPNR(pnr));
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<Map<String, Object>>> getUserBookings(
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(bookingService.getUserBookings(token));
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AIRLINE_ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }
}
