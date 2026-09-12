package fixture.complex.component;

import fixture.complex.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LegacyNotifier {
    @Autowired
    public void setAuditService(AuditService auditService) {
    }
}
