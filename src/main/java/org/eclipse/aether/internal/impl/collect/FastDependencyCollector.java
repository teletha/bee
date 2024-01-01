/*
 * Copyright (C) 2024 The BEE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package org.eclipse.aether.internal.impl.collect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.collection.VersionFilter;
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

import bee.util.Profiling;

@Singleton
@Named
public class FastDependencyCollector implements DependencyCollector {

    private final RemoteRepositoryManager remoteRepositoryManager;

    private final ArtifactDescriptorReader descriptorReader;

    private final VersionRangeResolver versionRangeResolver;

    @Inject
    FastDependencyCollector(RemoteRepositoryManager remoteRepositoryManager, ArtifactDescriptorReader artifactDescriptorReader, VersionRangeResolver versionRangeResolver) {
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.descriptorReader = artifactDescriptorReader;
        this.versionRangeResolver = versionRangeResolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CollectResult collectDependencies(RepositorySystemSession session, CollectRequest request) throws DependencyCollectionException {
        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);
        CollectResult result = new CollectResult(request);
        DependencySelector depSelector = session.getDependencySelector();
        DependencyManager depManager = session.getDependencyManager();
        DependencyTraverser depTraverser = session.getDependencyTraverser();
        VersionFilter verFilter = session.getVersionFilter();
        Dependency root = request.getRoot();
        List<RemoteRepository> repositories = request.getRepositories();
        List<Dependency> dependencies = request.getDependencies();
        List<Dependency> managedDependencies = request.getManagedDependencies();

        DefaultDependencyNode node;
        if (root != null) {
            try {
                VersionRangeRequest rangeRequest = new VersionRangeRequest(root.getArtifact(), repositories, request.getRequestContext());
                rangeRequest.setTrace(trace);
                VersionRangeResult rangeResult = versionRangeResolver.resolveVersionRange(session, rangeRequest);
                List<? extends Version> versions = filterVersions(root, rangeResult, verFilter, new DefaultVersionFilterContext(session));

                Version version = versions.get(versions.size() - 1);
                root = root.setArtifact(root.getArtifact().setVersion(version.toString()));

                ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
                descriptorRequest.setArtifact(root.getArtifact());
                descriptorRequest.setRepositories(request.getRepositories());
                descriptorRequest.setRequestContext(request.getRequestContext());
                descriptorRequest.setTrace(trace);

                ArtifactDescriptorResult descriptorResult = isLackingDescriptor(root.getArtifact())
                        ? new ArtifactDescriptorResult(descriptorRequest)
                        : descriptorReader.readArtifactDescriptor(session, descriptorRequest);

                root = root.setArtifact(descriptorResult.getArtifact());

                if (!session.isIgnoreArtifactDescriptorRepositories()) {
                    repositories = remoteRepositoryManager
                            .aggregateRepositories(session, repositories, descriptorResult.getRepositories(), true);
                }
                dependencies = mergeDeps(dependencies, descriptorResult.getDependencies());
                managedDependencies = mergeDeps(managedDependencies, descriptorResult.getManagedDependencies());

                node = new DefaultDependencyNode(root);
                node.setRequestContext(request.getRequestContext());
                node.setRelocations(descriptorResult.getRelocations());
                node.setVersionConstraint(rangeResult.getVersionConstraint());
                node.setVersion(version);
                node.setAliases(descriptorResult.getAliases());
                node.setRepositories(request.getRepositories());
            } catch (Exception e) {
                result.addException(e);
                throw new DependencyCollectionException(result, e.getMessage());
            }
        } else {
            node = new DefaultDependencyNode(request.getRootArtifact());
            node.setRequestContext(request.getRequestContext());
            node.setRepositories(request.getRepositories());
        }

        result.setRoot(node);

        boolean traverse = root == null || depTraverser == null || depTraverser.traverseDependency(root);
        if (traverse && !dependencies.isEmpty()) {
            try (var x = Profiling.of("Dependency Collect")) {
                DataPool pool = new DataPool(session);
                DefaultDependencyCollectionContext context = new DefaultDependencyCollectionContext(session, request
                        .getRootArtifact(), root, managedDependencies);

                DefaultVersionFilterContext versionContext = new DefaultVersionFilterContext(session);
                Args args = new Args(session, trace, pool, context, versionContext, request);

                process(args, dependencies, repositories, depSelector != null ? depSelector.deriveChildSelector(context)
                        : null, depManager != null ? depManager.deriveChildManager(context) : null, depTraverser != null
                                ? depTraverser.deriveChildTraverser(context)
                                : null, verFilter != null ? verFilter.deriveChildFilter(context) : null, node);

                args.fork.awaitQuiescence(60, TimeUnit.SECONDS);
            }
        }

        DependencyGraphTransformer transformer = session.getDependencyGraphTransformer();
        if (transformer != null) {
            try (var x = Profiling.of("Dependency Transform")) {
                DefaultDependencyGraphTransformationContext context = new DefaultDependencyGraphTransformationContext(session);
                result.setRoot(transformer.transformGraph(node, context));
            } catch (RepositoryException e) {
                result.addException(e);
            }
        }

        if (!result.getExceptions().isEmpty()) {
            throw new DependencyCollectionException(result);
        }
        return result;
    }

    private List<Dependency> mergeDeps(List<Dependency> dominant, List<Dependency> recessive) {
        List<Dependency> result;
        if (dominant == null || dominant.isEmpty()) {
            result = recessive;
        } else if (recessive == null || recessive.isEmpty()) {
            result = dominant;
        } else {
            int initialCapacity = dominant.size() + recessive.size();
            result = new ArrayList<>(initialCapacity);
            Collection<String> ids = new HashSet<>(initialCapacity, 1.0f);
            for (Dependency dependency : dominant) {
                ids.add(getId(dependency.getArtifact()));
                result.add(dependency);
            }
            for (Dependency dependency : recessive) {
                if (!ids.contains(getId(dependency.getArtifact()))) {
                    result.add(dependency);
                }
            }
        }
        return result;
    }

    private static String getId(Artifact a) {
        return a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getClassifier() + ':' + a.getExtension();
    }

    private void process(final Args args, List<Dependency> dependencies, List<RemoteRepository> repositories, DependencySelector depSelector, DependencyManager depManager, DependencyTraverser depTraverser, VersionFilter verFilter, DependencyNode node) {
        for (Dependency dependency : dependencies) {
            args.fork.submit(() -> {
                processDependency(args, repositories, depSelector, depManager, depTraverser, verFilter, dependency, Collections
                        .emptyList(), node);
            });
        }
    }

    private void processDependency(Args args, List<RemoteRepository> repositories, DependencySelector depSelector, DependencyManager depManager, DependencyTraverser depTraverser, VersionFilter verFilter, Dependency dependency, List<Artifact> relocations, DependencyNode node) {
        if (depSelector != null && !depSelector.selectDependency(dependency)) {
            return;
        }

        boolean noDescriptor = isLackingDescriptor(dependency.getArtifact());
        boolean traverse = !noDescriptor && (depTraverser == null || depTraverser.traverseDependency(dependency));

        List<? extends Version> versions;
        VersionRangeResult rangeResult;
        try {
            VersionRangeRequest rangeRequest = createVersionRangeRequest(args, repositories, dependency);

            rangeResult = cachedResolveRangeResult(rangeRequest, args.pool, args.session);

            versions = filterVersions(dependency, rangeResult, verFilter, args.versionContext);
        } catch (VersionRangeResolutionException e) {
            return;
        }

        for (Version version : versions) {
            Artifact originalArtifact = dependency.getArtifact().setVersion(version.toString());
            Dependency d = dependency.setArtifact(originalArtifact);

            ArtifactDescriptorRequest descriptorRequest = createArtifactDescriptorRequest(args, repositories, d);
            ArtifactDescriptorResult descriptorResult = getArtifactDescriptorResult(args, noDescriptor, d, descriptorRequest);
            if (descriptorResult != null) {
                d = d.setArtifact(descriptorResult.getArtifact());
                List<Artifact> subRelocations = descriptorResult.getRelocations();

                if (!subRelocations.isEmpty()) {
                    processDependency(args, repositories, depSelector, depManager, depTraverser, verFilter, d, subRelocations, node);
                } else {
                    d = args.pool.intern(d.setArtifact(args.pool.intern(d.getArtifact())));

                    DefaultDependencyNode child = createDependencyNode(relocations, rangeResult, version, d, descriptorResult
                            .getAliases(), repositories, args);

                    synchronized (node) {
                        node.getChildren().add(child);
                    }

                    boolean recurse = traverse && !descriptorResult.getDependencies().isEmpty();
                    if (recurse) {
                        doRecurse(args, repositories, depSelector, depManager, depTraverser, verFilter, d, descriptorResult, child);
                    }
                }
            } else {
                DefaultDependencyNode child = createDependencyNode(relocations, rangeResult, version, d, null, repositories, args);
                synchronized (node) {
                    node.getChildren().add(child);
                }
            }
        }
    }

    private void doRecurse(Args args, List<RemoteRepository> repositories, DependencySelector depSelector, DependencyManager depManager, DependencyTraverser depTraverser, VersionFilter verFilter, Dependency d, ArtifactDescriptorResult descriptorResult, DefaultDependencyNode child) {
        DefaultDependencyCollectionContext context = args.collectionContext;
        context.set(d, descriptorResult.getManagedDependencies());

        DependencySelector childSelector = depSelector != null ? depSelector.deriveChildSelector(context) : null;
        DependencyManager childManager = depManager != null ? depManager.deriveChildManager(context) : null;
        DependencyTraverser childTraverser = depTraverser != null ? depTraverser.deriveChildTraverser(context) : null;
        VersionFilter childFilter = verFilter != null ? verFilter.deriveChildFilter(context) : null;

        final List<RemoteRepository> childRepos = args.ignoreRepos ? repositories
                : remoteRepositoryManager.aggregateRepositories(args.session, repositories, descriptorResult.getRepositories(), true);

        Object key = args.pool.toKey(d.getArtifact(), childRepos, childSelector, childManager, childTraverser, childFilter);

        List<DependencyNode> children = args.pool.getChildren(key);
        if (children == null) {
            args.pool.putChildren(key, child.getChildren());

            process(args, descriptorResult.getDependencies(), childRepos, childSelector, childManager, childTraverser, childFilter, child);

        } else {
            child.setChildren(children);
        }
    }

    private ArtifactDescriptorResult getArtifactDescriptorResult(Args args, boolean noDescriptor, Dependency d, ArtifactDescriptorRequest descriptorRequest) {
        return noDescriptor ? new ArtifactDescriptorResult(descriptorRequest)
                : resolveCachedArtifactDescriptor(args.pool, descriptorRequest, args.session, d, args);

    }

    private ArtifactDescriptorResult resolveCachedArtifactDescriptor(DataPool pool, ArtifactDescriptorRequest descriptorRequest, RepositorySystemSession session, Dependency d, Args args) {
        Object key = pool.toKey(descriptorRequest);
        ArtifactDescriptorResult descriptorResult = pool.getDescriptor(key, descriptorRequest);
        if (descriptorResult == null) {
            try {
                descriptorResult = descriptorReader.readArtifactDescriptor(session, descriptorRequest);
                pool.putDescriptor(key, descriptorResult);
            } catch (ArtifactDescriptorException e) {
                pool.putDescriptor(key, e);
                return null;
            }

        } else if (descriptorResult == DataPool.NO_DESCRIPTOR) {
            return null;
        }

        return descriptorResult;
    }

    private VersionRangeResult cachedResolveRangeResult(VersionRangeRequest rangeRequest, DataPool pool, RepositorySystemSession session)
            throws VersionRangeResolutionException {
        Object key = pool.toKey(rangeRequest);
        VersionRangeResult rangeResult = pool.getConstraint(key, rangeRequest);
        if (rangeResult == null) {
            rangeResult = versionRangeResolver.resolveVersionRange(session, rangeRequest);
            pool.putConstraint(key, rangeResult);
        }
        return rangeResult;
    }

    private static DefaultDependencyNode createDependencyNode(List<Artifact> relocations, VersionRangeResult rangeResult, Version version, Dependency d, Collection<Artifact> aliases, List<RemoteRepository> repositories, Args args) {
        ArtifactRepository repository = rangeResult.getRepository(version);
        if (repository instanceof RemoteRepository) {
            repositories = Collections.singletonList((RemoteRepository) repository);
        } else if (repository != null) {
            repositories = Collections.emptyList();
        }

        DefaultDependencyNode child = new DefaultDependencyNode(d);
        child.setRelocations(relocations);
        child.setVersionConstraint(rangeResult.getVersionConstraint());
        child.setVersion(version);
        child.setAliases(aliases);
        child.setRepositories(repositories);
        child.setRequestContext(args.request.getRequestContext());
        return child;
    }

    private static ArtifactDescriptorRequest createArtifactDescriptorRequest(Args args, List<RemoteRepository> repositories, Dependency d) {
        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact(d.getArtifact());
        descriptorRequest.setRepositories(repositories);
        descriptorRequest.setRequestContext(args.request.getRequestContext());
        descriptorRequest.setTrace(args.trace);
        return descriptorRequest;
    }

    private static VersionRangeRequest createVersionRangeRequest(Args args, List<RemoteRepository> repositories, Dependency dependency) {
        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(dependency.getArtifact());
        rangeRequest.setRepositories(repositories);
        rangeRequest.setRequestContext(args.request.getRequestContext());
        rangeRequest.setTrace(args.trace);
        return rangeRequest;
    }

    private static boolean isLackingDescriptor(Artifact artifact) {
        return artifact.getProperty(ArtifactProperties.LOCAL_PATH, null) != null;
    }

    private static List<? extends Version> filterVersions(Dependency dependency, VersionRangeResult rangeResult, VersionFilter verFilter, DefaultVersionFilterContext verContext)
            throws VersionRangeResolutionException {
        if (rangeResult.getVersions().isEmpty()) {
            throw new VersionRangeResolutionException(rangeResult, "No versions available for " + dependency
                    .getArtifact() + " within specified range");
        }

        List<? extends Version> versions;
        if (verFilter != null && rangeResult.getVersionConstraint().getRange() != null) {
            verContext.set(dependency, rangeResult);
            try {
                verFilter.filterVersions(verContext);
            } catch (RepositoryException e) {
                throw new VersionRangeResolutionException(rangeResult, "Failed to filter versions for " + dependency
                        .getArtifact() + ": " + e.getMessage(), e);
            }
            versions = verContext.get();
            if (versions.isEmpty()) {
                throw new VersionRangeResolutionException(rangeResult, "No acceptable versions for " + dependency
                        .getArtifact() + ": " + rangeResult.getVersions());
            }
        } else {
            versions = rangeResult.getVersions();
        }
        return versions;
    }

    static class Args {

        final RepositorySystemSession session;

        final boolean ignoreRepos;

        final RequestTrace trace;

        final DataPool pool;

        final DefaultDependencyCollectionContext collectionContext;

        final DefaultVersionFilterContext versionContext;

        final CollectRequest request;

        final ForkJoinPool fork;

        Args(RepositorySystemSession session, RequestTrace trace, DataPool pool, DefaultDependencyCollectionContext collectionContext, DefaultVersionFilterContext versionContext, CollectRequest request) {
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