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
public class FlightResponse {
    private Long id;
    private String flightNumber;
    private String sourceAirport;
    private String sourceCity;
    private String destinationAirport;
    private String destinationCity;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer duration;
    private Double basePriceEconomy;
    private Double basePriceBusiness;
    private Double basePriceFirstClass;
    private String status;
    private String airlineName;
    private String aircraftModel;
}
