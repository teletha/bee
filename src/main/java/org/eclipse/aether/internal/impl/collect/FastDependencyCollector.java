/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.eclipse.aether.internal.impl.collect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.eclipse.aether.collection.DependencyCollectionException;
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

@Singleton
@Named
public class FastDependencyCollector implements DependencyCollector {

    private static final String CONFIG_PROP_MAX_EXCEPTIONS = "aether.dependencyCollector.maxExceptions";

    private static final int CONFIG_PROP_MAX_EXCEPTIONS_DEFAULT = 50;

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
        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);

        CollectResult result = new CollectResult(request);

        DependencyTraverser traverser = session.getDependencyTraverser();
        VersionFilter verFilter = session.getVersionFilter();

        Dependency root = request.getRoot();
        List<RemoteRepository> repositories = request.getRepositories();
        List<Dependency> dependencies = request.getDependencies();
        List<Dependency> managedDependencies = request.getManagedDependencies();

        DefaultDependencyNode node = new DefaultDependencyNode(request.getRootArtifact());
        node.setRequestContext(request.getRequestContext());
        node.setRepositories(request.getRepositories());

        result.setRoot(node);

        boolean traversable = root == null || traverser == null || traverser.traverseDependency(root);
        if (traversable && !dependencies.isEmpty()) {
            Deque<DependencyNode> nodes = new ArrayDeque();
            nodes.addLast(node);

            DefaultDependencyCollectionContext context = new DefaultDependencyCollectionContext(session, request
                    .getRootArtifact(), root, managedDependencies);

            DefaultVersionFilterContext versionContext = new DefaultVersionFilterContext(session);

            Args args = new Args(session, trace, nodes, context, versionContext, request, new ForkJoinPool(ConfigUtils
                    .getInteger(session, Runtime.getRuntime().availableProcessors() * 2, "maven.artifact.threads")), ConcurrentHashMap
                            .newKeySet());
            Results results = new Results(result, session);

            process(args, results, dependencies, repositories, session.getDependencySelector().deriveChildSelector(context), session
                    .getDependencyManager()
                    .deriveChildManager(context), session.getDependencyTraverser()
                            .deriveChildTraverser(context), verFilter != null ? verFilter.deriveChildFilter(context) : null);

            args.fork.awaitQuiescence(60, TimeUnit.SECONDS);
        }

        try {
            DefaultDependencyGraphTransformationContext context = new DefaultDependencyGraphTransformationContext(session);
            result.setRoot(session.getDependencyGraphTransformer().transformGraph(node, context));
        } catch (RepositoryException e) {
            result.addException(e);
        }

        if (!result.getExceptions().isEmpty()) {
            throw new DependencyCollectionException(result);
        }

        return result;
    }

    private void process(final Args args, Results results, List<Dependency> dependencies, List<RemoteRepository> repositories, DependencySelector depSelector, DependencyManager depManager, DependencyTraverser depTraverser, VersionFilter verFilter) {
        args.fork.submit(() -> {
            for (Dependency dependency : dependencies) {
                if (!depSelector.selectDependency(dependency) || !args.deps.add(dependency)) {
                    continue;
                }

                process(args.fork(), results, repositories, depSelector, depManager, depTraverser, verFilter, dependency, List.of(), false);
            }
        });
    }

    private void process(Args args, Results results, List<RemoteRepository> repositories, DependencySelector depSelector, DependencyManager depManager, DependencyTraverser depTraverser, VersionFilter verFilter, Dependency dependency, List<Artifact> relocations, boolean disableVersionManagement) {
        PremanagedDependency preManaged = PremanagedDependency.create(depManager, dependency, disableVersionManagement, ConfigUtils
                .getBoolean(args.session, false, DependencyManagerUtils.CONFIG_PROP_VERBOSE));
        dependency = preManaged.managedDependency;

        boolean noDescriptor = isLackingDescriptor(dependency.getArtifact());
        boolean traverse = !noDescriptor && (depTraverser == null || depTraverser.traverseDependency(dependency));

        try {
            VersionRangeRequest rangeRequest = createVersionRangeRequest(args, repositories, dependency);
            VersionRangeResult rangeResult = versionRangeResolver.resolveVersionRange(args.session, rangeRequest);

            for (Version version : rangeResult.getVersions()) {
                Artifact originalArtifact = dependency.getArtifact().setVersion(version.toString());
                Dependency dep = dependency.setArtifact(originalArtifact);

                ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
                descriptorRequest.setArtifact(dep.getArtifact());
                descriptorRequest.setRepositories(repositories);
                descriptorRequest.setRequestContext(args.request.getRequestContext());
                descriptorRequest.setTrace(args.trace);

                ArtifactDescriptorResult descriptorResult = noDescriptor ? new ArtifactDescriptorResult(descriptorRequest)
                        : descriptorReader.readArtifactDescriptor(args.session, descriptorRequest);

                dep = dep.setArtifact(descriptorResult.getArtifact());

                DependencyNode node = args.nodes.peekLast();

                DefaultDependencyNode child = new DefaultDependencyNode(dep);
                preManaged.applyTo(child);
                child.setRelocations(relocations);
                child.setVersionConstraint(rangeResult.getVersionConstraint());
                child.setVersion(version);
                child.setAliases(descriptorResult.getAliases());
                child.setRepositories(getRemoteRepositories(rangeResult.getRepository(version), repositories));
                child.setRequestContext(args.request.getRequestContext());

                node.getChildren().add(child);

                if (traverse && !descriptorResult.getDependencies().isEmpty()) {
                    doRecurse(args, results, repositories, depSelector, depManager, depTraverser, verFilter, dep, descriptorResult, child);
                }
            }
        } catch (VersionRangeResolutionException | ArtifactDescriptorException e) {
            results.addException(dependency, e, args.nodes);
            return;
        }
    }

    private void doRecurse(Args args, Results results, List<RemoteRepository> repositories, DependencySelector depSelector, DependencyManager depManager, DependencyTraverser depTraverser, VersionFilter verFilter, Dependency d, ArtifactDescriptorResult descriptorResult, DefaultDependencyNode child) {
        DefaultDependencyCollectionContext context = args.collectionContext;
        context.set(d, descriptorResult.getManagedDependencies());

        DependencySelector childSelector = depSelector != null ? depSelector.deriveChildSelector(context) : null;
        DependencyManager childManager = depManager != null ? depManager.deriveChildManager(context) : null;
        DependencyTraverser childTraverser = depTraverser != null ? depTraverser.deriveChildTraverser(context) : null;
        VersionFilter childFilter = verFilter != null ? verFilter.deriveChildFilter(context) : null;

        List<RemoteRepository> childRepos = args.session.isIgnoreArtifactDescriptorRepositories() ? repositories
                : remoteRepositoryManager.aggregateRepositories(args.session, repositories, descriptorResult.getRepositories(), true);

        args.nodes.addLast(child);

        process(args, results, descriptorResult.getDependencies(), childRepos, childSelector, childManager, childTraverser, childFilter);
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

    /**
     * 
     */
    private static class Args {

        final RepositorySystemSession session;

        final RequestTrace trace;

        final Deque<DependencyNode> nodes;

        final DefaultDependencyCollectionContext collectionContext;

        final DefaultVersionFilterContext versionContext;

        final CollectRequest request;

        final ForkJoinPool fork;

        final Set<Dependency> deps;

        Args(RepositorySystemSession session, RequestTrace trace, Deque<DependencyNode> nodes, DefaultDependencyCollectionContext collectionContext, DefaultVersionFilterContext versionContext, CollectRequest request, ForkJoinPool fork, Set<Dependency> deps) {
            this.session = session;
            this.request = request;
            this.trace = trace;
            this.nodes = nodes;
            this.collectionContext = collectionContext;
            this.versionContext = versionContext;
            this.fork = fork;
            this.deps = deps;
        }

        Args fork() {
            return new Args(session, trace, new ArrayDeque(nodes), collectionContext, versionContext, request, fork, deps);
        }
    }

    /**
     * 
     */
    private static class Results {

        private final CollectResult result;

        final int maxExceptions;

        Results(CollectResult result, RepositorySystemSession session) {
            this.result = result;

            maxExceptions = ConfigUtils.getInteger(session, CONFIG_PROP_MAX_EXCEPTIONS_DEFAULT, CONFIG_PROP_MAX_EXCEPTIONS);
        }

        public void addException(Dependency dependency, Exception e, Deque<DependencyNode> nodes) {
            if (maxExceptions < 0 || result.getExceptions().size() < maxExceptions) {
                result.addException(e);
            }
        }
    }

    /**
     * 
     */
    private static class PremanagedDependency {

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
