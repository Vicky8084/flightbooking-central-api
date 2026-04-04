package central_api.central_api.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    private Long id;
    private String seatNumber;
    private String seatClass;
    private String seatType;
    private Boolean hasExtraLegroom;
    private Boolean isNearExit;
    private Double extraPrice;
    private Boolean isActive;
}