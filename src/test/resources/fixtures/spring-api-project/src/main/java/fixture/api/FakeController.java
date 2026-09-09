package fixture.api;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class FakeController {

    @GetMapping
    public void fake() {
    }
}
