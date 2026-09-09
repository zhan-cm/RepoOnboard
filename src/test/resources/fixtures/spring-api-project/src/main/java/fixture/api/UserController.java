package fixture.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/api", "/internal"}, headers = "X-Tenant")
public class UserController {

    @GetMapping(path = {"/users", "/members"}, params = "active=true", produces = "application/json")
    public String list() {
        return "";
    }

    @RequestMapping(path = "/search", method = {RequestMethod.GET, RequestMethod.POST},
            consumes = "application/json")
    public String search() {
        return "";
    }

    @DeleteMapping(PATH_PREFIX)
    public void unresolvedPath() {
    }

    @PatchMapping(path = "/users/{id}", headers = HEADER_CONDITION)
    public void unresolvedCondition() {
    }
}
