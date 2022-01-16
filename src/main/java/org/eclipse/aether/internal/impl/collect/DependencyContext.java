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

import java.util.List;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.graph.Dependency;

public class DependencyContext implements DependencyCollectionContext {

    private final RepositorySystemSession session;

    private final Artifact artifact;

    private final Dependency dependency;

    private final List<Dependency> dependencies;

    /**
     * @param dependency
     * @param dependencies
     */
    DependencyContext(RepositorySystemSession session, Dependency dependency, List<Dependency> dependencies) {
        this.session = session;
        this.artifact = dependency.getArtifact();
        this.dependency = dependency;
        this.dependencies = dependencies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepositorySystemSession getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dependency getDependency() {
        return dependency;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Dependency> getManagedDependencies() {
        return dependencies;
    }
}
