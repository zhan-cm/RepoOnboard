package fixture.inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FieldClient {
    @Autowired
    private CheckoutService service;
}
