/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.definition;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.Builder;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.CacheUtils;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;

/**
 * @version 2012/03/25 20:37:31
 */
class MavenVersionResolver implements VersionResolver {

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    private static final String RELEASE = "RELEASE";

    private static final String LATEST = "LATEST";

    private static final String SNAPSHOT = "SNAPSHOT";

    MetadataResolver metadataResolver;

    SyncContextFactory syncContextFactory;

    RepositoryEventDispatcher repositoryEventDispatcher;

    /**
     * {@inheritDoc}
     */
    public VersionResult resolveVersion(RepositorySystemSession session, VersionRequest request)
            throws VersionResolutionException {
        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);

        Artifact artifact = request.getArtifact();

        String version = artifact.getVersion();

        VersionResult result = new VersionResult(request);

        Key cacheKey = null;
        RepositoryCache cache = session.getCache();
        if (cache != null) {
            cacheKey = new Key(session, request);

            Object obj = cache.get(session, cacheKey);
            if (obj instanceof Record) {
                Record record = (Record) obj;
                result.setVersion(record.version);
                result.setRepository(CacheUtils.getRepository(session, request.getRepositories(), record.repoClass, record.repoId));
                return result;
            }
        }

        Metadata metadata;

        if (RELEASE.equals(version)) {
            metadata = new DefaultMetadata(artifact.getGroupId(), artifact.getArtifactId(), MAVEN_METADATA_XML, Metadata.Nature.RELEASE);
        } else if (LATEST.equals(version)) {
            metadata = new DefaultMetadata(artifact.getGroupId(), artifact.getArtifactId(), MAVEN_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT);
        } else if (version.endsWith(SNAPSHOT)) {
            WorkspaceReader workspace = session.getWorkspaceReader();
            if (workspace != null && workspace.findVersions(artifact).contains(version)) {
                metadata = null;
                result.setRepository(workspace.getRepository());
            } else {
                metadata = new DefaultMetadata(artifact.getGroupId(), artifact.getArtifactId(), version, MAVEN_METADATA_XML, Metadata.Nature.SNAPSHOT);
            }
        } else {
            metadata = null;
        }

        if (metadata == null) {
            result.setVersion(version);
        } else {
            List<MetadataRequest> metadataRequests = new ArrayList<MetadataRequest>(request.getRepositories().size());

            metadataRequests.add(new MetadataRequest(metadata, null, request.getRequestContext()));

            for (RemoteRepository repository : request.getRepositories()) {
                MetadataRequest metadataRequest = new MetadataRequest(metadata, repository, request.getRequestContext());
                metadataRequest.setDeleteLocalCopyIfMissing(true);
                metadataRequest.setFavorLocalRepository(true);
                metadataRequest.setTrace(trace);
                metadataRequests.add(metadataRequest);
            }

            List<MetadataResult> metadataResults = metadataResolver.resolveMetadata(session, metadataRequests);

            Map<String, VersionInfo> infos = new HashMap<String, VersionInfo>();

            for (MetadataResult metadataResult : metadataResults) {
                result.addException(metadataResult.getException());

                ArtifactRepository repository = metadataResult.getRequest().getRepository();
                if (repository == null) {
                    repository = session.getLocalRepository();
                }

                Versioning versioning = readVersions(session, trace, metadataResult.getMetadata(), repository, result);
                merge(artifact, infos, versioning, repository);
            }

            if (RELEASE.equals(version)) {
                resolve(result, infos, RELEASE);
            } else if (LATEST.equals(version)) {
                if (!resolve(result, infos, LATEST)) {
                    resolve(result, infos, RELEASE);
                }

                if (result.getVersion() != null && result.getVersion().endsWith(SNAPSHOT)) {
                    VersionRequest subRequest = new VersionRequest();
                    subRequest.setArtifact(artifact.setVersion(result.getVersion()));
                    if (result.getRepository() instanceof RemoteRepository) {
                        subRequest.setRepositories(Collections.singletonList((RemoteRepository) result.getRepository()));
                    } else {
                        subRequest.setRepositories(request.getRepositories());
                    }
                    VersionResult subResult = resolveVersion(session, subRequest);
                    result.setVersion(subResult.getVersion());
                    result.setRepository(subResult.getRepository());
                    for (Exception exception : subResult.getExceptions()) {
                        result.addException(exception);
                    }
                }
            } else {
                String key = SNAPSHOT + getKey(artifact.getClassifier(), artifact.getExtension());
                merge(infos, SNAPSHOT, key);
                if (!resolve(result, infos, key)) {
                    result.setVersion(version);
                }
            }

            if (result.getVersion().isEmpty()) {
                throw new VersionResolutionException(result);
            }
        }

