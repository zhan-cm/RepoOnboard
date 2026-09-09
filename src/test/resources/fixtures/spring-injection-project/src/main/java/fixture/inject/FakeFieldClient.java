package fixture.inject;

import org.springframework.stereotype.Component;

@Component
public class FakeFieldClient {
    @Autowired
    private CheckoutService service;
}
