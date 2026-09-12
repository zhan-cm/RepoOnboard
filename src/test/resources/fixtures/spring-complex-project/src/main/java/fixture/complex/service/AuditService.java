package fixture.complex.service;

import org.springframework.stereotype.Service;

@Service
public class AuditService {
    public AuditService(PricingService pricingService) {
    }
}
