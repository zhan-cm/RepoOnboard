package io.github.zhancm.repoonboard.cli;

import io.github.zhancm.repoonboard.analysis.ProjectAnalysisInput;
import io.github.zhancm.repoonboard.analysis.ProjectModelAssembler;
import io.github.zhancm.repoonboard.analyzer.java.JavaDeclarationIndex;
import io.github.zhancm.repoonboard.analyzer.java.JavaFileDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceRootDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceParser;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceResolver;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceStatus;
import io.github.zhancm.repoonboard.analyzer.maven.MavenMetadataValue;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModule;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalysis;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectDetection;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectDetector;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectMetadata;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentDependencyAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentKind;
import io.github.zhancm.repoonboard.analyzer.spring.SpringConfigurationAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringDependencyStatus;
import io.github.zhancm.repoonboard.analyzer.spring.SpringEndpointAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringInjectionAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringInjectionStatus;
import io.github.zhancm.repoonboard.analyzer.spring.SpringMvcMappingAnalyzer;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.Diagnostic;
import io.github.zhancm.repoonboard.core.model.DiagnosticSeverity;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import io.github.zhancm.repoonboard.web.LocalUiApplication;
import io.github.zhancm.repoonboard.web.LocalUiLauncher;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
        name = "repoonboard",
        description = "Detect a Maven project at a repository path.",
        mixinStandardHelpOptions = true,
        version = "RepoOnboard 0.1.0-SNAPSHOT")
public final class RepoOnboardCommand implements Callable<Integer> {

    private final LocalUiLauncher uiLauncher;

    @Parameters(index = "0", paramLabel = "PATH", description = "Repository directory to inspect.")
    private Path target;

    @Option(names = "--profile", split = ",", description = "Explicit Maven profile IDs (repeatable).")
    private List<String> profiles = List.of();

    @Option(names = "--local-repository", description = "Local POM cache (default: ~/.m2/repository).")
    private Path localRepository;

    @Option(names = "--no-open", description = "Start the local UI without opening a browser.")
    private boolean noOpen;

    @Spec
    private CommandSpec commandSpec;

    public RepoOnboardCommand() {
        this(new LocalUiApplication());
    }

    RepoOnboardCommand(LocalUiLauncher uiLauncher) {
        this.uiLauncher = java.util.Objects.requireNonNull(uiLauncher, "uiLauncher");
    }

    public static void main(String[] args) {
        int exitCode = execute(
                args,
                new PrintWriter(System.out, true),
                new PrintWriter(System.err, true),
                new LocalUiApplication());
        System.exit(exitCode);
    }

    static int execute(String[] args, PrintWriter out, PrintWriter err) {
        return execute(args, out, err, (report, noOpen, standardOut, standardErr) -> { });
    }

    static int execute(
            String[] args,
            PrintWriter out,
            PrintWriter err,
            LocalUiLauncher uiLauncher) {
        CommandLine commandLine = new CommandLine(new RepoOnboardCommand(uiLauncher));
        commandLine.setOut(out);
        commandLine.setErr(err);
        commandLine.setParameterExceptionHandler((exception, arguments) -> {
            CliErrorReporter.printError(
                    exception.getCommandLine().getErr(),
                    "CLI_ARGUMENT_INVALID",
                    "CLI",
                    null,
                    exception.getMessage(),
                    "Run 'repoonboard --help', correct the command arguments, and try again.");
            exception.getCommandLine().usage(exception.getCommandLine().getErr());
            return CommandLine.ExitCode.USAGE;
        });
        commandLine.setExecutionExceptionHandler((exception, invokedCommand, parseResult) -> {
            CliErrorReporter.printError(
                    invokedCommand.getErr(),
                    "ANALYSIS_UNEXPECTED_FAILURE",
                    "CLI",
                    "Failure type: " + exception.getClass().getSimpleName(),
                    "Analysis stopped because of an unexpected internal error.",
                    "Retry once; if it repeats, report this diagnostic code and the RepoOnboard version.");
            return CommandLine.ExitCode.SOFTWARE;
        });
        return commandLine.execute(args);
    }

