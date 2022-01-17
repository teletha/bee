/*
 * Copyright (C) 2022 Nameless Production Committee
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencyManagement;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
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
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.version.Version;

import bee.util.Profile;

@Singleton
@Named
public class FastDependencyCollector implements DependencyCollector {

    private static final String CONFIG_PROP_MAX_EXCEPTIONS = "aether.dependencyCollector.maxExceptions";

    private static final int CONFIG_PROP_MAX_EXCEPTIONS_DEFAULT = 50;

    private static final String CONFIG_PROP_MAX_CYCLES = "aether.dependencyCollector.maxCycles";

    private static final int CONFIG_PROP_MAX_CYCLES_DEFAULT = 10;

    private RemoteRepositoryManager remoteRepositoryManager;

    private ArtifactDescriptorReader descriptorReader;

    private VersionRangeResolver versionRangeResolver;

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
        session = optimizeSession(session);

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
            List<? extends Version> versions;
            VersionRangeResult rangeResult;
            try {
                VersionRangeRequest rangeRequest = new VersionRangeRequest(root.getArtifact(), request.getRepositories(), request
                        .getRequestContext());
                rangeRequest.setTrace(trace);
                rangeResult = versionRangeResolver.resolveVersionRange(session, rangeRequest);
                versions = filterVersions(root, rangeResult, verFilter, new DefaultVersionFilterContext(session));
            } catch (VersionRangeResolutionException e) {
                result.addException(e);
                throw new DependencyCollectionException(result, e.getMessage());
            }

            Version version = versions.get(versions.size() - 1);
            root = root.setArtifact(root.getArtifact().setVersion(version.toString()));

            ArtifactDescriptorResult descriptorResult;
            try {
                ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
                descriptorRequest.setArtifact(root.getArtifact());
                descriptorRequest.setRepositories(request.getRepositories());
                descriptorRequest.setRequestContext(request.getRequestContext());
                descriptorRequest.setTrace(trace);
                if (isLackingDescriptor(root.getArtifact())) {
                    descriptorResult = new ArtifactDescriptorResult(descriptorRequest);
                } else {
                    descriptorResult = descriptorReader.readArtifactDescriptor(session, descriptorRequest);
                }
            } catch (ArtifactDescriptorException e) {
                result.addException(e);
                throw new DependencyCollectionException(result, e.getMessage());
            }

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
        } else {
            node = new DefaultDependencyNode(request.getRootArtifact());
            node.setRequestContext(request.getRequestContext());
            node.setRepositories(request.getRepositories());
        }

        result.setRoot(node);

        boolean traverse = root == null || depTraverser == null || depTraverser.traverseDependency(root);
        if (traverse && !dependencies.isEmpty()) {
            DataPool pool = new DataPool(session);
            DefaultDependencyCollectionContext context = new DefaultDependencyCollectionContext(session, request
                    .getRootArtifact(), root, managedDependencies);

            DefaultVersionFilterContext versionContext = new DefaultVersionFilterContext(session);
            Args args = new Args(session, trace, pool, context, versionContext, request);

            try (var x = Profile.of("Dependency Collect")) {
                process(args, dependencies, repositories, depSelector != null ? depSelector.deriveChildSelector(context)
                        : null, depManager != null ? depManager.deriveChildManager(context) : null, depTraverser != null
                                ? depTraverser.deriveChildTraverser(context)
                                : null, verFilter != null ? verFilter.deriveChildFilter(context) : null, node);

                args.fork.awaitQuiescence(60, TimeUnit.SECONDS);
            }
        }

        DependencyGraphTransformer transformer = session.getDependencyGraphTransformer();
        if (transformer != null) {
            try (var x = Profile.of("Dependency Transform")) {
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

    private static RepositorySystemSession optimizeSession(RepositorySystemSession session) {
        DefaultRepositorySystemSession optimized = new DefaultRepositorySystemSession(session);
        optimized.setArtifactTypeRegistry(CachingArtifactTypeRegistry.newInstance(session));
        return optimized;
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
                        .emptyList(), false, node);
            });
        }
    }

    private void processDependency(Args args, List<RemoteRepository> repositories, DependencySelector depSelector, DependencyManager depManager, DependencyTraverser depTraverser, VersionFilter verFilter, Dependency dependency, List<Artifact> relocations, boolean disableVersionManagement, DependencyNode node) {
        if (depSelector != null && !depSelector.selectDependency(dependency)) {
            return;
        }

        PremanagedDependency preManaged = PremanagedDependency
                .create(depManager, dependency, disableVersionManagement, args.premanagedState);
        dependency = preManaged.managedDependency;

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

            final ArtifactDescriptorResult descriptorResult = getArtifactDescriptorResult(args, noDescriptor, d, descriptorRequest);
            if (descriptorResult != null) {
                d = d.setArtifact(descriptorResult.getArtifact());

                if (!descriptorResult.getRelocations().isEmpty()) {
                    boolean disableVersionManagementSubsequently = originalArtifact.getGroupId()
                            .equals(d.getArtifact().getGroupId()) && originalArtifact.getArtifactId()
                                    .equals(d.getArtifact().getArtifactId());

                    processDependency(args, repositories, depSelector, depManager, depTraverser, verFilter, d, descriptorResult
                            .getRelocations(), disableVersionManagementSubsequently, node);
                    return;
                } else {
                    d = args.pool.intern(d.setArtifact(args.pool.intern(d.getArtifact())));

                    List<RemoteRepository> repos = getRemoteRepositories(rangeResult.getRepository(version), repositories);

                    DefaultDependencyNode child = createDependencyNode(relocations, preManaged, rangeResult, version, d, descriptorResult
                            .getAliases(), repos, args.request.getRequestContext());

                    synchronized (node) {
                        node.getChildren().add(child);
                    }

                    boolean recurse = traverse && !descriptorResult.getDependencies().isEmpty();
                    if (recurse) {
                        doRecurse(args, repositories, depSelector, depManager, depTraverser, verFilter, d, descriptorResult, child);
                    }
                }
            } else {
                List<RemoteRepository> repos = getRemoteRepositories(rangeResult.getRepository(version), repositories);
                DefaultDependencyNode child = createDependencyNode(relocations, preManaged, rangeResult, version, d, null, repos, args.request
                        .getRequestContext());
                node.getChildren().add(child);
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

    private static DefaultDependencyNode createDependencyNode(List<Artifact> relocations, PremanagedDependency preManaged, VersionRangeResult rangeResult, Version version, Dependency d, Collection<Artifact> aliases, List<RemoteRepository> repos, String requestContext) {
        DefaultDependencyNode child = new DefaultDependencyNode(d);
        preManaged.applyTo(child);
        child.setRelocations(relocations);
        child.setVersionConstraint(rangeResult.getVersionConstraint());
        child.setVersion(version);
        child.setAliases(aliases);
        child.setRepositories(repos);
        child.setRequestContext(requestContext);
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

    private static List<RemoteRepository> getRemoteRepositories(ArtifactRepository repository, List<RemoteRepository> repositories) {
        if (repository instanceof RemoteRepository) {
            return Collections.singletonList((RemoteRepository) repository);
        }
        if (repository != null) {
            return Collections.emptyList();
        }
        return repositories;
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

        final boolean premanagedState;

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
            this.premanagedState = ConfigUtils.getBoolean(session, false, DependencyManagerUtils.CONFIG_PROP_VERBOSE);
            this.trace = trace;
            this.pool = pool;
            this.collectionContext = collectionContext;
            this.versionContext = versionContext;
            this.fork = new ForkJoinPool(ConfigUtils
                    .getInteger(session, Runtime.getRuntime().availableProcessors() * 2, "maven.artifact.threads"));
        }

    }

    static class Results {

        private final CollectResult result;

        final int maxExceptions;

        final int maxCycles;

        Results(CollectResult result, RepositorySystemSession session) {
            this.result = result;

            maxExceptions = ConfigUtils.getInteger(session, CONFIG_PROP_MAX_EXCEPTIONS_DEFAULT, CONFIG_PROP_MAX_EXCEPTIONS);

            maxCycles = ConfigUtils.getInteger(session, CONFIG_PROP_MAX_CYCLES_DEFAULT, CONFIG_PROP_MAX_CYCLES);
        }

        public void addException(Dependency dependency, Exception e) {
            if (maxExceptions < 0 || result.getExceptions().size() < maxExceptions) {
                result.addException(e);
            }
        }

        public void addCycle(NodeStack nodes, int cycleEntry, Dependency dependency) {
            if (maxCycles < 0 || result.getCycles().size() < maxCycles) {
                result.addCycle(new DefaultDependencyCycle(nodes, cycleEntry, dependency));
            }
        }

    }

    static class PremanagedDependency {

        final String premanagedVersion;

        final String premanagedScope;

        final Boolean premanagedOptional;

        /**
         * @since 1.1.0
         */
        final Collection<Exclusion> premanagedExclusions;

        /**
         * @since 1.1.0
         */
        final Map<String, String> premanagedProperties;

        final int managedBits;

        final Dependency managedDependency;

        final boolean premanagedState;

        PremanagedDependency(String premanagedVersion, String premanagedScope, Boolean premanagedOptional, Collection<Exclusion> premanagedExclusions, Map<String, String> premanagedProperties, int managedBits, Dependency managedDependency, boolean premanagedState) {
            this.premanagedVersion = premanagedVersion;
            this.premanagedScope = premanagedScope;
            this.premanagedOptional = premanagedOptional;
            this.premanagedExclusions = premanagedExclusions != null
                    ? Collections.unmodifiableCollection(new ArrayList<>(premanagedExclusions))
                    : null;

            this.premanagedProperties = premanagedProperties != null ? Collections.unmodifiableMap(new HashMap<>(premanagedProperties))
                    : null;

            this.managedBits = managedBits;
            this.managedDependency = managedDependency;
            this.premanagedState = premanagedState;
        }

        static PremanagedDependency create(DependencyManager depManager, Dependency dependency, boolean disableVersionManagement, boolean premanagedState) {
            DependencyManagement depMngt = depManager != null ? depManager.manageDependency(dependency) : null;

            int managedBits = 0;
            String premanagedVersion = null;
            String premanagedScope = null;
            Boolean premanagedOptional = null;
            Collection<Exclusion> premanagedExclusions = null;
            Map<String, String> premanagedProperties = null;

            if (depMngt != null) {
                if (depMngt.getVersion() != null && !disableVersionManagement) {
                    Artifact artifact = dependency.getArtifact();
                    premanagedVersion = artifact.getVersion();
                    dependency = dependency.setArtifact(artifact.setVersion(depMngt.getVersion()));
                    managedBits |= DependencyNode.MANAGED_VERSION;
                }
                if (depMngt.getProperties() != null) {
                    Artifact artifact = dependency.getArtifact();
                    premanagedProperties = artifact.getProperties();
                    dependency = dependency.setArtifact(artifact.setProperties(depMngt.getProperties()));
                    managedBits |= DependencyNode.MANAGED_PROPERTIES;
                }
                if (depMngt.getScope() != null) {
                    premanagedScope = dependency.getScope();
                    dependency = dependency.setScope(depMngt.getScope());
                    managedBits |= DependencyNode.MANAGED_SCOPE;
                }
                if (depMngt.getOptional() != null) {
                    premanagedOptional = dependency.isOptional();
                    dependency = dependency.setOptional(depMngt.getOptional());
                    managedBits |= DependencyNode.MANAGED_OPTIONAL;
                }
                if (depMngt.getExclusions() != null) {
                    premanagedExclusions = dependency.getExclusions();
                    dependency = dependency.setExclusions(depMngt.getExclusions());
                    managedBits |= DependencyNode.MANAGED_EXCLUSIONS;
                }
            }
            return new PremanagedDependency(premanagedVersion, premanagedScope, premanagedOptional, premanagedExclusions, premanagedProperties, managedBits, dependency, premanagedState);

        }

        public void applyTo(DefaultDependencyNode child) {
            child.setManagedBits(managedBits);
            if (premanagedState) {
                child.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_VERSION, premanagedVersion);
                child.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_SCOPE, premanagedScope);
                child.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_OPTIONAL, premanagedOptional);
                child.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_EXCLUSIONS, premanagedExclusions);
                child.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_PROPERTIES, premanagedProperties);
            }
        }

    }

}
