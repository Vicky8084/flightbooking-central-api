package central_api.central_api.client;

import central_api.central_api.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "db-api", url = "${db.api.url}")
public interface DbApiClient {

    // User endpoints
    @PostMapping("/users")
    Map<String, Object> createUser(@RequestBody Map<String, Object> user);

    @GetMapping("/users/email/{email}")
    UserResponse getUserByEmail(@PathVariable("email") String email);

    @GetMapping("/users/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);

    @GetMapping("/users/role/{role}")
    List<UserResponse> getUsersByRole(@PathVariable("role") String role);

    // Airline endpoints
    @PostMapping("/airlines")
    Map<String, Object> createAirline(@RequestBody Map<String, Object> airline);

    @GetMapping("/airlines/pending")
    List<Map<String, Object>> getPendingAirlines();

    @GetMapping("/airlines/active")
    List<Map<String, Object>> getActiveAirlines();

    // ✅ NEW METHOD - Get airline by ID
    @GetMapping("/airlines/{id}")
    Map<String, Object> getAirlineById(@PathVariable("id") Long id);

    @PutMapping("/airlines/{airlineId}/approve")
    Map<String, Object> approveAirline(@PathVariable("airlineId") Long airlineId, @RequestParam("adminId") Long adminId);

    @PutMapping("/airlines/{airlineId}/reject")
    Map<String, Object> rejectAirline(@PathVariable("airlineId") Long airlineId, @RequestParam("adminId") Long adminId, @RequestParam(value = "reason", required = false) String reason);

    // Flight endpoints
    @PostMapping("/flights/search")
    List<Object> searchFlights(@RequestBody Map<String, Object> searchRequest);

    @GetMapping("/flights/{flightId}")
    Map<String, Object> getFlightById(@PathVariable("flightId") Long flightId);

    @GetMapping("/flights/{flightId}/seatmap")
    Map<String, Object> getSeatMap(@PathVariable("flightId") Long flightId);

    // Booking endpoints
    @PostMapping("/bookings")
    Map<String, Object> createBooking(@RequestBody Map<String, Object> bookingRequest);

    @GetMapping("/bookings/pnr/{pnr}")
    Map<String, Object> getBookingByPNR(@PathVariable("pnr") String pnr);

    @GetMapping("/bookings/user/{userId}")
    List<Map<String, Object>> getUserBookings(@PathVariable("userId") Long userId);

    @PutMapping("/bookings/{bookingId}/cancel")
    Map<String, Object> cancelBooking(@PathVariable("bookingId") Long bookingId);

    @PutMapping("/users/{userId}/password")
    Map<String, Object> updatePassword(@PathVariable("userId") Long userId, @RequestParam("password") String password);
}