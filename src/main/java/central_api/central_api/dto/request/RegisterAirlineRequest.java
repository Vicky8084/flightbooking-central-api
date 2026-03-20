package central_api.central_api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterAirlineRequest {

    @Valid
    @NotNull(message = "Airline details are required")
    private AirlineDto airline;

    @Valid
    @NotNull(message = "Admin details are required")
    private AdminDto admin;

    @Data
    public static class AirlineDto {
        @NotBlank(message = "Airline name is required")
        private String name;

        @NotBlank(message = "Airline code is required")
        @Pattern(regexp = "^[A-Z]{2,3}$", message = "Airline code must be 2-3 uppercase letters")
        private String code;

        @NotBlank(message = "Registration number is required")
        private String registrationNumber;

        @NotBlank(message = "Contact email is required")
        @Email(message = "Invalid email format")
        private String contactEmail;

        @NotBlank(message = "Contact phone is required")
        private String contactPhone;

        private String address;
        private String website;
    }

    @Data
    public static class AdminDto {
        @NotBlank(message = "Admin email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Admin password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,}$",
                message = "Password must contain at least one letter and one number")
        private String password;

        @NotBlank(message = "Admin full name is required")
        private String fullName;

        @NotBlank(message = "Admin phone is required")
        private String phone;

        private String designation;
        private String department;
    }
}