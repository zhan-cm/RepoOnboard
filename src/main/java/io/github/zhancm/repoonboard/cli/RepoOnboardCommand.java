package io.github.zhancm.repoonboard.cli;

import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectDetection;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectDetector;
import io.github.zhancm.repoonboard.analyzer.maven.MavenMetadataValue;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectMetadata;
import io.github.zhancm.repoonboard.analyzer.maven.MavenProjectMetadataReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
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
            printMetadata(new MavenProjectMetadataReader().read(resolvedTarget));
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
    }

    private static String display(MavenMetadataValue value) {
        return value.resolvedValue().orElseGet(
                () -> value.rawValue().map(raw -> raw + " (unresolved)").orElse("<unknown>"));
    }
}
