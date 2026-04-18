package central_api.central_api.controller;

import central_api.central_api.client.DbApiClient;
import central_api.central_api.dto.response.FareClassDTO;
import central_api.central_api.dto.response.Seat;
import central_api.central_api.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/db")
@RequiredArgsConstructor
@Slf4j
public class DbProxyController {

    private final DbApiClient dbApiClient;

    // ========== FLIGHT ENDPOINTS ==========
    @GetMapping("/flights/airline/{airlineId}")
    public ResponseEntity<List<Map<String, Object>>> getFlightsByAirline(@PathVariable Long airlineId) {
        try {
            List<Map<String, Object>> flights = dbApiClient.getFlightsByAirline(airlineId);
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            log.error("Error fetching flights by airline: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/flights")
    public ResponseEntity<Map<String, Object>> createFlight(@RequestBody Map<String, Object> flight) {
        try {
            Map<String, Object> result = dbApiClient.createFlight(flight);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating flight: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/flights/{flightId}/delay")
    public ResponseEntity<Map<String, Object>> delayFlight(@PathVariable Long flightId, @RequestParam int minutes) {
        try {
            Map<String, Object> result = dbApiClient.delayFlight(flightId, minutes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error delaying flight: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/flights/{flightId}/reschedule")
    public ResponseEntity<Map<String, Object>> rescheduleFlight(
            @PathVariable Long flightId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDepartureTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newArrivalTime) {

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            String departureStr = newDepartureTime.format(formatter);
            String arrivalStr = newArrivalTime.format(formatter);
            Map<String, Object> result = dbApiClient.rescheduleFlight(flightId, departureStr, arrivalStr);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error rescheduling flight: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/flights/{flightId}")
    public ResponseEntity<Map<String, Object>> getFlightById(@PathVariable Long flightId) {
        try {
            Map<String, Object> flight = dbApiClient.getFlightById(flightId);
            return ResponseEntity.ok(flight);
        } catch (Exception e) {
            log.error("Error fetching flight by ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/flights/{flightId}/seatmap")
    public ResponseEntity<Map<String, Object>> getSeatMap(@PathVariable Long flightId) {
        try {
            Map<String, Object> seatMap = dbApiClient.getSeatMap(flightId);
            return ResponseEntity.ok(seatMap);
        } catch (Exception e) {
            log.error("Error fetching seat map: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/flights/search")
    public ResponseEntity<List<Object>> searchFlights(@RequestBody Map<String, Object> searchRequest) {
        try {
            List<Object> results = dbApiClient.searchFlights(searchRequest);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching flights: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== FARE CLASS ENDPOINTS ==========
    @GetMapping("/fare-classes")
    public ResponseEntity<List<FareClassDTO>> getFareClasses() {
        try {
            List<FareClassDTO> fareClasses = dbApiClient.getFareClasses();
            return ResponseEntity.ok(fareClasses);
        } catch (Exception e) {
            log.error("Error fetching fare classes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/fare-classes/{code}")
    public ResponseEntity<FareClassDTO> getFareClassByCode(@PathVariable String code) {
        try {
            FareClassDTO fareClass = dbApiClient.getFareClassByCode(code);
            return ResponseEntity.ok(fareClass);
        } catch (Exception e) {
            log.error("Error fetching fare class by code {}: {}", code, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/flights/{flightId}/price-breakdown")
    public ResponseEntity<Map<String, Object>> getPriceBreakdown(
            @PathVariable Long flightId,
            @RequestParam String seatClass,
            @RequestParam String fareClassCode) {
        try {
            Map<String, Object> breakdown = dbApiClient.getPriceBreakdown(flightId, seatClass, fareClassCode);
            return ResponseEntity.ok(breakdown);
        } catch (Exception e) {
            log.error("Error fetching price breakdown: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== AIRCRAFT ENDPOINTS ==========
    @GetMapping("/aircrafts/airline/{airlineId}")
    public ResponseEntity<List<Map<String, Object>>> getAircraftsByAirline(@PathVariable Long airlineId) {
        try {
            List<Map<String, Object>> aircrafts = dbApiClient.getAircraftsByAirline(airlineId);
            return ResponseEntity.ok(aircrafts);
        } catch (Exception e) {
            log.error("Error fetching aircrafts by airline: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/aircrafts")
    public ResponseEntity<Map<String, Object>> createAircraft(@RequestBody Map<String, Object> aircraft) {
        try {
            Map<String, Object> result = dbApiClient.createAircraft(aircraft);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating aircraft: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ========== AIRPORT ENDPOINTS ==========
    @GetMapping("/airports")
    public ResponseEntity<List<Map<String, Object>>> getAllAirports() {
        try {
            List<Map<String, Object>> airports = dbApiClient.getAllAirports();
            return ResponseEntity.ok(airports);
        } catch (Exception e) {
            log.error("Error fetching airports: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== BOOKING ENDPOINTS ==========
    @GetMapping("/bookings/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserBookings(@PathVariable Long userId) {
        try {
            Map<String, Object> bookings = dbApiClient.getUserBookings(userId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            log.error("Error fetching user bookings: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("bookings", new ArrayList<>());
            return ResponseEntity.ok(error);
        }
    }

    @GetMapping("/bookings/pnr/{pnr}")
    public ResponseEntity<Map<String, Object>> getBookingByPNR(@PathVariable String pnr) {
        try {
            log.info("🔍 Fetching booking by PNR: {}", pnr);
            Object response = dbApiClient.getBookingByPNR(pnr);

            // Convert Object to Map using Jackson
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> result = mapper.convertValue(response,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Error fetching booking by PNR {}: {}", pnr, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching booking: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/bookings")
    public ResponseEntity<Map<String, Object>> createBooking(@RequestBody Map<String, Object> booking) {
        try {
            Map<String, Object> result = dbApiClient.createBooking(booking);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating booking: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable Long bookingId) {
        try {
            Map<String, Object> result = dbApiClient.cancelBooking(bookingId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error cancelling booking: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<Map<String, Object>> getBookingById(@PathVariable Long bookingId) {
        try {
            Map<String, Object> booking = dbApiClient.getBookingById(bookingId);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            log.error("Error fetching booking by ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ========== REVIEW ENDPOINTS ==========
    @GetMapping("/reviews/airline/{airlineId}")
    public ResponseEntity<Map<String, Object>> getAirlineReviews(@PathVariable Long airlineId) {
        try {
            Map<String, Object> reviews = dbApiClient.getAirlineReviews(airlineId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            log.error("Error fetching airline reviews: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== AIRLINE ENDPOINTS ==========
    @GetMapping("/airlines/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingAirlines() {
        try {
            List<Map<String, Object>> airlines = dbApiClient.getPendingAirlines();
            return ResponseEntity.ok(airlines);
        } catch (Exception e) {
            log.error("Error fetching pending airlines: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/airlines/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveAirlines() {
        try {
            List<Map<String, Object>> airlines = dbApiClient.getActiveAirlines();
            return ResponseEntity.ok(airlines);
        } catch (Exception e) {
            log.error("Error fetching active airlines: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/airlines/{id}")
    public ResponseEntity<Map<String, Object>> getAirlineById(@PathVariable Long id) {
        try {
            Map<String, Object> airline = dbApiClient.getAirlineById(id);
            return ResponseEntity.ok(airline);
        } catch (Exception e) {
            log.error("Error fetching airline by ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/airlines/{airlineId}/approve")
    public ResponseEntity<Map<String, Object>> approveAirline(@PathVariable Long airlineId, @RequestParam Long adminId) {
        try {
            Map<String, Object> result = dbApiClient.approveAirline(airlineId, adminId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error approving airline: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/airlines/{airlineId}/reject")
    public ResponseEntity<Map<String, Object>> rejectAirline(
            @PathVariable Long airlineId,
            @RequestParam Long adminId,
            @RequestParam(required = false) String reason) {
        try {
            Map<String, Object> result = dbApiClient.rejectAirline(airlineId, adminId, reason);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error rejecting airline: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ========== USER ENDPOINTS ==========
    @GetMapping("/users/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        try {
            UserResponse user = dbApiClient.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error fetching user by email: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        try {
            UserResponse user = dbApiClient.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error fetching user by ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/users/{id}/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long id) {
        try {
            Map<String, Object> profile = dbApiClient.getUserProfile(id);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error fetching user profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ========== SEAT ENDPOINTS ==========
    @GetMapping("/seats/aircraft/{aircraftId}")
    public ResponseEntity<List<Seat>> getSeatsByAircraft(@PathVariable Long aircraftId) {
        try {
            List<Seat> seats = dbApiClient.getSeatsByAircraft(aircraftId);
            return ResponseEntity.ok(seats);
        } catch (Exception e) {
            log.error("Error fetching seats by aircraft: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/seats/aircraft/{aircraftId}/grouped")
    public ResponseEntity<Map<String, Object>> getSeatsGroupedByRow(@PathVariable Long aircraftId) {
        try {
            Map<String, Object> groupedSeats = dbApiClient.getSeatsGroupedByRow(aircraftId);
            return ResponseEntity.ok(groupedSeats);
        } catch (Exception e) {
            log.error("Error fetching grouped seats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/seats/aircraft/{aircraftId}/map")
    public ResponseEntity<Map<String, Object>> getSeatMapWithCategories(@PathVariable Long aircraftId) {
        try {
            Map<String, Object> seatMap = dbApiClient.getSeatMapWithCategories(aircraftId);
            return ResponseEntity.ok(seatMap);
        } catch (Exception e) {
            log.error("Error fetching seat map with categories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/bookings/user/{userId}/list")
    public ResponseEntity<List<Map<String, Object>>> getUserBookingsList(@PathVariable Long userId) {
        try {
            Map<String, Object> response = dbApiClient.getUserBookings(userId);
            List<Map<String, Object>> bookings = new ArrayList<>();

            if (response != null && response.get("bookings") != null) {
                bookings = (List<Map<String, Object>>) response.get("bookings");
            }

            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            log.error("Error fetching user bookings list: {}", e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // ✅ NEW: Get seat by ID
    @GetMapping("/seats/{seatId}")
    public ResponseEntity<Map<String, Object>> getSeatById(@PathVariable Long seatId) {
        try {
            Map<String, Object> seat = dbApiClient.getSeatById(seatId);
            return ResponseEntity.ok(seat);
        } catch (Exception e) {
            log.error("Error fetching seat by ID {}: {}", seatId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}