package central_api.central_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryDTO {
    private Long bookingId;
    private String pnrNumber;
    private String flightNumber;
    private String sourceCode;
    private String destinationCode;
    private LocalDateTime departureTime;
    private String status;
    private String fareClassCode;
    private Double totalAmount;
    private Integer passengerCount;
    private Integer checkInBaggageKg;
    private Boolean canCancel;
    private LocalDateTime bookingTime;
    private Integer numberOfStops;
}