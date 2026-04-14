package central_api.central_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailsDTO {
    private Long bookingId;
    private String pnrNumber;
    private LocalDateTime bookingTime;
    private Double totalAmount;
    private String status;
    private String fareClassCode;
    private String fareClassName;

    // Flight details
    private Long flightId;
    private String flightNumber;
    private String flightNumbers;  // ✅ For connecting flights - multiple flight numbers
    private String airlineName;
    private String sourceCode;
    private String sourceCity;
    private String destinationCode;
    private String destinationCity;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer duration;

    // Fare details
    private Double basePrice;
    private Double fareMultiplier;
    private Double timeMultiplier;
    private Double demandMultiplier;
    private Double dayMultiplier;
    private Double finalPrice;

    // Baggage
    private Integer cabinBaggageKg;
    private Integer checkInBaggageKg;
    private Double extraBaggageRatePerKg;

    // Benefits
    private Boolean mealIncluded;
    private Boolean seatSelectionFree;
    private Boolean priorityCheckin;
    private Boolean priorityBoarding;
    private Boolean loungeAccess;
    private Double cancellationFee;
    private Double changeFee;

    // Passengers
    private List<PassengerInfoDTO> passengers;

    // Cancellation info
    private Boolean canCancel;
    private Double refundAmount;
    private String cancellationPolicy;

    // ✅ NEW FIELDS
    private Integer numberOfStops;
    private List<FlightSegmentDTO> flightSegments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerInfoDTO {
        private String fullName;
        private Integer age;
        private String seatNumber;
        private String mealPreference;
        private Integer extraBaggageKg;
        private Double extraBaggagePrice;
    }

    // ✅ NEW INNER CLASS - FlightSegmentDTO for connecting flights
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightSegmentDTO {
        private Long flightId;
        private String flightNumber;
        private String airlineName;
        private String sourceCode;
        private String sourceCity;
        private String destinationCode;
        private String destinationCity;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private Integer duration;
        private List<PassengerSeatInfoDTO> passengerSeats;
    }

    // ✅ NEW INNER CLASS - PassengerSeatInfoDTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerSeatInfoDTO {
        private String passengerName;
        private String seatNumber;
        private Double seatPrice;
    }
}