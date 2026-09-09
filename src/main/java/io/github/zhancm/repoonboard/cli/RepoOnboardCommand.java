package io.github.zhancm.repoonboard.cli;

import io.github.zhancm.repoonboard.analyzer.java.JavaFileDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaDeclarationIndex;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceParser;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceResolver;
import io.github.zhancm.repoonboard.analyzer.java.JavaTypeReferenceStatus;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceRootDiscoverer;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectDetection;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectDetector;
import io.github.zhancm.repoonboard.analyzer.maven.MavenMetadataValue;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectMetadata;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModule;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringComponentKind;
import io.github.zhancm.repoonboard.analyzer.spring.SpringConfigurationAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringInjectionAnalyzer;
import io.github.zhancm.repoonboard.analyzer.spring.SpringInjectionStatus;
import java.util.List;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(
        name = "repoonboard",
        description = "Detect a Maven project at a repository path.",
        mixinStandardHelpOptions = true,
        version = "RepoOnboard 0.1.0-SNAPSHOT")
public final class RepoOnboardCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "PATH", description = "Repository directory to inspect.")
    private Path target;

    @Option(names = "--profile", split = ",", description = "Explicit Maven profile IDs (repeatable).")
    private List<String> profiles = List.of();

    @Option(names = "--local-repository", description = "Local POM cache (default: ~/.m2/repository).")
    private Path localRepository;

    @Spec
    private CommandSpec commandSpec;

    public static void main(String[] args) {
        int exitCode = execute(
                args,
                new PrintWriter(System.out, true),
                new PrintWriter(System.err, true));
        System.exit(exitCode);
    }

    static int execute(String[] args, PrintWriter out, PrintWriter err) {
        CommandLine commandLine = new CommandLine(new RepoOnboardCommand());
        commandLine.setOut(out);
        commandLine.setErr(err);
        return commandLine.execute(args);
    }

    @Override
    public Integer call() {
        Path resolvedTarget = target.toAbsolutePath().normalize();
        if (!Files.exists(resolvedTarget)) {
            commandSpec.commandLine().getErr()
                    .printf("Error: target path does not exist: %s%n", resolvedTarget);
            return CommandLine.ExitCode.USAGE;
        }
        if (!Files.isDirectory(resolvedTarget)) {
            commandSpec.commandLine().getErr()
                    .printf("Error: target path is not a directory: %s%n", resolvedTarget);
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
            AnalysisStatus status = combine(
                    analysis.status(), sourceRoots.status(), javaFiles.status(), javaFacts.status(),
                    springComponents.status(), springConfiguration.status(), springInjection.status());
            commandSpec.commandLine().getOut().printf("Analysis status: %s%n", status);
            for (var diagnostic : analysis.diagnostics()) {
                commandSpec.commandLine().getErr().printf("%s [%s]: %s%n",
                        diagnostic.fileId().orElse("pom.xml"), diagnostic.code(), diagnostic.message());
            }
            for (var diagnostic : sourceRoots.diagnostics()) {
                commandSpec.commandLine().getErr().printf("%s [%s]: %s%n",
                        diagnostic.fileId().orElse("pom.xml"), diagnostic.code(), diagnostic.message());
            }
            for (var diagnostic : javaFiles.diagnostics()) {
                commandSpec.commandLine().getErr().printf("%s [%s]: %s%n",
                        diagnostic.fileId().orElse("pom.xml"), diagnostic.code(), diagnostic.message());
            }
            for (var diagnostic : javaFacts.diagnostics()) {
                commandSpec.commandLine().getErr().printf("%s [%s]: %s%n",
                        diagnostic.fileId().orElse("pom.xml"), diagnostic.code(), diagnostic.message());
            }
            for (var diagnostic : springComponents.diagnostics()) {
                commandSpec.commandLine().getErr().printf("%s [%s]: %s%n",
                        diagnostic.fileId().orElse("pom.xml"), diagnostic.code(), diagnostic.message());
            }
            for (var diagnostic : springConfiguration.diagnostics()) {
                commandSpec.commandLine().getErr().printf("%s [%s]: %s%n",
                        diagnostic.fileId().orElse("pom.xml"), diagnostic.code(), diagnostic.message());
            }
            for (var diagnostic : springInjection.diagnostics()) {
                commandSpec.commandLine().getErr().printf("%s [%s]: %s%n",
                        diagnostic.fileId().orElse("pom.xml"), diagnostic.code(), diagnostic.message());
            }
            return switch (status) {
                case SUCCESS -> CommandLine.ExitCode.OK;
                case PARTIAL -> 3;
                case FAILED -> CommandLine.ExitCode.SOFTWARE;
            };
        } else {
            commandSpec.commandLine().getOut()
                    .println("Maven project: not detected (root pom.xml not found)");
        }
        return CommandLine.ExitCode.OK;
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

    private static AnalysisStatus combine(AnalysisStatus... statuses) {
        AnalysisStatus result = AnalysisStatus.SUCCESS;
        for (AnalysisStatus status : statuses) {
            if (status == AnalysisStatus.FAILED) {
                return AnalysisStatus.FAILED;
            }
            if (status == AnalysisStatus.PARTIAL) {
                result = AnalysisStatus.PARTIAL;
            }
        }
        return result;
    }
}
