package central_api.central_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private Long airlineId;
    private String airlineName;
    private String status;
    private Boolean isActive;
    private String memberSince;  // Registration date
    private Integer totalBookings;
    private Integer upcomingTrips;
    private Integer completedTrips;
    private Integer cancelledTrips;
}