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
public class BookingResponse {
    private Long id;
    private String pnrNumber;
    private Long flightId;
    private String flightNumber;
    private String sourceAirport;
    private String destinationAirport;
    private LocalDateTime departureTime;
    private List<String> seatNumbers;
    private List<PassengerInfo> passengers;
    private Double totalAmount;
    private String status;
    private LocalDateTime bookingTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerInfo {
        private String fullName;
        private Integer age;
        private String seatNumber;
    }
}