    @Override
    public Integer call() {
        Path resolvedTarget = target.toAbsolutePath().normalize();
        if (!Files.exists(resolvedTarget)) {
            CliErrorReporter.printError(
                    commandSpec.commandLine().getErr(),
                    "CLI_TARGET_NOT_FOUND",
                    "CLI",
                    "Target: " + resolvedTarget,
                    "The target path does not exist.",
                    "Choose an existing Maven Spring Boot repository directory and rerun.");
            return CommandLine.ExitCode.USAGE;
        }
        if (!Files.isDirectory(resolvedTarget)) {
            CliErrorReporter.printError(
                    commandSpec.commandLine().getErr(),
                    "CLI_TARGET_NOT_DIRECTORY",
                    "CLI",
                    "Target: " + resolvedTarget,
                    "The target path is not a directory.",
                    "Choose a Maven Spring Boot repository directory instead of a file.");
            return CommandLine.ExitCode.USAGE;
        }

        commandSpec.commandLine().getOut().println("RepoOnboard");
        commandSpec.commandLine().getOut().printf("Target: %s%n", resolvedTarget);
        MavenProjectDetection detection = new MavenProjectDetector().detect(resolvedTarget);
        if (detection.detected()) {
            commandSpec.commandLine().getOut().println("Maven project: detected (pom.xml)");
            MavenModelOptions defaults = MavenModelOptions.defaults();
            var analysis = new MavenModuleAnalyzer().analyze(resolvedTarget,
                    new MavenModelOptions(localRepository == null ? defaults.localRepository() : localRepository,
                            profiles, defaults.maximumPomBytes()));
            analysis.root().ifPresent(root -> {
                printMetadata(root.metadata());
                commandSpec.commandLine().getOut().println("Maven modules:");
                printModule(root, 0);
            });
            var sourceRoots = new JavaSourceRootDiscoverer().discover(resolvedTarget, analysis);
            var javaFiles = new JavaFileDiscoverer().discover(resolvedTarget, sourceRoots);
            var javaParse = new JavaSourceParser().parse(resolvedTarget, javaFiles);
            var declarationIndex = JavaDeclarationIndex.build(javaParse);
            var javaFacts = new JavaTypeReferenceResolver().resolve(
                    javaParse, analysis, declarationIndex);
            var springComponents = new SpringComponentAnalyzer().analyze(javaFacts);
            var springConfiguration = new SpringConfigurationAnalyzer().analyze(javaFacts);
            var springInjection = new SpringInjectionAnalyzer().analyze(
                    javaFacts, springComponents, springConfiguration);
            var springMappings = new SpringMvcMappingAnalyzer().analyze(
                    javaFacts, springComponents);
            var springEndpoints = new SpringEndpointAnalyzer().analyze(springMappings);
            var springDependencies = new SpringComponentDependencyAnalyzer().analyze(
                    springComponents, springInjection);
            commandSpec.commandLine().getOut()
                    .printf("Java source roots: %d%n", sourceRoots.sourceRoots().size());
            for (var sourceRoot : sourceRoots.sourceRoots()) {
                commandSpec.commandLine().getOut().printf("  %s (%s)%n",
                        sourceRoot.relativePath(), sourceRoot.modulePomFileId());
            }
            commandSpec.commandLine().getOut().printf("Java source files: %d%n", javaFiles.files().size());
            commandSpec.commandLine().getOut()
                    .printf("Java compilation units: %d%n", javaParse.compilationUnits().size());
            long declarationCount = javaFacts.compilationUnits().stream()
                    .mapToLong(unit -> unit.types().size())
                    .sum();
            commandSpec.commandLine().getOut().printf("Java declarations: %d%n", declarationCount);
            commandSpec.commandLine().getOut().printf(
                    "Java declaration index: %d entries (ambiguous qualified names: %d)%n",
                    declarationIndex.declarations().size(),
                    declarationIndex.ambiguousQualifiedNameCount());
            long referenceCount = javaFacts.compilationUnits().stream()
                    .mapToLong(unit -> unit.typeReferences().size())
                    .sum();
            long resolvedReferenceCount = javaFacts.compilationUnits().stream()
                    .flatMap(unit -> unit.typeReferences().stream())
                    .filter(reference -> reference.status() == JavaTypeReferenceStatus.RESOLVED)
                    .count();
            commandSpec.commandLine().getOut().printf(
                    "Java type references: %d (project-local resolved: %d)%n",
                    referenceCount,
                    resolvedReferenceCount);
            commandSpec.commandLine().getOut()
                    .printf("Spring components: %d%n", springComponents.components().size());
            for (SpringComponentKind kind : SpringComponentKind.values()) {
                long count = springComponents.components().stream()
                        .filter(component -> component.kind() == kind)
                        .count();
                if (count > 0) {
                    commandSpec.commandLine().getOut().printf("  %s: %d%n", kind, count);
                }
            }
            commandSpec.commandLine().getOut().printf(
                    "Spring configurations: %d%n", springConfiguration.configurations().size());
            commandSpec.commandLine().getOut().printf(
                    "Spring application entry points: %d%n", springConfiguration.entryPoints().size());
            long confirmedInjections = springInjection.candidates().stream()
                    .filter(candidate -> candidate.status() == SpringInjectionStatus.CONFIRMED)
                    .count();
            long ambiguousInjections = springInjection.candidates().stream()
                    .filter(candidate -> candidate.status() == SpringInjectionStatus.AMBIGUOUS_CONSTRUCTOR)
                    .count();
            commandSpec.commandLine().getOut().printf(
                    "Spring injection candidates: %d (confirmed: %d, ambiguous: %d)%n",
                    springInjection.candidates().size(), confirmedInjections, ambiguousInjections);
            commandSpec.commandLine().getOut().printf(
                    "Spring MVC mapping declarations: %d%n", springMappings.mappings().size());
            long unresolvedEndpoints = springEndpoints.endpoints().stream()
                    .filter(endpoint -> endpoint.unresolvedPath()
                            || endpoint.httpMethod() == io.github.zhancm.repoonboard.analyzer.spring.SpringHttpMethod.UNRESOLVED)
                    .count();
            commandSpec.commandLine().getOut().printf(
                    "Spring HTTP endpoints: %d (unresolved: %d)%n",
                    springEndpoints.endpoints().size(), unresolvedEndpoints);
            long confirmedDependencies = springDependencies.dependencies().stream()
                    .filter(dependency -> dependency.status() == SpringDependencyStatus.CONFIRMED)
                    .count();
            commandSpec.commandLine().getOut().printf(
                    "Spring component dependencies: %d (confirmed: %d, unresolved: %d)%n",
                    springDependencies.dependencies().size(),
                    confirmedDependencies,
                    springDependencies.dependencies().size() - confirmedDependencies);
            if (analysis.root().isPresent()) {
                List<Diagnostic> additionalDiagnostics = new ArrayList<>(
                        java.util.stream.Stream.of(
                                        sourceRoots.diagnostics(),
                                        javaFiles.diagnostics(),
                                        springInjection.diagnostics(),
                                        springMappings.diagnostics())
                                .flatMap(List::stream)
                                .distinct()
                                .toList());
                springBootSupportDiagnostic(analysis).ifPresent(additionalDiagnostics::add);
                var report = new ProjectModelAssembler().assemble(new ProjectAnalysisInput(
                        analysis,
                        javaFacts,
                        springComponents,
                        springConfiguration,
                        springEndpoints,
                        springDependencies,
                        additionalDiagnostics));
                AnalysisStatus status = report.status();
                commandSpec.commandLine().getOut().printf("Analysis status: %s%n", status);
                CliErrorReporter.printAnalysisSummary(commandSpec.commandLine().getErr(), status);
                CliErrorReporter.printDiagnostics(commandSpec.commandLine().getErr(), report.diagnostics());
                try {
                    uiLauncher.launch(
                            report,
                            noOpen,
                            commandSpec.commandLine().getOut(),
                            commandSpec.commandLine().getErr());
                } catch (IOException exception) {
                    CliErrorReporter.printError(
                            commandSpec.commandLine().getErr(),
                            "LOCAL_UI_START_FAILED",
                            "LOCAL_UI",
                            "Loopback host: 127.0.0.1",
                            "The local read-only UI could not be started.",
                            "Check local loopback networking and retry; use --no-open only to disable "
                                    + "browser opening.");
                    return CommandLine.ExitCode.SOFTWARE;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    CliErrorReporter.printError(
                            commandSpec.commandLine().getErr(),
                            "LOCAL_UI_INTERRUPTED",
                            "LOCAL_UI",
                            null,
                            "The local UI was interrupted before shutdown completed.",
                            "Rerun RepoOnboard when the interruption has cleared.");
                    return CommandLine.ExitCode.SOFTWARE;
                }
                return exitCode(status);
            }
            AnalysisStatus status = AnalysisStatus.FAILED;
            commandSpec.commandLine().getOut().printf("Analysis status: %s%n", status);
            CliErrorReporter.printAnalysisSummary(commandSpec.commandLine().getErr(), status);
            CliErrorReporter.printDiagnostics(commandSpec.commandLine().getErr(), analysis.diagnostics());
            commandSpec.commandLine().getErr().println(
                    "Action: no UI was started because a report could not be assembled; "
                            + "fix the Maven errors and rerun.");
            return CommandLine.ExitCode.SOFTWARE;
        } else {
            commandSpec.commandLine().getOut()
                    .println("Maven project: not detected (root pom.xml not found)");
            CliErrorReporter.printError(
                    commandSpec.commandLine().getErr(),
                    "PROJECT_NOT_MAVEN",
                    "PROJECT_DETECTION",
                    "Target: " + resolvedTarget,
                    "No root pom.xml was found; this is not a supported Maven project root.",
                    "Run RepoOnboard from the root of a Maven Spring Boot repository.");
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

    private void printMetadata(MavenProjectMetadata metadata) {
        commandSpec.commandLine().getOut().println("Maven metadata:");
        commandSpec.commandLine().getOut().printf("  groupId: %s%n", display(metadata.groupId()));
        commandSpec.commandLine().getOut()
                .printf("  artifactId: %s%n", display(metadata.artifactId()));
        commandSpec.commandLine().getOut().printf("  version: %s%n", display(metadata.version()));
        commandSpec.commandLine().getOut()
                .printf("  packaging: %s%n", display(metadata.packaging()));
        commandSpec.commandLine().getOut().printf("Metadata status: %s%n", metadata.status());
        if (!metadata.activeProfileIds().isEmpty()) {
            commandSpec.commandLine().getOut().printf("Active profiles: %s%n", metadata.activeProfileIds());
        }
    }

    private void printModule(MavenModule module, int depth) {
        commandSpec.commandLine().getOut().printf("%s%s [%s] source: %s%n", "  ".repeat(depth),
                module.baseDirectory(), display(module.metadata().artifactId()), display(module.sourceDirectory()));
        for (var dependency : module.dependencies()) {
            commandSpec.commandLine().getOut().printf("%sdependency: %s:%s:%s [%s]%n",
                    "  ".repeat(depth + 1), display(dependency.groupId()), display(dependency.artifactId()),
                    display(dependency.version()), display(dependency.scope()));
        }
        if (module.springBoot().detected()) {
            commandSpec.commandLine().getOut().printf("%sSpring Boot build: detected; version: %s%n",
                    "  ".repeat(depth + 1), module.springBoot().version().orElse("<unknown or conflicting>"));
            for (var signal : module.springBoot().evidence()) {
                commandSpec.commandLine().getOut().printf("%s%s %s (%s)%n", "  ".repeat(depth + 2),
                        signal.kind(), signal.artifactId(), signal.origin().sourceFileId());
            }
        }
        module.children().forEach(child -> printModule(child, depth + 1));
    }

    private static String display(MavenMetadataValue value) {
        return value.resolvedValue().orElseGet(
                () -> value.rawValue().map(raw -> raw + " (unresolved)").orElse("<unknown>"));
    }

    private static Optional<Diagnostic> springBootSupportDiagnostic(MavenModuleAnalysis analysis) {
        MavenModule root = analysis.root().orElseThrow();
        if (containsSpringBoot(root)) {
            return Optional.empty();
        }
        boolean mavenComplete = analysis.status() == AnalysisStatus.SUCCESS;
        String message = mavenComplete
                ? "No Spring Boot build evidence was found in the Maven modules. RepoOnboard V0.1 "
                        + "supports Spring Boot Maven repositories; choose the repository root or add a "
                        + "recognized Spring Boot parent, BOM, or dependency, then rerun."
                : "Spring Boot build evidence could not be confirmed while Maven model analysis is "
                        + "incomplete. Restore missing parent or BOM files in the local Maven cache, then rerun.";
        SourceLocation location = SourceLocation.file(root.pomFileId());
        return Optional.of(new Diagnostic(
                "SPRING_BOOT_NOT_DETECTED",
                mavenComplete ? DiagnosticSeverity.ERROR : DiagnosticSeverity.WARNING,
                "SPRING_BOOT_DETECTION",
                Optional.empty(),
                Optional.of(root.pomFileId()),
                Optional.of(location),
                message));
    }

    private static boolean containsSpringBoot(MavenModule module) {
        return module.springBoot().detected()
                || module.children().stream().anyMatch(RepoOnboardCommand::containsSpringBoot);
    }

    private static int exitCode(AnalysisStatus status) {
        return switch (status) {
            case SUCCESS -> CommandLine.ExitCode.OK;
            case PARTIAL -> 3;
            case FAILED -> CommandLine.ExitCode.SOFTWARE;
        };
    }
}
