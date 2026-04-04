package central_api.central_api.service;

import central_api.central_api.client.DbApiClient;
import central_api.central_api.client.NotificationApiClient;
import central_api.central_api.dto.request.BookingRequest;
import central_api.central_api.dto.response.FareClassDTO;
import central_api.central_api.dto.response.UserResponse;
import central_api.central_api.exception.CustomExceptions;
import central_api.central_api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final DbApiClient dbApiClient;
    private final JwtUtil jwtUtil;
    private final NotificationApiClient notificationApiClient;

    public Map<String, Object> createBooking(BookingRequest request, String token) {
        log.info("==========================================");
        log.info("🔵 BOOKING CREATION STARTED");
        log.info("==========================================");

        Long userId = jwtUtil.extractUserId(token.substring(7));
        log.info("📌 User ID: {}", userId);

        // Get logged-in user details
        UserResponse loggedInUser = dbApiClient.getUserById(userId);
        String loggedInUserEmail = loggedInUser.getEmail();
        log.info("📧 Logged-in user email: {}", loggedInUserEmail);

        // Log all passenger emails
        log.info("📋 Passenger emails from request:");
        for (int i = 0; i < request.getPassengers().size(); i++) {
            BookingRequest.PassengerDto p = request.getPassengers().get(i);
            log.info("   Passenger {}: {} - Email: {}, Phone: {}",
                    i+1, p.getFullName(), p.getEmail(), p.getPhoneNumber());
        }

        // Prepare booking request for DB API
        Map<String, Object> bookingRequest = new HashMap<>();
        bookingRequest.put("userId", userId);
        bookingRequest.put("fareClassCode", request.getFareClassCode());

        // Prepare flights
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

        // Prepare passengers with emails
        List<Map<String, Object>> passengers = request.getPassengers().stream()
                .map(p -> {
                    Map<String, Object> passengerMap = new HashMap<>();
                    passengerMap.put("fullName", p.getFullName());
                    passengerMap.put("age", p.getAge());
                    passengerMap.put("gender", p.getGender());
                    passengerMap.put("passportNumber", p.getPassportNumber());
                    passengerMap.put("nationality", p.getNationality());
                    passengerMap.put("email", p.getEmail());
                    passengerMap.put("phoneNumber", p.getPhoneNumber());
                    return passengerMap;
                })
                .collect(Collectors.toList());
        bookingRequest.put("passengers", passengers);

        // Payment
        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentMethod", request.getPayment().getPaymentMethod());
        payment.put("transactionId", request.getPayment().getTransactionId());
        payment.put("amount", request.getPayment().getAmount());
        bookingRequest.put("payment", payment);

        try {
            log.info("📡 Calling DB API to create booking...");
            Map<String, Object> bookingResponse = dbApiClient.createBooking(bookingRequest);
            log.info("✅ Booking created in DB: {}", bookingResponse);

            String pnr = (String) bookingResponse.get("pnr");
            log.info("🎫 PNR: {}", pnr);

            // Get flight details
            Map<String, Object> flightDetails = dbApiClient.getFlightById(request.getFlightId());
            log.info("✈️ Flight details retrieved: {}", flightDetails.get("flightNumber"));

            // Get fare class details
            FareClassDTO fareClass = null;
            try {
                fareClass = dbApiClient.getFareClassByCode(request.getFareClassCode());
                log.info("🏷️ Fare class retrieved: {}", fareClass.getCode());
            } catch (Exception e) {
                log.warn("⚠️ Could not fetch fare class: {}", e.getMessage());
            }

            // Prepare email data
            Map<String, Object> emailData = prepareEmailData(request, flightDetails, fareClass, pnr);

            // Collect unique emails from passengers
            Set<String> passengerEmails = request.getPassengers().stream()
                    .map(p -> p.getEmail())
                    .filter(email -> email != null && !email.trim().isEmpty())
                    .collect(Collectors.toSet());

            log.info("📧 Unique passenger emails: {}", passengerEmails);

            // Create final list of emails to send
            Set<String> emailsToSend = new LinkedHashSet<>();

            // Rule 1: Always add logged-in user's email
            if (loggedInUserEmail != null && !loggedInUserEmail.isEmpty()) {
                emailsToSend.add(loggedInUserEmail);
                log.info("✅ Added logged-in user email: {}", loggedInUserEmail);
            } else {
                log.warn("⚠️ Logged-in user email is null or empty!");
            }

            // Rule 2: Add all unique passenger emails
            emailsToSend.addAll(passengerEmails);
            log.info("📧 Final unique emails to send: {}", emailsToSend);

            // Send email to each unique email
            log.info("==========================================");
            log.info("📧 STARTING EMAIL SENDING PROCESS");
            log.info("==========================================");

            int successCount = 0;
            int failCount = 0;

            for (String email : emailsToSend) {
                try {
                    log.info("📤 Sending booking confirmation email to: {}", email);
                    notificationApiClient.sendBookingConfirmation(email, emailData);
                    successCount++;
                    log.info("✅ Email sent successfully to: {}", email);
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ Failed to send email to {}: {}", email, e.getMessage());
                    log.error("   Exception details: ", e);
                }
            }

            log.info("==========================================");
            log.info("📧 EMAIL SENDING SUMMARY");
            log.info("✅ Successful: {}", successCount);
            log.info("❌ Failed: {}", failCount);
            log.info("📧 Total unique recipients: {}", emailsToSend.size());
            log.info("==========================================");

            return bookingResponse;

        } catch (Exception e) {
            log.error("❌ Failed to create booking: {}", e.getMessage(), e);
            throw new CustomExceptions.BookingException("Failed to create booking: " + e.getMessage());
        }
    }

    private Map<String, Object> prepareEmailData(BookingRequest request,
                                                 Map<String, Object> flightDetails,
                                                 FareClassDTO fareClass,
                                                 String pnr) {

        Map<String, Object> emailData = new HashMap<>();

        emailData.put("pnr", pnr);
        emailData.put("flightNumber", flightDetails.get("flightNumber"));

        // Airline name
        String airlineName = "AirNova";
        if (flightDetails.get("aircraft") != null) {
            Map<String, Object> aircraft = (Map<String, Object>) flightDetails.get("aircraft");
            if (aircraft.get("airline") != null) {
                Map<String, Object> airline = (Map<String, Object>) aircraft.get("airline");
                airlineName = (String) airline.get("name");
            }
        }
        emailData.put("airlineName", airlineName);

        // Route details
        Map<String, Object> sourceAirport = (Map<String, Object>) flightDetails.get("sourceAirport");
        Map<String, Object> destAirport = (Map<String, Object>) flightDetails.get("destinationAirport");

        emailData.put("sourceCode", sourceAirport.get("code"));
        emailData.put("sourceCity", sourceAirport.get("city"));
        emailData.put("destinationCode", destAirport.get("code"));
        emailData.put("destinationCity", destAirport.get("city"));

        // Time details
        String departureTime = (String) flightDetails.get("departureTime");
        String arrivalTime = (String) flightDetails.get("arrivalTime");

        emailData.put("departureTime", formatTime(departureTime));
        emailData.put("departureDate", formatDate(departureTime));
        emailData.put("arrivalTime", formatTime(arrivalTime));
        emailData.put("arrivalDate", formatDate(arrivalTime));
        emailData.put("duration", flightDetails.get("duration"));

        // Passenger details with email and phone
        List<Map<String, Object>> passengerList = new ArrayList<>();
        for (int i = 0; i < request.getPassengers().size(); i++) {
            BookingRequest.PassengerDto p = request.getPassengers().get(i);
            Map<String, Object> passengerInfo = new HashMap<>();
            passengerInfo.put("fullName", p.getFullName());
            passengerInfo.put("age", p.getAge());
            passengerInfo.put("gender", p.getGender());
            // Get seat number from selected seats (if available)
            if (request.getSeatIds() != null && i < request.getSeatIds().size()) {
                passengerInfo.put("seatNumber", "Seat-" + (i + 1));
            } else {
                passengerInfo.put("seatNumber", "Auto-assigned");
            }
            passengerInfo.put("email", p.getEmail());           // ✅ CRITICAL
            passengerInfo.put("phoneNumber", p.getPhoneNumber()); // ✅ CRITICAL
            passengerList.add(passengerInfo);

            log.info("📧 Added passenger: {} - Email: {}, Phone: {}",
                    p.getFullName(), p.getEmail(), p.getPhoneNumber());
        }
        emailData.put("passengers", passengerList);

        // Fare details
        emailData.put("fareClass", request.getFareClassCode());
        emailData.put("totalAmount", request.getPayment().getAmount());

        // Baggage
        if (fareClass != null) {
            emailData.put("cabinBaggageKg", fareClass.getCabinBaggageKg());
            emailData.put("checkInBaggageKg", fareClass.getCheckInBaggageKg());
            emailData.put("cancellationFee", fareClass.getCancellationFee());
        } else {
            emailData.put("cabinBaggageKg", 7);
            emailData.put("checkInBaggageKg", 15);
            emailData.put("cancellationFee", 1000);
        }

        emailData.put("bookingTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));

        log.info("📧 Email data prepared with {} passengers", passengerList.size());
        return emailData;
    }

    private String formatTime(String dateTimeStr) {
        if (dateTimeStr == null) return "N/A";
        try {
            LocalDateTime dt = LocalDateTime.parse(dateTimeStr);
            return dt.format(DateTimeFormatter.ofPattern("hh:mm a"));
        } catch (Exception e) {
            return dateTimeStr;
        }
    }

    private String formatDate(String dateTimeStr) {
        if (dateTimeStr == null) return "N/A";
        try {
            LocalDateTime dt = LocalDateTime.parse(dateTimeStr);
            return dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        } catch (Exception e) {
            return dateTimeStr;
        }
    }

    public Map<String, Object> getBookingByPNR(String pnr) {
        try {
            return dbApiClient.getBookingByPNR(pnr);
        } catch (Exception e) {
            throw new CustomExceptions.BookingException("Booking not found with PNR: " + pnr);
        }
    }

    public List<Map<String, Object>> getUserBookings(String token) {
        try {
            Long userId = jwtUtil.extractUserId(token.substring(7));
            Map<String, Object> response = dbApiClient.getUserBookings(userId);

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