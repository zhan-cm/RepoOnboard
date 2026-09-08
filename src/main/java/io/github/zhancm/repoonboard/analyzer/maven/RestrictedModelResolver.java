package io.github.zhancm.repoonboard.analyzer.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

/** Resolves exact parent and BOM POMs from the configured local repository only. */
final class RestrictedModelResolver implements ModelResolver {

    private final RestrictedPomRepository repository;

    RestrictedModelResolver(RestrictedPomRepository repository) {
        this.repository = repository;
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        return repository.localRepositorySource(groupId, artifactId, version)
                .orElseThrow(() -> new UnresolvableModelException(
                        "The model is unavailable in the configured local repository; network resolution is disabled.",
                        groupId,
                        artifactId,
                        version));
    }

    @Override
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
        return resolveModel(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion());
    }

    @Override
    public void addRepository(Repository repository) {
        // Remote repositories declared by the analyzed project are intentionally ignored.
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
        // Remote repositories declared by the analyzed project are intentionally ignored.
    }

    @Override
    public ModelResolver newCopy() {
        return new RestrictedModelResolver(repository);
    }
}
