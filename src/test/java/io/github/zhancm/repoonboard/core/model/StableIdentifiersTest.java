package io.github.zhancm.repoonboard.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StableIdentifiersTest {
    @Test
    void semanticIdentityIsIndependentOfConditionOrder() {
        String component = StableIdentifiers.component(
                StableIdentifiers.module("api/pom.xml"), "example.UserController");
        var first = new EndpointConditions(
                List.of("active=true", "role=admin"), List.of(), List.of(), List.of(), false);
        var reordered = new EndpointConditions(
                List.of("role=admin", "active=true"), List.of(), List.of(), List.of(), false);

        assertEquals(
                StableIdentifiers.endpoint(
                        component, "list", "GET", Optional.of("/users"), false, first),
                StableIdentifiers.endpoint(
                        component, "list", "GET", Optional.of("/users"), false, reordered));
    }

    @Test
    void renameAndEndpointSignatureChangesProduceNewIdentifiers() {
        String module = StableIdentifiers.module("pom.xml");
        String original = StableIdentifiers.component(module, "example.UserController");
        String renamed = StableIdentifiers.component(module, "example.AccountController");
        assertNotEquals(original, renamed);

        EndpointConditions conditions = new EndpointConditions(
                List.of(), List.of(), List.of(), List.of(), false);
        String getUsers = StableIdentifiers.endpoint(
                original, "list", "GET", Optional.of("/users"), false, conditions);
        String postUsers = StableIdentifiers.endpoint(
                original, "list", "POST", Optional.of("/users"), false, conditions);
        String renamedHandler = StableIdentifiers.endpoint(
                original, "findAll", "GET", Optional.of("/users"), false, conditions);
        assertNotEquals(getUsers, postUsers);
        assertNotEquals(getUsers, renamedHandler);
    }

    @Test
    void pathEncodingPreventsStructuralDelimiterCollisions() {
        assertNotEquals(
                StableIdentifiers.component("module:a:b", "c"),
                StableIdentifiers.component("module:a", "b:c"));
    }

    @Test
    void dependencyIdentityChangesWithRelationshipTarget() {
        String source = StableIdentifiers.component(
                StableIdentifiers.module("pom.xml"), "example.OrderController");
        String service = StableIdentifiers.component(
                StableIdentifiers.module("pom.xml"), "example.OrderService");
        String repository = StableIdentifiers.component(
                StableIdentifiers.module("pom.xml"), "example.OrderRepository");

        assertNotEquals(
                StableIdentifiers.dependency(
                        DependencyKind.COMPONENT_INJECTION, source, Optional.of(service),
                        "example.OrderService"),
                StableIdentifiers.dependency(
                        DependencyKind.COMPONENT_INJECTION, source, Optional.of(repository),
                        "example.OrderRepository"));
    }
}
