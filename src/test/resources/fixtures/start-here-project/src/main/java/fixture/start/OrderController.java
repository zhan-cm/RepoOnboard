package fixture.start;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
    public OrderController(OrderService orderService) {
    }

    @GetMapping
    public void list() {
    }

    @GetMapping("/{id}")
    public void find() {
    }
}
