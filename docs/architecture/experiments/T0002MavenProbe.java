import java.io.StringReader;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.building.StringModelSource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

/** Review-only API experiment. Not a product analyzer or security implementation. */
public class T0002MavenProbe {
    private static int passed;

    public static void main(String[] args) throws Exception {
        String coordinates = "<groupId>probe</groupId><artifactId>child</artifactId><version>1</version>";
        String plain = pom(coordinates + "<properties><release>17</release></properties>"
                + "<name>${release}</name>");
        Model plainModel = build(plain, true, new RejectingResolver()).getEffectiveModel();
        check("local interpolation", "17".equals(plainModel.getName()));

        String missing = pom("<parent><groupId>probe</groupId><artifactId>missing</artifactId>"
                + "<version>1</version><relativePath/></parent><artifactId>child</artifactId>");
        RejectingResolver resolver = new RejectingResolver();
        boolean missingFailed = false;
        try {
            build(missing, true, resolver);
        } catch (ModelBuildingException expected) {
            missingFailed = !expected.getProblems().isEmpty();
        }
        Model raw = new MavenXpp3Reader().read(new StringReader(missing));
        check("missing parent has diagnostics and independently readable raw model",
                missingFailed && resolver.calls > 0 && "child".equals(raw.getArtifactId()));

        String osName = System.getProperty("os.name");
        String profile = pom(coordinates + "<profiles>"
                + "<profile><id>baseline</id><activation><activeByDefault>true</activeByDefault></activation>"
                + "<properties><chosen>baseline</chosen></properties></profile>"
                + "<profile><id>host</id><activation><os><name>" + osName + "</name></os></activation>"
                + "<properties><chosen>host</chosen></properties></profile></profiles>");
        check("empty request properties do not isolate host OS activation",
                "host".equals(build(profile, false, new RejectingResolver())
                        .getEffectiveModel().getProperties().getProperty("chosen")));
        check("selector without implicit activators retains activeByDefault",
                "baseline".equals(build(profile, true, new RejectingResolver())
                        .getEffectiveModel().getProperties().getProperty("chosen")));

        DefaultModelBuildingRequest explicit = request(profile, new RejectingResolver());
        explicit.setActiveProfileIds(List.of("host"));
        check("explicit profile still activates without implicit activators",
                "host".equals(builder(true).build(explicit).getEffectiveModel()
                        .getProperties().getProperty("chosen")));

        RejectingResolver fileResolver = new RejectingResolver();
        DefaultModelBuildingRequest fileRequest = request(plain, fileResolver);
        fileRequest.setModelSource(null);
        fileRequest.setPomFile(Path.of(args[0], "child", "pom.xml").toFile());
        Model escaped = builder(true).build(fileRequest).getEffectiveModel();
        check("relative parent lookup bypasses ModelResolver",
                "outside-child".equals(escaped.getProperties().getProperty("origin"))
                        && fileResolver.calls == 0);

        Path childRoot = Path.of(args[0], "child").toRealPath();
        RejectingResolver guardedResolver = new RejectingResolver();
        DefaultModelBuildingRequest guarded = request(plain, guardedResolver);
        guarded.setModelSource(new BoundedSource(childRoot.resolve("pom.xml"), childRoot));
        boolean denied = false;
        try {
            builder(true).build(guarded);
        } catch (ModelBuildingException expected) {
            denied = !expected.getProblems().isEmpty();
        }
        check("bounded ModelSource2 denies parent outside scan root",
                denied && guardedResolver.calls > 0);

        String dtd = "<!DOCTYPE project [<!ENTITY marker 'expanded'>]>"
                + pom(coordinates + "<name>&marker;</name>");
        check("XML preflight accepts ordinary POM and rejects DTD",
                xmlAllowed(plain) && !xmlAllowed(dtd));
        System.out.println("PASS: " + passed + " review assertions; no Maven lifecycle or network resolver used.");
    }

    private static String pom(String body) {
        return "<project><modelVersion>4.0.0</modelVersion>" + body + "</project>";
    }

    private static DefaultModelBuildingRequest request(String xml, ModelResolver resolver) {
        return new DefaultModelBuildingRequest()
                .setModelSource(new StringModelSource(xml, "review-memory-pom"))
                .setModelResolver(resolver)
                .setSystemProperties(new Properties())
                .setUserProperties(new Properties())
                .setProcessPlugins(false)
                .setLocationTracking(true);
    }

    private static DefaultModelBuilder builder(boolean restrictedProfiles) {
        DefaultModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        if (restrictedProfiles) {
            builder.setProfileSelector(new DefaultProfileSelector());
        }
        return builder;
    }

    private static ModelBuildingResult build(String xml, boolean restricted, ModelResolver resolver)
            throws ModelBuildingException {
        return builder(restricted).build(request(xml, resolver));
    }

    private static boolean xmlAllowed(String xml) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
            throw new javax.xml.stream.XMLStreamException("External XML access forbidden");
        });
        var reader = factory.createXMLStreamReader(new StringReader(xml));
        try {
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.DTD) {
                    return false;
                }
            }
            return true;
        } finally {
            reader.close();
        }
    }

    private static void check(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        passed++;
        System.out.println("PASS: " + name);
    }

    private record BoundedSource(Path file, Path root) implements ModelSource2 {
        public InputStream getInputStream() throws IOException {
            Path real = file.toRealPath();
            if (!real.startsWith(root)) throw new IOException("Outside review scan root");
            return Files.newInputStream(real);
        }
        public String getLocation() { return file.toString(); }
        public URI getLocationURI() { return file.toUri(); }
        public ModelSource2 getRelatedSource(String relative) {
            try {
                Path candidate = file.getParent().resolve(relative).toRealPath();
                if (Files.isDirectory(candidate)) candidate = candidate.resolve("pom.xml").toRealPath();
                return candidate.startsWith(root) ? new BoundedSource(candidate, root) : null;
            } catch (IOException invalidPath) {
                return null;
            }
        }
    }

    private static class RejectingResolver implements ModelResolver {
        int calls;

        public ModelSource resolveModel(String groupId, String artifactId, String version)
                throws UnresolvableModelException {
            calls++;
            throw new UnresolvableModelException("Intentionally unavailable in offline probe",
                    groupId, artifactId, version);
        }

        public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
            return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        }

        public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
            return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        }

        public void addRepository(Repository repository) { }
        public void addRepository(Repository repository, boolean replace) { }
        public ModelResolver newCopy() { return this; }
    }
}
