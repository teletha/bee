/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package bee.api;

import java.nio.file.Path;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import kiss.I;

/**
 * @version 2017/01/08 21:30:27
 */
public class Library implements Comparable<Library> {

    /** The group name. */
    public final String group;

    /** The artifact name. */
    public final String name;

    /** The artifact classfier. */
    public final String classfier;

    /** The version identifier. */
    public final String version;

    /** The scope. */
    public Scope scope = Scope.Compile;

    /** The actual artifact. */
    final Artifact artifact;

    /**
     * @param name
     * @param group
     * @param version
     */
    Library(String qualified) {
        this(new DefaultArtifact(qualified));
    }

    /**
     * @param group
     * @param artifact
     * @param version
     * @param classifier
     */
    Library(String group, String artifact, String version) {
        this(group, artifact, "", version);
    }

    /**
     * @param group
     * @param artifact
     * @param classifier
     * @param version
     */
    Library(String group, String artifact, String classifier, String version) {
        this(new DefaultArtifact(group, artifact, classifier, "jar", version));
    }

    /**
     * 
     */
    Library(Artifact artifact) {
        this.artifact = artifact;
        this.group = artifact.getGroupId();
        this.name = artifact.getArtifactId();
        this.classfier = artifact.getClassifier();
        this.version = artifact.isSnapshot() ? artifact.getBaseVersion() : artifact.getVersion();
    }

    /**
     * <p>
     * This library is needed at compile phase.
     * </p>
     * 
     * @return
     */
    public Library atCompile() {
        scope = Scope.Compile;
        return this;
    }

    /**
     * <p>
     * This library is needed at compile phase.
     * </p>
     * 
     * @return
     */
    public Library atTest() {
        scope = Scope.Test;
        return this;
    }

    /**
     * <p>
     * This library is needed at compile phase.
     * </p>
     * 
     * @return
     */
    public Library atProvided() {
        scope = Scope.Provided;
        return this;
    }

    /**
     * <p>
     * This library is needed at compile phase.
     * </p>
     * 
     * @return
     */
    public Library atSystem() {
        scope = Scope.System;
        return this;
    }

    /**
     * <p>
     * This library is needed at compile phase.
     * </p>
     * 
     * @return
     */
    public Library atExtension() {
        return this;
    }

    /**
     * <p>
     * Get jar path.
     * </p>
     * 
     * @return
     */
    public String getJar() {
        return localPath(".jar");
    }

    /**
     * <p>
     * Get jar file.
     * </p>
     * 
     * @return
     */
    public Path getLocalJar() {
        return I.make(Repository.class).getLocalRepository().resolve(getJar());
    }

    /**
     * <p>
     * Get source jar path.
     * </p>
     * 
     * @return
     */
    public String getSourceJar() {
        return localPath("-sources.jar");
    }

    /**
     * <p>
     * Get source jar file.
     * </p>
     * 
     * @return
     */
    public Path getLocalSourceJar() {
        Path path = I.make(Repository.class).resolveSource(this);

        if (path == null) {
            return I.make(Repository.class).getLocalRepository().resolve(getSourceJar());
        } else {
            return path;
        }
    }

    /**
     * <p>
     * Get javadoc jar path.
     * </p>
     * 
     * @return
     */
    public String getJavadocJar() {
        return localPath("-javadoc.jar");
    }

    /**
     * <p>
     * Get javadoc jar file.
     * </p>
     * 
     * @return
     */
    public Path getLocalJavadocJar() {
        Path path = I.make(Repository.class).resolveJavadoc(this);

        if (path == null) {
            return I.make(Repository.class).getLocalRepository().resolve(getJavadocJar());
        } else {
            return path;
        }
    }

    /**
     * <p>
     * Get pom path.
     * </p>
     * 
     * @return
     */
    public String getPOM() {
        return localPath(".pom");
    }

    /**
     * <p>
     * Get jar file.
     * </p>
     * 
     * @return
     */
    public Path getLocalPOM() {
        return I.make(Repository.class).getLocalRepository().resolve(getPOM());
    }

    /**
     * <p>
     * Compute local relative path.
     * </p>
     * 
     * @param suffix
     * @return
     */
    String localPath(String suffix) {
        StringBuilder builder = new StringBuilder();
        builder.append(group.replace('.', '/')).append('/');
        builder.append(name).append('/');
        builder.append(version).append('/');
        builder.append(name).append('-').append(version);
        if (classfier.length() != 0) builder.append('-').append(classfier);
        builder.append(suffix);

        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Library other = (Library) obj;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (group == null) {
            if (other.group != null) return false;
        } else if (!group.equals(other.group)) return false;
        if (version == null) {
            if (other.version != null) return false;
        } else if (!version.equals(other.version)) return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Library o) {
        return toString().compareTo(o.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append('-').append(version);
        if (classfier.length() != 0) {
            builder.append('-').append(classfier);
        }
        return builder.toString();
    }

    /**
     * <p>
     * Helper method to check whether this {@link Library} is Java's tools or not.
     * </p>
     * 
     * @return A result.
     */
    final boolean isJavaTools() {
        if (!group.equals("sun.jdk") && !group.equals("com.sun") && !group.equals("jdk.tools") && !group.equals("jdk")) {
            return false;
        }

        if (!name.equals("tools") && !name.equals("jdk.tools")) {
            return false;
        }
        return true;
    }
}
