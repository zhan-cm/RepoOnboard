package fixture.complex.web;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class FakeController {
    @GetMapping("/guessed")
    public void guessed() {
    }
}
