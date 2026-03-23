package central_api.central_api.service;

import central_api.central_api.client.DbApiClient;
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

    /**
     * Get user profile with stats
     */
    public UserProfileDTO getUserProfile(Long userId) {
        try {
            Map<String, Object> userData = dbApiClient.getUserProfile(userId);

            if (userData == null || userData.isEmpty()) {
                throw new CustomExceptions.UserNotFoundException("User not found");
            }

            log.debug("User profile data: {}", userData);

            // ✅ FIX: Now getUserBookings returns Map, extract list from "bookings" key
            Map<String, Object> bookingsResponse = dbApiClient.getUserBookings(userId);
            List<Map<String, Object>> allBookings = new ArrayList<>();

            if (bookingsResponse != null && bookingsResponse.get("bookings") != null) {
                allBookings = (List<Map<String, Object>>) bookingsResponse.get("bookings");
            }

            LocalDateTime now = LocalDateTime.now();

            int upcomingCount = 0;
            int completedCount = 0;
            int cancelledCount = 0;

            if (allBookings != null) {
                for (Map<String, Object> booking : allBookings) {
                    String status = (String) booking.get("status");
                    if ("CANCELLED".equals(status)) {
                        cancelledCount++;
                    } else if ("CONFIRMED".equals(status)) {
                        // Check if flight is in future
                        List<Map<String, Object>> bookingFlights = (List<Map<String, Object>>) booking.get("bookingFlights");
                        if (bookingFlights != null && !bookingFlights.isEmpty()) {
                            Map<String, Object> flight = (Map<String, Object>) bookingFlights.get(0).get("flight");
                            if (flight != null) {
                                String departureTimeStr = (String) flight.get("departureTime");
                                if (departureTimeStr != null) {
                                    LocalDateTime departureTime = LocalDateTime.parse(departureTimeStr);
                                    if (departureTime.isAfter(now)) {
                                        upcomingCount++;
                                    } else {
                                        completedCount++;
                                    }
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
                    .memberSince(userData.get("createdAt") != null ?
                            ((String) userData.get("createdAt")).substring(0, 10) : "N/A")
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
            Map<String, Object> bookingsResponse = dbApiClient.getUserBookings(userId);
            List<Map<String, Object>> bookings = new ArrayList<>();

            if (bookingsResponse != null && bookingsResponse.get("bookings") != null) {
                bookings = (List<Map<String, Object>>) bookingsResponse.get("bookings");
            }

            return bookings.stream()
                    .map(this::convertToBookingSummary)
                    .sorted((b1, b2) -> b2.getDepartureTime().compareTo(b1.getDepartureTime()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching user bookings: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get upcoming bookings
     */
    public List<BookingSummaryDTO> getUpcomingBookings(Long userId) {
        try {
            Map<String, Object> bookingsResponse = dbApiClient.getUserUpcomingBookings(userId);
            List<Map<String, Object>> bookings = new ArrayList<>();

            if (bookingsResponse != null && bookingsResponse.get("bookings") != null) {
                bookings = (List<Map<String, Object>>) bookingsResponse.get("bookings");
            }

            return bookings.stream()
                    .map(this::convertToBookingSummary)
                    .sorted((b1, b2) -> b1.getDepartureTime().compareTo(b2.getDepartureTime()))
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
            Map<String, Object> bookingsResponse = dbApiClient.getUserPastBookings(userId);
            List<Map<String, Object>> bookings = new ArrayList<>();

            if (bookingsResponse != null && bookingsResponse.get("bookings") != null) {
                bookings = (List<Map<String, Object>>) bookingsResponse.get("bookings");
            }

            return bookings.stream()
                    .map(this::convertToBookingSummary)
                    .sorted((b1, b2) -> b2.getDepartureTime().compareTo(b1.getDepartureTime()))
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
            Map<String, Object> bookingsResponse = dbApiClient.getUserCancelledBookings(userId);
            List<Map<String, Object>> bookings = new ArrayList<>();

            if (bookingsResponse != null && bookingsResponse.get("bookings") != null) {
                bookings = (List<Map<String, Object>>) bookingsResponse.get("bookings");
            }

            return bookings.stream()
                    .map(this::convertToBookingSummary)
                    .sorted((b1, b2) -> b2.getBookingTime().compareTo(b1.getBookingTime()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching cancelled bookings: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get booking details by ID
     */
    public BookingDetailsDTO getBookingDetails(Long bookingId, Long userId) {
        try {
            Map<String, Object> booking = dbApiClient.getBookingById(bookingId);

            // Verify booking belongs to user
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
     * Cancel booking
     */
    public Map<String, Object> cancelBooking(Long bookingId, Long userId, String reason) {
        try {
            // First get booking to verify ownership
            Map<String, Object> booking = dbApiClient.getBookingById(bookingId);
            Map<String, Object> user = (Map<String, Object>) booking.get("user");
            Long bookingUserId = ((Number) user.get("id")).longValue();

            if (!bookingUserId.equals(userId)) {
                throw new CustomExceptions.UnauthorizedException("You cannot cancel someone else's booking");
            }

            // Get fare class for refund calculation
            String fareClassCode = (String) booking.get("fareClassCode");
            FareClassDTO fareClass = fareClassService.getFareClassByCode(fareClassCode);

            // Calculate refund amount
            Double totalAmount = (Double) booking.get("totalAmount");
            LocalDateTime now = LocalDateTime.now();

            // Get flight departure time
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

            // Call cancel API
            Map<String, Object> cancelResponse = dbApiClient.cancelBooking(bookingId);

            cancelResponse.put("refundAmount", refundAmount);
            cancelResponse.put("cancellationFee", fareClass.getCancellationFee());
            cancelResponse.put("daysBeforeDeparture", daysBeforeDeparture);

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
     * Convert booking map to BookingSummaryDTO
     */
    private BookingSummaryDTO convertToBookingSummary(Map<String, Object> booking) {
        Long bookingId = ((Number) booking.get("id")).longValue();
        String pnrNumber = (String) booking.get("pnrNumber");
        String status = (String) booking.get("status");
        Double totalAmount = (Double) booking.get("totalAmount");
        String fareClassCode = (String) booking.get("fareClassCode");
        LocalDateTime bookingTime = LocalDateTime.parse((String) booking.get("bookingTime"));

        // Get flight details
        List<Map<String, Object>> bookingFlights = (List<Map<String, Object>>) booking.get("bookingFlights");
        String flightNumber = "N/A";
        String sourceCode = "N/A";
        String destinationCode = "N/A";
        LocalDateTime departureTime = null;

        if (bookingFlights != null && !bookingFlights.isEmpty()) {
            Map<String, Object> flightData = (Map<String, Object>) bookingFlights.get(0).get("flight");
            flightNumber = (String) flightData.get("flightNumber");

            Map<String, Object> sourceAirport = (Map<String, Object>) flightData.get("sourceAirport");
            Map<String, Object> destAirport = (Map<String, Object>) flightData.get("destinationAirport");

            if (sourceAirport != null) sourceCode = (String) sourceAirport.get("code");
            if (destAirport != null) destinationCode = (String) destAirport.get("code");

            departureTime = LocalDateTime.parse((String) flightData.get("departureTime"));
        }

        // Get passenger count
        List<Map<String, Object>> passengers = (List<Map<String, Object>>) booking.get("passengers");
        int passengerCount = passengers != null ? passengers.size() : 0;

        // Get baggage allowance from fare class
        int checkInBaggageKg = 15; // default
        try {
            FareClassDTO fareClass = fareClassService.getFareClassByCode(fareClassCode);
            checkInBaggageKg = fareClass.getCheckInBaggageKg();
        } catch (Exception e) {
            log.warn("Could not fetch fare class for baggage: {}", fareClassCode);
        }

        boolean canCancel = "CONFIRMED".equals(status) && departureTime != null &&
                departureTime.isAfter(LocalDateTime.now().plusHours(24));

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
                .build();
    }

    /**
     * Convert booking map to BookingDetailsDTO
     */
    private BookingDetailsDTO convertToBookingDetails(Map<String, Object> booking) {
        Long bookingId = ((Number) booking.get("id")).longValue();
        String pnrNumber = (String) booking.get("pnrNumber");
        LocalDateTime bookingTime = LocalDateTime.parse((String) booking.get("bookingTime"));
        Double totalAmount = (Double) booking.get("totalAmount");
        String status = (String) booking.get("status");
        String fareClassCode = (String) booking.get("fareClassCode");

        // Get fare class details
        FareClassDTO fareClass = null;
        try {
            fareClass = fareClassService.getFareClassByCode(fareClassCode);
        } catch (Exception e) {
            log.warn("Could not fetch fare class: {}", fareClassCode);
        }

        // Get flight details
        List<Map<String, Object>> bookingFlights = (List<Map<String, Object>>) booking.get("bookingFlights");
        Map<String, Object> flightData = null;
        if (bookingFlights != null && !bookingFlights.isEmpty()) {
            flightData = (Map<String, Object>) bookingFlights.get(0).get("flight");
        }

        String flightNumber = flightData != null ? (String) flightData.get("flightNumber") : "N/A";
        String airlineName = "Airline";

        // Get aircraft and airline
        if (flightData != null) {
            Map<String, Object> aircraft = (Map<String, Object>) flightData.get("aircraft");
            if (aircraft != null) {
                Map<String, Object> airline = (Map<String, Object>) aircraft.get("airline");
                if (airline != null) {
                    airlineName = (String) airline.get("name");
                }
            }
        }

        // Get source/destination airports
        String sourceCode = "N/A";
        String sourceCity = "N/A";
        String destinationCode = "N/A";
        String destinationCity = "N/A";
        LocalDateTime departureTime = null;
        LocalDateTime arrivalTime = null;
        Integer duration = null;

        if (flightData != null) {
            Map<String, Object> sourceAirport = (Map<String, Object>) flightData.get("sourceAirport");
            Map<String, Object> destAirport = (Map<String, Object>) flightData.get("destinationAirport");

            if (sourceAirport != null) {
                sourceCode = (String) sourceAirport.get("code");
                sourceCity = (String) sourceAirport.get("city");
            }
            if (destAirport != null) {
                destinationCode = (String) destAirport.get("code");
                destinationCity = (String) destAirport.get("city");
            }

            departureTime = LocalDateTime.parse((String) flightData.get("departureTime"));
            arrivalTime = LocalDateTime.parse((String) flightData.get("arrivalTime"));
            duration = (Integer) flightData.get("duration");
        }

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
                    if (seat != null) {
                        seatNumber = (String) seat.get("seatNumber");
                    }
                }

                passengerList.add(BookingDetailsDTO.PassengerInfoDTO.builder()
                        .fullName(fullName)
                        .age(age)
                        .seatNumber(seatNumber)
                        .mealPreference(mealPreference)
                        .extraBaggageKg(extraBaggageKg != null ? extraBaggageKg : 0)
                        .extraBaggagePrice(extraBaggagePrice != null ? extraBaggagePrice : 0.0)
                        .build());
            }
        }

        // Calculate refund amount
        boolean canCancel = "CONFIRMED".equals(status) && departureTime != null &&
                departureTime.isAfter(LocalDateTime.now().plusHours(24));

        double refundAmount = 0;
        if (canCancel && fareClass != null) {
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
                .flightId(flightData != null ? ((Number) flightData.get("id")).longValue() : null)
                .flightNumber(flightNumber)
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
                .timeMultiplier(flightData != null ? ((Number) flightData.getOrDefault("timeMultiplier", 1.0)).doubleValue() : 1.0)
                .demandMultiplier(flightData != null ? ((Number) flightData.getOrDefault("demandMultiplier", 1.0)).doubleValue() : 1.0)
                .dayMultiplier(flightData != null ? ((Number) flightData.getOrDefault("dayMultiplier", 1.0)).doubleValue() : 1.0)
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