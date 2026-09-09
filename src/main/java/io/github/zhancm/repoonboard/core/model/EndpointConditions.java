package io.github.zhancm.repoonboard.core.model;

import java.util.List;
import java.util.Objects;

/** Framework-neutral HTTP routing conditions. */
public record EndpointConditions(
        List<String> params,
        List<String> headers,
        List<String> consumes,
        List<String> produces,
        boolean unresolved) {
    public EndpointConditions {
        params = List.copyOf(Objects.requireNonNull(params, "params"));
        headers = List.copyOf(Objects.requireNonNull(headers, "headers"));
        consumes = List.copyOf(Objects.requireNonNull(consumes, "consumes"));
        produces = List.copyOf(Objects.requireNonNull(produces, "produces"));
    }
}
