package io.github.zhancm.repoonboard.cli;

import io.github.zhancm.repoonboard.analyzer.java.JavaFileDiscoverer;
import io.github.zhancm.repoonboard.analyzer.java.JavaSourceRootDiscoverer;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectDetection;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectDetector;
import io.github.zhancm.repoonboard.analyzer.maven.MavenMetadataValue;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectMetadata;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModuleAnalyzer;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModule;
import io.github.zhancm.repoonboard.analyzer.maven.MavenModelOptions;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
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
            commandSpec.commandLine().getOut()
                    .printf("Java source roots: %d%n", sourceRoots.sourceRoots().size());
            for (var sourceRoot : sourceRoots.sourceRoots()) {
                commandSpec.commandLine().getOut().printf("  %s (%s)%n",
                        sourceRoot.relativePath(), sourceRoot.modulePomFileId());
            }
            commandSpec.commandLine().getOut().printf("Java source files: %d%n", javaFiles.files().size());
            AnalysisStatus status = combine(
                    analysis.status(), sourceRoots.status(), javaFiles.status());
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
