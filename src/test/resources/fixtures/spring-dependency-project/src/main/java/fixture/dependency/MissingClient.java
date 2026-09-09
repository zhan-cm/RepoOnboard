package fixture.dependency;

import org.springframework.stereotype.Component;

@Component
public class MissingClient {
    public MissingClient(ExternalGateway externalGateway) {
    }
}
