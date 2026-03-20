package central_api.central_api.controller;

import central_api.central_api.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AIRLINE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<Object>> searchFlights(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Boolean includeConnecting,
            @RequestParam(required = false) Double maxPrice) {
        return ResponseEntity.ok(flightService.searchFlights(source, destination, date, includeConnecting, maxPrice));
    }

    @GetMapping("/{flightId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AIRLINE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getFlightById(@PathVariable Long flightId) {
        return ResponseEntity.ok(flightService.getFlightById(flightId));
    }

    @GetMapping("/{flightId}/seatmap")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AIRLINE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getSeatMap(@PathVariable Long flightId) {
        return ResponseEntity.ok(flightService.getSeatMap(flightId));
    }
}
