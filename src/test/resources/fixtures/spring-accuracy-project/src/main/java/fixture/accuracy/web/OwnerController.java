package fixture.accuracy.web;

import fixture.accuracy.repository.OwnerRepository;
import fixture.accuracy.service.OwnerService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owners")
public class OwnerController {
    private final OwnerService service;
    private final OwnerRepository owners;

    public OwnerController(OwnerService service, OwnerRepository owners) {
        this.service = service;
        this.owners = owners;
    }

    @GetMapping
    public List<String> list() {
        return List.of();
    }
}
