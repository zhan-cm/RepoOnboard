package fixture.start;

import org.springframework.stereotype.Service;

@Service
public class OrderService {
    public OrderService(OrderRepository orderRepository) {
    }
}
