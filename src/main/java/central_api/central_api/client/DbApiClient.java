package central_api.central_api.client;

import central_api.central_api.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "db-api", url = "${db.api.url}")
public interface DbApiClient {

    // ========== USER ENDPOINTS ==========

    @PostMapping("/users")
    Map<String, Object> createUser(@RequestBody Map<String, Object> user);

    @GetMapping("/users/email/{email}")
    UserResponse getUserByEmail(@PathVariable("email") String email);

    @GetMapping("/users/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);

    @GetMapping("/users/role/{role}")
    List<UserResponse> getUsersByRole(@PathVariable("role") String role);

    @PutMapping("/users/{userId}/password")
    Void updatePassword(@PathVariable("userId") Long userId, @RequestParam("password") String password);

    // ========== AIRLINE ENDPOINTS ==========

    @PostMapping("/airlines")
    Map<String, Object> createAirline(@RequestBody Map<String, Object> airline);

    @GetMapping("/airlines/pending")
    List<Map<String, Object>> getPendingAirlines();

    @GetMapping("/airlines/active")
    List<Map<String, Object>> getActiveAirlines();

    @GetMapping("/airlines/{id}")
    Map<String, Object> getAirlineById(@PathVariable("id") Long id);

    @PutMapping("/airlines/{airlineId}/approve")
    Map<String, Object> approveAirline(@PathVariable("airlineId") Long airlineId, @RequestParam("adminId") Long adminId);

    @PutMapping("/airlines/{airlineId}/reject")
    Map<String, Object> rejectAirline(@PathVariable("airlineId") Long airlineId,
                                      @RequestParam("adminId") Long adminId,
                                      @RequestParam(value = "reason", required = false) String reason);

    // ========== AIRCRAFT ENDPOINTS ==========

    @GetMapping("/aircrafts/airline/{airlineId}")
    List<Map<String, Object>> getAircraftsByAirline(@PathVariable("airlineId") Long airlineId);

    @PostMapping("/aircrafts")
    Map<String, Object> createAircraft(@RequestBody Map<String, Object> aircraft);

    // ========== AIRPORT ENDPOINTS ==========

    @GetMapping("/airports")
    List<Map<String, Object>> getAllAirports();

    // ========== FLIGHT ENDPOINTS ==========

    @PostMapping("/flights")
    Map<String, Object> createFlight(@RequestBody Map<String, Object> flight);

    @PostMapping("/flights/search")
    List<Object> searchFlights(@RequestBody Map<String, Object> searchRequest);

    @GetMapping("/flights/{flightId}")
    Map<String, Object> getFlightById(@PathVariable("flightId") Long flightId);

    @GetMapping("/flights/airline/{airlineId}")
    List<Map<String, Object>> getFlightsByAirline(@PathVariable("airlineId") Long airlineId);

    @GetMapping("/flights/{flightId}/seatmap")
    Map<String, Object> getSeatMap(@PathVariable("flightId") Long flightId);

    @PutMapping("/flights/{flightId}/delay")
    Map<String, Object> delayFlight(@PathVariable("flightId") Long flightId, @RequestParam("minutes") int minutes);

    @PutMapping("/flights/{flightId}/reschedule")
    Map<String, Object> rescheduleFlight(@PathVariable("flightId") Long flightId,
                                         @RequestParam("newDepartureTime") String newDepartureTime,
                                         @RequestParam("newArrivalTime") String newArrivalTime);

    @PutMapping("/flights/{flightId}/cancel")
    Map<String, Object> cancelFlight(@PathVariable("flightId") Long flightId);

    // ========== REVIEW ENDPOINTS ==========

    @GetMapping("/reviews/airline/{airlineId}")
    Map<String, Object> getAirlineReviews(@PathVariable("airlineId") Long airlineId);

    // ========== BOOKING ENDPOINTS ==========

    @PostMapping("/bookings")
    Map<String, Object> createBooking(@RequestBody Map<String, Object> bookingRequest);

    @GetMapping("/bookings/pnr/{pnr}")
    Map<String, Object> getBookingByPNR(@PathVariable("pnr") String pnr);

    @GetMapping("/bookings/user/{userId}")
    List<Map<String, Object>> getUserBookings(@PathVariable("userId") Long userId);

    @PutMapping("/bookings/{bookingId}/cancel")
    Map<String, Object> cancelBooking(@PathVariable("bookingId") Long bookingId);
}