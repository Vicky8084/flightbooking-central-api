package central_api.central_api.service;

import central_api.central_api.client.DbApiClient;
import central_api.central_api.client.NotificationApiClient;
import central_api.central_api.dto.response.BookingDetailsDTO;
import central_api.central_api.dto.response.BookingSummaryDTO;
import central_api.central_api.dto.response.FareClassDTO;
import central_api.central_api.dto.response.UserProfileDTO;
import central_api.central_api.exception.CustomExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDashboardService {

    private final DbApiClient dbApiClient;
    private final FareClassService fareClassService;
    private final NotificationApiClient notificationApiClient;

    /**
     * Get user profile with stats
     */
    public UserProfileDTO getUserProfile(Long userId) {
        try {
            Map<String, Object> userData = dbApiClient.getUserProfile(userId);
            if (userData == null || userData.isEmpty()) {
                throw new CustomExceptions.UserNotFoundException("User not found");
            }

            List<Map<String, Object>> allBookings = dbApiClient.getUserBookingsList(userId);
            LocalDateTime now = LocalDateTime.now();

            int upcomingCount = 0, completedCount = 0, cancelledCount = 0;

            if (allBookings != null) {
                for (Map<String, Object> booking : allBookings) {
                    String status = (String) booking.get("status");
                    if ("CANCELLED".equals(status)) {
                        cancelledCount++;
                    } else if ("CONFIRMED".equals(status)) {
                        List<Map<String, Object>> bookingFlights = (List<Map<String, Object>>) booking.get("bookingFlights");
                        if (bookingFlights != null && !bookingFlights.isEmpty()) {
                            Map<String, Object> flight = (Map<String, Object>) bookingFlights.get(0).get("flight");
                            if (flight != null) {
                                String departureTimeStr = (String) flight.get("departureTime");
                                if (departureTimeStr != null) {
                                    LocalDateTime departureTime = LocalDateTime.parse(departureTimeStr);
                                    if (departureTime.isAfter(now)) upcomingCount++;
                                    else completedCount++;
                                }
                            }
                        }
                    } else if ("COMPLETED".equals(status)) {
                        completedCount++;
                    }
                }
            }

            return UserProfileDTO.builder()
                    .id(((Number) userData.get("id")).longValue())
                    .email((String) userData.get("email"))
                    .fullName((String) userData.get("fullName"))
                    .phoneNumber((String) userData.get("phoneNumber"))
                    .role((String) userData.get("role"))
                    .status((String) userData.get("status"))
                    .isActive((Boolean) userData.get("isActive"))
                    .memberSince(userData.get("createdAt") != null ? ((String) userData.get("createdAt")).substring(0, 10) : "N/A")
                    .totalBookings(allBookings != null ? allBookings.size() : 0)
                    .upcomingTrips(upcomingCount)
                    .completedTrips(completedCount)
                    .cancelledTrips(cancelledCount)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching user profile: {}", e.getMessage(), e);
            throw new CustomExceptions.UserNotFoundException("Failed to fetch user profile");
        }
    }

    /**
     * Get user bookings summary
     */
    public List<BookingSummaryDTO> getUserBookingsSummary(Long userId) {
        try {
            List<Map<String, Object>> bookings = dbApiClient.getUserBookingsList(userId);
            return bookings.stream()
                    .map(this::convertToBookingSummary)
                    .sorted((b1, b2) -> {
                        if (b1.getDepartureTime() == null || b2.getDepartureTime() == null) return 0;
                        return b2.getDepartureTime().compareTo(b1.getDepartureTime());
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching user bookings: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get booking details by ID
     */
    public BookingDetailsDTO getBookingDetails(Long bookingId, Long userId) {
        try {
            Map<String, Object> booking = dbApiClient.getBookingById(bookingId);
            Map<String, Object> user = (Map<String, Object>) booking.get("user");
            Long bookingUserId = ((Number) user.get("id")).longValue();
            if (!bookingUserId.equals(userId)) {
                throw new CustomExceptions.UnauthorizedException("You don't have access to this booking");
            }
            return convertToBookingDetails(booking);
        } catch (Exception e) {
            log.error("Error fetching booking details: {}", e.getMessage());
            throw new CustomExceptions.BookingException("Failed to fetch booking details");
        }
    }

    /**
     * Cancel booking with email notification
     */
    public Map<String, Object> cancelBooking(Long bookingId, Long userId, String reason) {
        try {
            Map<String, Object> booking = dbApiClient.getBookingById(bookingId);
            Map<String, Object> user = (Map<String, Object>) booking.get("user");
            Long bookingUserId = ((Number) user.get("id")).longValue();

            if (!bookingUserId.equals(userId)) {
                throw new CustomExceptions.UnauthorizedException("You cannot cancel someone else's booking");
            }

            String fareClassCode = (String) booking.get("fareClassCode");
            FareClassDTO fareClass = fareClassService.getFareClassByCode(fareClassCode);
            Double totalAmount = (Double) booking.get("totalAmount");
            LocalDateTime now = LocalDateTime.now();

            List<Map<String, Object>> bookingFlights = (List<Map<String, Object>>) booking.get("bookingFlights");
            LocalDateTime departureTime = null;
            if (bookingFlights != null && !bookingFlights.isEmpty()) {
                Map<String, Object> flightData = (Map<String, Object>) bookingFlights.get(0).get("flight");
                departureTime = LocalDateTime.parse((String) flightData.get("departureTime"));
            }

            long daysBeforeDeparture = 0;
            if (departureTime != null) {
                daysBeforeDeparture = java.time.Duration.between(now, departureTime).toDays();
            }

            double refundAmount = calculateRefundAmount(fareClass, totalAmount, (int) daysBeforeDeparture);
            Map<String, Object> cancelResponse = dbApiClient.cancelBooking(bookingId);
            cancelResponse.put("refundAmount", refundAmount);
            cancelResponse.put("cancellationFee", fareClass.getCancellationFee());
            cancelResponse.put("daysBeforeDeparture", daysBeforeDeparture);

            // Send cancellation email
            try {
                String userEmail = (String) user.get("email");
                String userName = (String) user.get("fullName");
                String pnrNumber = (String) booking.get("pnrNumber");

                Map<String, Object> flightData = null;
                if (bookingFlights != null && !bookingFlights.isEmpty()) {
                    flightData = (Map<String, Object>) bookingFlights.get(0).get("flight");
                }

                Map<String, Object> emailData = new HashMap<>();
                emailData.put("pnr", pnrNumber);
                emailData.put("userName", userName);
                emailData.put("totalAmount", totalAmount);
                emailData.put("refundAmount", refundAmount);
                emailData.put("cancellationFee", fareClass.getCancellationFee());
                emailData.put("cancellationReason", reason != null && !reason.isEmpty() ? reason : "No reason provided");

                if (flightData != null) {
                    Map<String, Object> sourceAirport = (Map<String, Object>) flightData.get("sourceAirport");
                    Map<String, Object> destAirport = (Map<String, Object>) flightData.get("destinationAirport");
                    Map<String, Object> aircraft = (Map<String, Object>) flightData.get("aircraft");
                    Map<String, Object> airline = aircraft != null ? (Map<String, Object>) aircraft.get("airline") : null;

                    emailData.put("flightNumber", flightData.get("flightNumber"));
                    emailData.put("airlineName", airline != null ? airline.get("name") : "Airline");
                    emailData.put("sourceCode", sourceAirport != null ? sourceAirport.get("code") : "N/A");
                    emailData.put("destinationCode", destAirport != null ? destAirport.get("code") : "N/A");
                    emailData.put("departureTime", flightData.get("departureTime"));
                }

                notificationApiClient.sendBookingCancellation(userEmail, emailData);
                log.info("✅ Cancellation email sent to: {}", userEmail);
            } catch (Exception e) {
                log.error("Failed to send cancellation email: {}", e.getMessage());
            }

            return cancelResponse;
        } catch (Exception e) {
            log.error("Error cancelling booking: {}", e.getMessage());
            throw new CustomExceptions.BookingException("Failed to cancel booking: " + e.getMessage());
        }
    }

    /**
     * Update user profile
     */
    public Map<String, Object> updateUserProfile(Long userId, Map<String, Object> updates) {
        try {
            Map<String, Object> userData = new HashMap<>();
            userData.put("fullName", updates.get("fullName"));
            userData.put("phoneNumber", updates.get("phoneNumber"));
            return dbApiClient.updateUser(userId, userData);
        } catch (Exception e) {
            log.error("Error updating user profile: {}", e.getMessage());
            throw new CustomExceptions.BadRequestException("Failed to update profile");
        }
    }

    /**
     * Update user password - This calls DB API to update password
     * Note: Password should be already encrypted by Auth API
     */
    public Map<String, Object> updateUserPassword(Long userId, String encryptedNewPassword) {
        try {
            // Call DB API to update password
            dbApiClient.updatePassword(userId, encryptedNewPassword);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password updated successfully");
            return response;
        } catch (Exception e) {
            log.error("Error updating password: {}", e.getMessage());
            throw new CustomExceptions.BadRequestException("Failed to update password: " + e.getMessage());
        }
    }

    /**
     * Get upcoming bookings
     */
    public List<BookingSummaryDTO> getUpcomingBookings(Long userId) {
        try {
            List<Map<String, Object>> allBookings = dbApiClient.getUserBookingsList(userId);
            LocalDateTime now = LocalDateTime.now();

            return allBookings.stream()
                    .filter(booking -> {
                        String status = (String) booking.get("status");
                        if (!"CONFIRMED".equals(status)) return false;
                        List<Map<String, Object>> bookingFlights = (List<Map<String, Object>>) booking.get("bookingFlights");
                        if (bookingFlights == null || bookingFlights.isEmpty()) return false;
                        Map<String, Object> flight = (Map<String, Object>) bookingFlights.get(0).get("flight");
                        if (flight == null) return false;
                        String departureTimeStr = (String) flight.get("departureTime");
                        if (departureTimeStr == null) return false;
                        LocalDateTime departureTime = LocalDateTime.parse(departureTimeStr);
                        return departureTime.isAfter(now);
                    })
                    .map(this::convertToBookingSummary)
                    .sorted((b1, b2) -> {
                        if (b1.getDepartureTime() == null || b2.getDepartureTime() == null) return 0;
                        return b1.getDepartureTime().compareTo(b2.getDepartureTime());
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching upcoming bookings: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get past bookings
     */
    public List<BookingSummaryDTO> getPastBookings(Long userId) {
        try {
            List<Map<String, Object>> allBookings = dbApiClient.getUserBookingsList(userId);
            LocalDateTime now = LocalDateTime.now();

            return allBookings.stream()
                    .filter(booking -> {
                        String status = (String) booking.get("status");
                        if ("CANCELLED".equals(status)) return false;
                        List<Map<String, Object>> bookingFlights = (List<Map<String, Object>>) booking.get("bookingFlights");
                        if (bookingFlights == null || bookingFlights.isEmpty()) return false;
                        Map<String, Object> flight = (Map<String, Object>) bookingFlights.get(0).get("flight");
                        if (flight == null) return false;
                        String departureTimeStr = (String) flight.get("departureTime");
                        if (departureTimeStr == null) return false;
                        LocalDateTime departureTime = LocalDateTime.parse(departureTimeStr);
                        return departureTime.isBefore(now) || "COMPLETED".equals(status);
                    })
                    .map(this::convertToBookingSummary)
                    .sorted((b1, b2) -> {
                        if (b1.getDepartureTime() == null || b2.getDepartureTime() == null) return 0;
                        return b2.getDepartureTime().compareTo(b1.getDepartureTime());
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching past bookings: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get cancelled bookings
     */
    public List<BookingSummaryDTO> getCancelledBookings(Long userId) {
        try {
            List<Map<String, Object>> allBookings = dbApiClient.getUserBookingsList(userId);
            return allBookings.stream()
                    .filter(booking -> {
                        String status = (String) booking.get("status");
                        return "CANCELLED".equals(status);
                    })
                    .map(this::convertToBookingSummary)
                    .sorted((b1, b2) -> {
                        if (b1.getBookingTime() == null || b2.getBookingTime() == null) return 0;
                        return b2.getBookingTime().compareTo(b1.getBookingTime());
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching cancelled bookings: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Convert booking map to BookingSummaryDTO with connecting flight support
     */
    private BookingSummaryDTO convertToBookingSummary(Map<String, Object> booking) {
        Long bookingId = ((Number) booking.get("id")).longValue();
        String pnrNumber = (String) booking.get("pnrNumber");
        String status = (String) booking.get("status");
        Double totalAmount = (Double) booking.get("totalAmount");
        String fareClassCode = (String) booking.get("fareClassCode");
        LocalDateTime bookingTime = LocalDateTime.parse((String) booking.get("bookingTime"));

        List<Map<String, Object>> bookingFlights = (List<Map<String, Object>>) booking.get("bookingFlights");
        String flightNumber = "N/A";
        String sourceCode = "N/A";
        String destinationCode = "N/A";
        LocalDateTime departureTime = null;

        if (bookingFlights != null && !bookingFlights.isEmpty()) {
            List<String> flightNumbers = new ArrayList<>();
            for (int i = 0; i < bookingFlights.size(); i++) {
                Map<String, Object> flightData = (Map<String, Object>) bookingFlights.get(i).get("flight");
                if (flightData != null) {
                    String fn = (String) flightData.get("flightNumber");
                    if (fn != null) flightNumbers.add(fn);

                    if (i == 0) {
                        Map<String, Object> sourceAirport = (Map<String, Object>) flightData.get("sourceAirport");
                        if (sourceAirport != null) sourceCode = (String) sourceAirport.get("code");
                        String departureTimeStr = (String) flightData.get("departureTime");
                        if (departureTimeStr != null) departureTime = LocalDateTime.parse(departureTimeStr);
                    }
                    if (i == bookingFlights.size() - 1) {
                        Map<String, Object> destAirport = (Map<String, Object>) flightData.get("destinationAirport");
                        if (destAirport != null) destinationCode = (String) destAirport.get("code");
                    }
                }
            }
            flightNumber = String.join(" → ", flightNumbers);
        }

        List<Map<String, Object>> passengers = (List<Map<String, Object>>) booking.get("passengers");
        int passengerCount = passengers != null ? passengers.size() : 0;

        int checkInBaggageKg = 15;
        try {
            FareClassDTO fareClass = fareClassService.getFareClassByCode(fareClassCode);
            if (fareClass != null) checkInBaggageKg = fareClass.getCheckInBaggageKg();
        } catch (Exception e) {}

        boolean canCancel = "CONFIRMED".equals(status) && departureTime != null &&
                departureTime.isAfter(LocalDateTime.now().plusHours(24));

        int numberOfStops = (bookingFlights != null ? bookingFlights.size() - 1 : 0);

        return BookingSummaryDTO.builder()
                .bookingId(bookingId)
                .pnrNumber(pnrNumber)
                .flightNumber(flightNumber)
                .sourceCode(sourceCode)
                .destinationCode(destinationCode)
                .departureTime(departureTime)
                .status(status)
                .fareClassCode(fareClassCode)
                .totalAmount(totalAmount)
                .passengerCount(passengerCount)
                .checkInBaggageKg(checkInBaggageKg)
                .canCancel(canCancel)
                .bookingTime(bookingTime)
                .numberOfStops(numberOfStops)
                .build();
    }

    /**
     * Convert booking map to BookingDetailsDTO with full details
     */
    private BookingDetailsDTO convertToBookingDetails(Map<String, Object> booking) {
        Long bookingId = ((Number) booking.get("id")).longValue();
        String pnrNumber = (String) booking.get("pnrNumber");
        LocalDateTime bookingTime = LocalDateTime.parse((String) booking.get("bookingTime"));
        Double totalAmount = (Double) booking.get("totalAmount");
        String status = (String) booking.get("status");
        String fareClassCode = (String) booking.get("fareClassCode");

        FareClassDTO fareClass = null;
        try {
            fareClass = fareClassService.getFareClassByCode(fareClassCode);
        } catch (Exception e) {}

        List<Map<String, Object>> bookingFlights = (List<Map<String, Object>>) booking.get("bookingFlights");

        // Build flight segments for connecting flights
        List<BookingDetailsDTO.FlightSegmentDTO> flightSegments = new ArrayList<>();
        List<String> flightNumbersList = new ArrayList<>();
        String airlineName = "Airline";
        String sourceCode = "N/A", sourceCity = "N/A";
        String destinationCode = "N/A", destinationCity = "N/A";
        LocalDateTime departureTime = null, arrivalTime = null;
        Integer duration = null;

        if (bookingFlights != null) {
            for (int i = 0; i < bookingFlights.size(); i++) {
                Map<String, Object> flightData = (Map<String, Object>) bookingFlights.get(i).get("flight");
                if (flightData != null) {
                    String fn = (String) flightData.get("flightNumber");
                    if (fn != null) flightNumbersList.add(fn);

                    Map<String, Object> sourceAirport = (Map<String, Object>) flightData.get("sourceAirport");
                    Map<String, Object> destAirport = (Map<String, Object>) flightData.get("destinationAirport");
                    Map<String, Object> aircraft = (Map<String, Object>) flightData.get("aircraft");
                    Map<String, Object> airline = aircraft != null ? (Map<String, Object>) aircraft.get("airline") : null;

                    if (i == 0) {
                        airlineName = airline != null ? (String) airline.get("name") : "Airline";
                        if (sourceAirport != null) {
                            sourceCode = (String) sourceAirport.get("code");
                            sourceCity = (String) sourceAirport.get("city");
                        }
                        String departureTimeStr = (String) flightData.get("departureTime");
                        if (departureTimeStr != null) departureTime = LocalDateTime.parse(departureTimeStr);
                        duration = (Integer) flightData.get("duration");
                    }
                    if (i == bookingFlights.size() - 1) {
                        if (destAirport != null) {
                            destinationCode = (String) destAirport.get("code");
                            destinationCity = (String) destAirport.get("city");
                        }
                        String arrivalTimeStr = (String) flightData.get("arrivalTime");
                        if (arrivalTimeStr != null) arrivalTime = LocalDateTime.parse(arrivalTimeStr);
                    }

                    // Build segment
                    List<BookingDetailsDTO.PassengerSeatInfoDTO> passengerSeatsForSegment = new ArrayList<>();
                    List<Map<String, Object>> passengerSeatsList = (List<Map<String, Object>>) bookingFlights.get(i).get("passengerSeats");
                    if (passengerSeatsList != null) {
                        for (Map<String, Object> ps : passengerSeatsList) {
                            Map<String, Object> passenger = (Map<String, Object>) ps.get("passenger");
                            Map<String, Object> seat = (Map<String, Object>) ps.get("seat");
                            passengerSeatsForSegment.add(BookingDetailsDTO.PassengerSeatInfoDTO.builder()
                                    .passengerName(passenger != null ? (String) passenger.get("fullName") : "Unknown")
                                    .seatNumber(seat != null ? (String) seat.get("seatNumber") : "N/A")
                                    .seatPrice((Double) ps.get("seatPrice"))
                                    .build());
                        }
                    }

                    flightSegments.add(BookingDetailsDTO.FlightSegmentDTO.builder()
                            .flightId(((Number) flightData.get("id")).longValue())
                            .flightNumber(fn)
                            .airlineName(airline != null ? (String) airline.get("name") : "Airline")
                            .sourceCode(sourceAirport != null ? (String) sourceAirport.get("code") : "N/A")
                            .sourceCity(sourceAirport != null ? (String) sourceAirport.get("city") : "N/A")
                            .destinationCode(destAirport != null ? (String) destAirport.get("code") : "N/A")
                            .destinationCity(destAirport != null ? (String) destAirport.get("city") : "N/A")
                            .departureTime(LocalDateTime.parse((String) flightData.get("departureTime")))
                            .arrivalTime(LocalDateTime.parse((String) flightData.get("arrivalTime")))
                            .duration((Integer) flightData.get("duration"))
                            .passengerSeats(passengerSeatsForSegment)
                            .build());
                }
            }
        }

        String flightNumbers = String.join(" → ", flightNumbersList);
        int numberOfStops = (bookingFlights != null ? bookingFlights.size() - 1 : 0);

        // Get passengers with seat numbers
        List<Map<String, Object>> passengers = (List<Map<String, Object>>) booking.get("passengers");
        List<BookingDetailsDTO.PassengerInfoDTO> passengerList = new ArrayList<>();

        if (passengers != null) {
            for (Map<String, Object> passenger : passengers) {
                String fullName = (String) passenger.get("fullName");
                Integer age = (Integer) passenger.get("age");
                String mealPreference = (String) passenger.get("mealPreference");
                Integer extraBaggageKg = (Integer) passenger.get("extraBaggageKg");
                Double extraBaggagePrice = (Double) passenger.get("extraBaggagePrice");

                String seatNumber = "N/A";
                Map<String, Object> passengerSeat = (Map<String, Object>) passenger.get("passengerSeat");
                if (passengerSeat != null) {
                    Map<String, Object> seat = (Map<String, Object>) passengerSeat.get("seat");
                    if (seat != null) seatNumber = (String) seat.get("seatNumber");
                }

                passengerList.add(BookingDetailsDTO.PassengerInfoDTO.builder()
                        .fullName(fullName)
                        .age(age)
                        .seatNumber(seatNumber)
                        .mealPreference(mealPreference != null ? mealPreference : "Not selected")
                        .extraBaggageKg(extraBaggageKg != null ? extraBaggageKg : 0)
                        .extraBaggagePrice(extraBaggagePrice != null ? extraBaggagePrice : 0.0)
                        .build());
            }
        }

        boolean canCancel = "CONFIRMED".equals(status) && departureTime != null &&
                departureTime.isAfter(LocalDateTime.now().plusHours(24));

        double refundAmount = 0;
        if (canCancel && fareClass != null && departureTime != null) {
            long daysBeforeDeparture = java.time.Duration.between(LocalDateTime.now(), departureTime).toDays();
            refundAmount = fareClassService.calculateRefundAmount(fareClass, totalAmount, (int) daysBeforeDeparture);
        }

        return BookingDetailsDTO.builder()
                .bookingId(bookingId)
                .pnrNumber(pnrNumber)
                .bookingTime(bookingTime)
                .totalAmount(totalAmount)
                .status(status)
                .fareClassCode(fareClassCode)
                .fareClassName(fareClass != null ? fareClass.getName() : "Standard")
                .flightNumbers(flightNumbers)
                .airlineName(airlineName)
                .sourceCode(sourceCode)
                .sourceCity(sourceCity)
                .destinationCode(destinationCode)
                .destinationCity(destinationCity)
                .departureTime(departureTime)
                .arrivalTime(arrivalTime)
                .duration(duration)
                .basePrice(fareClass != null ? totalAmount / fareClass.getPriceMultiplier() : totalAmount)
                .fareMultiplier(fareClass != null ? fareClass.getPriceMultiplier() : 1.0)
                .finalPrice(totalAmount)
                .cabinBaggageKg(fareClass != null ? fareClass.getCabinBaggageKg() : 7)
                .checkInBaggageKg(fareClass != null ? fareClass.getCheckInBaggageKg() : 15)
                .extraBaggageRatePerKg(fareClass != null ? fareClass.getExtraBaggageRatePerKg() : 500.0)
                .mealIncluded(fareClass != null ? fareClass.getMealIncluded() : false)
                .seatSelectionFree(fareClass != null ? fareClass.getSeatSelectionFree() : false)
                .priorityCheckin(fareClass != null ? fareClass.getPriorityCheckin() : false)
                .priorityBoarding(fareClass != null ? fareClass.getPriorityBoarding() : false)
                .loungeAccess(fareClass != null ? fareClass.getLoungeAccess() : false)
                .cancellationFee(fareClass != null ? fareClass.getCancellationFee() : 0.0)
                .changeFee(fareClass != null ? fareClass.getChangeFee() : 0.0)
                .passengers(passengerList)
                .canCancel(canCancel)
                .refundAmount(refundAmount)
                .cancellationPolicy(fareClass != null ? fareClass.getRefundPercentageByDays() : "No refund")
                .numberOfStops(numberOfStops)
                .flightSegments(flightSegments)
                .build();
    }

    /**
     * Calculate refund amount based on fare class and days before departure
     */
    private double calculateRefundAmount(FareClassDTO fareClass, double totalAmount, int daysBeforeDeparture) {
        if (fareClass == null) return 0;
        String refundPolicy = fareClass.getRefundPercentageByDays();
        if (refundPolicy == null) {
            return Math.max(0, totalAmount - fareClass.getCancellationFee());
        }
        String[] rules = refundPolicy.split(",");
        for (String rule : rules) {
            String[] parts = rule.split(":");
            int thresholdDays = Integer.parseInt(parts[0]);
            double refundPercent = Double.parseDouble(parts[1]);
            if (daysBeforeDeparture >= thresholdDays) {
                return totalAmount * refundPercent / 100;
            }
        }
        return 0;
    }
}