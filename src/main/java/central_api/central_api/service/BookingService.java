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

        // Extract user ID from token
        Long userId = jwtUtil.extractUserId(token.substring(7));
        log.info("📌 User ID: {}", userId);

        // Get logged-in user details
        UserResponse loggedInUser = dbApiClient.getUserById(userId);
        String loggedInUserEmail = loggedInUser.getEmail();
        log.info("📧 Logged-in user email: {}", loggedInUserEmail);

        // Log passenger emails
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

        // ============================================
        // ✅ CRITICAL: Handle Connecting Flight vs Direct Flight
        // ============================================

        List<BookingRequest.FlightSegmentDto> flightsList = request.getFlights();
        List<Long> allSeatIds = new ArrayList<>();

        if (flightsList != null && !flightsList.isEmpty()) {
            // ✅ CONNECTING FLIGHT - Multiple segments
            log.info("🔗 Connecting flight detected with {} segments", flightsList.size());

            List<Map<String, Object>> dbFlights = new ArrayList<>();

            for (BookingRequest.FlightSegmentDto segment : flightsList) {
                log.info("   Processing segment: flightId={}, sequence={}, seats={}",
                        segment.getFlightId(), segment.getSequence(),
                        segment.getPassengerSeats() != null ? segment.getPassengerSeats().size() : 0);

                Map<String, Object> flightMap = new HashMap<>();
                flightMap.put("flightId", segment.getFlightId());
                flightMap.put("sequence", segment.getSequence() != null ? segment.getSequence() : 1);

                List<Map<String, Object>> passengerSeats = new ArrayList<>();
                if (segment.getPassengerSeats() != null) {
                    for (BookingRequest.PassengerSeatDto ps : segment.getPassengerSeats()) {
                        Map<String, Object> psMap = new HashMap<>();
                        psMap.put("seatId", ps.getSeatId());
                        psMap.put("passengerIndex", ps.getPassengerIndex());
                        passengerSeats.add(psMap);
                        allSeatIds.add(ps.getSeatId());
                    }
                }
                flightMap.put("passengerSeats", passengerSeats);
                dbFlights.add(flightMap);
            }

            bookingRequest.put("flights", dbFlights);
            bookingRequest.put("seatIds", allSeatIds);

            log.info("📤 Connecting flight request prepared with {} flights and {} total seats",
                    dbFlights.size(), allSeatIds.size());

        } else {
            // ✅ DIRECT FLIGHT - Single flight
            log.info("✈️ Direct flight detected for flight ID: {}", request.getFlightId());

            List<Map<String, Object>> flights = new ArrayList<>();
            Map<String, Object> flight = new HashMap<>();
            flight.put("flightId", request.getFlightId());
            flight.put("sequence", 1);

            List<Map<String, Object>> passengerSeats = new ArrayList<>();
            for (int i = 0; i < request.getSeatIds().size(); i++) {
                Map<String, Object> ps = new HashMap<>();
                ps.put("seatId", request.getSeatIds().get(i));
                ps.put("passengerIndex", i);
                passengerSeats.add(ps);
                allSeatIds.add(request.getSeatIds().get(i));
            }
            flight.put("passengerSeats", passengerSeats);
            flights.add(flight);

            bookingRequest.put("flights", flights);
            bookingRequest.put("seatIds", request.getSeatIds());

            log.info("📤 Direct flight request prepared with {} seats", request.getSeatIds().size());
        }

        // Prepare passengers with email and phone
        List<Map<String, Object>> passengers = request.getPassengers().stream()
                .map(p -> {
                    Map<String, Object> passengerMap = new HashMap<>();
                    passengerMap.put("fullName", p.getFullName());
                    passengerMap.put("age", p.getAge());
                    passengerMap.put("gender", p.getGender());
                    passengerMap.put("passportNumber", p.getPassportNumber() != null ? p.getPassportNumber() : "");
                    passengerMap.put("nationality", p.getNationality() != null ? p.getNationality() : "Indian");
                    passengerMap.put("email", p.getEmail() != null ? p.getEmail() : "");
                    passengerMap.put("phoneNumber", p.getPhoneNumber() != null ? p.getPhoneNumber() : "");
                    return passengerMap;
                })
                .collect(Collectors.toList());
        bookingRequest.put("passengers", passengers);

        // Payment details
        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentMethod", request.getPayment().getPaymentMethod());
        payment.put("transactionId", request.getPayment().getTransactionId());
        payment.put("amount", request.getPayment().getAmount());
        bookingRequest.put("payment", payment);

        log.info("📡 Sending booking request to DB API...");
        log.info("   Request keys: {}", bookingRequest.keySet());
        log.info("   Flights count: {}",
                bookingRequest.get("flights") != null ? ((List<?>) bookingRequest.get("flights")).size() : 0);

        try {
            // Call DB API to create booking
            Map<String, Object> bookingResponse = dbApiClient.createBooking(bookingRequest);
            log.info("✅ Booking created successfully in DB: {}", bookingResponse);

            String pnr = (String) bookingResponse.get("pnr");
            log.info("🎫 PNR generated: {}", pnr);

            // Get flight details for email
            Map<String, Object> flightDetails = null;
            try {
                if (flightsList != null && !flightsList.isEmpty()) {
                    // For connecting flight, get first flight details
                    flightDetails = dbApiClient.getFlightById(flightsList.get(0).getFlightId());
                } else {
                    flightDetails = dbApiClient.getFlightById(request.getFlightId());
                }
                log.info("✈️ Flight details retrieved: {}", flightDetails.get("flightNumber"));
            } catch (Exception e) {
                log.warn("⚠️ Could not fetch flight details: {}", e.getMessage());
            }

            // Get fare class details
            FareClassDTO fareClass = null;
            try {
                fareClass = dbApiClient.getFareClassByCode(request.getFareClassCode());
                log.info("🏷️ Fare class retrieved: {}", fareClass.getCode());
            } catch (Exception e) {
                log.warn("⚠️ Could not fetch fare class: {}", e.getMessage());
            }

            // Prepare email data
            Map<String, Object> emailData = prepareEmailData(request, flightDetails, fareClass, pnr, flightsList);

            // Collect unique emails to send confirmation
            Set<String> emailsToSend = new LinkedHashSet<>();

            // Add logged-in user's email
            if (loggedInUserEmail != null && !loggedInUserEmail.isEmpty()) {
                emailsToSend.add(loggedInUserEmail);
                log.info("✅ Added logged-in user email: {}", loggedInUserEmail);
            }

            // Add all passenger emails
            for (BookingRequest.PassengerDto passenger : request.getPassengers()) {
                if (passenger.getEmail() != null && !passenger.getEmail().isEmpty()) {
                    emailsToSend.add(passenger.getEmail());
                    log.info("✅ Added passenger email: {}", passenger.getEmail());
                }
            }

            // Send email to each unique email
            log.info("==========================================");
            log.info("📧 STARTING EMAIL SENDING PROCESS");
            log.info("📧 Total recipients: {}", emailsToSend.size());
            log.info("==========================================");

            int successCount = 0;
            int failCount = 0;

            for (String email : emailsToSend) {
                try {
                    notificationApiClient.sendBookingConfirmation(email, emailData);
                    successCount++;
                    log.info("✅ Email sent successfully to: {}", email);
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ Failed to send email to {}: {}", email, e.getMessage());
                }
            }

            log.info("📧 EMAIL SUMMARY - Success: {}, Failed: {}", successCount, failCount);
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
                                                 String pnr,
                                                 List<BookingRequest.FlightSegmentDto> flightsList) {

        Map<String, Object> emailData = new HashMap<>();

        emailData.put("pnr", pnr);
        emailData.put("totalAmount", request.getPayment().getAmount());
        emailData.put("fareClass", request.getFareClassCode());

        boolean isConnecting = (flightsList != null && flightsList.size() > 0);
        emailData.put("isConnectingFlight", isConnecting);

        // Baggage
        if (fareClass != null) {
            emailData.put("cabinBaggageKg", fareClass.getCabinBaggageKg());
            emailData.put("checkInBaggageKg", fareClass.getCheckInBaggageKg());
        } else {
            emailData.put("cabinBaggageKg", 7);
            emailData.put("checkInBaggageKg", 15);
        }

        // ============================================
        // ✅ PASSENGER DETAILS WITH SEAT NUMBERS
        // ============================================
        List<Map<String, Object>> passengerList = new ArrayList<>();

        if (isConnecting && flightsList != null) {
            // CONNECTING FLIGHT - Get seats from first flight
            BookingRequest.FlightSegmentDto firstFlight = flightsList.get(0);
            List<BookingRequest.PassengerSeatDto> seatList = firstFlight.getPassengerSeats();

            for (int i = 0; i < request.getPassengers().size(); i++) {
                BookingRequest.PassengerDto p = request.getPassengers().get(i);
                Map<String, Object> passengerInfo = new HashMap<>();
                passengerInfo.put("fullName", p.getFullName());
                passengerInfo.put("email", p.getEmail());
                passengerInfo.put("phoneNumber", p.getPhoneNumber());

                // In prepareEmailData method, replace the seat fetching code:
                String seatNumber = "Auto-assigned";
                for (BookingRequest.PassengerSeatDto seatDto : seatList) {
                    if (seatDto.getPassengerIndex() == i) {
                        try {
                            log.info("🔍 Fetching seat for seatId: {}", seatDto.getSeatId());
                            Map<String, Object> seat = dbApiClient.getSeatById(seatDto.getSeatId());
                            if (seat != null) {
                                seatNumber = (String) seat.get("seatNumber");
                                log.info("✅ Seat number found: {} for passenger {}", seatNumber, p.getFullName());
                            } else {
                                log.warn("⚠️ Seat is null for seatId: {}", seatDto.getSeatId());
                                seatNumber = "Seat-" + (i + 1);
                            }
                        } catch (Exception e) {
                            log.error("❌ Failed to fetch seat for seatId {}: {}", seatDto.getSeatId(), e.getMessage());
                            seatNumber = "Seat-" + (i + 1);
                        }
                        break;
                    }
                }
                passengerInfo.put("seatNumber", seatNumber);
                passengerList.add(passengerInfo);
            }

            // ✅ Also prepare segments with seat numbers for each flight
            List<Map<String, Object>> emailSegments = new ArrayList<>();
            List<Map<String, Object>> emailLayovers = new ArrayList<>();
            int totalDuration = 0;
            int totalLayover = 0;

            for (int i = 0; i < flightsList.size(); i++) {
                BookingRequest.FlightSegmentDto segment = flightsList.get(i);
                Map<String, Object> emailSegment = new HashMap<>();

                try {
                    Map<String, Object> flight = dbApiClient.getFlightById(segment.getFlightId());
                    Map<String, Object> sourceAirport = (Map<String, Object>) flight.get("sourceAirport");
                    Map<String, Object> destAirport = (Map<String, Object>) flight.get("destinationAirport");
                    Map<String, Object> aircraft = (Map<String, Object>) flight.get("aircraft");
                    Map<String, Object> airline = aircraft != null ? (Map<String, Object>) aircraft.get("airline") : null;

                    String departureTime = (String) flight.get("departureTime");
                    String arrivalTime = (String) flight.get("arrivalTime");
                    Integer duration = (Integer) flight.get("duration");

                    emailSegment.put("airlineName", airline != null ? airline.get("name") : "Airline");
                    emailSegment.put("flightNumber", flight.get("flightNumber"));
                    emailSegment.put("sourceCode", sourceAirport != null ? sourceAirport.get("code") : "N/A");
                    emailSegment.put("sourceCity", sourceAirport != null ? sourceAirport.get("city") : "N/A");
                    emailSegment.put("destinationCode", destAirport != null ? destAirport.get("code") : "N/A");
                    emailSegment.put("destinationCity", destAirport != null ? destAirport.get("city") : "N/A");
                    emailSegment.put("departureTime", formatTime(departureTime));
                    emailSegment.put("departureDate", formatDate(departureTime));
                    emailSegment.put("arrivalTime", formatTime(arrivalTime));
                    emailSegment.put("arrivalDate", formatDate(arrivalTime));
                    emailSegment.put("duration", duration != null ? duration : 0);

                    totalDuration += duration != null ? duration : 0;

                    // ✅ Add passenger seats for this segment
                    List<Map<String, Object>> segmentPassengers = new ArrayList<>();
                    List<BookingRequest.PassengerSeatDto> segmentSeats = segment.getPassengerSeats();
                    for (BookingRequest.PassengerSeatDto seatDto : segmentSeats) {
                        BookingRequest.PassengerDto p = request.getPassengers().get(seatDto.getPassengerIndex());
                        Map<String, Object> segPassenger = new HashMap<>();
                        segPassenger.put("fullName", p.getFullName());

                        // Get actual seat number
                        String seatNum = "Auto-assigned";
                        try {
                            Map<String, Object> seat = dbApiClient.getSeatById(seatDto.getSeatId());
                            seatNum = (String) seat.get("seatNumber");
                        } catch (Exception e) {}
                        segPassenger.put("seatNumber", seatNum);
                        segmentPassengers.add(segPassenger);
                    }
                    emailSegment.put("passengers", segmentPassengers);

                    // Calculate layover
                    if (i < flightsList.size() - 1) {
                        BookingRequest.FlightSegmentDto nextSegment = flightsList.get(i + 1);
                        Map<String, Object> nextFlight = dbApiClient.getFlightById(nextSegment.getFlightId());
                        String nextDepartureTime = (String) nextFlight.get("departureTime");

                        if (arrivalTime != null && nextDepartureTime != null) {
                            try {
                                LocalDateTime arrival = LocalDateTime.parse(arrivalTime);
                                LocalDateTime nextDeparture = LocalDateTime.parse(nextDepartureTime);
                                int layoverMinutes = (int) java.time.Duration.between(arrival, nextDeparture).toMinutes();
                                totalLayover += layoverMinutes;
                                totalDuration += layoverMinutes;

                                Map<String, Object> layover = new HashMap<>();
                                layover.put("airportCode", destAirport != null ? destAirport.get("code") : "N/A");
                                layover.put("airportCity", destAirport != null ? destAirport.get("city") : "N/A");
                                layover.put("duration", layoverMinutes);
                                emailLayovers.add(layover);
                            } catch (Exception e) {}
                        }
                    }

                } catch (Exception e) {
                    log.warn("Could not fetch segment details: {}", e.getMessage());
                    emailSegment.put("airlineName", "Airline");
                    emailSegment.put("flightNumber", "N/A");
                    emailSegment.put("sourceCode", "N/A");
                    emailSegment.put("destinationCode", "N/A");
                    emailSegment.put("departureTime", "N/A");
                    emailSegment.put("arrivalTime", "N/A");
                    emailSegment.put("passengers", new ArrayList<>());
                }

                emailSegments.add(emailSegment);
            }

            emailData.put("segments", emailSegments);
            emailData.put("layovers", emailLayovers);
            emailData.put("totalDuration", totalDuration);
            emailData.put("totalLayover", totalLayover);
            emailData.put("numberOfStops", flightsList.size() - 1);

        } else {
            // DIRECT FLIGHT
            for (int i = 0; i < request.getPassengers().size(); i++) {
                BookingRequest.PassengerDto p = request.getPassengers().get(i);
                Map<String, Object> passengerInfo = new HashMap<>();
                passengerInfo.put("fullName", p.getFullName());
                passengerInfo.put("email", p.getEmail());
                passengerInfo.put("phoneNumber", p.getPhoneNumber());

                // ✅ Get seat number for direct flight
                String seatNumber = "Auto-assigned";
                if (request.getSeatIds() != null && i < request.getSeatIds().size()) {
                    try {
                        Map<String, Object> seat = dbApiClient.getSeatById(request.getSeatIds().get(i));
                        seatNumber = (String) seat.get("seatNumber");
                        log.info("✅ Seat number for passenger {}: {}", p.getFullName(), seatNumber);
                    } catch (Exception e) {
                        seatNumber = "Seat-" + (i + 1);
                    }
                }
                passengerInfo.put("seatNumber", seatNumber);
                passengerList.add(passengerInfo);
            }

            // Direct flight details
            if (flightDetails != null) {
                Map<String, Object> sourceAirport = (Map<String, Object>) flightDetails.get("sourceAirport");
                Map<String, Object> destAirport = (Map<String, Object>) flightDetails.get("destinationAirport");
                Map<String, Object> aircraft = (Map<String, Object>) flightDetails.get("aircraft");
                Map<String, Object> airline = aircraft != null ? (Map<String, Object>) aircraft.get("airline") : null;

                String departureTime = (String) flightDetails.get("departureTime");
                String arrivalTime = (String) flightDetails.get("arrivalTime");

                emailData.put("airlineName", airline != null ? airline.get("name") : "Airline");
                emailData.put("flightNumber", flightDetails.get("flightNumber"));
                emailData.put("sourceCode", sourceAirport != null ? sourceAirport.get("code") : "N/A");
                emailData.put("sourceCity", sourceAirport != null ? sourceAirport.get("city") : "N/A");
                emailData.put("destinationCode", destAirport != null ? destAirport.get("code") : "N/A");
                emailData.put("destinationCity", destAirport != null ? destAirport.get("city") : "N/A");
                emailData.put("departureTime", formatTime(departureTime));
                emailData.put("departureDate", formatDate(departureTime));
                emailData.put("arrivalTime", formatTime(arrivalTime));
                emailData.put("arrivalDate", formatDate(arrivalTime));
                emailData.put("duration", flightDetails.get("duration"));
            }
        }

        emailData.put("passengers", passengerList);
        emailData.put("bookingTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));

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