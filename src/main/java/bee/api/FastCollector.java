/*
 * Copyright (C) 2025 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package bee.api;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.internal.impl.collect.DataPool;
import org.eclipse.aether.internal.impl.collect.DefaultDependencyCollectionContext;
import org.eclipse.aether.internal.impl.collect.DefaultVersionFilterContext;
import org.eclipse.aether.internal.impl.collect.DependencyCollectorDelegate;
import org.eclipse.aether.internal.impl.collect.PremanagedDependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.ConfigUtils;

import bee.util.Profiling;
import kiss.I;

/**
 * Enable parallel dependency requests.
 */
@Named("fast")
class FastCollector extends DependencyCollectorDelegate {

    private FastCollector(RemoteRepositoryManager remoteRepositoryManager, ArtifactDescriptorReader artifactDescriptorReader, VersionRangeResolver versionRangeResolver) {
        super(remoteRepositoryManager, artifactDescriptorReader, versionRangeResolver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doCollectDependencies(RepositorySystemSession session, RequestTrace trace, DataPool pool, DefaultDependencyCollectionContext context, DefaultVersionFilterContext versionContext, CollectRequest request, DependencyNode node, List<RemoteRepository> repositories, List<Dependency> dependencies, List<Dependency> managedDependencies, Results results)
            throws DependencyCollectionException {
        try (var x = Profiling.of("Dependency Collect")) {
            FastCollector.Context args = new Context(session, trace, pool, context, versionContext, request);

            DependencySelector selector = session.getDependencySelector();
            DependencyManager manager = session.getDependencyManager();
            DependencyTraverser traverser = session.getDependencyTraverser();
            VersionFilter filter = session.getVersionFilter();

            selector = selector == null ? null : selector.deriveChildSelector(context);
            manager = manager == null ? null : manager.deriveChildManager(context);
            traverser = traverser == null ? null : traverser.deriveChildTraverser(context);
            filter = filter == null ? null : filter.deriveChildFilter(context);

            process(args, dependencies, repositories, selector, manager, traverser, filter, node);

            args.fork.awaitQuiescence(60, TimeUnit.SECONDS);
        }
    }

    private void process(FastCollector.Context con, List<Dependency> dependencies, List<RemoteRepository> repositories, DependencySelector selector, DependencyManager manager, DependencyTraverser traverser, VersionFilter filter, DependencyNode node) {
        for (Dependency dependency : dependencies) {
            con.fork.submit(() -> {
                processDependency(con, repositories, selector, manager, traverser, filter, dependency, Collections.EMPTY_LIST, node);
            });
        }
    }

    private void processDependency(FastCollector.Context con, List<RemoteRepository> repositories, DependencySelector selector, DependencyManager manager, DependencyTraverser traverser, VersionFilter filter, Dependency dependency, List<Artifact> relocations, DependencyNode node) {
        if (selector != null && !selector.selectDependency(dependency)) {
            return;
        }

        boolean noDescriptor = isLackingDescriptor(dependency.getArtifact());
        boolean traverse = !noDescriptor && (traverser == null || traverser.traverseDependency(dependency));

        try {
            VersionRangeRequest rangeRequest = createVersionRangeRequest(con.request
                    .getRequestContext(), con.trace, repositories, dependency);
            VersionRangeResult rangeResult = cachedResolveRangeResult(rangeRequest, con.pool, con.session);

            I.signal(filterVersions(dependency, rangeResult, filter, con.versionContext)).joinAll(version -> {
                Artifact originalArtifact = dependency.getArtifact().setVersion(version.toString());
                Dependency d = dependency.setArtifact(originalArtifact);

                ArtifactDescriptorRequest descriptorRequest = createArtifactDescriptorRequest(con.request
                        .getRequestContext(), con.trace, repositories, d);
                ArtifactDescriptorResult descriptorResult = retrieveArtifactDescriptor(con, noDescriptor, d, descriptorRequest);
                if (descriptorResult != null) {
                    d = d.setArtifact(descriptorResult.getArtifact());
                    List<Artifact> subRelocations = descriptorResult.getRelocations();

                    if (!subRelocations.isEmpty()) {
                        processDependency(con, repositories, selector, manager, traverser, filter, d, subRelocations, node);
                    } else {
                        d = con.pool.intern(d.setArtifact(con.pool.intern(d.getArtifact())));

                        DefaultDependencyNode child = createDependencyNode(relocations, PremanagedDependency
                                .create(manager, d, false, false), rangeResult, version, d, descriptorResult
                                        .getAliases(), repositories, con.request.getRequestContext());

                        synchronized (node) {
                            node.getChildren().add(child);
                        }

                        if (traverse && !descriptorResult.getDependencies().isEmpty()) {
                            DefaultDependencyCollectionContext context = con.collectionContext;
                            context.set(d, descriptorResult.getManagedDependencies());

                            DependencySelector subSelector = selector != null ? selector.deriveChildSelector(context) : null;
                            DependencyManager subManager = manager != null ? manager.deriveChildManager(context) : null;
                            DependencyTraverser subTraverser = traverser != null ? traverser.deriveChildTraverser(context) : null;
                            VersionFilter subFilter = filter != null ? filter.deriveChildFilter(context) : null;

                            List<RemoteRepository> subRepos = con.ignoreRepos ? repositories
                                    : remoteRepositoryManager
                                            .aggregateRepositories(con.session, repositories, descriptorResult.getRepositories(), true);

                            Object key = con.pool.toKey(d.getArtifact(), subRepos, subSelector, subManager, subTraverser, subFilter);

                            List<DependencyNode> children = con.pool.getChildren(key);
                            if (children == null) {
                                con.pool.putChildren(key, child.getChildren());
                                process(con, descriptorResult
                                        .getDependencies(), subRepos, subSelector, subManager, subTraverser, subFilter, child);
                            } else {
                                child.setChildren(children);
                            }
                        }
                    }
                } else {
                    DefaultDependencyNode child = createDependencyNode(relocations, PremanagedDependency
                            .create(manager, d, false, false), rangeResult, version, d, null, repositories, con.request
                                    .getRequestContext());
                    synchronized (node) {
                        node.getChildren().add(child);
                    }
                }
                return null;
            }, con.fork).to();
        } catch (VersionRangeResolutionException e) {
            return;
        }
    }

    private ArtifactDescriptorResult retrieveArtifactDescriptor(FastCollector.Context con, boolean noDescriptor, Dependency d, ArtifactDescriptorRequest descriptorRequest) {
        if (noDescriptor) {
            return new ArtifactDescriptorResult(descriptorRequest);
        } else {
            Object key = con.pool.toKey(descriptorRequest);
            ArtifactDescriptorResult descriptorResult = con.pool.getDescriptor(key, descriptorRequest);
            if (descriptorResult == null) {
                try {
                    descriptorResult = descriptorReader.readArtifactDescriptor(con.session, descriptorRequest);
                    con.pool.putDescriptor(key, descriptorResult);
                } catch (ArtifactDescriptorException e) {
                    con.pool.putDescriptor(key, e);
                    return null;
                }
            }
            return descriptorResult == DataPool.NO_DESCRIPTOR ? null : descriptorResult;
        }
    }

    private static class Context {

        final RepositorySystemSession session;

        final boolean ignoreRepos;

        final RequestTrace trace;

        final DataPool pool;

        final DefaultDependencyCollectionContext collectionContext;

        final DefaultVersionFilterContext versionContext;

        final CollectRequest request;

        final ForkJoinPool fork;

        Context(RepositorySystemSession session, RequestTrace trace, DataPool pool, DefaultDependencyCollectionContext collectionContext, DefaultVersionFilterContext versionContext, CollectRequest request) {
            this.session = session;
            this.request = request;
            this.ignoreRepos = session.isIgnoreArtifactDescriptorRepositories();
            this.trace = trace;
            this.pool = pool;
            this.collectionContext = collectionContext;
            this.versionContext = versionContext;
            this.fork = new ForkJoinPool(ConfigUtils
                    .getInteger(session, Runtime.getRuntime().availableProcessors() * 2, "maven.artifact.threads"));
        }
    }
}