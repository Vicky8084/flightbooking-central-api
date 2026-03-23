package central_api.central_api.service;

import central_api.central_api.client.DbApiClient;
import central_api.central_api.dto.request.BookingRequest;
import central_api.central_api.exception.CustomExceptions;
import central_api.central_api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final DbApiClient dbApiClient;
    private final JwtUtil jwtUtil;

    public Map<String, Object> createBooking(BookingRequest request, String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));

        Map<String, Object> bookingRequest = new HashMap<>();
        bookingRequest.put("userId", userId);

        List<Map<String, Object>> flights = new ArrayList<>();
        Map<String, Object> flight = new HashMap<>();
        flight.put("flightId", request.getFlightId());
        flight.put("sequence", 1);

        List<Map<String, Object>> passengerSeats = request.getSeatIds().stream()
                .map(seatId -> {
                    Map<String, Object> ps = new HashMap<>();
                    ps.put("seatId", seatId);
                    ps.put("passengerIndex", request.getSeatIds().indexOf(seatId));
                    return ps;
                })
                .collect(Collectors.toList());

        flight.put("passengerSeats", passengerSeats);
        flights.add(flight);
        bookingRequest.put("flights", flights);

        List<Map<String, Object>> passengers = request.getPassengers().stream()
                .map(p -> {
                    Map<String, Object> passengerMap = new HashMap<>();
                    passengerMap.put("fullName", p.getFullName());
                    passengerMap.put("age", p.getAge());
                    passengerMap.put("gender", p.getGender());
                    passengerMap.put("passportNumber", p.getPassportNumber());
                    passengerMap.put("nationality", p.getNationality());
                    return passengerMap;
                })
                .collect(Collectors.toList());
        bookingRequest.put("passengers", passengers);

        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentMethod", request.getPayment().getPaymentMethod());
        payment.put("transactionId", "TXN" + System.currentTimeMillis());
        payment.put("amount", request.getPayment().getAmount());
        bookingRequest.put("payment", payment);

        try {
            return dbApiClient.createBooking(bookingRequest);
        } catch (Exception e) {
            throw new CustomExceptions.BookingException("Failed to create booking: " + e.getMessage());
        }
    }

    public Map<String, Object> getBookingByPNR(String pnr) {
        try {
            return dbApiClient.getBookingByPNR(pnr);
        } catch (Exception e) {
            throw new CustomExceptions.BookingException("Booking not found with PNR: " + pnr);
        }
    }

    // ✅ FIXED: getUserBookings now returns Map and extracts list
    public List<Map<String, Object>> getUserBookings(String token) {
        try {
            Long userId = jwtUtil.extractUserId(token.substring(7));
            Map<String, Object> response = dbApiClient.getUserBookings(userId);

            // Extract bookings list from response
            List<Map<String, Object>> bookings = new ArrayList<>();
            if (response != null && response.get("bookings") != null) {
                bookings = (List<Map<String, Object>>) response.get("bookings");
            }

            log.debug("Fetched {} bookings for user: {}", bookings.size(), userId);
            return bookings;

        } catch (Exception e) {
            log.error("Error fetching user bookings: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> cancelBooking(Long bookingId) {
        try {
            return dbApiClient.cancelBooking(bookingId);
        } catch (Exception e) {
            throw new CustomExceptions.BookingException("Failed to cancel booking: " + e.getMessage());
        }
    }
}