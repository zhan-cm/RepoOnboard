package fixture.dependency;

import org.springframework.stereotype.Service;

@Service
public class OrderService {
    public OrderService(InventoryService inventoryService, OrderRepository orderRepository) {
    }
}
