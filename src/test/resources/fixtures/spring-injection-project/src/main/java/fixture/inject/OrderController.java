package fixture.inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    public OrderController() {
    }

    @Autowired
    public OrderController(CheckoutService service) {
    }
}
