package fixture.complex.service;

import fixture.complex.port.PaymentPort;
import fixture.complex.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
    public CheckoutService(
            PricingService pricingService,
            OrderRepository orderRepository,
            PaymentPort paymentPort) {
    }
}
