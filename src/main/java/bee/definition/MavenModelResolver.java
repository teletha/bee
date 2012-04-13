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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.RequestTrace;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.impl.RemoteRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * A model resolver to assist building of dependency POMs. This resolver gives priority to those
 * repositories that have been initially specified and repositories discovered in dependency POMs
 * are recessively merged into the search chain.
 * 
 * @author Benjamin Bentmann
 * @see MavenArtifactDescriptorReader
 */
class MavenModelResolver implements ModelResolver {

    private final RepositorySystemSession session;

    private final RequestTrace trace;

    private final String context;

    private List<RemoteRepository> repositories;

    private final ArtifactResolver resolver;

    private final RemoteRepositoryManager remoteRepositoryManager;

    private final Set<String> repositoryIds;

    /**
     * @param session
     * @param trace
     * @param context
     * @param resolver
     * @param remoteRepositoryManager
     * @param repositories
     */
    MavenModelResolver(RepositorySystemSession session, RequestTrace trace, String context, ArtifactResolver resolver, RemoteRepositoryManager remoteRepositoryManager, List<RemoteRepository> repositories) {
        this.session = session;
        this.trace = trace;
        this.context = context;
        this.resolver = resolver;
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.repositories = repositories;
        this.repositoryIds = new HashSet<String>();
    }

    /**
     * @param original
     */
    private MavenModelResolver(MavenModelResolver original) {
        this.session = original.session;
        this.trace = original.trace;
        this.context = original.context;
        this.resolver = original.resolver;
        this.remoteRepositoryManager = original.remoteRepositoryManager;
        this.repositories = original.repositories;
        this.repositoryIds = new HashSet<String>(original.repositoryIds);
    }

    /**
     * {@inheritDoc}
     */
    public void addRepository(Repository repository) throws InvalidRepositoryException {
        if (!repositoryIds.add(repository.getId())) {
            return;
        }

        List<RemoteRepository> newRepositories = Collections.singletonList(MavenUtils.convert(repository));

        this.repositories = remoteRepositoryManager.aggregateRepositories(session, repositories, newRepositories, true);
    }

    /**
     * {@inheritDoc}
     */
    public ModelResolver newCopy() {
        return new MavenModelResolver(this);
    }

    /**
     * {@inheritDoc}
     */
    public ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);

        try {
            ArtifactRequest request = new ArtifactRequest(pomArtifact, repositories, context);
            request.setTrace(trace);
            pomArtifact = resolver.resolveArtifact(session, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
        }

        File pomFile = pomArtifact.getFile();

        return new FileModelSource(pomFile);
    }
}
