package io.github.zhancm.repoonboard.analyzer.maven;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelSource2;

final class RestrictedPomSource implements ModelSource2 {

    private final RestrictedPomRepository repository;
    private final Path realFile;
    private final Path boundaryRoot;
    private final RestrictedPomRepository.Origin origin;
    private final String sourceFileId;
    private final byte[] snapshot;
    private final Model rawModel;

    RestrictedPomSource(
            RestrictedPomRepository repository,
            Path realFile,
            Path boundaryRoot,
            RestrictedPomRepository.Origin origin,
            String sourceFileId,
            byte[] snapshot,
            Model rawModel) {
        this.repository = repository;
        this.realFile = realFile;
        this.boundaryRoot = boundaryRoot;
        this.origin = origin;
        this.sourceFileId = sourceFileId;
        this.snapshot = snapshot.clone();
        this.rawModel = rawModel;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        Path currentRealFile = realFile.toRealPath();
        if (!currentRealFile.equals(realFile) || !currentRealFile.startsWith(boundaryRoot)) {
            throw new IOException("POM source no longer resolves inside its allowed root");
        }
        return new ByteArrayInputStream(snapshot);
    }

    @Override
    public String getLocation() {
        return sourceFileId;
    }

    @Override
    public URI getLocationURI() {
        return realFile.toUri();
    }

    @Override
    public ModelSource2 getRelatedSource(String relativePath) {
        return repository.relatedSource(this, relativePath).orElse(null);
    }

    Path realFile() {
        return realFile;
    }

    Path boundaryRoot() {
        return boundaryRoot;
    }

    RestrictedPomRepository.Origin origin() {
        return origin;
    }

    String sourceFileId() {
        return sourceFileId;
    }

    Model rawModel() {
        return rawModel;
    }
}
