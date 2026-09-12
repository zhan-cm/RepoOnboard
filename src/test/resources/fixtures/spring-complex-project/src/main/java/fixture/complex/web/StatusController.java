package fixture.complex.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StatusController {
    @GetMapping("/status")
    public String status() {
        return "ok";
    }
}
