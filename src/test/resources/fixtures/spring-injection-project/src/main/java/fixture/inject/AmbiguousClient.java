package fixture.inject;

import org.springframework.stereotype.Component;

@Component
public class AmbiguousClient {
    public AmbiguousClient(CheckoutService service) {
    }

    public AmbiguousClient(Port port) {
    }
}
