package central_api.central_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareClassDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Double priceMultiplier;
    private Integer cabinBaggageKg;
    private Integer checkInBaggageKg;
    private Double extraBaggageRatePerKg;
    private Boolean mealIncluded;
    private Double cancellationFee;
    private Double changeFee;
    private String refundPercentageByDays;
    private Boolean seatSelectionFree;
    private Boolean priorityCheckin;
    private Boolean priorityBoarding;
    private Boolean loungeAccess;
    private Boolean isActive;
}