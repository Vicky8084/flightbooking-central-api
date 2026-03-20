package central_api.central_api.service;

import central_api.central_api.client.DbApiClient;
import central_api.central_api.exception.CustomExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final DbApiClient dbApiClient;

    public List<Object> searchFlights(String source, String destination, LocalDate date, Boolean includeConnecting, Double maxPrice) {
        Map<String, Object> searchRequest = new HashMap<>();
        searchRequest.put("sourceCode", source);
        searchRequest.put("destinationCode", destination);
        searchRequest.put("travelDate", date.toString());
        searchRequest.put("includeConnectingFlights", includeConnecting != null ? includeConnecting : true);
        searchRequest.put("maxPrice", maxPrice);

        return dbApiClient.searchFlights(searchRequest);
    }

    public Map<String, Object> getFlightById(Long flightId) {
        try {
            return dbApiClient.getFlightById(flightId);
        } catch (Exception e) {
            throw new CustomExceptions.FlightNotFoundException("Flight not found with ID: " + flightId);
        }
    }

    public Map<String, Object> getSeatMap(Long flightId) {
        try {
            return dbApiClient.getSeatMap(flightId);
        } catch (Exception e) {
            throw new CustomExceptions.FlightNotFoundException("Flight not found with ID: " + flightId);
        }
    }
}
