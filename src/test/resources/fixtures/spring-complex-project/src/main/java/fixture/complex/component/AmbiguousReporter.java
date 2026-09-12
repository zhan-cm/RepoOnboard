package fixture.complex.component;

import fixture.complex.service.AuditService;
import fixture.complex.service.PricingService;
import org.springframework.stereotype.Component;

@Component
public class AmbiguousReporter {
    public AmbiguousReporter(PricingService pricingService) {
    }

    public AmbiguousReporter(AuditService auditService) {
    }
}
