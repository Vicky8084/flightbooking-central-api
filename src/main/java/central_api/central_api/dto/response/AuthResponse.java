package central_api.central_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private Long airlineId;
    private String status;
    private String message;

    public AuthResponse(String token, Long id, String email, String fullName, String role, Long airlineId, String status) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.airlineId = airlineId;
        this.status = status;
    }

    public AuthResponse(String token, Long id, String email, String fullName, String role, Long airlineId, String status, String message) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.airlineId = airlineId;
        this.status = status;
        this.message = message;
    }
}