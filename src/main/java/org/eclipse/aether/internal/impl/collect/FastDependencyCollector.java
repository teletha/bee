/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package org.eclipse.aether.internal.impl.collect;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DependencyCollector;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.version.Version;

import bee.util.Profile;
import kiss.I;

@Named
@Singleton
public class FastDependencyCollector implements DependencyCollector {

    private RemoteRepositoryManager manager;

    private ArtifactDescriptorReader descriptorReader;

    private VersionRangeResolver versionRangeResolver;

    @Inject
    FastDependencyCollector(RemoteRepositoryManager remoteRepositoryManager, ArtifactDescriptorReader artifactDescriptorReader, VersionRangeResolver versionRangeResolver) {
        this.manager = remoteRepositoryManager;
        this.descriptorReader = artifactDescriptorReader;
        this.versionRangeResolver = versionRangeResolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CollectResult collectDependencies(RepositorySystemSession session, CollectRequest request) throws DependencyCollectionException {
        Dependency root = request.getRoot();
        List<RemoteRepository> repositories = request.getRepositories();
        List<Dependency> dependencies = request.getDependencies();

        DefaultDependencyNode node = new DefaultDependencyNode(request.getRootArtifact());
        node.setRequestContext(request.getRequestContext());
        node.setRepositories(request.getRepositories());

        boolean traversable = root == null || session.getDependencyTraverser().traverseDependency(root);
        if (traversable && !dependencies.isEmpty()) {
            Args args = new Args(session, request);
            DependencyCollectionContext context = new DefaultDependencyCollectionContext(session, request.getRootArtifact(), root, request
                    .getManagedDependencies());

            try (Profile.of("Dependency Resolving").start) {
                process(args, dependencies, repositories, session.getDependencySelector().deriveChildSelector(context), node);

                args.pool.awaitQuiescence(60, TimeUnit.SECONDS);
            }
        }

        try (Profile.of("Dependency Transform").start) {
            CollectResult result = new CollectResult(request).setRoot(node);
            DependencyGraphTransformationContext context = new DefaultDependencyGraphTransformationContext(session);
            result.setRoot(session.getDependencyGraphTransformer().transformGraph(node, context));

            return result;
        } catch (RepositoryException e) {
            throw I.quiet(e);
        }
    }

    private void process(Args args, List<Dependency> dependencies, List<RemoteRepository> remotes, DependencySelector selector, DependencyNode node) {
        for (Dependency dep : dependencies) {
            // must check duplications at first
            Artifact artifact = dep.getArtifact();
            String id = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getClassifier();

            if (!args.deps.add(id)) {
                continue;
            }

            // second check
            if (!selector.selectDependency(dep)) {
                continue;
            }

            args.pool.submit(() -> {
                try {
                    VersionRangeRequest rangeRequest = new VersionRangeRequest();
                    rangeRequest.setArtifact(dep.getArtifact());
                    rangeRequest.setRepositories(remotes);
                    rangeRequest.setRequestContext(args.request.getRequestContext());
                    rangeRequest.setTrace(args.trace);

                    VersionRangeResult rangeResult = versionRangeResolver.resolveVersionRange(args.session, rangeRequest);

                    for (Version version : rangeResult.getVersions()) {
                        Dependency versioned = dep.setArtifact(dep.getArtifact().setVersion(version.toString()));

                        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
                        request.setArtifact(versioned.getArtifact());
                        request.setRepositories(remotes);
                        request.setRequestContext(args.request.getRequestContext());
                        request.setTrace(args.trace);

                        boolean noDescriptor = dep.getArtifact().getProperty(ArtifactProperties.LOCAL_PATH, null) != null;
                        ArtifactDescriptorResult result = noDescriptor ? new ArtifactDescriptorResult(request)
                                : descriptorReader.readArtifactDescriptor(args.session, request);

                        versioned = versioned.setArtifact(result.getArtifact());

                        DefaultDependencyNode sub = new DefaultDependencyNode(versioned);
                        sub.setVersionConstraint(rangeResult.getVersionConstraint());
                        sub.setVersion(version);
                        sub.setAliases(result.getAliases());
                        sub.setRepositories(getRemoteRepositories(rangeResult.getRepository(version), remotes));
                        sub.setRequestContext(args.request.getRequestContext());

                        synchronized (node) {
                            node.getChildren().add(sub);
                        }

                        DependencyCollectionContext subContext = new DefaultDependencyCollectionContext(args.session, versioned
                                .getArtifact(), versioned, result.getManagedDependencies());
                        List<RemoteRepository> subRemotes = args.session.isIgnoreArtifactDescriptorRepositories() ? remotes
                                : manager.aggregateRepositories(args.session, remotes, result.getRepositories(), true);

                        process(args, result.getDependencies(), subRemotes, selector.deriveChildSelector(subContext), sub);
                    }
                } catch (VersionRangeResolutionException | ArtifactDescriptorException e) {
                    throw I.quiet(e);
                }
            });
        }
    }

    private static List<RemoteRepository> getRemoteRepositories(ArtifactRepository repository, List<RemoteRepository> repositories) {
        if (repository instanceof RemoteRepository) {
            return Collections.singletonList((RemoteRepository) repository);
        }
        if (repository != null) {
            return Collections.emptyList();
        }
        return repositories;
    }

    /**
     * 
     */
    private static class Args {

        private final RepositorySystemSession session;

        private final CollectRequest request;

        private final RequestTrace trace;

        private final Set<String> deps;

        private final ForkJoinPool pool;

        private Args(RepositorySystemSession session, CollectRequest request) {
            this.session = session;
            this.request = request;
            this.trace = RequestTrace.newChild(request.getTrace(), request);
            this.deps = ConcurrentHashMap.newKeySet();
            this.pool = new ForkJoinPool(ConfigUtils
                    .getInteger(session, Runtime.getRuntime().availableProcessors() * 2, "maven.artifact.threads"));
        }
    }
}
