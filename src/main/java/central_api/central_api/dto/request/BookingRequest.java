package central_api.central_api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BookingRequest {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

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
    public static class PaymentDto {
        @NotBlank(message = "Payment method is required")
        private String paymentMethod;

        private Double amount;

        private String transactionId;
    }
}