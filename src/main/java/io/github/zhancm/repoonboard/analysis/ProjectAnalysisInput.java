package io.github.zhancm.repoonboard.analysis;

import io.github.zhancm.repoonboard.analyzer.java.JavaParseAnalysis;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalysis;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentAnalysis;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyAnalysis;
import io.github.zhancm.repoonboard.analyzer.spring.SpringConfigurationAnalysis;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointAnalysis;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import java.util.List;
import java.util.Objects;

/** Analyzer outputs consumed by the public report assembly boundary. */
public record ProjectAnalysisInput(
        MavenModuleAnalysis maven,
        JavaParseAnalysis java,
        SpringComponentAnalysis springComponents,
        SpringConfigurationAnalysis springConfiguration,
        SpringEndpointAnalysis springEndpoints,
        SpringComponentDependencyAnalysis springDependencies,
        List<Diagnostic> additionalDiagnostics) {
    public ProjectAnalysisInput {
        maven = Objects.requireNonNull(maven, "maven");
        java = Objects.requireNonNull(java, "java");
        springComponents = Objects.requireNonNull(springComponents, "springComponents");
        springConfiguration = Objects.requireNonNull(springConfiguration, "springConfiguration");
        springEndpoints = Objects.requireNonNull(springEndpoints, "springEndpoints");
        springDependencies = Objects.requireNonNull(springDependencies, "springDependencies");
        additionalDiagnostics = List.copyOf(
                Objects.requireNonNull(additionalDiagnostics, "additionalDiagnostics"));
    }

    public ProjectAnalysisInput(
            MavenModuleAnalysis maven,
            JavaParseAnalysis java,
            SpringComponentAnalysis springComponents,
            SpringConfigurationAnalysis springConfiguration,
            SpringEndpointAnalysis springEndpoints,
            SpringComponentDependencyAnalysis springDependencies) {
        this(maven, java, springComponents, springConfiguration,
                springEndpoints, springDependencies, List.of());
    }
}
