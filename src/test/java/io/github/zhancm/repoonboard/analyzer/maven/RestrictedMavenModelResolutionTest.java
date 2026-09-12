package io.github.zhancm.repoonboard.analyzer.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RestrictedMavenModelResolutionTest {

    private final MavenProjectMetadataReader reader = new MavenProjectMetadataReader();

    @Test
    void resolvesProjectAndVersionProperties(@TempDir Path temporaryDirectory) throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path localRepository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        writePom(project, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>property-project</artifactId>
                  <version>${revision}</version>
                  <properties>
                    <revision>1.2.3</revision>
                    <java.version>21</java.version>
                    <boot.base>3.5.5</boot.base>
                    <spring-boot.version>${boot.base}</spring-boot.version>
                  </properties>
                </project>
                """);

        MavenProjectMetadata metadata = reader.read(project, options(localRepository));

        assertResolved(metadata.version(), "${revision}", "1.2.3");
        assertResolved(metadata.properties().get("java.version"), "21", "21");
        assertResolved(
                metadata.properties().get("spring-boot.version"), "${boot.base}", "3.5.5");
        assertEquals(AnalysisStatus.SUCCESS, metadata.status());
    }

    @Test
    void ignoresBlankPropertiesButUsesThemDuringInterpolation(
            @TempDir Path temporaryDirectory) throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path localRepository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        writePom(project, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>blank-property-project</artifactId>
                  <version>1</version>
                  <properties>
                    <profile.flag/>
                    <spring.profiles.active>dev${profile.flag}</spring.profiles.active>
                  </properties>
                </project>
                """);

        MavenProjectMetadata metadata = reader.read(project, options(localRepository));

        assertFalse(metadata.properties().containsKey("profile.flag"));
        assertResolved(
                metadata.properties().get("spring.profiles.active"),
                "dev${profile.flag}",
                "dev");
        assertFalse(hasDiagnostic(metadata, "MAVEN_PROPERTY_UNRESOLVED"));
        assertEquals(AnalysisStatus.SUCCESS, metadata.status());
    }

    @Test
    void resolvesRelativeParentInsideScanRoot(@TempDir Path temporaryDirectory) throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path parent = Files.createDirectory(project.resolve("parent"));
        Path localRepository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        writePom(parent, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example.parent</groupId>
                  <artifactId>base</artifactId>
                  <version>2.0.0</version>
                  <packaging>pom</packaging>
                  <properties><java.version>17</java.version></properties>
                </project>
                """);
        writePom(project, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>example.parent</groupId>
                    <artifactId>base</artifactId>
                    <version>2.0.0</version>
                    <relativePath>parent/pom.xml</relativePath>
                  </parent>
                  <artifactId>child</artifactId>
                </project>
                """);

        MavenProjectMetadata metadata = reader.read(project, options(localRepository));

        assertEquals("example.parent", metadata.groupId().resolvedValue().orElseThrow());
        assertEquals("2.0.0", metadata.version().resolvedValue().orElseThrow());
        assertEquals("parent/pom.xml", metadata.groupId().origin().sourceFileId());
        assertEquals("17", metadata.properties().get("java.version").resolvedValue().orElseThrow());
        assertEquals(List.of("pom.xml", "parent/pom.xml"), metadata.sourcePomIds());
        assertEquals(AnalysisStatus.SUCCESS, metadata.status());
    }

    @Test
    void resolvesParentAndBomOnlyFromValidatedLocalRepository(
            @TempDir Path temporaryDirectory) throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path localRepository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        installLocalPom(localRepository, "com.acme", "base", "1", """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.acme</groupId>
                  <artifactId>base</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <properties><java.version>21</java.version></properties>
                </project>
                """);
        installLocalPom(localRepository, "com.acme", "platform", "1", """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.acme</groupId>
                  <artifactId>platform</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <properties><bom.only>not-inherited</bom.only></properties>
                  <dependencyManagement><dependencies/></dependencyManagement>
                </project>
                """);
        writePom(project, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>com.acme</groupId>
                    <artifactId>base</artifactId>
                    <version>1</version>
                    <relativePath/>
                  </parent>
                  <artifactId>app</artifactId>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.acme</groupId>
                        <artifactId>platform</artifactId>
                        <version>1</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """);

        MavenProjectMetadata metadata = reader.read(project, options(localRepository));

        assertEquals("com.acme", metadata.groupId().resolvedValue().orElseThrow());
        assertEquals("21", metadata.properties().get("java.version").resolvedValue().orElseThrow());
        assertTrue(metadata.sourcePomIds().contains("local-repository/com/acme/base/1/base-1.pom"));
        assertTrue(metadata.sourcePomIds().contains(
                "local-repository/com/acme/platform/1/platform-1.pom"));
        assertFalse(metadata.properties().containsKey("bom.only"));
        assertEquals(AnalysisStatus.SUCCESS, metadata.status());
    }

    @Test
    void incompleteModelUsesOnlySelectedRootProperties(@TempDir Path temporaryDirectory)
            throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path repository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        writePom(project, """
                <project><modelVersion>4.0.0</modelVersion>
                  <parent><groupId>missing</groupId><artifactId>base</artifactId>
                    <version>1</version><relativePath/></parent><artifactId>child</artifactId>
                  <profiles>
                    <profile><id>default</id><activation><activeByDefault>true</activeByDefault></activation>
                      <properties><default.only>no</default.only><chosen>default</chosen></properties></profile>
                    <profile><id>manual</id><properties><chosen>manual</chosen></properties></profile>
                  </profiles>
                </project>
                """);
        MavenProjectMetadata metadata = reader.read(project, new MavenModelOptions(
                repository, List.of("manual"), MavenModelOptions.DEFAULT_MAXIMUM_POM_BYTES));
        assertEquals(AnalysisStatus.PARTIAL, metadata.status());
        assertEquals(List.of("manual"), metadata.activeProfileIds());
        assertEquals("manual", metadata.property("chosen").orElseThrow().resolvedValue().orElseThrow());
        assertFalse(metadata.properties().containsKey("default.only"));
    }

    @Test
    void rejectedRelativeParentDoesNotContributeProperties(@TempDir Path temporaryDirectory)
            throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path parent = Files.createDirectory(project.resolve("parent"));
        Path repository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        writePom(parent, """
                <project><modelVersion>4.0.0</modelVersion><groupId>wrong</groupId>
                <artifactId>base</artifactId><version>1</version><packaging>pom</packaging>
                <properties><unrelated>must-not-leak</unrelated></properties></project>
                """);
        writePom(project, """
                <project><modelVersion>4.0.0</modelVersion>
                <parent><groupId>expected</groupId><artifactId>base</artifactId><version>1</version>
                <relativePath>parent/pom.xml</relativePath></parent><artifactId>child</artifactId></project>
                """);
        MavenProjectMetadata metadata = reader.read(project, options(repository));
        assertEquals(AnalysisStatus.PARTIAL, metadata.status());
        assertFalse(metadata.properties().containsKey("unrelated"));
    }

    @Test
    void rejectsExternalEntitiesForEveryPomSource(@TempDir Path temporaryDirectory)
            throws Exception {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path repository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        String hostile = """
                <!DOCTYPE project SYSTEM "http://127.0.0.1:9/never-fetch.dtd">
                <project><modelVersion>4.0.0</modelVersion><groupId>hostile</groupId>
                <artifactId>base</artifactId><version>1</version><packaging>pom</packaging></project>
                """;
        installLocalPom(repository, "hostile", "base", "1", hostile);
        writePom(Files.createDirectory(project.resolve("parent")), hostile);
        writePom(project, """
                <project><modelVersion>4.0.0</modelVersion><groupId>app</groupId>
                <artifactId>app</artifactId><version>1</version></project>
                """);
        RestrictedPomRepository sources = new RestrictedPomRepository(project, options(repository));
        RestrictedPomSource root = sources.rootSource();
        assertTrue(sources.relatedSource(root, "parent/pom.xml").isEmpty());
        assertTrue(sources.localRepositorySource("hostile", "base", "1").isEmpty());
        assertTrue(sources.problems().stream().allMatch(p -> p.code().equals("MAVEN_POM_DTD_FORBIDDEN")));
        assertEquals(2, sources.problems().size());
    }

    @Test
    void rejectsDeepXmlAndUnsafeCoordinates(@TempDir Path temporaryDirectory) throws Exception {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path repository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        writePom(project, "<project>" + "<x>".repeat(129) + "</x>".repeat(129) + "</project>");
        assertTrue(hasDiagnostic(reader.read(project, options(repository)), "MAVEN_POM_XML_DEPTH_LIMIT"));
        RestrictedPomRepository sources = new RestrictedPomRepository(project, options(repository));
        assertTrue(sources.localRepositorySource("../outside", "base", "1").isEmpty());
        assertEquals("MAVEN_MODEL_COORDINATES_REJECTED", sources.problems().getFirst().code());
    }

    @Test
    void parentAndChildProfilesWithSameIdKeepCorrectEvidence(@TempDir Path temporaryDirectory)
            throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path repository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        writePom(Files.createDirectory(project.resolve("parent")), """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                <artifactId>base</artifactId><version>1</version><packaging>pom</packaging>
                <profiles><profile><id>shared</id><activation><activeByDefault>true</activeByDefault></activation>
                <properties><parent.only>yes</parent.only></properties></profile></profiles></project>
                """);
        writePom(project, """
                <project><modelVersion>4.0.0</modelVersion>
                <parent><groupId>example</groupId><artifactId>base</artifactId><version>1</version>
                <relativePath>parent/pom.xml</relativePath></parent><artifactId>child</artifactId>
                <profiles><profile><id>shared</id><properties><child.inactive>no</child.inactive></properties>
                </profile></profiles></project>
                """);
        MavenProjectMetadata metadata = reader.read(project, options(repository));
        assertFalse(metadata.properties().containsKey("child.inactive"));
        assertEquals("yes", metadata.property("parent.only").orElseThrow().rawValue().orElseThrow());
        assertEquals("parent/pom.xml", metadata.property("parent.only").orElseThrow().origin().sourceFileId());
    }

    @Test
    void rejectsRelativeParentOutsideScanRoot(@TempDir Path temporaryDirectory) throws IOException {
        Path parent = Files.createDirectory(temporaryDirectory.resolve("outside-parent"));
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path localRepository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        writePom(parent, """
                <project><modelVersion>4.0.0</modelVersion><groupId>outside</groupId>
                  <artifactId>parent</artifactId><version>1</version></project>
                """);
        writePom(project, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent><groupId>outside</groupId><artifactId>parent</artifactId><version>1</version>
                    <relativePath>../outside-parent/pom.xml</relativePath></parent>
                  <artifactId>child</artifactId>
                </project>
                """);

        MavenProjectMetadata metadata = reader.read(project, options(localRepository));

        assertEquals("child", metadata.artifactId().resolvedValue().orElseThrow());
        assertEquals(AnalysisStatus.PARTIAL, metadata.status());
        assertTrue(hasDiagnostic(metadata, "MAVEN_POM_SOURCE_OUTSIDE_ALLOWED_ROOT"));
        assertFalse(metadata.sourcePomIds().stream().anyMatch(id -> id.contains("outside-parent")));
    }

    @Test
    void missingBomKeepsRawFactsAndReturnsPartial(@TempDir Path temporaryDirectory)
            throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path localRepository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        writePom(project, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId><artifactId>app</artifactId><version>1</version>
                  <properties><java.version>21</java.version></properties>
                  <dependencyManagement><dependencies><dependency>
                    <groupId>missing</groupId><artifactId>platform</artifactId><version>1</version>
                    <type>pom</type><scope>import</scope>
                  </dependency></dependencies></dependencyManagement>
                </project>
                """);

        MavenProjectMetadata metadata = reader.read(project, options(localRepository));

        assertEquals("example", metadata.groupId().rawValue().orElseThrow());
        assertEquals("21", metadata.properties().get("java.version").resolvedValue().orElseThrow());
        assertEquals(AnalysisStatus.PARTIAL, metadata.status());
        assertTrue(hasDiagnostic(metadata, "MAVEN_MODEL_PROBLEM"));
    }

    @Test
    void controlsExplicitDefaultAndHostProfiles(@TempDir Path temporaryDirectory)
            throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path localRepository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        String hostOs = System.getProperty("os.name");
        writePom(project, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId><artifactId>profiles</artifactId><version>1</version>
                  <profiles>
                    <profile><id>baseline</id><activation><activeByDefault>true</activeByDefault></activation>
                      <properties><chosen>baseline</chosen></properties></profile>
                    <profile><id>manual</id><properties><chosen>manual</chosen></properties></profile>
                    <profile><id>host</id><activation><os><name>%s</name></os></activation>
                      <properties><host.marker>must-not-activate</host.marker></properties></profile>
                  </profiles>
                </project>
                """.formatted(hostOs));

        MavenProjectMetadata defaults = reader.read(project, options(localRepository));
        MavenProjectMetadata explicit = reader.read(project, new MavenModelOptions(
                localRepository, List.of("manual"), MavenModelOptions.DEFAULT_MAXIMUM_POM_BYTES));

        assertEquals("baseline", defaults.properties().get("chosen").resolvedValue().orElseThrow());
        assertEquals(List.of("baseline"), defaults.activeProfileIds());
        assertFalse(defaults.properties().containsKey("host.marker"));
        assertTrue(hasDiagnostic(defaults, "MAVEN_PROFILE_ACTIVATION_IGNORED"));
        assertEquals("manual", explicit.properties().get("chosen").resolvedValue().orElseThrow());
        assertEquals(List.of("manual"), explicit.activeProfileIds());
        assertFalse(explicit.properties().containsKey("host.marker"));
    }

    @Test
    void rejectsDtdAndOversizedRootPom(@TempDir Path temporaryDirectory) throws IOException {
        Path dtdProject = Files.createDirectory(temporaryDirectory.resolve("dtd"));
        Path largeProject = Files.createDirectory(temporaryDirectory.resolve("large"));
        Path localRepository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        writePom(dtdProject, """
                <!DOCTYPE project [<!ENTITY marker "expanded">]>
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                  <artifactId>&marker;</artifactId><version>1</version></project>
                """);
        writePom(largeProject, """
                <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                  <artifactId>larger-than-limit</artifactId><version>1</version></project>
                """);

        MavenProjectMetadata dtd = reader.read(dtdProject, options(localRepository));
        MavenProjectMetadata large = reader.read(largeProject, new MavenModelOptions(
                localRepository, List.of(), 64));

        assertEquals(AnalysisStatus.FAILED, dtd.status());
        assertEquals("MAVEN_POM_DTD_FORBIDDEN", dtd.diagnostics().get(0).code());
        assertEquals(AnalysisStatus.FAILED, large.status());
        assertEquals("MAVEN_POM_TOO_LARGE", large.diagnostics().get(0).code());
    }

    @Test
    void usesLockedMavenModelBuilderVersion() {
        assertEquals(
                "3.9.16",
                org.apache.maven.model.building.DefaultModelBuilderFactory.class
                        .getPackage()
                        .getImplementationVersion());
    }

    @Test
    void declaredRemoteRepositoryAndExternalDtdNeverReceiveRequests(@TempDir Path directory)
            throws Exception {
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        var server = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort();
            Path project = Files.createDirectory(directory.resolve("project"));
            Path repository = Files.createDirectory(directory.resolve("repository"));
            writePom(project, """
                    <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
                    <artifactId>offline</artifactId><version>1</version>
                    <repositories><repository><id>remote</id><url>%s</url></repository></repositories>
                    <dependencyManagement><dependencies><dependency><groupId>missing</groupId>
                    <artifactId>bom</artifactId><version>1</version><type>pom</type><scope>import</scope>
                    </dependency></dependencies></dependencyManagement></project>
                    """.formatted(url));
            assertEquals(AnalysisStatus.PARTIAL, reader.read(project, options(repository)).status());
            writePom(project, "<!DOCTYPE project SYSTEM \"" + url + "/external.dtd\"><project/>");
            assertEquals(AnalysisStatus.FAILED, reader.read(project, options(repository)).status());
            assertEquals(0, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void boundsEverySourceAndTotalSourceCount(@TempDir Path directory) throws Exception {
        Path project = Files.createDirectory(directory.resolve("project"));
        Path repository = Files.createDirectory(directory.resolve("repository"));
        String plain = "<project><modelVersion>4.0.0</modelVersion></project>";
        writePom(project, plain);
        installLocalPom(repository, "example", "large", "1", plain + " ".repeat(300));
        writePom(Files.createDirectory(project.resolve("large")), plain + " ".repeat(300));
        RestrictedPomRepository sources = new RestrictedPomRepository(project,
                new MavenModelOptions(repository, List.of(), 200));
        RestrictedPomSource root = sources.rootSource();
        assertTrue(sources.relatedSource(root, "large/pom.xml").isEmpty());
        assertTrue(sources.localRepositorySource("example", "large", "1").isEmpty());
        assertTrue(sources.problems().stream().allMatch(p -> p.code().equals("MAVEN_POM_TOO_LARGE")));
        for (int i = 0; i < 64; i++) {
            Path parent = Files.createDirectory(project.resolve("p" + i));
            writePom(parent, plain);
            sources.relatedSource(root, "p" + i + "/pom.xml");
        }
        assertEquals(64, sources.sources().size());
        assertTrue(sources.problems().stream().anyMatch(p -> p.code().equals("MAVEN_POM_SOURCE_LIMIT")));
    }

    private static MavenModelOptions options(Path localRepository) {
        return new MavenModelOptions(
                localRepository, List.of(), MavenModelOptions.DEFAULT_MAXIMUM_POM_BYTES);
    }

    private static void installLocalPom(
            Path repository,
            String groupId,
            String artifactId,
            String version,
            String contents) throws IOException {
        Path directory = repository
                .resolve(groupId.replace('.', '/'))
                .resolve(artifactId)
                .resolve(version);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(artifactId + "-" + version + ".pom"), contents);
    }

    private static void writePom(Path project, String contents) throws IOException {
        Files.writeString(project.resolve("pom.xml"), contents);
    }

    private static boolean hasDiagnostic(MavenProjectMetadata metadata, String code) {
        return metadata.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(code));
    }

    private static void assertResolved(
            MavenMetadataValue value, String raw, String resolved) {
        assertEquals(MavenResolutionStatus.RESOLVED, value.resolutionStatus());
        assertEquals(raw, value.rawValue().orElseThrow());
        assertEquals(resolved, value.resolvedValue().orElseThrow());
    }
}
