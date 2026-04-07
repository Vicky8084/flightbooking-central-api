package central_api.central_api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    // For direct flight (backward compatibility)
    private Long flightId;

    // For connecting flight - LIST OF FLIGHTS ✅
    @Valid
    private List<FlightSegmentDto> flights = new ArrayList<>();

    @NotEmpty(message = "At least one seat is required")
    private List<@NotNull Long> seatIds;

    @NotEmpty(message = "Passenger details are required")
    @Valid
    private List<PassengerDto> passengers;

    @NotNull(message = "Payment details are required")
    @Valid
    private PaymentDto payment;

    private String fareClassCode;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightSegmentDto {
        @NotNull(message = "Flight ID is required")
        private Long flightId;

        private Integer sequence;

        @NotEmpty(message = "Seat selection required for each passenger")
        @Valid
        private List<PassengerSeatDto> passengerSeats = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerSeatDto {
        @NotNull(message = "Seat ID is required")
        private Long seatId;

        private Integer passengerIndex;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerDto {
        @NotBlank(message = "Passenger name is required")
        private String fullName;

        private Integer age;
        private String gender;
        private String passportNumber;
        private String nationality;

        @Email(message = "Invalid email format")
        private String email;

        private String phoneNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDto {
        @NotBlank(message = "Payment method is required")
        private String paymentMethod;

        private Double amount;

        private String transactionId;
    }
}