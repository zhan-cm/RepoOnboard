package io.github.zhancm.repoonboard.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.analysis.ProjectAnalysisInput;
import io.github.zhancm.repoonboard.analysis.ProjectModelAssembler;
import io.github.zhancm.repoonboard.analysis.RecommendationExplanationGenerator;
import io.github.zhancm.repoonboard.analyzer.java.JavaDeclarationIndex;
import io.github.zhancm.repoonboard.analyzer.java.JavaFileDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceParser;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceRootDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceResolver;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringConfigurationAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringInjectionAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringMvcMappingAnalyzer;
import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Component;
import io.github.zhancm.repoonboard.core.model.ComponentKind;
import io.github.zhancm.repoonboard.core.model.Dependency;
import io.github.zhancm.repoonboard.core.model.DependencyKind;
import io.github.zhancm.repoonboard.core.model.Endpoint;
import io.github.zhancm.repoonboard.core.model.ResolutionStatus;
import io.github.zhancm.repoonboard.serialization.AnalysisReportJson;
import io.github.zhancm.repoonboard.serialization.StartHereJson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class ComplexSpringRegressionFixtureTest {

    private static final String MODULE_ID = "module:pom.xml";

    @TempDir
    Path temporaryDirectory;

    @Test
    void coversComplexSpringFactsWithoutGuessingAmbiguousRelationships() throws IOException {
        Path fixture = FixturePaths.project("spring-complex-project");
        Set<String> filesBefore = relativeFiles(fixture);
        Path emptyLocalRepository = temporaryDirectory.resolve("empty-analysis-repository");

        AnalysisReport report = analyze(fixture, emptyLocalRepository);
        AnalysisReport repeated = analyze(fixture, emptyLocalRepository);

        assertEquals(report, repeated);
        assertEquals(AnalysisStatus.PARTIAL, report.status());
        assertEquals("project:pom.xml", report.project().id());
        assertEquals(List.of(MODULE_ID), report.modules().stream().map(module -> module.id()).toList());

        Map<String, Component> components = report.components().stream().collect(Collectors.toMap(
                Component::qualifiedName, Function.identity()));
        assertEquals(13, components.size());
        assertComponent(components, "fixture.complex.web.OrderController", ComponentKind.REST_CONTROLLER);
        assertComponent(components, "fixture.complex.web.StatusController", ComponentKind.CONTROLLER);
        assertComponent(components, "fixture.complex.web.FakeController", ComponentKind.REST_CONTROLLER);
        assertComponent(components, "fixture.complex.service.CheckoutService", ComponentKind.SERVICE);
        assertComponent(components, "fixture.complex.service.PricingService", ComponentKind.SERVICE);
        assertComponent(components, "fixture.complex.service.AuditService", ComponentKind.SERVICE);
        assertComponent(components, "fixture.complex.repository.OrderRepository", ComponentKind.REPOSITORY);
        assertComponent(components, "fixture.complex.config.AppConfiguration", ComponentKind.CONFIGURATION);
        assertComponent(components, "fixture.complex.Application", ComponentKind.CONFIGURATION);
        assertEquals(1, report.entryPoints().size());
        assertEquals("fixture.complex.Application", report.entryPoints().getFirst().qualifiedName());

        assertEquals(12, report.endpoints().size());
        assertEquals(Set.of(
                        "/api/orders", "/api/purchases",
                        "/internal/orders", "/internal/purchases"),
                endpoints(report, "list").stream()
                        .map(endpoint -> endpoint.path().orElseThrow())
                        .collect(Collectors.toSet()));
        assertTrue(endpoints(report, "list").stream().allMatch(endpoint ->
                endpoint.httpMethod().equals("GET")
                        && endpoint.conditions().headers().equals(List.of("X-Tenant"))
                        && endpoint.conditions().params().equals(List.of("state=active"))
                        && endpoint.conditions().produces().equals(List.of("application/json"))
                        && !endpoint.conditions().unresolved()));
        assertEquals(Set.of(
                        "GET /api/search", "POST /api/search",
                        "GET /internal/search", "POST /internal/search"),
                endpoints(report, "search").stream()
                        .map(endpoint -> endpoint.httpMethod() + " " + endpoint.path().orElseThrow())
                        .collect(Collectors.toSet()));
        assertEquals(List.of("/status"), endpoints(report, "status").stream()
                .map(endpoint -> endpoint.path().orElseThrow()).toList());
        assertTrue(report.endpoints().stream().noneMatch(endpoint ->
                endpoint.componentId().equals(componentId("fixture.complex.web.FakeController"))));

        Endpoint unresolvedPath = endpoints(report, "unresolvedPath").getFirst();
        assertTrue(unresolvedPath.unresolvedPath());
        assertTrue(unresolvedPath.path().isEmpty());
        assertEquals(2, endpoints(report, "unresolvedCondition").size());
        assertTrue(endpoints(report, "unresolvedCondition").stream().allMatch(endpoint ->
                endpoint.conditions().unresolved()
                        && endpoint.conditions().headers().equals(List.of("X-Tenant"))));

        List<Dependency> componentDependencies = report.dependencies().stream()
                .filter(dependency -> dependency.kind() == DependencyKind.COMPONENT_INJECTION)
                .toList();
        Map<String, String> namesById = report.components().stream().collect(Collectors.toMap(
                Component::id, Component::qualifiedName));
        Set<String> confirmedEdges = componentDependencies.stream()
                .filter(dependency -> dependency.status() == ResolutionStatus.CONFIRMED)
                .map(dependency -> simple(namesById.get(dependency.sourceId())) + "->"
                        + simple(namesById.get(dependency.targetId().orElseThrow())))
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "OrderController->CheckoutService",
                "CheckoutService->PricingService",
                "CheckoutService->OrderRepository",
                "PricingService->AuditService",
                "AuditService->PricingService"), confirmedEdges);

        Dependency interfaceDependency = componentDependencies.stream()
                .filter(dependency -> dependency.sourceId().equals(
                        componentId("fixture.complex.service.CheckoutService")))
                .filter(dependency -> dependency.declaredTarget().equals("PaymentPort"))
                .findFirst()
                .orElseThrow();
        assertEquals(ResolutionStatus.UNRESOLVED, interfaceDependency.status());
        assertTrue(interfaceDependency.targetId().isEmpty());
        assertTrue(componentDependencies.stream().noneMatch(dependency -> dependency.targetId()
                .filter(target -> target.equals(componentId("fixture.complex.port.CardPaymentPort"))
                        || target.equals(componentId("fixture.complex.port.BankPaymentPort")))
                .isPresent()));

        List<Dependency> ambiguousConstructors = componentDependencies.stream()
                .filter(dependency -> dependency.sourceId().equals(
                        componentId("fixture.complex.component.AmbiguousReporter")))
                .toList();
        assertEquals(2, ambiguousConstructors.size());
        assertTrue(ambiguousConstructors.stream().allMatch(dependency ->
                dependency.status() == ResolutionStatus.AMBIGUOUS
                        && dependency.targetId().isEmpty()));
        assertTrue(componentDependencies.stream().noneMatch(dependency -> dependency.sourceId().equals(
                componentId("fixture.complex.component.LegacyNotifier"))));

        Set<String> diagnosticCodes = report.diagnostics().stream()
                .map(diagnostic -> diagnostic.code())
                .collect(Collectors.toSet());
        assertTrue(diagnosticCodes.containsAll(Set.of(
                "SPRING_CONSTRUCTOR_INJECTION_AMBIGUOUS",
                "SPRING_DEPENDENCY_INJECTION_AMBIGUOUS",
                "SPRING_DEPENDENCY_TARGET_UNRESOLVED",
                "SPRING_METHOD_INJECTION_UNSUPPORTED",
                "SPRING_MVC_CONDITION_UNRESOLVED",
                "SPRING_MVC_PATH_UNRESOLVED")));

        assertEquals(filesBefore, relativeFiles(fixture));
        assertFalse(Files.exists(fixture.resolve("target")));
        assertFalse(Files.exists(emptyLocalRepository));
    }

    @Test
    void usesOneStableReportSnapshotForUiAndStartHere() throws Exception {
        Path fixture = FixturePaths.project("spring-complex-project");
        Path emptyLocalRepository = temporaryDirectory.resolve("empty-snapshot-repository");
        AnalysisReport report = analyze(fixture, emptyLocalRepository);
        AnalysisReport repeated = analyze(fixture, emptyLocalRepository);

        AnalysisReportJson reportCodec = new AnalysisReportJson();
        StartHereJson startHereCodec = new StartHereJson();
        String uiSnapshot = reportCodec.serialize(report);
        String repeatedUiSnapshot = reportCodec.serialize(repeated);
        String startHereSnapshot = startHereCodec.serialize(report);
        String repeatedStartHereSnapshot = startHereCodec.serialize(repeated);

        assertEquals(uiSnapshot, repeatedUiSnapshot);
        assertEquals(startHereSnapshot, repeatedStartHereSnapshot);
        assertEquals(report, reportCodec.deserialize(uiSnapshot));

        var mapper = JsonMapper.builder().build();
        var uiJson = mapper.readTree(uiSnapshot);
        var startHereJson = mapper.readTree(startHereSnapshot);
        assertEquals(uiJson.get("schemaVersion").textValue(),
                startHereJson.get("reportSchemaVersion").textValue());
        assertEquals(uiJson.get("project").get("id").textValue(),
                startHereJson.get("projectId").textValue());
        assertEquals(uiJson.get("status").textValue(),
                startHereJson.get("analysisStatus").textValue());
        assertTrue(startHereJson.get("coverageLimited").booleanValue());

        var explained = new RecommendationExplanationGenerator().generate(report);
        List<String> expectedReadingPath = List.of(
                "pom.xml",
                "src/main/java/fixture/complex/Application.java",
                "src/main/java/fixture/complex/config/AppConfiguration.java",
                "src/main/java/fixture/complex/web/OrderController.java",
                "src/main/java/fixture/complex/web/StatusController.java",
                "src/main/java/fixture/complex/service/CheckoutService.java",
                "src/main/java/fixture/complex/service/PricingService.java",
                "src/main/java/fixture/complex/repository/OrderRepository.java",
                "src/main/java/fixture/complex/service/AuditService.java");
        List<String> recommendedPath = explained.path().items().stream()
                .map(item -> item.sourceFileId())
                .toList();
        assertEquals(expectedReadingPath, recommendedPath);
        List<String> serializedReadingPath = new ArrayList<>();
        startHereJson.get("items").forEach(item ->
                serializedReadingPath.add(item.get("sourceFileId").textValue()));
        assertEquals(expectedReadingPath, serializedReadingPath);
        assertTrue(recommendedPath.stream().noneMatch(path ->
                path.endsWith("FakeController.java")
                        || path.endsWith("AmbiguousReporter.java")
                        || path.endsWith("LegacyNotifier.java")
                        || path.endsWith("CardPaymentPort.java")
                        || path.endsWith("BankPaymentPort.java")));

        Set<String> reportEntityIds = new HashSet<>();
        report.modules().forEach(module -> reportEntityIds.add(module.id()));
        report.components().forEach(component -> reportEntityIds.add(component.id()));
        report.endpoints().forEach(endpoint -> reportEntityIds.add(endpoint.id()));
        report.entryPoints().forEach(entryPoint -> reportEntityIds.add(entryPoint.id()));
        report.dependencies().forEach(dependency -> reportEntityIds.add(dependency.id()));
        assertTrue(explained.explanations().stream()
                .flatMap(explanation -> explanation.reasons().stream())
                .flatMap(reason -> reason.supportingEntityIds().stream())
                .allMatch(reportEntityIds::contains));
        assertEquals(explained.path().items().size(), startHereJson.get("totalItemCount").intValue());
        assertFalse(Files.exists(emptyLocalRepository));
    }

    private static void assertComponent(
            Map<String, Component> components, String qualifiedName, ComponentKind kind) {
        Component component = components.get(qualifiedName);
        assertEquals(componentId(qualifiedName), component.id());
        assertEquals(MODULE_ID, component.moduleId());
        assertEquals(kind, component.kind());
        assertTrue(component.location().startLine().isPresent());
        assertFalse(component.evidence().isEmpty());
    }

    private static List<Endpoint> endpoints(AnalysisReport report, String handler) {
        return report.endpoints().stream()
                .filter(endpoint -> endpoint.handlerMethod().equals(handler))
                .toList();
    }

    private static String componentId(String qualifiedName) {
        return "component:module%3Apom.xml:" + qualifiedName;
    }

    private static String simple(String qualifiedName) {
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }

    private static AnalysisReport analyze(Path fixture, Path localRepository) {
        var maven = new MavenModuleAnalyzer().analyze(
                fixture, new MavenModelOptions(localRepository, List.of(), 1_048_576));
        var roots = new JavaSourceRootDiscoverer().discover(fixture, maven);
        var files = new JavaFileDiscoverer().discover(fixture, roots);
        var parsed = new JavaSourceParser().parse(fixture, files);
        var java = new JavaTypeReferenceResolver().resolve(
                parsed, maven, JavaDeclarationIndex.build(parsed));
        var components = new SpringComponentAnalyzer().analyze(java);
        var configuration = new SpringConfigurationAnalyzer().analyze(java);
        var injection = new SpringInjectionAnalyzer().analyze(java, components, configuration);
        var mappings = new SpringMvcMappingAnalyzer().analyze(java, components);
        var endpoints = new SpringEndpointAnalyzer().analyze(mappings);
        var dependencies = new SpringComponentDependencyAnalyzer().analyze(components, injection);
        var additionalDiagnostics = Stream.of(
                        roots.diagnostics(), files.diagnostics(),
                        injection.diagnostics(), mappings.diagnostics())
                .flatMap(List::stream)
                .distinct()
                .toList();
        return new ProjectModelAssembler().assemble(new ProjectAnalysisInput(
                maven, java, components, configuration, endpoints, dependencies,
                additionalDiagnostics));
    }

    private static Set<String> relativeFiles(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }
}
