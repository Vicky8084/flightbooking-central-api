package central_api.central_api.service;

import central_api.central_api.client.DbApiClient;
import central_api.central_api.dto.response.FareClassDTO;
import central_api.central_api.exception.CustomExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FareClassService {

    private final DbApiClient dbApiClient;

    /**
     * Get all active fare classes
     */
    public List<FareClassDTO> getAllFareClasses() {
        try {
            return dbApiClient.getFareClasses();
        } catch (Exception e) {
            log.error("Error fetching fare classes: {}", e.getMessage());
            throw new CustomExceptions.BadRequestException("Failed to fetch fare classes");
        }
    }

    /**
     * Get fare class by code
     */
    public FareClassDTO getFareClassByCode(String code) {
        try {
            return dbApiClient.getFareClassByCode(code);
        } catch (Exception e) {
            log.error("Error fetching fare class {}: {}", code, e.getMessage());
            throw new CustomExceptions.BadRequestException("Fare class not found: " + code);
        }
    }

    /**
     * Calculate baggage cost for extra kg
     */
    public double calculateExtraBaggageCost(String fareClassCode, int extraKg) {
        FareClassDTO fareClass = getFareClassByCode(fareClassCode);
        if (extraKg <= 0) return 0;
        return extraKg * fareClass.getExtraBaggageRatePerKg();
    }

    /**
     * Calculate refund amount based on days before departure
     */
    public double calculateRefundAmount(FareClassDTO fareClass, double totalAmount, int daysBeforeDeparture) {
        if (fareClass == null) return 0;

        String refundPolicy = fareClass.getRefundPercentageByDays();
        if (refundPolicy == null || refundPolicy.isEmpty()) {
            return Math.max(0, totalAmount - fareClass.getCancellationFee());
        }

        String[] rules = refundPolicy.split(",");
        for (String rule : rules) {
            String[] parts = rule.split(":");
            int thresholdDays = Integer.parseInt(parts[0]);
            double refundPercent = Double.parseDouble(parts[1]);
            if (daysBeforeDeparture >= thresholdDays) {
                return totalAmount * refundPercent / 100;
            }
        }

        return 0;
    }
}