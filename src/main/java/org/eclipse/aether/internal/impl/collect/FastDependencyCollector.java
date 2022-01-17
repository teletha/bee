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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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

import kiss.I;

@Singleton
@Named
public class FastDependencyCollector implements DependencyCollector {

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
        long start = System.nanoTime();
        Dependency root = request.getRoot();
        List<RemoteRepository> repositories = request.getRepositories();
        List<Dependency> dependencies = request.getDependencies();

        DefaultDependencyNode node = new DefaultDependencyNode(request.getRootArtifact());
        node.setRequestContext(request.getRequestContext());
        node.setRepositories(request.getRepositories());

        boolean traversable = root == null || session.getDependencyTraverser().traverseDependency(root);
        if (traversable && !dependencies.isEmpty()) {
            ForkJoinPool pool = new ForkJoinPool(ConfigUtils
                    .getInteger(session, Runtime.getRuntime().availableProcessors() * 2, "maven.artifact.threads"));
            Set<Dependency> deps = ConcurrentHashMap.newKeySet();

            Args args = new Args(session, request, pool, deps);

            DefaultDependencyCollectionContext context = new DefaultDependencyCollectionContext(session, request
                    .getRootArtifact(), root, request.getManagedDependencies());

            process(args, dependencies, repositories, session.getDependencySelector().deriveChildSelector(context), node);

            args.fork.awaitQuiescence(60, TimeUnit.SECONDS);
        }

        long end = System.nanoTime();
        Set<DependencyNode> list = I.signal(node.getChildren())
                .recurseMap(x -> x.flatIterable(v -> v.getChildren()))
                .skipNull()
                .toCollection(new TreeSet<DependencyNode>(Comparator.comparing(x -> x.toString())));
        System.out.println((end - start) + "ns   " + list.size() + "   " + list);

        CollectResult result = new CollectResult(request).setRoot(node);

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

    private void process(Args args, List<Dependency> dependencies, List<RemoteRepository> repositories, DependencySelector selector, DependencyNode node) {
        // args.fork.submit(() -> {
        for (Dependency dependency : dependencies) {
            // must check at first
            if (!args.deps.add(dependency)) {
                continue;
            }

            // second check
            if (!selector.selectDependency(dependency)) {
                continue;
            }
            args.fork.submit(() -> {
                process(args, dependency, repositories, selector, node);
            });
        }
        // });
    }

    private void process(Args args, Dependency dependency, List<RemoteRepository> repositories, DependencySelector selector, DependencyNode node) {
        boolean noDescriptor = isLackingDescriptor(dependency.getArtifact());

        try {
            VersionRangeRequest rangeRequest = new VersionRangeRequest();
            rangeRequest.setArtifact(dependency.getArtifact());
            rangeRequest.setRepositories(repositories);
            rangeRequest.setRequestContext(args.request.getRequestContext());
            rangeRequest.setTrace(args.trace);

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

                DefaultDependencyNode child = new DefaultDependencyNode(dep);
                child.setVersionConstraint(rangeResult.getVersionConstraint());
                child.setVersion(version);
                child.setAliases(descriptorResult.getAliases());
                child.setRepositories(getRemoteRepositories(rangeResult.getRepository(version), repositories));
                child.setRequestContext(args.request.getRequestContext());

                synchronized (node) {
                    node.getChildren().add(child);
                }

                DefaultDependencyCollectionContext context = new DefaultDependencyCollectionContext(args.session, dep
                        .getArtifact(), dep, descriptorResult.getManagedDependencies());
                List<RemoteRepository> childRepos = args.session.isIgnoreArtifactDescriptorRepositories() ? repositories
                        : remoteRepositoryManager
                                .aggregateRepositories(args.session, repositories, descriptorResult.getRepositories(), true);

                process(args, descriptorResult.getDependencies(), childRepos, selector.deriveChildSelector(context), child);
            }
        } catch (VersionRangeResolutionException | ArtifactDescriptorException e) {
            throw I.quiet(e);
        }
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

        final CollectRequest request;

        final ForkJoinPool fork;

        final Set<Dependency> deps;

        Args(RepositorySystemSession session, CollectRequest request, ForkJoinPool fork, Set<Dependency> deps) {
            this.session = session;
            this.request = request;
            this.trace = RequestTrace.newChild(request.getTrace(), request);
            this.fork = fork;
            this.deps = deps;
        }
    }
}
