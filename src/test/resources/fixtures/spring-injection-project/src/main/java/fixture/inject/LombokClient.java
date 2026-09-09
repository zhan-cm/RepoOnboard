package fixture.inject;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LombokClient {
    private final CheckoutService service;
}
