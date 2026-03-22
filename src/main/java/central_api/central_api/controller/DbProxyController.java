package central_api.central_api.controller;

import central_api.central_api.client.DbApiClient;
import central_api.central_api.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db")
@RequiredArgsConstructor
public class DbProxyController {

    private final DbApiClient dbApiClient;

    // ========== FLIGHT ENDPOINTS ==========

    @GetMapping("/flights/airline/{airlineId}")
    public ResponseEntity<List<Map<String, Object>>> getFlightsByAirline(@PathVariable Long airlineId) {
        return ResponseEntity.ok(dbApiClient.getFlightsByAirline(airlineId));
    }

    @PostMapping("/flights")
    public ResponseEntity<Map<String, Object>> createFlight(@RequestBody Map<String, Object> flight) {
        return ResponseEntity.ok(dbApiClient.createFlight(flight));
    }

    @PutMapping("/flights/{flightId}/delay")
    public ResponseEntity<Map<String, Object>> delayFlight(@PathVariable Long flightId, @RequestParam int minutes) {
        return ResponseEntity.ok(dbApiClient.delayFlight(flightId, minutes));
    }

    @PutMapping("/flights/{flightId}/reschedule")
    public ResponseEntity<Map<String, Object>> rescheduleFlight(
            @PathVariable Long flightId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDepartureTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newArrivalTime) {

        // Convert LocalDateTime to String for Feign client
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        String departureStr = newDepartureTime.format(formatter);
        String arrivalStr = newArrivalTime.format(formatter);

        return ResponseEntity.ok(dbApiClient.rescheduleFlight(flightId, departureStr, arrivalStr));
    }

    @GetMapping("/flights/{flightId}")
    public ResponseEntity<Map<String, Object>> getFlightById(@PathVariable Long flightId) {
        return ResponseEntity.ok(dbApiClient.getFlightById(flightId));
    }

    @GetMapping("/flights/{flightId}/seatmap")
    public ResponseEntity<Map<String, Object>> getSeatMap(@PathVariable Long flightId) {
        return ResponseEntity.ok(dbApiClient.getSeatMap(flightId));
    }

    @PostMapping("/flights/search")
    public ResponseEntity<List<Object>> searchFlights(@RequestBody Map<String, Object> searchRequest) {
        return ResponseEntity.ok(dbApiClient.searchFlights(searchRequest));
    }

    // ========== AIRCRAFT ENDPOINTS ==========

    @GetMapping("/aircrafts/airline/{airlineId}")
    public ResponseEntity<List<Map<String, Object>>> getAircraftsByAirline(@PathVariable Long airlineId) {
        return ResponseEntity.ok(dbApiClient.getAircraftsByAirline(airlineId));
    }

    @PostMapping("/aircrafts")
    public ResponseEntity<Map<String, Object>> createAircraft(@RequestBody Map<String, Object> aircraft) {
        return ResponseEntity.ok(dbApiClient.createAircraft(aircraft));
    }

    // ========== AIRPORT ENDPOINTS ==========

    @GetMapping("/airports")
    public ResponseEntity<List<Map<String, Object>>> getAllAirports() {
        return ResponseEntity.ok(dbApiClient.getAllAirports());
    }

    // ========== BOOKING ENDPOINTS ==========

    @GetMapping("/bookings/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserBookings(@PathVariable Long userId) {
        return ResponseEntity.ok(dbApiClient.getUserBookings(userId));
    }

    @GetMapping("/bookings/pnr/{pnr}")
    public ResponseEntity<Map<String, Object>> getBookingByPNR(@PathVariable String pnr) {
        return ResponseEntity.ok(dbApiClient.getBookingByPNR(pnr));
    }

    @PostMapping("/bookings")
    public ResponseEntity<Map<String, Object>> createBooking(@RequestBody Map<String, Object> booking) {
        return ResponseEntity.ok(dbApiClient.createBooking(booking));
    }

    @PutMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(dbApiClient.cancelBooking(bookingId));
    }

    // ========== REVIEW ENDPOINTS ==========

    @GetMapping("/reviews/airline/{airlineId}")
    public ResponseEntity<Map<String, Object>> getAirlineReviews(@PathVariable Long airlineId) {
        return ResponseEntity.ok(dbApiClient.getAirlineReviews(airlineId));
    }

    // ========== AIRLINE ENDPOINTS ==========

    @GetMapping("/airlines/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingAirlines() {
        return ResponseEntity.ok(dbApiClient.getPendingAirlines());
    }

    @GetMapping("/airlines/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveAirlines() {
        return ResponseEntity.ok(dbApiClient.getActiveAirlines());
    }

    @GetMapping("/airlines/{id}")
    public ResponseEntity<Map<String, Object>> getAirlineById(@PathVariable Long id) {
        return ResponseEntity.ok(dbApiClient.getAirlineById(id));
    }

    @PutMapping("/airlines/{airlineId}/approve")
    public ResponseEntity<Map<String, Object>> approveAirline(@PathVariable Long airlineId, @RequestParam Long adminId) {
        return ResponseEntity.ok(dbApiClient.approveAirline(airlineId, adminId));
    }

    @PutMapping("/airlines/{airlineId}/reject")
    public ResponseEntity<Map<String, Object>> rejectAirline(
            @PathVariable Long airlineId,
            @RequestParam Long adminId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(dbApiClient.rejectAirline(airlineId, adminId, reason));
    }

    // ========== USER ENDPOINTS ==========

    @GetMapping("/users/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(dbApiClient.getUserByEmail(email));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(dbApiClient.getUserById(id));
    }
}