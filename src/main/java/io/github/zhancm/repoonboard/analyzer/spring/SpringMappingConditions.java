package io.github.zhancm.repoonboard.analyzer.spring;

import java.util.List;
import java.util.Objects;

/** Literal Spring MVC mapping conditions retained from one annotation. */
public record SpringMappingConditions(
        List<String> params,
        List<String> headers,
        List<String> consumes,
        List<String> produces,
        boolean unresolved) {

    public SpringMappingConditions {
        params = List.copyOf(Objects.requireNonNull(params, "params"));
        headers = List.copyOf(Objects.requireNonNull(headers, "headers"));
        consumes = List.copyOf(Objects.requireNonNull(consumes, "consumes"));
        produces = List.copyOf(Objects.requireNonNull(produces, "produces"));
    }

    public static SpringMappingConditions empty() {
        return new SpringMappingConditions(List.of(), List.of(), List.of(), List.of(), false);
    }
}