        if (cacheKey != null && metadata != null && isSafelyCacheable(session, artifact)) {
            cache.put(session, cacheKey, new Record(result.getVersion(), result.getRepository()));
        }

        return result;
    }

    /**
     * @param result
     * @param infos
     * @param key
     * @return
     */
    private boolean resolve(VersionResult result, Map<String, VersionInfo> infos, String key) {
        VersionInfo info = infos.get(key);
        if (info != null) {
            result.setVersion(info.version);
            result.setRepository(info.repository);
        }
        return info != null;
    }

    /**
     * @param session
     * @param trace
     * @param metadata
     * @param repository
     * @param result
     * @return
     */
    private Versioning readVersions(RepositorySystemSession session, RequestTrace trace, Metadata metadata, ArtifactRepository repository, VersionResult result) {
        Versioning versioning = null;

        FileInputStream fis = null;
        try {
            if (metadata != null) {
                SyncContext syncContext = syncContextFactory.newInstance(session, true);

                try {
                    syncContext.acquire(null, Collections.singleton(metadata));

                    if (metadata.getFile() != null && metadata.getFile().exists()) {
                        fis = new FileInputStream(metadata.getFile());
                        org.apache.maven.artifact.repository.metadata.Metadata m = new MetadataXpp3Reader().read(fis, false);
                        versioning = m.getVersioning();

                        /*
                         * NOTE: Users occasionally misuse the id "local" for remote repos which
                         * screws up the metadata of the local repository. This is especially
                         * troublesome during snapshot resolution so we try to handle that
                         * gracefully.
                         */
                        if (versioning != null && repository instanceof LocalRepository) {
                            if (versioning.getSnapshot() != null && versioning.getSnapshot().getBuildNumber() > 0) {
                                Versioning repaired = new Versioning();
                                repaired.setLastUpdated(versioning.getLastUpdated());
                                Snapshot snapshot = new Snapshot();
                                snapshot.setLocalCopy(true);
                                repaired.setSnapshot(snapshot);
                                versioning = repaired;

                                throw new IOException("Snapshot information corrupted with remote repository data" + ", please verify that no remote repository uses the id '" + repository.getId() + "'");
                            }
                        }
                    }
                } finally {
                    syncContext.close();
                }
            }
        } catch (Exception e) {
            invalidMetadata(session, trace, metadata, repository, e);
            result.addException(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // If this exception will be thrown, it is bug of this program. So we must
                    // rethrow the wrapped error in here.
                    throw new Error(e);
                }
            }
        }

        return (versioning != null) ? versioning : new Versioning();
    }

    /**
     * @param session
     * @param trace
     * @param metadata
     * @param repository
     * @param exception
     */
    private void invalidMetadata(RepositorySystemSession session, RequestTrace trace, Metadata metadata, ArtifactRepository repository, Exception exception) {
        Builder builder = new RepositoryEvent.Builder(session, EventType.METADATA_INVALID);
        builder.setMetadata(metadata).setException(exception).setRepository(repository);

        repositoryEventDispatcher.dispatch(builder.build());
    }

    /**
     * @param artifact
     * @param infos
     * @param versioning
     * @param repository
     */
    private void merge(Artifact artifact, Map<String, VersionInfo> infos, Versioning versioning, ArtifactRepository repository) {
        if (!isEmpty(versioning.getRelease())) {
            merge(RELEASE, infos, versioning.getLastUpdated(), versioning.getRelease(), repository);
        }

        if (!isEmpty(versioning.getLatest())) {
            merge(LATEST, infos, versioning.getLastUpdated(), versioning.getLatest(), repository);
        }

        for (SnapshotVersion sv : versioning.getSnapshotVersions()) {
            if (!isEmpty(sv.getVersion())) {
                String key = getKey(sv.getClassifier(), sv.getExtension());
                merge(SNAPSHOT + key, infos, sv.getUpdated(), sv.getVersion(), repository);
            }
        }

        Snapshot snapshot = versioning.getSnapshot();
        if (snapshot != null && versioning.getSnapshotVersions().isEmpty()) {
            String version = artifact.getVersion();
            if (snapshot.getTimestamp() != null && snapshot.getBuildNumber() > 0) {
                String qualifier = snapshot.getTimestamp() + '-' + snapshot.getBuildNumber();
                version = version.substring(0, version.length() - SNAPSHOT.length()) + qualifier;
            }
            merge(SNAPSHOT, infos, versioning.getLastUpdated(), version, repository);
        }
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /**
     * @param key
     * @param infos
     * @param timestamp
     * @param version
     * @param repository
     */
    private void merge(String key, Map<String, VersionInfo> infos, String timestamp, String version, ArtifactRepository repository) {
        VersionInfo info = infos.get(key);
        if (info == null) {
            info = new VersionInfo(timestamp, version, repository);
            infos.put(key, info);
        } else if (info.isOutdated(timestamp)) {
            info.version = version;
            info.repository = repository;
            info.timestamp = timestamp;
        }
    }

    /**
     * @param infos
     * @param srcKey
     * @param dstKey
     */
    private void merge(Map<String, VersionInfo> infos, String srcKey, String dstKey) {
        VersionInfo srcInfo = infos.get(srcKey);
        VersionInfo dstInfo = infos.get(dstKey);

        if (dstInfo == null || (srcInfo != null && dstInfo.isOutdated(srcInfo.timestamp) && srcInfo.repository != dstInfo.repository)) {
            infos.put(dstKey, srcInfo);
        }
    }

    /**
     * @param classifier
     * @param extension
     * @return
     */
    private String getKey(String classifier, String extension) {
        return clean(classifier) + ':' + clean(extension);
    }

    /**
     * @param value
     * @return
     */
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * @param session
     * @param artifact
     * @return
     */
    private boolean isSafelyCacheable(RepositorySystemSession session, Artifact artifact) {
        /*
         * The workspace/reactor is in flux so we better not assume definitive information for any
         * of its artifacts/projects.
         */

        WorkspaceReader workspace = session.getWorkspaceReader();
        if (workspace == null) {
            return true;
        }

        Artifact pomArtifact = MavenUtils.convert(artifact);

        return workspace.findArtifact(pomArtifact) == null;
    }

    /**
     * @version 2012/03/25 20:33:44
     */
    private static class VersionInfo {

        String timestamp;

        String version;

        ArtifactRepository repository;

        /**
         * @param timestamp
         * @param version
         * @param repository
         */
        public VersionInfo(String timestamp, String version, ArtifactRepository repository) {
            this.timestamp = (timestamp != null) ? timestamp : "";
            this.version = version;
            this.repository = repository;
        }

        /**
         * @param timestamp
         * @return
         */
        public boolean isOutdated(String timestamp) {
            return timestamp != null && timestamp.compareTo(this.timestamp) > 0;
        }
    }

    /**
     * @version 2012/03/25 20:33:48
     */
    private static class Key {

        private final String groupId;

        private final String artifactId;

        private final String classifier;

        private final String extension;

        private final String version;

        private final String context;

        private final File localRepo;

        private final WorkspaceRepository workspace;

        private final List<RemoteRepository> repositories;

        private final int hashCode;

        /**
         * @param session
         * @param request
         */
        public Key(RepositorySystemSession session, VersionRequest request) {
            Artifact artifact = request.getArtifact();
            groupId = artifact.getGroupId();
            artifactId = artifact.getArtifactId();
            classifier = artifact.getClassifier();
            extension = artifact.getExtension();
            version = artifact.getVersion();
            localRepo = session.getLocalRepository().getBasedir();
            workspace = CacheUtils.getWorkspace(session);
            repositories = new ArrayList<RemoteRepository>(request.getRepositories().size());
            boolean repoMan = false;
            for (RemoteRepository repository : request.getRepositories()) {
                if (repository.isRepositoryManager()) {
                    repoMan = true;
                    repositories.addAll(repository.getMirroredRepositories());
                } else {
                    repositories.add(repository);
                }
            }
            context = repoMan ? request.getRequestContext() : "";

            int hash = 17;
            hash = hash * 31 + groupId.hashCode();
            hash = hash * 31 + artifactId.hashCode();
            hash = hash * 31 + classifier.hashCode();
            hash = hash * 31 + extension.hashCode();
            hash = hash * 31 + version.hashCode();
            hash = hash * 31 + localRepo.hashCode();
            hash = hash * 31 + CacheUtils.repositoriesHashCode(repositories);
            hashCode = hash;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj == null || !getClass().equals(obj.getClass())) {
                return false;
            }

            Key that = (Key) obj;
            return artifactId.equals(that.artifactId) && groupId.equals(that.groupId) && classifier.equals(that.classifier) && extension.equals(that.extension) && version.equals(that.version) && context.equals(that.context) && localRepo.equals(that.localRepo) && CacheUtils.eq(workspace, that.workspace) && CacheUtils.repositoriesEquals(repositories, that.repositories);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * @version 2012/03/25 20:34:13
     */
    private static class Record {

        final String version;

        final String repoId;

        final Class<?> repoClass;

        /**
         * @param version
         * @param repository
         */
        public Record(String version, ArtifactRepository repository) {
            this.version = version;
            if (repository != null) {
                repoId = repository.getId();
                repoClass = repository.getClass();
            } else {
                repoId = null;
                repoClass = null;
            }
        }
    }
}
