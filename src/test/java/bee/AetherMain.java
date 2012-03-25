/*
 * Copyright (C) 2012 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee;

import java.util.List;

import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.DependencyFilterUtils;

import demo.util.Booter;

/**
 * @version 2012/03/25 0:45:52
 */
public class AetherMain {

    public static void main(String[] args) throws Exception {
        System.out.println("------------------------------------------------------------");
        System.out.println(AetherMain.class.getSimpleName());

        RepositorySystem system = Booter.newRepositorySystem();

        RepositorySystemSession session = Booter.newRepositorySystemSession(system);

        Artifact artifact = new DefaultArtifact("org.sonatype.aether:aether-impl:1.9");

        RemoteRepository repo = Booter.newCentralRepository();

        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
        collectRequest.addRepository(repo);

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);

        List<ArtifactResult> artifactResults = system.resolveDependencies(session, dependencyRequest)

        .getArtifactResults();

        for (ArtifactResult artifactResult : artifactResults) {
            System.out.println(artifactResult.getArtifact() + " resolved to " + artifactResult.getArtifact().getFile());
        }
    }
}
