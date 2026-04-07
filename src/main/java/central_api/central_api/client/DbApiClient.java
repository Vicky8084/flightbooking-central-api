package central_api.central_api.client;

import central_api.central_api.dto.response.*;
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

    @PutMapping("/users/{userId}")
    Map<String, Object> updateUser(@PathVariable("userId") Long userId, @RequestBody Map<String, Object> userDetails);

    @GetMapping("/users/{id}/profile")
    Map<String, Object> getUserProfile(@PathVariable("id") Long id);

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

    @GetMapping("/flights/{flightId}/price-breakdown")
    Map<String, Object> getPriceBreakdown(@PathVariable("flightId") Long flightId,
                                          @RequestParam("seatClass") String seatClass,
                                          @RequestParam("fareClassCode") String fareClassCode);

    // ========== FARE CLASS ENDPOINTS ==========
    @GetMapping("/fare-classes")
    List<FareClassDTO> getFareClasses();

    @GetMapping("/fare-classes/{code}")
    FareClassDTO getFareClassByCode(@PathVariable("code") String code);

    // ========== REVIEW ENDPOINTS ==========
    @GetMapping("/reviews/airline/{airlineId}")
    Map<String, Object> getAirlineReviews(@PathVariable("airlineId") Long airlineId);

    @PostMapping("/reviews/flight")
    Map<String, Object> createFlightReview(@RequestBody Map<String, Object> review);

    // ========== BOOKING ENDPOINTS ==========
    @PostMapping("/bookings")
    Map<String, Object> createBooking(@RequestBody Map<String, Object> bookingRequest);

    @GetMapping("/bookings/pnr/{pnr}")
    Map<String, Object> getBookingByPNR(@PathVariable("pnr") String pnr);

    @GetMapping("/bookings/{bookingId}")
    Map<String, Object> getBookingById(@PathVariable("bookingId") Long bookingId);

    @PutMapping("/bookings/{bookingId}/cancel")
    Map<String, Object> cancelBooking(@PathVariable("bookingId") Long bookingId);

    @GetMapping("/bookings/user/{userId}/upcoming")
    Map<String, Object> getUserUpcomingBookings(@PathVariable("userId") Long userId);

    @GetMapping("/bookings/user/{userId}/past")
    Map<String, Object> getUserPastBookings(@PathVariable("userId") Long userId);

    @GetMapping("/bookings/user/{userId}/cancelled")
    Map<String, Object> getUserCancelledBookings(@PathVariable("userId") Long userId);

    // ✅ FIXED: For getting bookings as Map (with success, count, bookings)
    @GetMapping("/bookings/user/{userId}")
    Map<String, Object> getUserBookings(@PathVariable("userId") Long userId);

    // ✅ FIXED: For getting direct list of bookings (NO duplicate /api/db)
    @GetMapping("/bookings/user/{userId}/list")
    List<Map<String, Object>> getUserBookingsList(@PathVariable("userId") Long userId);

    // ========== SEAT ENDPOINTS ==========
    @GetMapping("/seats/aircraft/{aircraftId}")
    List<Seat> getSeatsByAircraft(@PathVariable("aircraftId") Long aircraftId);

    @GetMapping("/seats/aircraft/{aircraftId}/grouped")
    Map<String, Object> getSeatsGroupedByRow(@PathVariable("aircraftId") Long aircraftId);

    @GetMapping("/seats/aircraft/{aircraftId}/map")
    Map<String, Object> getSeatMapWithCategories(@PathVariable("aircraftId") Long aircraftId);

    // ✅ FIXED: Get seat by ID (NO duplicate /api/db)
    @GetMapping("/seats/{seatId}")
    Map<String, Object> getSeatById(@PathVariable("seatId") Long seatId);
}