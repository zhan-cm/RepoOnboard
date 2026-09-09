package fixture.inject;

import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
    private final Port port;

    public CheckoutService(Port port) {
        this.port = port;
    }
}
