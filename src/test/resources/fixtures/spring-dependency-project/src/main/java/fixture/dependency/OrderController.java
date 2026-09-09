package fixture.dependency;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    public OrderController(OrderService orderService) {
    }
}
