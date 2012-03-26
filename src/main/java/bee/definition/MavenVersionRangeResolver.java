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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.Builder;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;

/**
 * @version 2012/03/25 20:40:54
 */
class MavenVersionRangeResolver implements VersionRangeResolver {

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    MetadataResolver metadataResolver;

    SyncContextFactory syncContextFactory;

    RepositoryEventDispatcher repositoryEventDispatcher;

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionRangeResult resolveVersionRange(RepositorySystemSession session, VersionRangeRequest request)
            throws VersionRangeResolutionException {
        VersionRangeResult result = new VersionRangeResult(request);

        VersionScheme versionScheme = new GenericVersionScheme();

        VersionConstraint versionConstraint;
        try {
            versionConstraint = versionScheme.parseVersionConstraint(request.getArtifact().getVersion());
        } catch (InvalidVersionSpecificationException e) {
            result.addException(e);
            throw new VersionRangeResolutionException(result);
        }

        result.setVersionConstraint(versionConstraint);

        if (versionConstraint.getRange() == null) {
            result.addVersion(versionConstraint.getVersion());
        } else {
            Map<String, ArtifactRepository> versionIndex = getVersions(session, result, request);

            List<Version> versions = new ArrayList<Version>();
            for (Map.Entry<String, ArtifactRepository> v : versionIndex.entrySet()) {
                try {
                    Version ver = versionScheme.parseVersion(v.getKey());
                    if (versionConstraint.containsVersion(ver)) {
                        versions.add(ver);
                        result.setRepository(ver, v.getValue());
                    }
                } catch (InvalidVersionSpecificationException e) {
                    result.addException(e);
                }
            }

            Collections.sort(versions);
            result.setVersions(versions);
        }

        return result;
    }

    /**
     * @param session
     * @param result
     * @param request
     * @return
     */
    private Map<String, ArtifactRepository> getVersions(RepositorySystemSession session, VersionRangeResult result, VersionRangeRequest request) {
        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);

        Map<String, ArtifactRepository> versionIndex = new HashMap<String, ArtifactRepository>();

        Metadata metadata = new DefaultMetadata(request.getArtifact().getGroupId(), request.getArtifact()
                .getArtifactId(), MAVEN_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT);

        List<MetadataRequest> metadataRequests = new ArrayList<MetadataRequest>(request.getRepositories().size());

        metadataRequests.add(new MetadataRequest(metadata, null, request.getRequestContext()));

        for (RemoteRepository repository : request.getRepositories()) {
            MetadataRequest metadataRequest = new MetadataRequest(metadata, repository, request.getRequestContext());
            metadataRequest.setDeleteLocalCopyIfMissing(true);
            metadataRequest.setTrace(trace);
            metadataRequests.add(metadataRequest);
        }

        List<MetadataResult> metadataResults = metadataResolver.resolveMetadata(session, metadataRequests);

        WorkspaceReader workspace = session.getWorkspaceReader();
        if (workspace != null) {
            List<String> versions = workspace.findVersions(request.getArtifact());
            for (String version : versions) {
                versionIndex.put(version, workspace.getRepository());
            }
        }

        for (MetadataResult metadataResult : metadataResults) {
            result.addException(metadataResult.getException());

            ArtifactRepository repository = metadataResult.getRequest().getRepository();
            if (repository == null) {
                repository = session.getLocalRepository();
            }

            Versioning versioning = readVersions(session, trace, metadataResult.getMetadata(), repository, result);
            for (String version : versioning.getVersions()) {
                if (!versionIndex.containsKey(version)) {
                    versionIndex.put(version, repository);
                }
            }
        }

        return versionIndex;
    }

    /**
     * @param session
     * @param trace
     * @param metadata
     * @param repository
     * @param result
     * @return
     */
    private Versioning readVersions(RepositorySystemSession session, RequestTrace trace, Metadata metadata, ArtifactRepository repository, VersionRangeResult result) {
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
                    }
                } finally {
                    syncContext.close();
                }
            }
        } catch (Exception e) {
            invalidMetadata(session, trace, metadata, repository, e);
            result.addException(e);
        } finally {
            IOUtil.close(fis);
        }

        return (versioning != null) ? versioning : new Versioning();
    }

    private void invalidMetadata(RepositorySystemSession session, RequestTrace trace, Metadata metadata, ArtifactRepository repository, Exception exception) {
        Builder builder = new RepositoryEvent.Builder(session, EventType.METADATA_INVALID);
        builder.setMetadata(metadata).setException(exception).setRepository(repository);

        repositoryEventDispatcher.dispatch(builder.build());
    }
}
