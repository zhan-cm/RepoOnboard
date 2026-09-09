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
        params = stable(params, "params");
        headers = stable(headers, "headers");
        consumes = stable(consumes, "consumes");
        produces = stable(produces, "produces");
    }

    private static List<String> stable(List<String> values, String name) {
        return Objects.requireNonNull(values, name).stream().sorted().toList();
    }
}
