package central_api.central_api.controller;

import central_api.central_api.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/airlines/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingAirlines() {
        return ResponseEntity.ok(adminService.getPendingAirlines());
    }

    @GetMapping("/airlines/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveAirlines() {
        return ResponseEntity.ok(adminService.getActiveAirlines());
    }

    @PutMapping("/airlines/{airlineId}/approve")
    public ResponseEntity<Map<String, Object>> approveAirline(
            @PathVariable Long airlineId,
            @RequestParam Long adminId) {
        return ResponseEntity.ok(adminService.approveAirline(airlineId, adminId));
    }

    @PutMapping("/airlines/{airlineId}/reject")
    public ResponseEntity<Map<String, Object>> rejectAirline(
            @PathVariable Long airlineId,
            @RequestParam Long adminId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(adminService.rejectAirline(airlineId, adminId, reason));
    }
}