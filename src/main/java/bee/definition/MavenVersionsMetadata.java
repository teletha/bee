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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;

/**
 * @version 2012/04/10 14:05:53
 */
class MavenVersionsMetadata extends MavenMetadata {

    private final Artifact artifact;

    /**
     * @param artifact
     */
    public MavenVersionsMetadata(Artifact artifact) {
        super(createMetadata(artifact), null);
        this.artifact = artifact;
    }

    /**
     * @param artifact
     * @param file
     */
    public MavenVersionsMetadata(Artifact artifact, File file) {
        super(createMetadata(artifact), file);
        this.artifact = artifact;
    }

    /**
     * @param artifact
     * @return
     */
    private static Metadata createMetadata(Artifact artifact) {
        Versioning versioning = new Versioning();
        versioning.addVersion(artifact.getBaseVersion());
        if (!artifact.isSnapshot()) {
            versioning.setRelease(artifact.getBaseVersion());
        }
        if ("maven-plugin".equals(artifact.getProperty(ArtifactProperties.TYPE, ""))) {
            versioning.setLatest(artifact.getBaseVersion());
        }

        Metadata metadata = new Metadata();
        metadata.setVersioning(versioning);
        metadata.setGroupId(artifact.getGroupId());
        metadata.setArtifactId(artifact.getArtifactId());

        return metadata;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void merge(Metadata recessive) {
        Versioning versioning = metadata.getVersioning();
        versioning.updateTimestamp();

        if (recessive.getVersioning() != null) {
            if (versioning.getLatest() == null) {
                versioning.setLatest(recessive.getVersioning().getLatest());
            }
            if (versioning.getRelease() == null) {
                versioning.setRelease(recessive.getVersioning().getRelease());
            }

            Collection<String> versions = new LinkedHashSet<String>(recessive.getVersioning().getVersions());
            versions.addAll(versioning.getVersions());
            versioning.setVersions(new ArrayList<String>(versions));
        }
    }

    /**
     * @return
     */
    public Object getKey() {
        return getGroupId() + ':' + getArtifactId();
    }

    /**
     * @param artifact
     * @return
     */
    public static Object getKey(Artifact artifact) {
        return artifact.getGroupId() + ':' + artifact.getArtifactId();
    }

    /**
     * {@inheritDoc}
     */
    public MavenMetadata setFile(File file) {
        return new MavenVersionsMetadata(artifact, file);
    }

    /**
     * {@inheritDoc}
     */
    public String getGroupId() {
        return artifact.getGroupId();
    }

    /**
     * {@inheritDoc}
     */
    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    /**
     * {@inheritDoc}
     */
    public String getVersion() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public Nature getNature() {
        return artifact.isSnapshot() ? Nature.RELEASE_OR_SNAPSHOT : Nature.RELEASE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProperty(String key, String defaultValue) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getProperties() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.eclipse.aether.metadata.Metadata setProperties(Map<String, String> properties) {
        return null;
    }
}
